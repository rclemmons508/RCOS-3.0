import React, { useState } from 'react';
import { Header } from './components/Header';
import { BottomNav, MainNavTab } from './components/BottomNav';
import { MoreMenuModal } from './components/MoreMenuModal';
import { EnterpriseSetupModal, EnterpriseProfile } from './components/EnterpriseSetupModal';
import { DashboardView } from './components/DashboardView';
import { AgentsView } from './components/AgentsView';
import { WorkflowsView } from './components/WorkflowsView';
import { JobsView } from './components/JobsView';
import { ClientsView } from './components/ClientsView';
import { PhoneView } from './components/PhoneView';
import { VoiceTranscriberView } from './components/VoiceTranscriberView';
import { GeminiChatView } from './components/GeminiChatView';
import { DeepThinkingView } from './components/DeepThinkingView';
import { CalendarView } from './components/CalendarView';
import { IntelligenceVaultView } from './components/IntelligenceVaultView';
import { GovernanceView } from './components/GovernanceView';
import { ApkCenterView } from './components/ApkCenterView';
import { CreateAgentModal } from './components/CreateAgentModal';
import { WorkspaceSyncView } from './components/WorkspaceSyncView';
import { GmailView } from './components/GmailView';
import { TeamMessagingView } from './components/TeamMessagingView';
import { employeeMessagingService } from './services/employeeMessagingService';
import { GoogleCalendarEvent, GooglePickerDocument } from './services/googleWorkspace';
import { auth, googleProvider, setManualAccessToken } from './services/firebaseAuth';
import { signInWithPopup, signOut, onAuthStateChanged, GoogleAuthProvider, User } from 'firebase/auth';
import { userDataService } from './services/userDataService';

