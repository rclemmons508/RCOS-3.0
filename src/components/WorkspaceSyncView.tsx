import React, { useState, useEffect, useRef } from 'react';
import { 
  Cloud, 
  HardDrive, 
  Calendar as CalendarIcon, 
  Mail, 
  RefreshCw, 
  CheckCircle2, 
  AlertCircle, 
  ExternalLink, 
  Clock, 
  FileText, 
  User, 
  ShieldCheck, 
  Zap, 
  ArrowRight,
  Search,
  Filter,
  Layers,
  Sparkles,
  ChevronRight,
  Info,
  FolderOpen,
  FolderPlus,
  FolderCheck,
  Trash2,
  Loader2,
  FileCheck,
  UploadCloud,
  Bot,
  Briefcase,
  Key,
  Settings2,
  Check,
  HelpCircle,
  LogIn
} from 'lucide-react';
import { 
  GoogleDriveFile, 
  GoogleCalendarEvent, 
  GmailMessageSnippet,
  GooglePickerDocument,
  openGooglePicker,
  initWorkspaceTokenClient,
  requestWorkspaceAuth,
  fetchDriveFiles,
  fetchCalendarEvents,
  fetchGmailMessages,
  getGoogleClientId,
  setCustomGoogleClientId,
  signInWithGoogleWorkspace,
  logoutGoogleWorkspace,
  DEFAULT_GOOGLE_CLIENT_ID
} from '../services/googleWorkspace';
import { Agent } from '../types';

interface WorkspaceSyncViewProps {
  agents?: Agent[];
  onDirectTask?: (agentId: string, task: string) => void;
  onImportCalendarEvent?: (event: GoogleCalendarEvent) => void;
  onImportDriveDocument?: (doc: GooglePickerDocument) => void;
}

