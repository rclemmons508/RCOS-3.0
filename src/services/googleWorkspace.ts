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
  starred?: boolean;
  labels: string[];
}

export interface GmailFullMessage extends GmailMessageSnippet {
  bodyHtml?: string;
  bodyText?: string;
  cc?: string;
  bcc?: string;
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

import { WORKSPACE_SCOPES, setManualAccessToken } from './firebaseAuth';
import firebaseConfig from '../../firebase-applet-config.json';
export const SCOPES = WORKSPACE_SCOPES.join(' ');

export {
  signInWithGoogleWorkspace,
  initAuthListener,
  logoutGoogleWorkspace,
  setManualAccessToken,
  getCachedAccessToken,
  isAccessTokenExpired,
  getAccessTokenExpiresAt
} from './firebaseAuth';

// Google Client ID configuration - aligned with configured Firebase Project
export const DEFAULT_GOOGLE_CLIENT_ID = firebaseConfig.oAuthClientId || '812425382282-ueuv0j8bmpkp8d89c83hen7atgg3nghe.apps.googleusercontent.com';

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
          // Publish into the shared Workspace session so Gmail, Drive and
          // Calendar all see this token without a second consent prompt.
          setManualAccessToken(
            tokenResponse.access_token,
            typeof tokenResponse.expires_in === 'number' ? tokenResponse.expires_in : null
          );
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

// Decode Gmail URL-safe base64 data
function decodeBase64UrlSafe(str: string): string {
  try {
    const cleaned = str.replace(/-/g, '+').replace(/_/g, '/');
    return decodeURIComponent(
      Array.prototype.map.call(atob(cleaned), (c: string) => {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join('')
    );
  } catch {
    try {
      return atob(str.replace(/-/g, '+').replace(/_/g, '/'));
    } catch {
      return '';
    }
  }
}

// Extract email body parts recursively from message payload
function extractBodyFromPayload(payload: any): { html?: string; text?: string } {
  if (!payload) return {};
  let html = '';
  let text = '';

  const traverse = (part: any) => {
    if (!part) return;
    if (part.mimeType === 'text/html' && part.body?.data) {
      html = decodeBase64UrlSafe(part.body.data);
    } else if (part.mimeType === 'text/plain' && part.body?.data) {
      text = decodeBase64UrlSafe(part.body.data);
    }
    if (part.parts && Array.isArray(part.parts)) {
      part.parts.forEach(traverse);
    }
  };

  if (payload.body?.data) {
    const decoded = decodeBase64UrlSafe(payload.body.data);
    if (payload.mimeType === 'text/html') {
      html = decoded;
    } else {
      text = decoded;
    }
  }

  if (payload.parts && Array.isArray(payload.parts)) {
    payload.parts.forEach(traverse);
  }

  return { html, text };
}

export interface FetchGmailOptions {
  maxResults?: number;
  labelIds?: string[];
  query?: string;
}

// Fetch email messages and headers from Gmail API v1
export async function fetchGmailMessages(
  accessToken: string,
  options?: FetchGmailOptions
): Promise<GmailMessageSnippet[]> {
  const maxResults = options?.maxResults || 20;
  const params = new URLSearchParams();
  params.set('maxResults', maxResults.toString());

  if (options?.labelIds && options.labelIds.length > 0) {
    options.labelIds.forEach(lbl => params.append('labelIds', lbl));
  }
  if (options?.query) {
    params.set('q', options.query);
  }

  const listUrl = `https://www.googleapis.com/gmail/v1/users/me/messages?${params.toString()}`;
  
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

  // Fetch message headers in parallel
  const messagePromises = rawMessages.slice(0, maxResults).map(async (msg) => {
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
      const labels: string[] = data.labelIds || [];

      return {
        id: data.id,
        threadId: data.threadId,
        snippet: data.snippet || '',
        internalDate: data.internalDate,
        subject: getHeader('Subject') || '(No Subject)',
        from: getHeader('From') || 'Unknown Sender',
        to: getHeader('To') || 'Me',
        date: getHeader('Date') || '',
        unread: labels.includes('UNREAD'),
        starred: labels.includes('STARRED'),
        labels
      } as GmailMessageSnippet;
    } catch {
      return null;
    }
  });

  const resolved = await Promise.all(messagePromises);
  return resolved.filter((m): m is GmailMessageSnippet => m !== null);
}

// Fetch full email message body and detailed headers
export async function fetchGmailMessageDetails(
  accessToken: string,
  messageId: string
): Promise<GmailFullMessage> {
  const url = `https://www.googleapis.com/gmail/v1/users/me/messages/${messageId}?format=full`;
  const response = await fetch(url, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      Accept: 'application/json'
    }
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Failed to fetch message details: ${response.status} - ${errText}`);
  }

  const data = await response.json();
  const headers = data.payload?.headers || [];
  const getHeader = (name: string) => headers.find((h: any) => h.name.toLowerCase() === name.toLowerCase())?.value || '';
  const labels: string[] = data.labelIds || [];

  const { html, text } = extractBodyFromPayload(data.payload);

  return {
    id: data.id,
    threadId: data.threadId,
    snippet: data.snippet || '',
    internalDate: data.internalDate,
    subject: getHeader('Subject') || '(No Subject)',
    from: getHeader('From') || 'Unknown Sender',
    to: getHeader('To') || 'Me',
    cc: getHeader('Cc') || undefined,
    bcc: getHeader('Bcc') || undefined,
    date: getHeader('Date') || '',
    unread: labels.includes('UNREAD'),
    starred: labels.includes('STARRED'),
    labels,
    bodyHtml: html,
    bodyText: text
  };
}

export interface SendEmailParams {
  to: string;
  subject: string;
  body: string;
  cc?: string;
  bcc?: string;
  threadId?: string;
}

// Send an email via Gmail API
export async function sendGmailMessage(
  accessToken: string,
  params: SendEmailParams
): Promise<{ id: string; threadId: string }> {
  // Construct RFC 2822 email format
  const headerLines = [
    `To: ${params.to}`,
    params.cc ? `Cc: ${params.cc}` : null,
    params.bcc ? `Bcc: ${params.bcc}` : null,
    `Subject: =?utf-8?B?${btoa(unescape(encodeURIComponent(params.subject)))}?=`,
    'MIME-Version: 1.0',
    'Content-Type: text/html; charset=utf-8',
    'Content-Transfer-Encoding: 7bit'
  ].filter(Boolean);

  const formattedBody = params.body.includes('<') && params.body.includes('>')
    ? params.body
    : params.body.replace(/\n/g, '<br/>');

  const fullEmail = headerLines.join('\r\n') + '\r\n\r\n' + formattedBody;

  // URL-safe base64 encoding
  const rawBase64 = btoa(unescape(encodeURIComponent(fullEmail)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');

  const payload: any = { raw: rawBase64 };
  if (params.threadId) {
    payload.threadId = params.threadId;
  }

  const response = await fetch('https://gmail.googleapis.com/gmail/v1/users/me/messages/send', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Gmail API send failed: ${response.status} - ${errText}`);
  }

