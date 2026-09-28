import React, { useState, useEffect } from 'react';
import {
  Mail,
  Send,
  Trash2,
  Star,
  RefreshCw,
  Search,
  Inbox,
  AlertCircle,
  CheckCircle2,
  CornerUpLeft,
  FileEdit,
  Sparkles,
  Bot,
  ExternalLink,
  ChevronLeft,
  X,
  Loader2,
  Key
} from 'lucide-react';
import {
  GmailMessageSnippet,
  GmailFullMessage,
  fetchGmailMessages,
  fetchGmailMessageDetails,
  sendGmailMessage,
  trashGmailMessage,
  markGmailAsRead,
  markGmailAsUnread,
  toggleStarGmailMessage,
  getCachedAccessToken,
  signInWithGoogleWorkspace,
  DEFAULT_GOOGLE_CLIENT_ID
} from '../services/googleWorkspace';
import { Agent } from '../types';

interface GmailViewProps {
  agents: Agent[];
  onDirectAgentTask: (agentId: string, task: string) => void;
  onCreateJobFromEmail?: (subject: string, sender: string, bodySnippet: string) => void;
  onOpenWorkspaceSetup?: () => void;
}

type MailboxFolder = 'INBOX' | 'SENT' | 'STARRED' | 'TRASH';