import { 
  INITIAL_AGENTS, 
  INITIAL_WORKFLOWS, 
  INITIAL_JOBS, 
  INITIAL_CLIENTS, 
  INITIAL_CALL_RECORDS, 
  INITIAL_INTELLIGENCE, 
  INITIAL_AUDIT_LOGS 
} from './data/mockData';
import { Agent, Workflow, Job, Client, CallRecord, AuditLog, JobStatus } from './types';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('dashboard');
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState<boolean>(false);
  const [isSetupModalOpen, setIsSetupModalOpen] = useState<boolean>(false);
  const [isCreateAgentModalOpen, setIsCreateAgentModalOpen] = useState<boolean>(false);

  // Enterprise Workspace Profile
  const [enterpriseProfile, setEnterpriseProfile] = useState<EnterpriseProfile>({
    name: 'RC Solutions',
    domain: 'rcos.global',
    headcount: '50-250 Employees',
    industry: 'Technology & Software',
    bottleneck: 'Autonomous enterprise workflows and multi-agent operations'
  });

  // Employee Messaging Unread Count
  const [unreadMsgCount, setUnreadMsgCount] = useState<number>(() => employeeMessagingService.getUnreadCount());

  // Firebase Auth State
  const [currentUser, setCurrentUser] = useState<User | null>(null);

  React.useEffect(() => {
    const unsubAuth = onAuthStateChanged(auth, async (user) => {
      setCurrentUser(user);
      if (user) {
        try {
          await userDataService.saveUserProfile(user);
        } catch (e) {
          console.warn('Could not save user profile to Firestore:', e);
        }
      }
    });
    return () => unsubAuth();
  }, []);

  React.useEffect(() => {
    const updateUnread = () => {
      setUnreadMsgCount(employeeMessagingService.getUnreadCount());
    };
    const unsub = employeeMessagingService.subscribe(updateUnread);
    return unsub;
  }, []);

  // Core State (No fictional companies or fake employees!)
  const [agents, setAgents] = useState<Agent[]>(INITIAL_AGENTS);
  const [workflows, setWorkflows] = useState<Workflow[]>(INITIAL_WORKFLOWS);
  const [jobs, setJobs] = useState<Job[]>(INITIAL_JOBS);
  const [clients, setClients] = useState<Client[]>(INITIAL_CLIENTS);
  const [calls, setCalls] = useState<CallRecord[]>(INITIAL_CALL_RECORDS);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>(INITIAL_AUDIT_LOGS);

  // Firestore Jobs & Calls Persistence Synchronization
  React.useEffect(() => {
    if (!currentUser) return;
    const unsubJobs = userDataService.subscribeToJobs(currentUser.uid, (firestoreJobs) => {
      if (firestoreJobs && firestoreJobs.length > 0) {
        setJobs(firestoreJobs);
      } else {
        // Seed initial jobs to Firestore for this authenticated user
        INITIAL_JOBS.forEach((j) => {
          userDataService.saveJob(currentUser.uid, j);
        });
      }
    });

    const unsubCalls = userDataService.subscribeToCalls(currentUser.uid, (firestoreCalls) => {
      if (firestoreCalls && firestoreCalls.length > 0) {
        setCalls(firestoreCalls);
      }
    });

    return () => {
      unsubJobs();
      unsubCalls();
    };
  }, [currentUser]);

  const handleGoogleSignIn = async () => {
    try {
      const result = await signInWithPopup(auth, googleProvider);
      // Publish the consented token into the shared Workspace session so the
      // Gmail hub, Drive sync and Calendar work off this single sign-in.
      const credential = GoogleAuthProvider.credentialFromResult(result);
      if (credential?.accessToken) {
        setManualAccessToken(credential.accessToken);
      }
    } catch (err) {
      console.error('Sign-in failed:', err);
    }
  };

  const handleSignOut = async () => {
    try {
      await signOut(auth);
    } catch (err) {
      console.error('Sign-out failed:', err);
    }
  };

  // Dynamic Handlers
  const handleToggleAgentStatus = (agentId: string) => {
    setAgents(prev => prev.map(agent => {
      if (agent.id === agentId) {
        const nextStatus = agent.status === 'active' || agent.status === 'executing' ? 'offline' : 'active';
        return {
          ...agent,
          status: nextStatus,
          lastActive: 'Just now'
        };
      }
      return agent;
    }));

    const agent = agents.find(a => a.id === agentId);
    if (agent) {
      logAction('TOGGLE_AGENT_STATUS', `Updated ${agent.name} status`);
    }
  };

  const handleDirectAgentTask = (agentId: string, taskDescription: string) => {
    setAgents(prev => prev.map(agent => {
      if (agent.id === agentId) {
        return {
          ...agent,
          status: 'executing',
          currentTask: taskDescription,
          tasksCompleted: agent.tasksCompleted + 1,
          lastActive: 'Just now'
        };
      }
      return agent;
    }));

    const targetAgent = agents.find(a => a.id === agentId);
    const newJob: Job = {
      id: `job-${Date.now()}`,
      title: taskDescription,
      clientName: enterpriseProfile.name || 'Internal Operations',
      assignedAgent: targetAgent ? targetAgent.name : 'AI Workflow Engine',
      priority: 'High',
      status: 'In Progress',
      dueDate: 'Today',
      approvalRequired: false,
      isApproved: true,
      progress: 0.25,
      summary: `Dispatched direct workload to ${targetAgent?.name || 'Agent'}.`,
      budget: '$200 compute',
      tags: ['Direct Dispatch', 'Live']
    };
    setJobs(prev => [newJob, ...prev]);

    logAction('DIRECT_AGENT_TASK', `Dispatched workload to ${targetAgent?.name}: "${taskDescription}"`);
  };

  const handleTriggerWorkflow = (workflowId: string) => {
    setWorkflows(prev => prev.map(wf => {
      if (wf.id === workflowId) {
        return {
          ...wf,
          totalExecutions: wf.totalExecutions + 1,
          lastRun: 'Just now'
        };
      }
      return wf;
    }));

    const wf = workflows.find(w => w.id === workflowId);
    logAction('TRIGGER_WORKFLOW', `Executed pipeline: ${wf?.title || workflowId}`);
  };

  const handleCreateWorkflow = (newWf: Partial<Workflow>) => {
    const fullWf: Workflow = {
      id: `wf-${Date.now()}`,
      title: newWf.title || 'Enterprise Multi-Agent Pipeline',
      category: newWf.category || 'Operations',
      description: newWf.description || 'Automated multi-agent execution pipeline.',
      assignedAgent: newWf.assignedAgent || 'AI Workflow Engine',
      trigger: {
        type: 'THRESHOLD_EVENT',
        name: 'Event Gateway Listener',
        description: 'Fired on continuous system telemetry threshold',
        isActive: true
      },
      approvalPolicy: {
        level: 'AUTO_APPROVE',
        riskThresholdPct: 20,
        approverRole: 'Compliance Officer'
      },
      steps: newWf.steps || [
        { stepNumber: 1, title: 'Extract operational payload', actionType: 'INGEST', assignedAgent: 'AI Workflow Engine', isCompleted: true },
        { stepNumber: 2, title: 'Execute verification check', actionType: 'VERIFY', assignedAgent: 'Compliance & Security Audit', isCompleted: false }
      ],
      isActive: true,
      totalExecutions: 1,
      lastRun: 'Just now'
    };

    setWorkflows(prev => [fullWf, ...prev]);
    logAction('CREATE_WORKFLOW', `Created pipeline: ${fullWf.title}`);
  };

  const handleUpdateJobStatus = (jobId: string, status: JobStatus) => {
    setJobs(prev => prev.map(job => {
      if (job.id === jobId) {
        const updated = {
          ...job,
          status,
          progress: status === 'Completed' ? 1.0 : job.progress
        };
        if (currentUser) {
          userDataService.saveJob(currentUser.uid, updated);
        }
        return updated;
      }
      return job;
    }));

    logAction('UPDATE_JOB_STATUS', `Updated deliverable #${jobId} to ${status}`);
  };

  const handleCreateJob = (job: Partial<Job>) => {
    const newJob: Job = {
      id: `job-${Date.now()}`,
      title: job.title || 'New Deliverable',
      clientName: job.clientName || enterpriseProfile.name || 'Internal Operations',
      assignedAgent: job.assignedAgent || 'AI Workflow Engine',
      priority: job.priority || 'Normal',
      status: job.status || 'Pending',
      dueDate: job.dueDate || 'Today',
      approvalRequired: !!job.approvalRequired,
      isApproved: job.isApproved !== undefined ? job.isApproved : true,
      progress: job.progress || 0.0,
      summary: job.summary || 'Enterprise deliverable initialized.',
      budget: job.budget || '$500',
      tags: job.tags || ['Task']
    };

    setJobs(prev => [newJob, ...prev]);
    if (currentUser) {
      userDataService.saveJob(currentUser.uid, newJob);
    }
    logAction('CREATE_JOB', `Created task: ${newJob.title}`);
  };

  const handleCreateClient = (client: Partial<Client>) => {
    const newClient: Client = {
      id: `client-${Date.now()}`,
      companyName: client.companyName || 'New Client',
      industry: client.industry || 'Technology',
      accountEmail: client.accountEmail || 'contact@client.com',
      phone: client.phone || '+1 (555) 000-0000',
      status: client.status || 'Active Enterprise',
      headquarters: client.headquarters || 'Corporate HQ',
      contractValue: client.contractValue || '$50,000 / yr',
      onboardingDate: client.onboardingDate || 'Sep 2026',
      primaryContact: client.primaryContact || {
        name: 'Primary Contact',
        role: 'Operations Lead',
        email: 'contact@client.com',
        phone: '+1 (555) 000-0000',
        preferredChannel: 'Email'
      },
      assignedAgent: client.assignedAgent || 'Compliance & Security Audit',
      notes: client.notes || 'Registered in RCOS enterprise directory.',
      activeJobsCount: 0
    };

    setClients(prev => [newClient, ...prev]);
    logAction('CREATE_CLIENT', `Registered client organization: ${newClient.companyName}`);
  };

  const handleLogCall = (call: CallRecord) => {
    setCalls(prev => [call, ...prev.filter(c => c.id !== call.id)]);
    if (currentUser) {
      userDataService.saveCall(currentUser.uid, call);
    }
    logAction('VOIP_CALL_LOGGED', `Recorded call with ${call.phoneNumber} (${call.duration}) - Answered by: ${call.answeredBy || 'ai'}`);
  };

  const handleDeleteCall = (callId: string) => {
    setCalls(prev => prev.filter(c => c.id !== callId));
    if (currentUser) {
      userDataService.removeCall(callId);
    }
    logAction('VOIP_CALL_DELETED', `Removed call record #${callId}`);
  };

  const handleProvisionAgent = (newAgentData: Partial<Agent>) => {
    const newAgent: Agent = {
      id: `agent-${Date.now()}`,
      name: newAgentData.name || 'Nexus Agent',
      codeName: newAgentData.codeName || 'NEXUS-01',
      role: newAgentData.role || 'Autonomous Specialist',
      department: newAgentData.department || 'Executive Operations',
      modelTier: newAgentData.modelTier || 'gemini-3.7-flash',
      riskClassification: newAgentData.riskClassification || 'Low',
      permissionLevel: newAgentData.permissionLevel || 'Regular Staff',
      capabilityProfile: newAgentData.capabilityProfile || 'Autonomous enterprise agent.',
      status: 'active',
      tasksCompleted: 0,
      uptime: '100%',
      lastActive: 'Just now',
      avatarSeed: (newAgentData.name || 'nexus').toLowerCase()
    };

    setAgents(prev => [newAgent, ...prev]);
    logAction('PROVISION_AGENT', `Commissioned autonomous agent: ${newAgent.name}`);
  };

  const logAction = (action: string, details: string) => {
    const newLog: AuditLog = {
      id: `log-${Date.now()}`,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      actor: 'Executive Director',
      action,
      resource: 'RCOS Web Kernel',
      status: 'SUCCESS',
      ipAddress: '127.0.0.1',
      details
    };
    setAuditLogs(prev => [newLog, ...prev]);
  };

  const handleSelectBottomTab = (tab: MainNavTab) => {
    if (tab === 'more') {
      setIsMoreMenuOpen(true);
    } else {
      setActiveTab(tab);
    }
  };

  return (
    <div 
      id="rcos-applet-root" 
      className="min-h-screen bg-[#060b08] text-slate-100 flex flex-col font-sans selection:bg-[#76d418]/30 selection:text-[#76d418]"
      style={{
        backgroundColor: '#060b08'
      }}
    >
      {/* Top Header matching Screenshot 1 */}
      <Header
        user={currentUser}
        onSignIn={handleGoogleSignIn}
        onSignOut={handleSignOut}
        onOpenSetup={() => setIsSetupModalOpen(true)}
        onRefresh={() => logAction('AUTO_SYNC', 'Synchronized real-time agent telemetry')}
        onOpenWorkspaceSync={() => setActiveTab('workspace')}
        onOpenMessaging={() => setActiveTab('messaging')}
        unreadMessagesCount={unreadMsgCount}
      />

      {/* Main Viewport */}
      <main className="flex-1 overflow-y-auto px-3 sm:px-4 py-3 sm:py-4">
        <div className="max-w-4xl mx-auto">
          {/* Breadcrumb / Back button when deep in a sub-view */}
          {activeTab !== 'dashboard' && (
            <div className="mb-3 flex items-center justify-between">
              <button
                onClick={() => setActiveTab('dashboard')}
                className="text-xs font-bold text-[#76d418] hover:underline flex items-center gap-1.5 cursor-pointer py-1 px-2 rounded-lg bg-[#0a150c] border border-[#76d418]/30"
              >
                <span>← Return to Dashboard</span>
              </button>

              <span className="text-xs font-semibold text-slate-400 capitalize">
                {activeTab} Module
              </span>
            </div>
          )}

          {activeTab === 'dashboard' && (
            <DashboardView
              agents={agents}
              jobs={jobs}
              workflows={workflows}
              calls={calls}
              orgName={enterpriseProfile.name}
              onNavigate={(tab) => {
                if (tab === 'more') {
                  setIsMoreMenuOpen(true);
                } else {
                  setActiveTab(tab);
                }
              }}
              onSelectAgent={() => setActiveTab('agents')}
              onDirectTask={handleDirectAgentTask}
              onOpenSetup={() => setIsSetupModalOpen(true)}
            />
          )}

          {activeTab === 'agents' && (
            <AgentsView
              agents={agents}
              onToggleStatus={handleToggleAgentStatus}
              onDirectTask={handleDirectAgentTask}
              onOpenCreateModal={() => setIsCreateAgentModalOpen(true)}
            />
          )}

          {activeTab === 'workflows' && (
            <WorkflowsView
              workflows={workflows}
              onTriggerWorkflow={handleTriggerWorkflow}
              onCreateWorkflow={handleCreateWorkflow}
            />
          )}

          {activeTab === 'jobs' && (
            <JobsView
              jobs={jobs}
              orgName={enterpriseProfile.name}
              onUpdateJobStatus={handleUpdateJobStatus}
              onCreateJob={handleCreateJob}
              onOpenMessaging={() => setActiveTab('messaging')}
            />
          )}

          {activeTab === 'clients' && (
            <ClientsView
              clients={clients}
              jobs={jobs}
              onCreateClient={handleCreateClient}
            />
          )}

          {activeTab === 'phone' && (
            <PhoneView
              calls={calls}
              orgName={enterpriseProfile.name}
              onLogCall={handleLogCall}
              onDeleteCall={handleDeleteCall}
              onCreateJobFromCall={(job) => {
                handleCreateJob(job);
                setActiveTab('jobs');
              }}
            />
          )}

          {activeTab === 'transcribe' && (
            <VoiceTranscriberView
              agents={agents}
              onDirectAgentTask={handleDirectAgentTask}
              onNavigate={(tab) => setActiveTab(tab)}
              onCreateJobFromVoice={(title, summary) => {
                handleCreateJob({
                  title,
                  summary,
                  priority: 'High',
                  status: 'In Progress'
                });
                setActiveTab('jobs');
              }}
            />
          )}

          {activeTab === 'chat' && (
            <GeminiChatView
              agents={agents}
            />
          )}

          {activeTab === 'reasoning' && (
            <DeepThinkingView />
          )}

          {activeTab === 'workspace' && (
            <WorkspaceSyncView
              agents={agents}
              onDirectTask={handleDirectAgentTask}
              onOpenGmailHub={() => setActiveTab('gmail')}
              onImportCalendarEvent={(evt: GoogleCalendarEvent) => {
                const startTime = evt.start.dateTime 
                  ? new Date(evt.start.dateTime).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })
                  : evt.start.date || 'Today';

                handleCreateJob({
                  title: `Google Calendar: ${evt.summary || 'Scheduled Meeting'}`,
                  summary: `Synced from Google Calendar for ${startTime}. Location: ${evt.location || 'Online'}. ${evt.attendees?.length || 0} attendees invited.`,
                  priority: 'High',
                  status: 'In Progress'
                });
                logAction('CALENDAR_SYNC', `Imported calendar milestone: "${evt.summary || 'Meeting'}" into active fleet jobs`);
                setActiveTab('jobs');
              }}
              onImportDriveDocument={(doc: GooglePickerDocument) => {
                const sizeText = doc.sizeBytes ? ` (${(doc.sizeBytes / 1024).toFixed(1)} KB)` : '';
                handleCreateJob({
                  title: `Drive Document: ${doc.name}`,
                  summary: `Ingested via Google Picker${sizeText}. MIME: ${doc.mimeType}. URL: ${doc.url || 'In Google Drive'}. Assigned to autonomous review pipeline.`,
                  priority: 'Medium',
                  status: 'In Progress'
                });
                logAction('PICKER_INGESTION', `Ingested Google Drive document via Google Picker: "${doc.name}"`);
                setActiveTab('jobs');
              }}
            />
          )}

          {activeTab === 'gmail' && (
            <GmailView
              agents={agents}
              onDirectAgentTask={handleDirectAgentTask}
              onOpenWorkspaceSetup={() => setActiveTab('workspace')}
              onCreateJobFromEmail={(subject, sender, snippet) => {
                handleCreateJob({
                  title: `Email Action Item: ${subject.slice(0, 45)}`,
                  summary: `From ${sender}. Ingested from Gmail inbox: "${snippet}". Assigned to autonomous fleet queue.`,
                  priority: 'High',
                  status: 'In Progress'
                });
                logAction('GMAIL_JOB_CREATED', `Converted email "${subject.slice(0, 30)}" to fleet job`);
                setActiveTab('jobs');
              }}
            />
          )}

          {activeTab === 'calendar' && (
            <CalendarView
              onOpenWorkspaceSync={() => setActiveTab('workspace')}
            />
          )}

          {activeTab === 'saved' && (
            <IntelligenceVaultView
              reports={INITIAL_INTELLIGENCE}
            />
          )}

          {activeTab === 'governance' && (
            <GovernanceView
              auditLogs={auditLogs}
            />
          )}

          {activeTab === 'apk' && (
            <ApkCenterView />
          )}

          {activeTab === 'messaging' && (
            <TeamMessagingView
              onDispatchJobToSystem={handleCreateJob}
              onNavigateToJobs={() => setActiveTab('jobs')}
              onLogAuditAction={logAction}
            />
          )}
        </div>
      </main>

      {/* Fixed Bottom Navigation matching Screenshot 1 */}
      <BottomNav
        activeTab={activeTab}
        onSelectTab={handleSelectBottomTab}
        jobsBadgeCount={jobs.filter(j => j.status === 'In Progress' || j.status === 'Queued').length}
      />

      {/* More Navigation Menu Modal */}
      <MoreMenuModal
        isOpen={isMoreMenuOpen}
        onClose={() => setIsMoreMenuOpen(false)}
        onNavigate={(tab) => setActiveTab(tab)}
        onOpenSetup={() => setIsSetupModalOpen(true)}
      />

      {/* Enterprise Workspace Setup Modal matching Screenshot 2 */}
      <EnterpriseSetupModal
        isOpen={isSetupModalOpen}
        onClose={() => setIsSetupModalOpen(false)}
        currentProfile={enterpriseProfile}
        onComplete={(newProfile) => {
          setEnterpriseProfile(newProfile);
          logAction('WORKSPACE_SETUP_SAVED', `Updated identity for ${newProfile.name} (${newProfile.domain})`);
        }}
      />

      {/* Provision New Agent Modal */}
      <CreateAgentModal
        isOpen={isCreateAgentModalOpen}
        onClose={() => setIsCreateAgentModalOpen(false)}
        onCreate={handleProvisionAgent}
      />
    </div>
  );
};

export default App;
