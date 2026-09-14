// Google Workspace API Integration Service
// Scopes enabled:
// - https://www.googleapis.com/auth/drive.readonly
// - https://www.googleapis.com/auth/calendar.readonly
// - https://www.googleapis.com/auth/gmail.readonly

export interface GoogleDriveFile {
  id: string;
  name: string;
  mimeType: string;
  modifiedTime: string;
  size?: string;
  webViewLink?: string;
  iconLink?: string;
  owners?: { displayName: string; emailAddress: string }[];
}

export interface GoogleCalendarEvent {
  id: string;
  summary: string;
  description?: string;
  start: { dateTime?: string; date?: string };
  end: { dateTime?: string; date?: string };
  location?: string;
  htmlLink?: string;
  attendees?: { email: string; displayName?: string; responseStatus?: string }[];
  organizer?: { email: string; displayName?: string };
  status?: string;
}

export interface GmailMessageSnippet {
  id: string;
  threadId: string;
  snippet: string;
  internalDate: string;
  subject: string;
  from: string;
  to: string;
  date: string;
  unread: boolean;
  labels: string[];
}

export interface WorkspaceSyncStats {
  driveCount: number;
  calendarCount: number;
  gmailCount: number;
  lastSyncTime: string;
}

export interface GooglePickerDocument {
  id: string;
  name: string;
  mimeType: string;
  url: string;
  description?: string;
  sizeBytes?: number;
  lastEditedUtc?: number;
  iconUrl?: string;
}

import { WORKSPACE_SCOPES } from './firebaseAuth';
export const SCOPES = WORKSPACE_SCOPES.join(' ');

export {
  signInWithGoogleWorkspace,
  initAuthListener,
  logoutGoogleWorkspace,
  setManualAccessToken,
  getCachedAccessToken
} from './firebaseAuth';

// Google Client ID configuration
export const DEFAULT_GOOGLE_CLIENT_ID = '160087856875-lv292ie72a2jgg69udq00tldcg97hib0.apps.googleusercontent.com';

let userConfiguredClientId: string | null = null;

export function setCustomGoogleClientId(id: string) {
  userConfiguredClientId = id.trim() || null;
}

export function getGoogleClientId(): string {
  return userConfiguredClientId || import.meta.env.VITE_GOOGLE_CLIENT_ID || DEFAULT_GOOGLE_CLIENT_ID;
}

let tokenClient: any = null;

// Initialize the Google Identity Services Token Client
export function initWorkspaceTokenClient(
  onTokenReceived: (token: string) => void,
  onError?: (err: any) => void
): boolean {
  const clientId = getGoogleClientId();
  if (!clientId) {
    console.warn('Google Client ID not found');
    return false;
  }

  if (typeof window === 'undefined' || !(window as any).google?.accounts?.oauth2) {
    console.warn('Google Identity Services library not yet loaded');
    return false;
  }

  try {
    tokenClient = (window as any).google.accounts.oauth2.initTokenClient({
      client_id: clientId,
      scope: SCOPES,
      callback: (tokenResponse: any) => {
        if (tokenResponse && tokenResponse.access_token) {
          onTokenReceived(tokenResponse.access_token);
        } else if (tokenResponse && tokenResponse.error) {
          console.error('OAuth token error:', tokenResponse.error);
          if (onError) onError(tokenResponse.error);
        }
      },
      error_callback: (nonOAuthError: any) => {
        console.error('OAuth initialization/runtime error:', nonOAuthError);
        if (onError) onError(nonOAuthError);
      }
    });
    return true;
  } catch (err) {
    console.error('Failed to initialize Google token client:', err);
    if (onError) onError(err);
    return false;
  }
}

// Request OAuth access token via popup
export function requestWorkspaceAuth() {
  if (tokenClient) {
    tokenClient.requestAccessToken({ prompt: 'consent' });
  } else {
    throw new Error('Google OAuth client not initialized. Check your client ID and network connection.');
  }
}

