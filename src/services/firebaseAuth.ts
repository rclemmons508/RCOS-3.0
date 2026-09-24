import { initializeApp, getApps, getApp } from 'firebase/app';
import {
  getAuth,
  signInWithPopup,
  GoogleAuthProvider,
  onAuthStateChanged,
  signOut as firebaseSignOut,
  User
} from 'firebase/auth';
import { initializeFirestore, getFirestore, doc, getDocFromServer } from 'firebase/firestore';
import firebaseConfig from '../../firebase-applet-config.json';

const app = !getApps().length ? initializeApp(firebaseConfig) : getApp();

function initFirestoreInstance() {
  const databaseId = (firebaseConfig as any).firestoreDatabaseId;
  try {
    return databaseId
      ? initializeFirestore(app, { experimentalForceLongPolling: true }, databaseId)
      : initializeFirestore(app, { experimentalForceLongPolling: true });
  } catch {
    return databaseId
      ? getFirestore(app, databaseId)
      : getFirestore(app);
  }
}

export const db = initFirestoreInstance();
export const auth = getAuth(app);

// Test Firestore connection on boot as required by Firebase skill
async function testConnection() {
  try {
    await getDocFromServer(doc(db, 'test', 'connection'));
  } catch (error) {
    if (error instanceof Error && error.message.includes('the client is offline')) {
      console.warn('Firestore offline notice: client is operating in cached mode until connected.');
    }
  }
}
testConnection();

export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
}

export interface FirestoreErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: {
    userId?: string | null;
    email?: string | null;
    emailVerified?: boolean | null;
    isAnonymous?: boolean | null;
    tenantId?: string | null;
    providerInfo?: {
      providerId?: string | null;
      email?: string | null;
    }[];
  };
}

export function handleFirestoreError(error: unknown, operationType: OperationType, path: string | null) {
  const errInfo: FirestoreErrorInfo = {
    error: error instanceof Error ? error.message : String(error),
    authInfo: {
      userId: auth.currentUser?.uid,
      email: auth.currentUser?.email,
      emailVerified: auth.currentUser?.emailVerified,
      isAnonymous: auth.currentUser?.isAnonymous,
      tenantId: auth.currentUser?.tenantId,
      providerInfo: auth.currentUser?.providerData?.map(provider => ({
        providerId: provider.providerId,
        email: provider.email,
      })) || []
    },
    operationType,
    path
  };
  console.error('Firestore Error: ', JSON.stringify(errInfo));
  throw new Error(JSON.stringify(errInfo));
}

export const WORKSPACE_SCOPES = [
  // Drive — covers listing, reading, and the Google Picker (drive.file +
  // drive.readonly are what Picker and Drive list/search actually require).
  'https://www.googleapis.com/auth/drive.readonly',
  'https://www.googleapis.com/auth/drive.file',

  // Calendar — read-only events listing.
  'https://www.googleapis.com/auth/calendar.readonly',

  // Gmail — read, triage (labels), compose drafts, and send.
  'https://www.googleapis.com/auth/gmail.readonly',
  'https://www.googleapis.com/auth/gmail.modify',
  'https://www.googleapis.com/auth/gmail.compose',
  'https://www.googleapis.com/auth/gmail.send'
];

export const googleProvider = new GoogleAuthProvider();
WORKSPACE_SCOPES.forEach(scope => googleProvider.addScope(scope));
// Set custom parameters to prompt consent and select account
googleProvider.setCustomParameters({
  prompt: 'consent'
});

let cachedAccessToken: string | null = null;
let cachedAccessTokenExpiresAt: number | null = null;
let isSigningIn = false;

// Treat a token as expired 60s early so a request never launches with a
// credential that lapses mid-flight.
const TOKEN_EXPIRY_SKEW_MS = 60 * 1000;

// Default Google OAuth access-token lifetime when the provider does not
// report one. Google issues 1-hour tokens.
const DEFAULT_TOKEN_TTL_MS = 60 * 60 * 1000;

function storeAccessToken(token: string, expiresInSeconds?: number | null) {
  cachedAccessToken = token;
  const ttlMs =
    typeof expiresInSeconds === 'number' && expiresInSeconds > 0
      ? expiresInSeconds * 1000
      : DEFAULT_TOKEN_TTL_MS;
  cachedAccessTokenExpiresAt = Date.now() + ttlMs;
}

function clearAccessToken() {
  cachedAccessToken = null;
  cachedAccessTokenExpiresAt = null;
}

export const initAuthListener = (
  onAuthSuccess?: (user: User, token: string) => void,
  onAuthFailure?: () => void
) => {
  return onAuthStateChanged(auth, async (user: User | null) => {
    if (user) {
      if (cachedAccessToken && !isAccessTokenExpired()) {
        if (onAuthSuccess) onAuthSuccess(user, cachedAccessToken);
      } else if (!isSigningIn) {
        clearAccessToken();
        if (onAuthFailure) onAuthFailure();
      }
    } else {
      clearAccessToken();
      if (onAuthFailure) onAuthFailure();
    }
  });
};

export const signInWithGoogleWorkspace = async (): Promise<{ user: User; accessToken: string } | null> => {
  try {
    isSigningIn = true;
    const result = await signInWithPopup(auth, googleProvider);
    const credential = GoogleAuthProvider.credentialFromResult(result);
    if (!credential?.accessToken) {
      throw new Error('Failed to obtain Google access token from authentication credential.');
    }
    storeAccessToken(credential.accessToken);
    return { user: result.user, accessToken: credential.accessToken };
  } catch (error: any) {
    // Normal user cancellation or popup closed before completing OAuth
    if (
      error?.code === 'auth/popup-closed-by-user' ||
      error?.code === 'auth/cancelled-popup-request' ||
      error?.message?.includes('auth/popup-closed-by-user')
    ) {
      console.info('Google sign-in popup was closed by user.');
      return null;
    }
    console.error('Firebase Google Sign-In error:', error);
    throw error;
  } finally {
    isSigningIn = false;
  }
};

/**
 * Publishes an access token obtained outside the Firebase popup flow (for
 * example the Google Identity Services token client or a manually supplied
 * token) into the SHARED session cache.
 *
 * Every Workspace surface — Gmail, Drive, Calendar, the Picker — reads this
 * one cache, so a token acquired in any one of them makes the others work
 * without a second consent prompt. Call this at every point a token arrives.
 */
export const setManualAccessToken = (token: string, expiresInSeconds?: number | null) => {
  storeAccessToken(token, expiresInSeconds);
};

/** True when the cached token is missing or past its usable lifetime. */
export const isAccessTokenExpired = (): boolean => {
  if (!cachedAccessToken) return true;
  if (cachedAccessTokenExpiresAt === null) return false;
  return Date.now() >= cachedAccessTokenExpiresAt - TOKEN_EXPIRY_SKEW_MS;
};

/** The shared token if it is still valid, otherwise null. */
export const getCachedAccessToken = (): string | null => {
  if (isAccessTokenExpired()) {
    clearAccessToken();
    return null;
  }
  return cachedAccessToken;
};

export const getAccessTokenExpiresAt = (): number | null => cachedAccessTokenExpiresAt;

export const logoutGoogleWorkspace = async () => {
  await firebaseSignOut(auth);
  clearAccessToken();
};