  return response.json();
}

// Move email message to Trash
export async function trashGmailMessage(
  accessToken: string,
  messageId: string
): Promise<boolean> {
  const response = await fetch(`https://gmail.googleapis.com/gmail/v1/users/me/messages/${messageId}/trash`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Failed to trash email: ${response.status} - ${errText}`);
  }

  return true;
}

// Modify Gmail message labels (e.g. read/unread, star/unstar)
export async function modifyGmailLabels(
  accessToken: string,
  messageId: string,
  addLabelIds: string[] = [],
  removeLabelIds: string[] = []
): Promise<boolean> {
  const response = await fetch(`https://gmail.googleapis.com/gmail/v1/users/me/messages/${messageId}/modify`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      addLabelIds,
      removeLabelIds
    })
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`Failed to modify labels: ${response.status} - ${errText}`);
  }

  return true;
}

// Helper to mark message as read
export async function markGmailAsRead(accessToken: string, messageId: string): Promise<boolean> {
  return modifyGmailLabels(accessToken, messageId, [], ['UNREAD']);
}

// Helper to mark message as unread
export async function markGmailAsUnread(accessToken: string, messageId: string): Promise<boolean> {
  return modifyGmailLabels(accessToken, messageId, ['UNREAD'], []);
}

// Helper to toggle star on message
export async function toggleStarGmailMessage(accessToken: string, messageId: string, isStarred: boolean): Promise<boolean> {
  if (isStarred) {
    return modifyGmailLabels(accessToken, messageId, [], ['STARRED']);
  } else {
    return modifyGmailLabels(accessToken, messageId, ['STARRED'], []);
  }
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