// Fetch files from Google Drive API v3
export async function fetchDriveFiles(accessToken: string): Promise<GoogleDriveFile[]> {
  const fields = 'files(id, name, mimeType, modifiedTime, size, webViewLink, iconLink, owners)';
  const url = `https://www.googleapis.com/drive/v3/files?pageSize=20&fields=${encodeURIComponent(fields)}&orderBy=modifiedTime desc`;
  
  const response = await fetch(url, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      Accept: 'application/json'
    }
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Google Drive API error: ${response.status} - ${errText}`);
  }

  const data = await response.json();
  return data.files || [];
}

// Fetch calendar events from Google Calendar API v3
export async function fetchCalendarEvents(accessToken: string): Promise<GoogleCalendarEvent[]> {
  const now = new Date();
  const timeMin = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString(); // 7 days ago
  const timeMax = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString(); // 30 days ahead

  const url = `https://www.googleapis.com/calendar/v3/calendars/primary/events?timeMin=${encodeURIComponent(timeMin)}&timeMax=${encodeURIComponent(timeMax)}&maxResults=25&singleEvents=true&orderBy=startTime`;

  const response = await fetch(url, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      Accept: 'application/json'
    }
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Google Calendar API error: ${response.status} - ${errText}`);
  }

  const data = await response.json();
  return data.items || [];
}

// Fetch email messages and headers from Gmail API v1
export async function fetchGmailMessages(accessToken: string): Promise<GmailMessageSnippet[]> {
  const listUrl = `https://www.googleapis.com/gmail/v1/users/me/messages?maxResults=15&q=label:INBOX`;
  
  const listResponse = await fetch(listUrl, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      Accept: 'application/json'
    }
  });

  if (!listResponse.ok) {
    const errText = await listResponse.text();
    throw new Error(`Gmail API error: ${listResponse.status} - ${errText}`);
  }

  const listData = await listResponse.json();
  const rawMessages: { id: string; threadId: string }[] = listData.messages || [];

  // Fetch message details in parallel
  const messagePromises = rawMessages.slice(0, 12).map(async (msg) => {
    try {
      const msgUrl = `https://www.googleapis.com/gmail/v1/users/me/messages/${msg.id}?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=To&metadataHeaders=Date`;
      const res = await fetch(msgUrl, {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Accept: 'application/json'
        }
      });
      if (!res.ok) return null;
      const data = await res.json();
      
      const headers = data.payload?.headers || [];
      const getHeader = (name: string) => headers.find((h: any) => h.name.toLowerCase() === name.toLowerCase())?.value || '';

      return {
        id: data.id,
        threadId: data.threadId,
        snippet: data.snippet || '',
        internalDate: data.internalDate,
        subject: getHeader('Subject') || '(No Subject)',
        from: getHeader('From') || 'Unknown Sender',
        to: getHeader('To') || 'Me',
        date: getHeader('Date') || '',
        unread: (data.labelIds || []).includes('UNREAD'),
        labels: data.labelIds || []
      } as GmailMessageSnippet;
    } catch {
      return null;
    }
  });

  const resolved = await Promise.all(messagePromises);
  return resolved.filter((m): m is GmailMessageSnippet => m !== null);
}

// Google Picker API Loader
let pickerApiLoadedPromise: Promise<void> | null = null;

export function loadGooglePickerApi(): Promise<void> {
  if (typeof window === 'undefined') return Promise.reject(new Error('Window object is not available'));
  if ((window as any).google?.picker) return Promise.resolve();
  if (pickerApiLoadedPromise) return pickerApiLoadedPromise;

  pickerApiLoadedPromise = new Promise((resolve, reject) => {
    let retries = 0;
    const maxRetries = 40;

    const tryLoad = () => {
      const gapi = (window as any).gapi;
      if (gapi && typeof gapi.load === 'function') {
        gapi.load('picker', {
          callback: () => resolve(),
          onerror: () => {
            pickerApiLoadedPromise = null;
            reject(new Error('Google Picker library failed to load via gapi.load'));
          }
        });
      } else {
        retries++;
        if (retries > maxRetries) {
          pickerApiLoadedPromise = null;
          reject(new Error('Timed out waiting for Google API Client script (gapi)'));
        } else {
          setTimeout(tryLoad, 150);
        }
      }
    };

    tryLoad();
  });

  return pickerApiLoadedPromise;
}

export interface OpenPickerOptions {
  accessToken: string;
  onPicked: (docs: GooglePickerDocument[]) => void;
  onCancel?: () => void;
  title?: string;
  multiselect?: boolean;
}

/**
 * Opens the native Google Picker UI dialog to pick Google Drive documents and folders.
 * Follows the prescribed client-side pattern using PickerBuilder and origin derivation.
 */
export async function openGooglePicker(options: OpenPickerOptions): Promise<void> {
  await loadGooglePickerApi();

  const google = (window as any).google;
  if (!google || !google.picker) {
    throw new Error('Google Picker is not initialized');
  }

  // Determine origin according to documentation guidelines
  const pickerOrigin =
    window.location.ancestorOrigins &&
    window.location.ancestorOrigins.length > 0
      ? window.location.ancestorOrigins[window.location.ancestorOrigins.length - 1]
      : window.location.origin;

  // Configure Docs View
  const docsView = new google.picker.DocsView(google.picker.ViewId.DOCS)
    .setIncludeFolders(true)
    .setSelectFolderEnabled(false);

  // Docs Upload View allows uploading new files to Drive directly in Picker
  const uploadView = new google.picker.DocsUploadView();

  const builder = new google.picker.PickerBuilder()
    .addView(docsView)
    .addView(uploadView)
    .setOAuthToken(options.accessToken)
    .setOrigin(pickerOrigin)
    .setCallback((data: any) => {
      if (data.action === google.picker.Action.PICKED) {
        const rawDocs = data.docs || [];
        const pickedDocuments: GooglePickerDocument[] = rawDocs.map((doc: any) => ({
          id: doc.id,
          name: doc.name || doc.title || 'Untitled Document',
          mimeType: doc.mimeType || doc.type || 'application/octet-stream',
          url: doc.url || doc.embedUrl || `https://drive.google.com/file/d/${doc.id}/view`,
          description: doc.description || '',
          sizeBytes: doc.sizeBytes,
          lastEditedUtc: doc.lastEditedUtc,
          iconUrl: doc.iconUrl
        }));
        options.onPicked(pickedDocuments);
      } else if (data.action === google.picker.Action.CANCEL) {
        if (options.onCancel) options.onCancel();
      }
    });

  if (options.title) {
    builder.setTitle(options.title);
  } else {
    builder.setTitle('Select Google Drive Documents for Fleet Ingestion');
  }

  if (options.multiselect !== false) {
    builder.enableFeature(google.picker.Feature.MULTISELECT_ENABLED);
  }

  const picker = builder.build();
  picker.setVisible(true);
}