export const GmailView: React.FC<GmailViewProps> = ({
  agents,
  onDirectAgentTask,
  onCreateJobFromEmail,
  onOpenWorkspaceSetup
}) => {
  // Auth state
  const [accessToken, setAccessToken] = useState<string | null>(getCachedAccessToken());
  const [isAuthenticating, setIsAuthenticating] = useState<boolean>(false);

  // Mail state
  const [currentFolder, setCurrentFolder] = useState<MailboxFolder>('INBOX');
  const [messages, setMessages] = useState<GmailMessageSnippet[]>([]);
  const [selectedMessage, setSelectedMessage] = useState<GmailFullMessage | null>(null);
  const [isLoadingMessages, setIsLoadingMessages] = useState<boolean>(false);
  const [isLoadingDetails, setIsLoadingDetails] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [statusNotification, setStatusNotification] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);

  // Compose modal state
  const [isComposeOpen, setIsComposeOpen] = useState<boolean>(false);
  const [composeTo, setComposeTo] = useState<string>('');
  const [composeCc, setComposeCc] = useState<string>('');
  const [composeSubject, setComposeSubject] = useState<string>('');
  const [composeBody, setComposeBody] = useState<string>('');
  const [replyThreadId, setReplyThreadId] = useState<string | undefined>(undefined);
  const [isGeneratingAiDraft, setIsGeneratingAiDraft] = useState<boolean>(false);

  // Confirmation dialogs (MANDATORY per Workspace Integration skill)
  const [pendingConfirmation, setPendingConfirmation] = useState<{
    action: 'send' | 'trash';
    title: string;
    details: string[];
    onConfirm: () => Promise<void>;
  } | null>(null);

  const [isActionPending, setIsActionPending] = useState<boolean>(false);

  // Auto-clear notifications after 4 seconds
  useEffect(() => {
    if (statusNotification) {
      const timer = setTimeout(() => setStatusNotification(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [statusNotification]);

  // Load messages whenever token or folder changes
  useEffect(() => {
    const token = accessToken || getCachedAccessToken();
    if (token) {
      setAccessToken(token);
      loadFolderMessages(token, currentFolder);
    }
  }, [currentFolder, accessToken]);

  const showNotification = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
    setStatusNotification({ message, type });
  };

  const handleConnectGoogle = async () => {
    try {
      setIsAuthenticating(true);
      const res = await signInWithGoogleWorkspace();
      if (!res) {
        // User closed the popup, cancel gracefully
        return;
      }
      setAccessToken(res.accessToken);
      showNotification(`Connected as ${res.user.email}`, 'success');
      loadFolderMessages(res.accessToken, currentFolder);
    } catch (err: any) {
      if (err?.code !== 'auth/popup-closed-by-user') {
        console.error('Sign in error:', err);
        showNotification(err.message || 'Failed to authenticate with Google Workspace', 'error');
      }
    } finally {
      setIsAuthenticating(false);
    }
  };

  const loadFolderMessages = async (token: string, folder: MailboxFolder) => {
    try {
      setIsLoadingMessages(true);
      let labelIds: string[] | undefined = undefined;
      let q: string | undefined = undefined;

      if (folder === 'INBOX') {
        labelIds = ['INBOX'];
      } else if (folder === 'SENT') {
        labelIds = ['SENT'];
      } else if (folder === 'STARRED') {
        labelIds = ['STARRED'];
      } else if (folder === 'TRASH') {
        labelIds = ['TRASH'];
      }

      const fetched = await fetchGmailMessages(token, {
        maxResults: 25,
        labelIds,
        query: searchQuery.trim() || q
      });

      setMessages(fetched);
    } catch (err: any) {
      console.error('Failed to load Gmail messages:', err);
      showNotification(err.message || 'Could not load Gmail messages', 'error');
    } finally {
      setIsLoadingMessages(false);
    }
  };

  const handleSelectMessage = async (msg: GmailMessageSnippet) => {
    if (!accessToken) return;
    try {
      setIsLoadingDetails(true);
      // Automatically mark as read if unread
      if (msg.unread) {
        markGmailAsRead(accessToken, msg.id).catch(() => {});
        setMessages(prev => prev.map(m => m.id === msg.id ? { ...m, unread: false } : m));
      }

      const details = await fetchGmailMessageDetails(accessToken, msg.id);
      setSelectedMessage(details);
    } catch (err: any) {
      console.error('Error fetching message details:', err);
      showNotification('Could not load full message details', 'error');
    } finally {
      setIsLoadingDetails(false);
    }
  };

  const handleToggleStar = async (e: React.MouseEvent, msgId: string, currentStarred?: boolean) => {
    e.stopPropagation();
    if (!accessToken) return;
    const nextState = !currentStarred;
    try {
      await toggleStarGmailMessage(accessToken, msgId, !!currentStarred);
      setMessages(prev => prev.map(m => m.id === msgId ? { ...m, starred: nextState } : m));
      if (selectedMessage && selectedMessage.id === msgId) {
        setSelectedMessage(prev => prev ? { ...prev, starred: nextState } : null);
      }
      showNotification(nextState ? 'Starred email' : 'Unstarred email', 'info');
    } catch (err: any) {
      showNotification('Failed to update star', 'error');
    }
  };

  // Require explicit confirmation before Trashing an email
  const promptTrashEmail = (msg: GmailMessageSnippet | GmailFullMessage) => {
    if (!accessToken) return;
    setPendingConfirmation({
      action: 'trash',
      title: 'Confirm Moving Email to Trash',
      details: [
        `Sender: ${msg.from}`,
        `Subject: ${msg.subject}`,
        `Date: ${msg.date}`
      ],
      onConfirm: async () => {
        try {
          setIsActionPending(true);
          await trashGmailMessage(accessToken, msg.id);
          setMessages(prev => prev.filter(m => m.id !== msg.id));
          if (selectedMessage?.id === msg.id) {
            setSelectedMessage(null);
          }
          showNotification('Email moved to Trash', 'success');
        } catch (err: any) {
          showNotification(err.message || 'Failed to move email to trash', 'error');
        } finally {
          setIsActionPending(false);
          setPendingConfirmation(null);
        }
      }
    });
  };

  // Require explicit confirmation before Sending an email
  const promptSendEmail = () => {
    if (!composeTo.trim()) {
      showNotification('Recipient (To:) cannot be empty', 'error');
      return;
    }
    if (!composeSubject.trim()) {
      showNotification('Subject cannot be empty', 'error');
      return;
    }

    setPendingConfirmation({
      action: 'send',
      title: 'Confirm Outgoing Email Dispatch',
      details: [
        `To: ${composeTo}`,
        composeCc ? `Cc: ${composeCc}` : '',
        `Subject: ${composeSubject}`,
        `Preview: ${composeBody.slice(0, 100)}${composeBody.length > 100 ? '...' : ''}`
      ].filter(Boolean),
      onConfirm: async () => {
        if (!accessToken) return;
        try {
          setIsActionPending(true);
          await sendGmailMessage(accessToken, {
            to: composeTo,
            cc: composeCc || undefined,
            subject: composeSubject,
            body: composeBody,
            threadId: replyThreadId
          });
          showNotification('Email dispatched successfully via Gmail API', 'success');
          setIsComposeOpen(false);
          setComposeTo('');
          setComposeCc('');
          setComposeSubject('');
          setComposeBody('');
          setReplyThreadId(undefined);
          // Refresh sent / inbox
          loadFolderMessages(accessToken, currentFolder);
        } catch (err: any) {
          showNotification(err.message || 'Failed to send email', 'error');
        } finally {
          setIsActionPending(false);
          setPendingConfirmation(null);
        }
      }
    });
  };

  const handleOpenReply = (msg: GmailFullMessage) => {
    setComposeTo(msg.from);
    setComposeSubject(msg.subject.startsWith('Re:') ? msg.subject : `Re: ${msg.subject}`);
    setReplyThreadId(msg.threadId);
    setComposeBody(`\n\n--- Original Message ---\nFrom: ${msg.from}\nDate: ${msg.date}\n\n${msg.bodyText || msg.snippet}`);
    setIsComposeOpen(true);
  };

  const handleAiDraftResponse = (templateType: 'professional' | 'affirmation' | 'deliverable') => {
    setIsGeneratingAiDraft(true);
    const agent = agents[0] || { name: 'Nexus AI Specialist' };
    setTimeout(() => {
      let draft = '';
      if (templateType === 'professional') {
        draft = `Hello,\n\nThank you for following up. We have thoroughly reviewed the latest operational objectives and our team has scheduled immediate execution.\n\nPlease let us know if additional technical specifications or documentation are required.\n\nBest regards,\n${agent.name}\nEnterprise Autonomous Fleet Operations`;
      } else if (templateType === 'affirmation') {
        draft = `Hello,\n\nConfirmed and approved. We are proceeding with the proposed milestones as outlined. All deliverables are synchronized with our active enterprise workflow.\n\nRespectfully,\n${agent.name}`;
      } else {
        draft = `Hello,\n\nWe have ingested your request into our task management system. Deliverables are currently underway and automated status reports will follow shortly.\n\nBest,\n${agent.name}`;
      }

      setComposeBody(prev => draft + (prev.includes('--- Original Message ---') ? '\n\n' + prev.slice(prev.indexOf('--- Original Message ---')) : ''));
      setIsGeneratingAiDraft(false);
      showNotification('Generated executive draft with AI Agent', 'success');
    }, 400);
  };

  // Convert email to Fleet Job
  const handleConvertToJob = (msg: GmailFullMessage) => {
    if (onCreateJobFromEmail) {
      onCreateJobFromEmail(msg.subject, msg.from, msg.snippet || msg.bodyText?.slice(0, 160) || '');
      showNotification(`Created fleet job from email: "${msg.subject.slice(0, 30)}..."`, 'success');
    } else {
      const targetAgent = agents[0];
      if (targetAgent) {
        onDirectAgentTask(targetAgent.id, `Triage and resolve incoming email from ${msg.from}: "${msg.subject}"`);
        showNotification(`Dispatched email triage to ${targetAgent.name}`, 'success');
      }
    }
  };

  const filteredMessages = messages.filter(m => {
    const q = searchQuery.toLowerCase();
    if (!q) return true;
    return (
      m.subject.toLowerCase().includes(q) ||
      m.from.toLowerCase().includes(q) ||
      m.snippet.toLowerCase().includes(q)
    );
  });

  return (
    <div id="rcos-gmail-engine" className="space-y-4 max-w-5xl mx-auto pb-20 animate-in fade-in duration-150">
      {/* Notifications Toast */}
      {statusNotification && (
        <div 
          className={`p-3 rounded-xl border flex items-center justify-between text-xs font-semibold shadow-lg transition-all ${
            statusNotification.type === 'success'
              ? 'bg-[#76d418]/15 text-[#76d418] border-[#76d418]/40'
              : statusNotification.type === 'error'
              ? 'bg-rose-500/15 text-rose-300 border-rose-500/40'
              : 'bg-sky-500/15 text-sky-300 border-sky-500/40'
          }`}
        >
          <div className="flex items-center gap-2">
            {statusNotification.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 shrink-0" />
            ) : statusNotification.type === 'error' ? (
              <AlertCircle className="w-4 h-4 shrink-0" />
            ) : (
              <Mail className="w-4 h-4 shrink-0" />
            )}
            <span>{statusNotification.message}</span>
          </div>
          <button onClick={() => setStatusNotification(null)} className="opacity-70 hover:opacity-100">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Main Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-rose-500/20 border border-rose-500/30 flex items-center justify-center text-rose-400 shrink-0">
            <Mail className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold text-white tracking-tight">Gmail Communications Hub</h1>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-400 border border-rose-500/30">
                Gmail API
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Read, compose, send, and triage enterprise emails with autonomous AI integration
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2">
          {accessToken ? (
            <>
              <button
                id="btn-gmail-compose"
                onClick={() => {
                  setComposeTo('');
                  setComposeCc('');
                  setComposeSubject('');
                  setComposeBody('');
                  setReplyThreadId(undefined);
                  setIsComposeOpen(true);
                }}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 text-xs font-bold transition-all shadow cursor-pointer"
              >
                <FileEdit className="w-3.5 h-3.5" />
                <span>Compose</span>
              </button>

              <button
                id="btn-gmail-refresh"
                onClick={() => loadFolderMessages(accessToken, currentFolder)}
                disabled={isLoadingMessages}
                className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-semibold transition-all cursor-pointer"
                title="Refresh Messages"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isLoadingMessages ? 'animate-spin' : ''}`} />
                <span className="hidden sm:inline">Refresh</span>
              </button>
            </>
          ) : (
            <button
              id="btn-gmail-connect"
              onClick={handleConnectGoogle}
              disabled={isAuthenticating}
              className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 text-xs font-black shadow transition-all cursor-pointer"
            >
              {isAuthenticating ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <Mail className="w-4 h-4" />
              )}
              <span>{isAuthenticating ? 'Connecting...' : 'Authorize Gmail'}</span>
            </button>
          )}

          {onOpenWorkspaceSetup && (
            <button
              onClick={onOpenWorkspaceSetup}
              className="p-1.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-400 hover:text-white transition-colors cursor-pointer"
              title="Workspace Credentials Settings"
            >
              <Key className="w-4 h-4 text-amber-400" />
            </button>
          )}
        </div>
      </div>

      {/* Main Container: Mailbox Layout */}
      {!accessToken ? (
        <div className="bg-[#091016] border border-slate-800 rounded-2xl p-8 text-center space-y-4">
          <div className="w-12 h-12 rounded-2xl bg-rose-500/10 border border-rose-500/30 flex items-center justify-center text-rose-400 mx-auto">
            <Mail className="w-6 h-6" />
          </div>
          <div className="max-w-md mx-auto space-y-1">
            <h2 className="text-base font-bold text-white">Connect Your Gmail Account</h2>
            <p className="text-xs text-slate-400">
              Authorize Google Workspace to view your inbox, send emails, manage communications, and enable autonomous AI agent email triaging.
            </p>
          </div>
          <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
            <button
              onClick={handleConnectGoogle}
              disabled={isAuthenticating}
              className="px-5 py-2.5 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 text-xs font-black shadow-lg shadow-[#76d418]/15 cursor-pointer flex items-center gap-2 transition-all"
            >
              {isAuthenticating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Mail className="w-4 h-4" />}
              <span>Authorize with Google</span>
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
          {/* Left Folder Nav & Search (4 columns on desktop) */}
          <div className="lg:col-span-4 space-y-3">
            {/* Folder Tabs */}
            <div className="bg-[#091016] border border-slate-800 rounded-2xl p-2 flex lg:flex-col gap-1 overflow-x-auto">
              <button
                onClick={() => { setCurrentFolder('INBOX'); setSelectedMessage(null); }}
                className={`flex-1 lg:w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  currentFolder === 'INBOX'
                    ? 'bg-[#76d418]/15 text-[#76d418] border border-[#76d418]/30'
                    : 'text-slate-400 hover:text-white hover:bg-slate-900/60'
                }`}
              >
                <div className="flex items-center gap-2">
                  <Inbox className="w-4 h-4" />
                  <span>Inbox</span>
                </div>
              </button>

              <button
                onClick={() => { setCurrentFolder('SENT'); setSelectedMessage(null); }}
                className={`flex-1 lg:w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  currentFolder === 'SENT'
                    ? 'bg-[#76d418]/15 text-[#76d418] border border-[#76d418]/30'
                    : 'text-slate-400 hover:text-white hover:bg-slate-900/60'
                }`}
              >
                <div className="flex items-center gap-2">
                  <Send className="w-4 h-4" />
                  <span>Sent</span>
                </div>
              </button>

              <button
                onClick={() => { setCurrentFolder('STARRED'); setSelectedMessage(null); }}
                className={`flex-1 lg:w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  currentFolder === 'STARRED'
                    ? 'bg-amber-500/15 text-amber-400 border border-amber-500/30'
                    : 'text-slate-400 hover:text-white hover:bg-slate-900/60'
                }`}
              >
                <div className="flex items-center gap-2">
                  <Star className="w-4 h-4" />
                  <span>Starred</span>
                </div>
              </button>

              <button
                onClick={() => { setCurrentFolder('TRASH'); setSelectedMessage(null); }}
                className={`flex-1 lg:w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  currentFolder === 'TRASH'
                    ? 'bg-rose-500/15 text-rose-400 border border-rose-500/30'
                    : 'text-slate-400 hover:text-white hover:bg-slate-900/60'
                }`}
              >
                <div className="flex items-center gap-2">
                  <Trash2 className="w-4 h-4" />
                  <span>Trash</span>
                </div>
              </button>
            </div>

            {/* Search Box */}
            <div className="relative">
              <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    loadFolderMessages(accessToken, currentFolder);
                  }
                }}
                placeholder="Search messages..."
                className="w-full pl-9 pr-8 py-2 bg-[#091016] border border-slate-800 focus:border-[#76d418] rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none transition-colors"
              />
              {searchQuery && (
                <button
                  onClick={() => { setSearchQuery(''); loadFolderMessages(accessToken, currentFolder); }}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            {/* Message List */}
            <div className="bg-[#091016] border border-slate-800 rounded-2xl p-2 max-h-[600px] overflow-y-auto space-y-1.5">
              {isLoadingMessages ? (
                <div className="py-12 text-center text-xs text-slate-400 space-y-2">
                  <Loader2 className="w-5 h-5 animate-spin mx-auto text-[#76d418]" />
                  <p>Fetching messages from Gmail...</p>
                </div>
              ) : filteredMessages.length === 0 ? (
                <div className="py-10 text-center text-xs text-slate-500 space-y-1">
                  <p>No messages found in {currentFolder}.</p>
                  {searchQuery && <p className="text-[11px] text-slate-600">Try adjusting your search filter.</p>}
                </div>
              ) : (
                filteredMessages.map((msg) => {
                  const isSelected = selectedMessage?.id === msg.id;
                  return (
                    <div
                      key={msg.id}
                      onClick={() => handleSelectMessage(msg)}
                      className={`p-2.5 rounded-xl border text-xs cursor-pointer transition-all ${
                        isSelected
                          ? 'bg-[#0e171b] border-[#76d418]/60 shadow-md'
                          : msg.unread
                          ? 'bg-[#070e0a] border-slate-700/70 hover:border-slate-600'
                          : 'bg-[#060a08] border-slate-800/80 hover:border-slate-700 opacity-90'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-1.5 mb-1">
                        <span className={`truncate text-xs ${msg.unread ? 'font-bold text-white' : 'font-medium text-slate-300'}`}>
                          {msg.from.split('<')[0].replace(/"/g, '')}
                        </span>
                        <div className="flex items-center gap-1 shrink-0">
                          <button
                            onClick={(e) => handleToggleStar(e, msg.id, msg.starred)}
                            className="p-1 text-slate-500 hover:text-amber-400 transition-colors"
                            title={msg.starred ? 'Unstar' : 'Star'}
                          >
                            <Star className={`w-3.5 h-3.5 ${msg.starred ? 'fill-amber-400 text-amber-400' : ''}`} />
                          </button>
                          <span className="text-[10px] text-slate-500 font-mono">
                            {msg.date ? new Date(msg.date).toLocaleDateString([], { month: 'short', day: 'numeric' }) : ''}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-1.5">
                        {msg.unread && (
                          <span className="w-1.5 h-1.5 rounded-full bg-rose-500 shrink-0" />
                        )}
                        <p className={`truncate text-[11px] ${msg.unread ? 'font-semibold text-slate-100' : 'text-slate-300'}`}>
                          {msg.subject || '(No Subject)'}
                        </p>
                      </div>

                      <p className="text-[11px] text-slate-400 line-clamp-1 mt-0.5">
                        {msg.snippet}
                      </p>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* Right Message Details / Preview Area (8 columns on desktop) */}
          <div className="lg:col-span-8 bg-[#091016] border border-slate-800 rounded-2xl p-5 min-h-[500px] flex flex-col">
            {isLoadingDetails ? (
              <div className="flex-1 flex flex-col items-center justify-center text-xs text-slate-400 space-y-2">
                <Loader2 className="w-6 h-6 animate-spin text-[#76d418]" />
                <p>Loading message details...</p>
              </div>
            ) : selectedMessage ? (
              <div className="space-y-4 flex-1 flex flex-col">
                {/* Header of selected message */}
                <div className="border-b border-slate-800 pb-4 space-y-3">
                  <div className="flex items-start justify-between gap-3">
                    <h2 className="text-base font-bold text-white">
                      {selectedMessage.subject || '(No Subject)'}
                    </h2>
                    
                    <div className="flex items-center gap-1.5 shrink-0">
                      <button
                        onClick={() => handleOpenReply(selectedMessage)}
                        className="px-2.5 py-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-200 text-xs font-semibold flex items-center gap-1 cursor-pointer transition-colors"
                        title="Reply to sender"
                      >
                        <CornerUpLeft className="w-3.5 h-3.5 text-[#76d418]" />
                        <span>Reply</span>
                      </button>

                      <button
                        onClick={() => promptTrashEmail(selectedMessage)}
                        className="p-1.5 rounded-lg bg-slate-900 hover:bg-rose-950/40 border border-slate-800 hover:border-rose-500/40 text-slate-400 hover:text-rose-400 transition-colors cursor-pointer"
                        title="Move to Trash"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  {/* Sender & Date Info */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between text-xs text-slate-400 gap-1 bg-[#060a08] p-3 rounded-xl border border-slate-800/80">
                    <div>
                      <p className="text-white font-medium">
                        From: <span className="text-slate-300">{selectedMessage.from}</span>
                      </p>
                      <p className="text-slate-400 text-[11px]">
                        To: {selectedMessage.to}
                        {selectedMessage.cc && ` • Cc: ${selectedMessage.cc}`}
                      </p>
                    </div>
                    <span className="text-[11px] text-slate-500 font-mono">
                      {selectedMessage.date}
                    </span>
                  </div>

                  {/* AI Agent Action Bar */}
                  <div className="flex flex-wrap items-center gap-2 pt-1">
                    <span className="text-[11px] font-bold text-slate-400 flex items-center gap-1 mr-1">
                      <Bot className="w-3.5 h-3.5 text-[#76d418]" />
                      <span>Agent Actions:</span>
                    </span>

                    <button
                      onClick={() => handleConvertToJob(selectedMessage)}
                      className="px-2.5 py-1 rounded-lg bg-[#76d418]/15 hover:bg-[#76d418]/25 text-[#76d418] border border-[#76d418]/30 text-[11px] font-bold flex items-center gap-1 cursor-pointer transition-colors"
                    >
                      <Sparkles className="w-3 h-3" />
                      <span>Convert to Fleet Task</span>
                    </button>

                    <button
                      onClick={() => {
                        handleOpenReply(selectedMessage);
                        handleAiDraftResponse('professional');
                      }}
                      className="px-2.5 py-1 rounded-lg bg-sky-500/15 hover:bg-sky-500/25 text-sky-400 border border-sky-500/30 text-[11px] font-bold flex items-center gap-1 cursor-pointer transition-colors"
                    >
                      <CornerUpLeft className="w-3 h-3" />
                      <span>Auto-Draft Response</span>
                    </button>
                  </div>
                </div>

                {/* Email Body Content */}
                <div className="flex-1 overflow-y-auto bg-[#060a08] p-4 rounded-xl border border-slate-800 text-xs leading-relaxed text-slate-200 min-h-[220px]">
                  {selectedMessage.bodyHtml ? (
                    <div 
                      className="prose prose-invert prose-xs max-w-none break-words"
                      dangerouslySetInnerHTML={{ __html: selectedMessage.bodyHtml }}
                    />
                  ) : selectedMessage.bodyText ? (
                    <pre className="whitespace-pre-wrap font-sans text-xs text-slate-200">
                      {selectedMessage.bodyText}
                    </pre>
                  ) : (
                    <p className="text-slate-400 italic">
                      {selectedMessage.snippet || '(No body content)'}
                    </p>
                  )}
                </div>
              </div>
            ) : (
              <div className="flex-1 flex flex-col items-center justify-center text-center text-slate-500 space-y-3">
                <div className="w-12 h-12 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-600">
                  <Mail className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-slate-300">No Message Selected</h3>
                  <p className="text-xs text-slate-500 mt-1 max-w-xs">
                    Choose an email from the left sidebar to preview content, draft replies, or convert into autonomous deliverables.
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Compose Email Modal */}
      {isComposeOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="bg-[#091016] border border-slate-700/80 rounded-2xl w-full max-w-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
            {/* Modal Header */}
            <div className="p-4 border-b border-slate-800 flex items-center justify-between bg-[#060a08]">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-lg bg-[#76d418]/20 border border-[#76d418]/30 flex items-center justify-center text-[#76d418]">
                  <FileEdit className="w-4 h-4" />
                </div>
                <h3 className="text-sm font-bold text-white">
                  {replyThreadId ? 'Reply to Email' : 'Compose New Message'}
                </h3>
              </div>
              <button
                onClick={() => setIsComposeOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Modal Form */}
            <div className="p-4 space-y-3 flex-1 overflow-y-auto">
              <div>
                <label className="text-[11px] font-bold text-slate-400 block mb-1">To:</label>
                <input
                  type="email"
                  value={composeTo}
                  onChange={(e) => setComposeTo(e.target.value)}
                  placeholder="recipient@example.com"
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 focus:border-[#76d418] rounded-xl text-xs text-white focus:outline-none transition-colors"
                />
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-400 block mb-1">Cc (optional):</label>
                <input
                  type="text"
                  value={composeCc}
                  onChange={(e) => setComposeCc(e.target.value)}
                  placeholder="cc@example.com"
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 focus:border-[#76d418] rounded-xl text-xs text-white focus:outline-none transition-colors"
                />
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-400 block mb-1">Subject:</label>
                <input
                  type="text"
                  value={composeSubject}
                  onChange={(e) => setComposeSubject(e.target.value)}
                  placeholder="Executive brief..."
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 focus:border-[#76d418] rounded-xl text-xs text-white focus:outline-none transition-colors"
                />
              </div>

              {/* AI Agent Draft Helpers */}
              <div className="p-2.5 rounded-xl bg-slate-900/60 border border-slate-800 flex flex-wrap items-center gap-2">
                <span className="text-[11px] font-bold text-[#76d418] flex items-center gap-1">
                  <Bot className="w-3.5 h-3.5" />
                  <span>Agent Auto-Draft:</span>
                </span>
                <button
                  type="button"
                  onClick={() => handleAiDraftResponse('professional')}
                  disabled={isGeneratingAiDraft}
                  className="px-2 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-[10px] font-bold transition-colors cursor-pointer"
                >
                  Executive Synthesis
                </button>
                <button
                  type="button"
                  onClick={() => handleAiDraftResponse('affirmation')}
                  disabled={isGeneratingAiDraft}
                  className="px-2 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-[10px] font-bold transition-colors cursor-pointer"
                >
                  Confirm & Approve
                </button>
                <button
                  type="button"
                  onClick={() => handleAiDraftResponse('deliverable')}
                  disabled={isGeneratingAiDraft}
                  className="px-2 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-[10px] font-bold transition-colors cursor-pointer"
                >
                  Task Ingestion Notice
                </button>
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-400 block mb-1">Message Body:</label>
                <textarea
                  rows={8}
                  value={composeBody}
                  onChange={(e) => setComposeBody(e.target.value)}
                  placeholder="Type your message or use AI auto-draft..."
                  className="w-full p-3 bg-slate-950 border border-slate-800 focus:border-[#76d418] rounded-xl text-xs text-white focus:outline-none transition-colors leading-relaxed font-sans"
                />
              </div>
            </div>

            {/* Modal Footer */}
            <div className="p-4 border-t border-slate-800 bg-[#060a08] flex items-center justify-between">
              <span className="text-[11px] text-slate-500">
                Requires user confirmation before sending via Gmail API
              </span>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => setIsComposeOpen(false)}
                  className="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors cursor-pointer"
                >
                  Cancel
                </button>

                <button
                  onClick={promptSendEmail}
                  className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 text-xs font-black shadow transition-all cursor-pointer"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>Send Email...</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Dialog Modal (MANDATORY per Workspace Integration Skill) */}
      {pendingConfirmation && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="bg-[#091016] border border-slate-700 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <div className="flex items-center gap-3">
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                pendingConfirmation.action === 'trash'
                  ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                  : 'bg-[#76d418]/20 text-[#76d418] border border-[#76d418]/30'
              }`}>
                {pendingConfirmation.action === 'trash' ? (
                  <Trash2 className="w-5 h-5" />
                ) : (
                  <Send className="w-5 h-5" />
                )}
              </div>
              <div>
                <h3 className="text-sm font-bold text-white">
                  {pendingConfirmation.title}
                </h3>
                <p className="text-[11px] text-slate-400">
                  Please review the details before confirming this action
                </p>
              </div>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 text-xs space-y-1.5">
              {pendingConfirmation.details.map((line, idx) => (
                <p key={idx} className="text-slate-300 break-words">
                  {line}
                </p>
              ))}
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                onClick={() => setPendingConfirmation(null)}
                disabled={isActionPending}
                className="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors cursor-pointer"
              >
                Cancel
              </button>

              <button
                onClick={() => pendingConfirmation.onConfirm()}
                disabled={isActionPending}
                className={`flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-black shadow transition-all cursor-pointer ${
                  pendingConfirmation.action === 'trash'
                    ? 'bg-rose-500 hover:bg-rose-600 text-white'
                    : 'bg-[#76d418] hover:bg-[#66bd14] text-slate-950'
                }`}
              >
                {isActionPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                <span>
                  {pendingConfirmation.action === 'trash' ? 'Confirm Move to Trash' : 'Confirm & Send Email'}
                </span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