export const WorkspaceSyncView: React.FC<WorkspaceSyncViewProps> = ({
  agents = [],
  onDirectTask,
  onImportCalendarEvent,
  onImportDriveDocument
}) => {
  const [activeSubTab, setActiveSubTab] = useState<'overview' | 'drive' | 'calendar' | 'gmail'>('overview');
  
  // Auth state
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [authError, setAuthError] = useState<string | null>(null);
  const [isAuthorizing, setIsAuthorizing] = useState<boolean>(false);
  const [hasClientId, setHasClientId] = useState<boolean>(false);

  // Credentials config & manual token state
  const [showSettingsModal, setShowSettingsModal] = useState<boolean>(false);
  const [customClientIdInput, setCustomClientIdInput] = useState<string>(getGoogleClientId());
  const [manualTokenInput, setManualTokenInput] = useState<string>('');
  const [clientIdSavedSuccess, setClientIdSavedSuccess] = useState<boolean>(false);

  // Sync state
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [lastSyncTime, setLastSyncTime] = useState<string | null>(null);
  const [syncError, setSyncError] = useState<string | null>(null);

  // Google Picker state
  const [pickedDocuments, setPickedDocuments] = useState<GooglePickerDocument[]>([]);
  const [isPickerLoading, setIsPickerLoading] = useState<boolean>(false);
  const [pickerError, setPickerError] = useState<string | null>(null);
  const pendingOpenPickerRef = useRef<boolean>(false);

  // Fetched data
  const [driveFiles, setDriveFiles] = useState<GoogleDriveFile[]>([]);
  const [calendarEvents, setCalendarEvents] = useState<GoogleCalendarEvent[]>([]);
  const [gmailMessages, setGmailMessages] = useState<GmailMessageSnippet[]>([]);

  // Filter / Search
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Check client ID availability and initialize token client
  useEffect(() => {
    const clientId = getGoogleClientId();
    if (clientId) {
      setHasClientId(true);
    }

    const checkGIS = () => {
      if ((window as any).google?.accounts?.oauth2) {
        initWorkspaceTokenClient(
          (token: string) => {
            setAccessToken(token);
            setIsAuthenticated(true);
            setIsAuthorizing(false);
            setAuthError(null);
            // Auto sync upon authentication
            performWorkspaceSync(token);

            // If user clicked Google Picker before auth, open picker now
            if (pendingOpenPickerRef.current) {
              pendingOpenPickerRef.current = false;
              launchPicker(token);
            }
          },
          (err: any) => {
            setIsAuthorizing(false);
            pendingOpenPickerRef.current = false;
            const errStr = typeof err === 'string' ? err : err?.message || JSON.stringify(err);
            if (errStr.includes('invalid_client') || errStr.includes('401')) {
              setAuthError(`OAuth Client Error (401 invalid_client): The Client ID is not recognized as a registered Web application client by Google. Try using 'Sign In with Firebase' or verify the Client ID in Settings.`);
            } else {
              setAuthError(`Authentication error: ${errStr}`);
            }
          }
        );
      } else {
        setTimeout(checkGIS, 300);
      }
    };

    checkGIS();
  }, []);

  const handleConnect = async (mode: 'firebase' | 'gis' | unknown = 'firebase') => {
    setAuthError(null);
    setIsAuthorizing(true);

    if (mode === 'gis') {
      try {
        requestWorkspaceAuth();
      } catch (err: any) {
        setIsAuthorizing(false);
        pendingOpenPickerRef.current = false;
        setAuthError(err?.message || 'Could not launch Google authentication window.');
      }
      return;
    }

    // Default & Recommended: Firebase Auth Popup (uses project's OAuth configuration)
    try {
      const result = await signInWithGoogleWorkspace();
      if (result && result.accessToken) {
        setAccessToken(result.accessToken);
        setIsAuthenticated(true);
        setIsAuthorizing(false);
        setAuthError(null);
        performWorkspaceSync(result.accessToken);

        if (pendingOpenPickerRef.current) {
          pendingOpenPickerRef.current = false;
          launchPicker(result.accessToken);
        }
        return;
      }
    } catch (fbErr: any) {
      console.warn('Firebase Auth attempt failed:', fbErr);
      if (fbErr?.code === 'auth/popup-closed-by-user') {
        setIsAuthorizing(false);
        pendingOpenPickerRef.current = false;
        return;
      }

      // If Firebase Auth fails, provide fallback to GIS
      try {
        requestWorkspaceAuth();
        return;
      } catch (gisErr: any) {
        console.warn('GIS fallback also threw:', gisErr);
      }

      setIsAuthorizing(false);
      pendingOpenPickerRef.current = false;
      const msg = fbErr?.message || 'Google authentication failed';
      setAuthError(msg);
    }
  };

  const handleDisconnect = async () => {
    try {
      await logoutGoogleWorkspace();
    } catch (e) {
      console.error(e);
    }
    setAccessToken(null);
    setIsAuthenticated(false);
    setDriveFiles([]);
    setCalendarEvents([]);
    setGmailMessages([]);
    setLastSyncTime(null);
  };

  const handleApplyManualToken = (token: string) => {
    const trimmed = token.trim();
    if (!trimmed) return;
    setAccessToken(trimmed);
    setIsAuthenticated(true);
    setAuthError(null);
    performWorkspaceSync(trimmed);
    setShowSettingsModal(false);
    if (pendingOpenPickerRef.current) {
      pendingOpenPickerRef.current = false;
      launchPicker(trimmed);
    }
  };

  const handleSaveCustomClientId = () => {
    setCustomGoogleClientId(customClientIdInput);
    setClientIdSavedSuccess(true);
    setTimeout(() => setClientIdSavedSuccess(false), 2500);
    // Reinit GIS
    initWorkspaceTokenClient(
      (token: string) => {
        setAccessToken(token);
        setIsAuthenticated(true);
        setIsAuthorizing(false);
        setAuthError(null);
        performWorkspaceSync(token);
      },
      (err: any) => {
        setIsAuthorizing(false);
        const errStr = typeof err === 'string' ? err : err?.message || JSON.stringify(err);
        setAuthError(`Authentication error: ${errStr}`);
      }
    );
  };

  // Launch Google Picker dialog
  const launchPicker = async (tokenOverride?: string) => {
    const token = tokenOverride || accessToken;
    if (!token) {
      pendingOpenPickerRef.current = true;
      handleConnect();
      return;
    }

    setIsPickerLoading(true);
    setPickerError(null);

    try {
      await openGooglePicker({
        accessToken: token,
        title: 'Select Documents from Google Drive for Fleet Processing',
        multiselect: true,
        onPicked: (docs) => {
          setIsPickerLoading(false);
          setPickedDocuments((prev) => {
            const existingIds = new Set(prev.map(d => d.id));
            const newDocs = docs.filter(d => !existingIds.has(d.id));
            return [...newDocs, ...prev];
          });
          setActiveSubTab('drive');
        },
        onCancel: () => {
          setIsPickerLoading(false);
        }
      });
    } catch (err: any) {
      setIsPickerLoading(false);
      console.error('Picker error:', err);
      setPickerError(err?.message || 'Failed to open Google Picker');
    }
  };

  const handleRemovePickedDoc = (docId: string) => {
    setPickedDocuments(prev => prev.filter(d => d.id !== docId));
  };

  const performWorkspaceSync = async (tokenOverride?: string) => {
    const token = tokenOverride || accessToken;
    if (!token) {
      setAuthError('Please connect your Google Workspace first.');
      return;
    }

    setIsSyncing(true);
    setSyncError(null);

    try {
      const [filesResult, eventsResult, mailResult] = await Promise.allSettled([
        fetchDriveFiles(token),
        fetchCalendarEvents(token),
        fetchGmailMessages(token)
      ]);

      let syncIssues: string[] = [];

      if (filesResult.status === 'fulfilled') {
        setDriveFiles(filesResult.value);
      } else {
        console.warn('Drive sync failed:', filesResult.reason);
        syncIssues.push('Drive');
      }

      if (eventsResult.status === 'fulfilled') {
        setCalendarEvents(eventsResult.value);
      } else {
        console.warn('Calendar sync failed:', eventsResult.reason);
        syncIssues.push('Calendar');
      }

      if (mailResult.status === 'fulfilled') {
        setGmailMessages(mailResult.value);
      } else {
        console.warn('Gmail sync failed:', mailResult.reason);
        syncIssues.push('Gmail');
      }

      const now = new Date();
      setLastSyncTime(now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }));

      if (syncIssues.length > 0 && syncIssues.length < 3) {
        setSyncError(`Partial sync complete. Notice: ${syncIssues.join(', ')} had temporary read delays.`);
      }
    } catch (err: any) {
      setSyncError(err?.message || 'Failed to sync with Google Workspace services.');
    } finally {
      setIsSyncing(false);
    }
  };

  // Dispatch an autonomous agent task based on synced email or document
  const handleDispatchAgentFromWorkspace = (sourceType: 'doc' | 'email' | 'event', title: string) => {
    if (!onDirectTask) return;
    
    // Choose relevant agent
    let targetAgent = agents.find(a => a.name.toLowerCase().includes('intelligence') || a.codeName.includes('COGNITO'));
    if (!targetAgent) targetAgent = agents[0];

    const taskText = `Review and synthesize synced ${sourceType.toUpperCase()}: "${title.slice(0, 60)}"`;
    onDirectTask(targetAgent.id, taskText);
  };

  // Filtered lists
  const filteredFiles = driveFiles.filter(f => 
    f.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredEvents = calendarEvents.filter(e => 
    (e.summary || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
    (e.location || '').toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredMails = gmailMessages.filter(m => 
    m.subject.toLowerCase().includes(searchQuery.toLowerCase()) ||
    m.from.toLowerCase().includes(searchQuery.toLowerCase()) ||
    m.snippet.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div id="google-workspace-sync-view" className="space-y-5 max-w-4xl mx-auto pb-20 animate-in fade-in duration-150">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800/80">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <Cloud className="w-5 h-5 text-[#76d418]" />
            <span>Google Workspace Connection & Live Sync</span>
          </h1>
          <p className="text-xs text-slate-400">
            Bidirectional synchronization with Google Drive, Google Calendar, and Gmail for autonomous agent execution
          </p>
        </div>

        {/* Sync Controls */}
        <div className="flex items-center gap-2">
          <button
            id="btn-workspace-settings"
            onClick={() => setShowSettingsModal(true)}
            className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-slate-300 text-xs font-semibold transition-all cursor-pointer"
            title="Configure Client ID or Direct Access Token"
          >
            <Key className="w-3.5 h-3.5 text-amber-400" />
            <span className="hidden sm:inline">Credentials</span>
          </button>

          <button
            id="btn-workspace-open-google-picker"
            onClick={() => launchPicker()}
            disabled={isPickerLoading}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-gradient-to-r from-sky-600/30 to-[#76d418]/30 hover:from-sky-600/40 hover:to-[#76d418]/40 text-sky-200 border border-[#76d418]/50 text-xs font-bold transition-all shadow-sm cursor-pointer"
            title="Open Google Drive Picker dialog"
          >
            {isPickerLoading ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin text-[#76d418]" />
            ) : (
              <FolderOpen className="w-3.5 h-3.5 text-[#76d418]" />
            )}
            <span>{isPickerLoading ? 'Opening Picker...' : 'Google Picker'}</span>
          </button>

          {isAuthenticated ? (
            <>
              <button
                id="btn-workspace-sync-now"
                onClick={() => performWorkspaceSync()}
                disabled={isSyncing}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#76d418] hover:bg-[#66bd14] disabled:opacity-50 text-slate-950 text-xs font-bold transition-all shadow cursor-pointer"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isSyncing ? 'animate-spin' : ''}`} />
                <span>{isSyncing ? 'Syncing...' : 'Sync Now'}</span>
              </button>
              <button
                id="btn-workspace-disconnect"
                onClick={handleDisconnect}
                className="px-2.5 py-1.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-400 hover:text-white text-xs transition-colors cursor-pointer"
              >
                Disconnect
              </button>
            </>
          ) : (
            <button
              id="btn-workspace-connect-google"
              onClick={() => handleConnect('firebase')}
              disabled={isAuthorizing}
              className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 text-xs font-black shadow-lg shadow-[#76d418]/15 cursor-pointer transition-all"
            >
              {isAuthorizing ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <svg className="w-4 h-4" viewBox="0 0 24 24">
                  <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                  <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                  <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" />
                  <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" />
                </svg>
              )}
              <span>{isAuthorizing ? 'Signing in...' : 'Sign In with Google'}</span>
            </button>
          )}
        </div>
      </div>

      {/* Connection & Auth Status Card */}
      <div className={`p-4 rounded-2xl border transition-all ${
        isAuthenticated 
          ? 'bg-[#09150d] border-[#76d418]/40 shadow-lg shadow-[#76d418]/5' 
          : 'bg-[#091016] border-slate-800'
      }`}>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 font-bold ${
              isAuthenticated 
                ? 'bg-[#76d418] text-slate-950' 
                : 'bg-slate-800 text-slate-400'
            }`}>
              {isAuthenticated ? <CheckCircle2 className="w-5 h-5 stroke-[2.5]" /> : <Cloud className="w-5 h-5" />}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-sm font-bold text-white">
                  {isAuthenticated ? 'Google Workspace Connected' : 'Google Workspace Disconnected'}
                </span>
                <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                  isAuthenticated 
                    ? 'bg-[#76d418]/20 text-[#76d418] border border-[#76d418]/40' 
                    : 'bg-slate-800 text-slate-400'
                }`}>
                  {isAuthenticated ? 'OAUTH 2.0 LIVE' : 'AWAITING AUTHORIZATION'}
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                {isAuthenticated 
                  ? `Active session • Last synchronized at ${lastSyncTime || 'Just now'}`
                  : 'Connect Google Workspace with your Google account to empower agent operations with Drive, Docs, Calendar, and Gmail.'
                }
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3 text-xs">
            <div className="text-left sm:text-right">
              <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">Connected Scopes</span>
              <div className="flex items-center gap-1.5 mt-0.5 text-slate-300">
                <span className="px-1.5 py-0.5 rounded bg-slate-800 text-[10px] font-mono">Drive</span>
                <span className="px-1.5 py-0.5 rounded bg-[#76d418]/20 border border-[#76d418]/30 text-[#76d418] text-[10px] font-mono font-bold">Google Picker</span>
                <span className="px-1.5 py-0.5 rounded bg-slate-800 text-[10px] font-mono">Calendar</span>
                <span className="px-1.5 py-0.5 rounded bg-slate-800 text-[10px] font-mono">Gmail</span>
              </div>
              <div className="mt-1 text-[10px] text-slate-500 font-mono truncate max-w-[260px] flex items-center justify-end gap-1">
                <span>Client:</span> 
                <span className="text-slate-400" title={getGoogleClientId()}>{getGoogleClientId().slice(0, 12)}...{getGoogleClientId().slice(-24)}</span>
                <button 
                  onClick={() => setShowSettingsModal(true)} 
                  className="text-amber-400 hover:text-amber-300 ml-1 underline cursor-pointer"
                >
                  Edit
                </button>
              </div>
            </div>
          </div>
        </div>

        {authError && (
          <div className="mt-3 p-4 rounded-xl bg-rose-950/40 border border-rose-800 text-rose-200 text-xs space-y-2">
            <div className="flex items-start gap-2">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <div className="flex-1">
                <span className="font-bold text-rose-300">Authorization Notice:</span>
                <p className="mt-0.5">{authError}</p>
              </div>
            </div>

            <div className="pt-2 border-t border-rose-900/60 flex flex-wrap items-center gap-2">
              <button
                onClick={() => handleConnect('firebase')}
                disabled={isAuthorizing}
                className="px-3 py-1.5 rounded-lg bg-rose-900/80 hover:bg-rose-800 text-rose-100 font-bold text-xs flex items-center gap-1.5 transition-all cursor-pointer"
              >
                <LogIn className="w-3.5 h-3.5" />
                <span>Retry via Firebase Auth Popup</span>
              </button>
              <button
                onClick={() => handleConnect('gis')}
                disabled={isAuthorizing}
                className="px-3 py-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-slate-300 text-xs font-semibold transition-all cursor-pointer"
              >
                <span>Retry via GIS Client</span>
              </button>
              <button
                onClick={() => setShowSettingsModal(true)}
                className="px-3 py-1.5 rounded-lg bg-amber-950/60 hover:bg-amber-900/80 text-amber-200 border border-amber-700/50 text-xs font-semibold flex items-center gap-1 transition-all cursor-pointer"
              >
                <Key className="w-3 h-3 text-amber-400" />
                <span>Paste Access Token / Edit Client ID</span>
              </button>
            </div>
          </div>
        )}

        {pickerError && (
          <div className="mt-3 p-3 rounded-xl bg-amber-950/40 border border-amber-800 text-amber-300 text-xs flex items-start gap-2">
            <AlertCircle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
            <div className="flex-1">
              <span className="font-semibold">Google Picker Notice:</span> {pickerError}
              <p className="text-amber-400/80 text-[11px] mt-0.5">
                Ensure Google Workspace permissions are approved in the popup dialog.
              </p>
            </div>
          </div>
        )}

        {syncError && (
          <div className="mt-3 p-3 rounded-xl bg-amber-950/40 border border-amber-800 text-amber-300 text-xs flex items-start gap-2">
            <Info className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
            <span>{syncError}</span>
          </div>
        )}
      </div>

      {/* Workspace Metric Tiles */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {/* Drive Card */}
        <div 
          onClick={() => setActiveSubTab('drive')}
          className={`p-4 rounded-2xl border cursor-pointer transition-all ${
            activeSubTab === 'drive'
              ? 'bg-[#0a1622] border-sky-500/50 shadow-lg shadow-sky-500/10'
              : 'bg-[#091016] border-slate-800 hover:border-slate-700'
          }`}
        >
          <div className="flex items-center justify-between">
            <div className="w-8 h-8 rounded-xl bg-sky-500/10 text-sky-400 flex items-center justify-center">
              <HardDrive className="w-4 h-4" />
            </div>
            <span className="text-xs font-mono font-bold text-sky-400">
              {driveFiles.length} Synced
            </span>
          </div>
          <h3 className="text-sm font-bold text-white mt-2">Google Drive</h3>
          <p className="text-[11px] text-slate-400">Corporate docs, spreadsheets & PDF briefs</p>
        </div>

        {/* Calendar Card */}
        <div 
          onClick={() => setActiveSubTab('calendar')}
          className={`p-4 rounded-2xl border cursor-pointer transition-all ${
            activeSubTab === 'calendar'
              ? 'bg-[#0e1c12] border-[#76d418]/50 shadow-lg shadow-[#76d418]/10'
              : 'bg-[#091016] border-slate-800 hover:border-slate-700'
          }`}
        >
          <div className="flex items-center justify-between">
            <div className="w-8 h-8 rounded-xl bg-[#76d418]/10 text-[#76d418] flex items-center justify-center">
              <CalendarIcon className="w-4 h-4" />
            </div>
            <span className="text-xs font-mono font-bold text-[#76d418]">
              {calendarEvents.length} Events
            </span>
          </div>
          <h3 className="text-sm font-bold text-white mt-2">Google Calendar</h3>
          <p className="text-[11px] text-slate-400">Client consultations & executive schedules</p>
        </div>

        {/* Gmail Card */}
        <div 
          onClick={() => setActiveSubTab('gmail')}
          className={`p-4 rounded-2xl border cursor-pointer transition-all ${
            activeSubTab === 'gmail'
              ? 'bg-[#1e1014] border-rose-500/50 shadow-lg shadow-rose-500/10'
              : 'bg-[#091016] border-slate-800 hover:border-slate-700'
          }`}
        >
          <div className="flex items-center justify-between">
            <div className="w-8 h-8 rounded-xl bg-rose-500/10 text-rose-400 flex items-center justify-center">
              <Mail className="w-4 h-4" />
            </div>
            <span className="text-xs font-mono font-bold text-rose-400">
              {gmailMessages.length} Messages
            </span>
          </div>
          <h3 className="text-sm font-bold text-white mt-2">Gmail Inbox</h3>
          <p className="text-[11px] text-slate-400">Incoming partner inquiries & SLA updates</p>
        </div>
      </div>

      {/* Sub-tab switcher & Search */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 pt-1">
        <div className="flex items-center gap-1.5 p-1 rounded-xl bg-[#060a08] border border-slate-800">
          <button
            onClick={() => setActiveSubTab('overview')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              activeSubTab === 'overview'
                ? 'bg-slate-800 text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            All Workspaces
          </button>
          <button
            onClick={() => setActiveSubTab('drive')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              activeSubTab === 'drive'
                ? 'bg-sky-500/20 text-sky-300 border border-sky-500/40 shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Drive ({driveFiles.length})
          </button>
          <button
            onClick={() => setActiveSubTab('calendar')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              activeSubTab === 'calendar'
                ? 'bg-[#76d418]/20 text-[#76d418] border border-[#76d418]/40 shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Calendar ({calendarEvents.length})
          </button>
          <button
            onClick={() => setActiveSubTab('gmail')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              activeSubTab === 'gmail'
                ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40 shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Gmail ({gmailMessages.length})
          </button>
        </div>

        {/* Search input */}
        <div className="relative min-w-[220px]">
          <Search className="w-3.5 h-3.5 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Filter synced workspace items..."
            className="w-full bg-[#060a08] border border-slate-800 focus:border-[#76d418] rounded-xl pl-8 pr-3 py-1.5 text-xs text-white placeholder:text-slate-500 outline-none"
          />
        </div>
      </div>

      {/* Main Workspace Feed Content */}
      <div className="space-y-4">
        {/* If not authenticated and no data */}
        {!isAuthenticated && driveFiles.length === 0 && calendarEvents.length === 0 && (
          <div className="p-8 rounded-2xl bg-[#091016] border border-slate-800 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-[#76d418]/10 text-[#76d418] flex items-center justify-center mx-auto">
              <Cloud className="w-6 h-6" />
            </div>
            <div className="space-y-1 max-w-md mx-auto">
              <h3 className="text-base font-bold text-white">Connect Google Workspace to Start Syncing</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Empower your autonomous fleet agents with direct context from your Google Drive corporate files, Google Calendar milestones, and Gmail client inquiries.
              </p>
            </div>
            <button
              onClick={() => handleConnect('firebase')}
              disabled={isAuthorizing}
              className="px-4 py-2 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 font-bold text-xs inline-flex items-center gap-2 cursor-pointer shadow-lg shadow-[#76d418]/20 transition-all"
            >
              <Zap className="w-4 h-4 fill-current" />
              <span>{isAuthorizing ? 'Authorizing in popup...' : 'Authorize & Start Syncing'}</span>
            </button>
          </div>
        )}

        {/* Google Picker Picked Documents Section */}
        {(activeSubTab === 'overview' || activeSubTab === 'drive') && (
          <div className="space-y-3">
            {/* Google Picker Callout / Launcher Card */}
            <div className="p-4 rounded-2xl bg-gradient-to-r from-[#091522] via-[#07131b] to-[#0a1811] border border-sky-500/30 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-lg shadow-sky-950/20">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-sky-400 shrink-0">
                  <FolderOpen className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-bold text-white">Google Picker File Selector</h3>
                    <span className="px-2 py-0.5 rounded-full bg-sky-500/20 border border-sky-500/40 text-sky-300 text-[10px] font-mono font-bold">
                      DRIVE.FILE READY
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Select specific Google Docs, Sheets, Slides, or PDFs directly from Drive to ingest into agent pipelines.
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <button
                  id="btn-drive-launch-picker"
                  onClick={() => launchPicker()}
                  disabled={isPickerLoading}
                  className="px-3.5 py-2 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 font-bold text-xs flex items-center gap-1.5 cursor-pointer shadow-md shadow-[#76d418]/20 transition-all"
                >
                  {isPickerLoading ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <FolderPlus className="w-3.5 h-3.5 stroke-[2.5]" />
                  )}
                  <span>{isPickerLoading ? 'Loading Picker...' : 'Open Google Picker'}</span>
                </button>
              </div>
            </div>

            {/* List of Picked Documents */}
            {pickedDocuments.length > 0 && (
              <div className="bg-[#09131c] border border-sky-500/30 rounded-2xl p-4 space-y-3">
                <div className="flex items-center justify-between pb-2 border-b border-slate-800">
                  <div className="flex items-center gap-2">
                    <FolderCheck className="w-4 h-4 text-sky-400" />
                    <h4 className="text-xs font-bold text-white uppercase tracking-wider">
                      Selected Via Google Picker
                    </h4>
                    <span className="px-1.5 py-0.5 rounded-full bg-sky-500/20 text-sky-300 text-[10px] font-mono font-bold">
                      {pickedDocuments.length}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => launchPicker()}
                      disabled={isPickerLoading}
                      className="px-2.5 py-1 rounded-lg bg-sky-500/15 hover:bg-sky-500/25 text-sky-300 text-xs font-semibold flex items-center gap-1 cursor-pointer transition-colors"
                    >
                      <FolderPlus className="w-3 h-3" />
                      <span>Add More</span>
                    </button>
                    <button
                      onClick={() => setPickedDocuments([])}
                      className="px-2 py-1 rounded-lg text-slate-500 hover:text-rose-400 text-xs transition-colors cursor-pointer"
                      title="Clear picked list"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>

                <div className="space-y-2">
                  {pickedDocuments.map((doc) => (
                    <div 
                      key={doc.id}
                      className="p-3 rounded-xl bg-[#060c14] border border-sky-500/20 hover:border-sky-500/40 flex items-center justify-between gap-3 text-xs transition-all"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="w-8 h-8 rounded-lg bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-sky-400 shrink-0">
                          <FileCheck className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-white truncate">{doc.name}</span>
                            <span className="px-1.5 py-0.5 rounded bg-slate-800 text-[10px] text-slate-400 font-mono shrink-0">
                              {doc.mimeType.split('/').pop()?.replace('vnd.google-apps.', '') || 'file'}
                            </span>
                          </div>
                          <p className="text-[11px] text-slate-400 flex items-center gap-2 mt-0.5">
                            {doc.sizeBytes ? <span>{(doc.sizeBytes / 1024).toFixed(1)} KB</span> : null}
                            <span>• Picked via Google Picker</span>
                            {doc.lastEditedUtc && (
                              <span>• Edited {new Date(doc.lastEditedUtc).toLocaleDateString()}</span>
                            )}
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        {onImportDriveDocument && (
                          <button
                            onClick={() => onImportDriveDocument(doc)}
                            className="px-2.5 py-1 rounded-lg bg-[#76d418] hover:bg-[#66bd14] text-slate-950 font-bold text-[11px] flex items-center gap-1 cursor-pointer transition-colors shadow-sm"
                            title="Create deliverable job referencing this document"
                          >
                            <Briefcase className="w-3 h-3" />
                            <span className="hidden sm:inline">Create Job</span>
                          </button>
                        )}

                        <button
                          onClick={() => handleDispatchAgentFromWorkspace('doc', doc.name)}
                          className="px-2.5 py-1 rounded-lg bg-sky-500/15 hover:bg-sky-500/25 text-sky-300 border border-sky-500/30 text-[11px] font-bold flex items-center gap-1 cursor-pointer transition-colors"
                          title="Dispatch cognitive agent analysis"
                        >
                          <Bot className="w-3 h-3" />
                          <span className="hidden sm:inline">Dispatch Agent</span>
                        </button>

                        {doc.url && (
                          <a
                            href={doc.url}
                            target="_blank"
                            rel="noreferrer"
                            className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition-colors"
                            title="Open in Google Drive"
                          >
                            <ExternalLink className="w-3.5 h-3.5" />
                          </a>
                        )}

                        <button
                          onClick={() => handleRemovePickedDoc(doc.id)}
                          className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-950/30 transition-colors cursor-pointer"
                          title="Remove from picked list"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Drive Files Section */}
        {(activeSubTab === 'overview' || activeSubTab === 'drive') && (
          <div className="bg-[#091016] border border-slate-800 rounded-2xl p-5 space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <HardDrive className="w-4 h-4 text-sky-400" />
                <h3 className="text-sm font-bold text-white">Google Drive Files</h3>
                <span className="text-[11px] text-slate-500 font-mono">({filteredFiles.length})</span>
              </div>

              <div className="flex items-center gap-2">
                <button
                  id="btn-drive-header-picker"
                  onClick={() => launchPicker()}
                  disabled={isPickerLoading}
                  className="px-2.5 py-1 rounded-lg bg-sky-500/15 hover:bg-sky-500/25 text-sky-300 border border-sky-500/30 text-xs font-bold flex items-center gap-1 transition-all cursor-pointer"
                >
                  <FolderOpen className="w-3 h-3" />
                  <span>Google Picker</span>
                </button>

                {isAuthenticated && (
                  <span className="text-[11px] text-sky-400 font-medium flex items-center gap-1">
                    <CheckCircle2 className="w-3 h-3" />
                    <span>Synced</span>
                  </span>
                )}
              </div>
            </div>

            {filteredFiles.length === 0 ? (
              <div className="py-5 text-center text-xs text-slate-500">
                {isAuthenticated 
                  ? 'No matching files found in Google Drive.' 
                  : 'Authorize Google Workspace above to load Google Drive documents.'}
              </div>
            ) : (
              <div className="space-y-2">
                {filteredFiles.map((file) => (
                  <div 
                    key={file.id} 
                    className="p-3 rounded-xl bg-[#060a08] border border-slate-800 hover:border-slate-700 transition-all flex items-center justify-between gap-3 text-xs"
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <div className="w-7 h-7 rounded-lg bg-sky-500/15 border border-sky-500/30 flex items-center justify-center text-sky-400 shrink-0">
                        <FileText className="w-3.5 h-3.5" />
                      </div>
                      <div className="min-w-0">
                        <p className="font-bold text-white truncate">{file.name}</p>
                        <p className="text-[11px] text-slate-400 flex items-center gap-2 mt-0.5 truncate">
                          <span>Updated {file.modifiedTime ? new Date(file.modifiedTime).toLocaleDateString() : 'Recently'}</span>
                          {file.owners && file.owners[0] && (
                            <>
                              <span>•</span>
                              <span>By {file.owners[0].displayName || file.owners[0].emailAddress}</span>
                            </>
                          )}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        onClick={() => handleDispatchAgentFromWorkspace('doc', file.name)}
                        className="px-2.5 py-1 rounded-lg bg-[#76d418]/15 hover:bg-[#76d418]/25 text-[#76d418] border border-[#76d418]/30 text-[11px] font-bold flex items-center gap-1 cursor-pointer transition-colors"
                        title="Dispatch agent analysis task"
                      >
                        <Bot className="w-3 h-3" />
                        <span className="hidden sm:inline">Dispatch Task</span>
                      </button>

                      {file.webViewLink && (
                        <a
                          href={file.webViewLink}
                          target="_blank"
                          rel="noreferrer"
                          className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition-colors"
                          title="Open in Google Drive"
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                        </a>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Calendar Events Section */}
        {(activeSubTab === 'overview' || activeSubTab === 'calendar') && (
          <div className="bg-[#091016] border border-slate-800 rounded-2xl p-5 space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <CalendarIcon className="w-4 h-4 text-[#76d418]" />
                <h3 className="text-sm font-bold text-white">Google Calendar Events & Deliverables</h3>
                <span className="text-[11px] text-slate-500 font-mono">({filteredEvents.length})</span>
              </div>

              {isAuthenticated && (
                <span className="text-[11px] text-[#76d418] font-medium flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" />
                  <span>Synced</span>
                </span>
              )}
            </div>

            {filteredEvents.length === 0 ? (
              <div className="py-5 text-center text-xs text-slate-500">
                {isAuthenticated 
                  ? 'No matching events found in primary Google Calendar.' 
                  : 'Authorize Google Workspace above to sync upcoming calendar schedules.'}
              </div>
            ) : (
              <div className="space-y-2">
                {filteredEvents.map((evt) => {
                  const eventTime = evt.start.dateTime 
                    ? new Date(evt.start.dateTime).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })
                    : evt.start.date || 'All Day';

                  return (
                    <div 
                      key={evt.id} 
                      className="p-3 rounded-xl bg-[#060a08] border border-slate-800 hover:border-slate-700 transition-all flex items-center justify-between gap-3 text-xs"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="w-7 h-7 rounded-lg bg-[#76d418]/15 border border-[#76d418]/30 flex items-center justify-center text-[#76d418] shrink-0 font-bold text-[10px]">
                          <Clock className="w-3.5 h-3.5" />
                        </div>
                        <div className="min-w-0">
                          <p className="font-bold text-white truncate">{evt.summary || '(Untitled Event)'}</p>
                          <p className="text-[11px] text-slate-400 flex items-center gap-2 mt-0.5 truncate">
                            <span className="text-[#76d418] font-mono">{eventTime}</span>
                            {evt.location && (
                              <>
                                <span>•</span>
                                <span>{evt.location}</span>
                              </>
                            )}
                            {evt.attendees && evt.attendees.length > 0 && (
                              <>
                                <span>•</span>
                                <span>{evt.attendees.length} Attendees</span>
                              </>
                            )}
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        {onImportCalendarEvent && (
                          <button
                            onClick={() => onImportCalendarEvent(evt)}
                            className="px-2.5 py-1 rounded-lg bg-emerald-500/15 hover:bg-emerald-500/25 text-emerald-400 border border-emerald-500/30 text-[11px] font-bold flex items-center gap-1 cursor-pointer transition-colors"
                            title="Import event to RCOS schedule"
                          >
                            <Sparkles className="w-3 h-3 text-emerald-400" />
                            <span className="hidden sm:inline">Import Event</span>
                          </button>
                        )}

                        <button
                          onClick={() => handleDispatchAgentFromWorkspace('event', evt.summary || 'Scheduled Meeting')}
                          className="px-2.5 py-1 rounded-lg bg-[#76d418]/15 hover:bg-[#76d418]/25 text-[#76d418] border border-[#76d418]/30 text-[11px] font-bold flex items-center gap-1 cursor-pointer transition-colors"
                        >
                          <Bot className="w-3 h-3" />
                          <span className="hidden sm:inline">Prep Brief</span>
                        </button>

                        {evt.htmlLink && (
                          <a
                            href={evt.htmlLink}
                            target="_blank"
                            rel="noreferrer"
                            className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition-colors"
                            title="Open in Google Calendar"
                          >
                            <ExternalLink className="w-3.5 h-3.5" />
                          </a>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* Gmail Messages Section */}
        {(activeSubTab === 'overview' || activeSubTab === 'gmail') && (
          <div className="bg-[#091016] border border-slate-800 rounded-2xl p-5 space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <Mail className="w-4 h-4 text-rose-400" />
                <h3 className="text-sm font-bold text-white">Gmail Synced Inbox</h3>
                <span className="text-[11px] text-slate-500 font-mono">({filteredMails.length})</span>
              </div>

              {isAuthenticated && (
                <span className="text-[11px] text-rose-400 font-medium flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" />
                  <span>Synced</span>
                </span>
              )}
            </div>

            {filteredMails.length === 0 ? (
              <div className="py-5 text-center text-xs text-slate-500">
                {isAuthenticated 
                  ? 'No messages found in Inbox.' 
                  : 'Authorize Google Workspace above to sync incoming partner and client emails.'}
              </div>
            ) : (
              <div className="space-y-2">
                {filteredMails.map((msg) => (
                  <div 
                    key={msg.id} 
                    className="p-3 rounded-xl bg-[#060a08] border border-slate-800 hover:border-slate-700 transition-all flex items-center justify-between gap-3 text-xs"
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <div className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 ${
                        msg.unread 
                          ? 'bg-rose-500/20 text-rose-400 border border-rose-500/40 font-bold' 
                          : 'bg-slate-900 text-slate-400 border border-slate-800'
                      }`}>
                        <Mail className="w-3.5 h-3.5" />
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className={`truncate ${msg.unread ? 'font-black text-white' : 'font-semibold text-slate-200'}`}>
                            {msg.subject}
                          </span>
                          {msg.unread && (
                            <span className="px-1.5 py-0.2 rounded bg-rose-500/20 text-rose-400 text-[9px] font-bold uppercase">
                              Unread
                            </span>
                          )}
                        </div>
                        <p className="text-[11px] text-slate-400 truncate mt-0.5">
                          <strong className="text-slate-300">{msg.from}</strong>: {msg.snippet}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        onClick={() => handleDispatchAgentFromWorkspace('email', `${msg.from}: ${msg.subject}`)}
                        className="px-2.5 py-1 rounded-lg bg-[#76d418]/15 hover:bg-[#76d418]/25 text-[#76d418] border border-[#76d418]/30 text-[11px] font-bold flex items-center gap-1 cursor-pointer transition-colors"
                        title="Auto-draft reply via AI Workflow Engine"
                      >
                        <Zap className="w-3 h-3" />
                        <span className="hidden sm:inline">Auto-Triage</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Credentials & Access Token Modal */}
      {showSettingsModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="bg-[#091016] border border-slate-700/80 rounded-2xl w-full max-w-xl p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-amber-500/20 border border-amber-500/30 flex items-center justify-center text-amber-400">
                  <Key className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-white">Google Workspace Credentials</h3>
                  <p className="text-[11px] text-slate-400">OAuth 2.0 Client ID & Direct Access Token</p>
                </div>
              </div>
              <button
                onClick={() => setShowSettingsModal(false)}
                className="w-7 h-7 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white flex items-center justify-center text-xs cursor-pointer"
              >
                ✕
              </button>
            </div>

            {/* Option 1: Configured Client ID */}
            <div className="space-y-2 p-3.5 rounded-xl bg-slate-900/60 border border-slate-800">
              <label className="text-xs font-bold text-white flex items-center justify-between">
                <span>Google OAuth Client ID</span>
                <span className="text-[10px] text-slate-500 font-normal font-mono">Web Application</span>
              </label>
              <p className="text-[11px] text-slate-400">
                To use Google Identity Services directly, ensure this client ID is registered in Google Cloud Console under "APIs & Services &gt; Credentials" with authorized JavaScript origins matching this app domain.
              </p>
              <div className="flex items-center gap-2 mt-2">
                <input
                  type="text"
                  value={customClientIdInput}
                  onChange={(e) => setCustomClientIdInput(e.target.value)}
                  className="flex-1 px-3 py-2 rounded-lg bg-slate-950 border border-slate-700 focus:border-[#76d418] text-slate-200 text-xs font-mono focus:outline-none"
                  placeholder="e.g. 160087856875-...apps.googleusercontent.com"
                />
                <button
                  onClick={handleSaveCustomClientId}
                  className="px-3 py-2 rounded-lg bg-[#76d418] hover:bg-[#66bd14] text-slate-950 text-xs font-bold transition-colors cursor-pointer shrink-0"
                >
                  {clientIdSavedSuccess ? 'Saved!' : 'Save ID'}
                </button>
              </div>
              <div className="flex items-center justify-between text-[10px] text-slate-500 pt-1">
                <span>Current: {getGoogleClientId() === DEFAULT_GOOGLE_CLIENT_ID ? 'Default Client' : 'Custom Client'}</span>
                <button
                  type="button"
                  onClick={() => {
                    setCustomClientIdInput(DEFAULT_GOOGLE_CLIENT_ID);
                    setCustomGoogleClientId(DEFAULT_GOOGLE_CLIENT_ID);
                  }}
                  className="text-slate-400 hover:text-white underline cursor-pointer"
                >
                  Reset to {DEFAULT_GOOGLE_CLIENT_ID.slice(0, 15)}...
                </button>
              </div>
            </div>

            {/* Option 2: Direct Token Ingestion */}
            <div className="space-y-2 p-3.5 rounded-xl bg-slate-900/60 border border-slate-800">
              <label className="text-xs font-bold text-amber-300 flex items-center justify-between">
                <span>Direct OAuth 2.0 Access Token (Instant Sync)</span>
                <span className="text-[10px] text-amber-500 font-normal font-mono">Bypasses Popups</span>
              </label>
              <p className="text-[11px] text-slate-400">
                If Google blocks popups or returns 401 invalid_client, you can paste an access token (e.g. from Google OAuth Playground or <code className="text-slate-300 font-mono">gcloud auth print-access-token</code>) to immediately test live synchronization.
              </p>
              <div className="flex items-center gap-2 mt-2">
                <input
                  type="password"
                  value={manualTokenInput}
                  onChange={(e) => setManualTokenInput(e.target.value)}
                  className="flex-1 px-3 py-2 rounded-lg bg-slate-950 border border-slate-700 focus:border-amber-400 text-slate-200 text-xs font-mono focus:outline-none"
                  placeholder="ya29.a0AcM612..."
                />
                <button
                  onClick={() => handleApplyManualToken(manualTokenInput)}
                  disabled={!manualTokenInput.trim()}
                  className="px-3 py-2 rounded-lg bg-amber-500 hover:bg-amber-400 disabled:opacity-50 text-slate-950 text-xs font-bold transition-colors cursor-pointer shrink-0"
                >
                  Apply & Sync
                </button>
              </div>
            </div>

            {/* Action footer */}
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                onClick={() => setShowSettingsModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-bold transition-colors cursor-pointer"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};


