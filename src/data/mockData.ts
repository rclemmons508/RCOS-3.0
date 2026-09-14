import { Agent, Client, Job, Workflow, CallRecord, IntelligenceReport, AuditLog } from '../types';

export const INITIAL_AGENTS: Agent[] = [
  {
    id: 'ag-01',
    name: 'Compliance & Security Audit',
    codeName: 'SENTINEL-AUDIT-01',
    role: 'Compliance & Regulatory Auditor',
    department: 'Legal & Compliance',
    status: 'executing',
    modelTier: 'gemini-2.5-pro',
    riskClassification: 'High',
    permissionLevel: 'Executive / Admin',
    capabilityProfile: 'Automated policy enforcement, regulatory compliance checking, anomaly isolation, and security verification.',
    tasksCompleted: 312,
    uptime: '100%',
    lastActive: 'Just now',
    currentTask: 'Executing Workflow Pipeline',
    avatarSeed: 'sentinel'
  },
  {
    id: 'ag-02',
    name: 'AI Workflow Engine',
    codeName: 'AEGIS-CORE-02',
    role: 'Autonomous Workload & Pipeline Dispatcher',
    department: 'Operations & Dispatch',
    status: 'active',
    modelTier: 'gemini-3.7-flash',
    riskClassification: 'Low',
    permissionLevel: 'Executive / Admin',
    capabilityProfile: 'Multi-agent orchestration, trigger condition evaluation, automated task dispatching, and resource allocation.',
    tasksCompleted: 489,
    uptime: '99.98%',
    lastActive: 'Just now',
    currentTask: 'System Telemetry Active',
    avatarSeed: 'aegis'
  },
  {
    id: 'ag-03',
    name: 'VoIP Telephony Engine',
    codeName: 'PULSE-VOICE-03',
    role: 'Telephony & Speech Intake Specialist',
    department: 'Communications',
    status: 'active',
    modelTier: 'gemini-3.7-flash',
    riskClassification: 'Low',
    permissionLevel: 'Regular Staff',
    capabilityProfile: 'Inbound and outbound VoIP call handling, live real-time speech transcription, sentiment analysis, and audio synthesis.',
    tasksCompleted: 520,
    uptime: '99.95%',
    lastActive: '1m ago',
    currentTask: 'SIP Trunk Listening',
    avatarSeed: 'pulse'
  },
  {
    id: 'ag-04',
    name: 'Enterprise Intelligence',
    codeName: 'COGNITO-DEEP-04',
    role: 'Deep Analytical & Strategic Reasoner',
    department: 'Strategic Analysis',
    status: 'active',
    modelTier: 'gemini-2.5-pro',
    riskClassification: 'Medium',
    permissionLevel: 'Executive / Admin',
    capabilityProfile: 'Multi-step root cause analysis, hypothesis evaluation, scenario stress testing, and executive memo generation.',
    tasksCompleted: 188,
    uptime: '99.85%',
    lastActive: '5m ago',
    currentTask: 'Ready for Directives',
    avatarSeed: 'cognito'
  },
  {
    id: 'ag-05',
    name: 'Master Orchestrator',
    codeName: 'RCOS-MASTER-05',
    role: 'Business Operating System Director',
    department: 'Executive Operations',
    status: 'active',
    modelTier: 'gemini-3.7-flash',
    riskClassification: 'Low',
    permissionLevel: 'Executive / Admin',
    capabilityProfile: 'Global system coordination, cross-agent synchrony, role-based access enforcement, and telemetry reporting.',
    tasksCompleted: 674,
    uptime: '100%',
    lastActive: 'Just now',
    currentTask: 'Fleet In Synchronization',
    avatarSeed: 'master'
  }
];

// No fictional companies or fictional employees! Starts empty so the user can add their real clients.
export const INITIAL_CLIENTS: Client[] = [];

// No fictional jobs assigned to fictional companies! Starts empty so the user can create real tasks.
export const INITIAL_JOBS: Job[] = [];

// System workflow blueprints
export const INITIAL_WORKFLOWS: Workflow[] = [
  {
    id: 'wf-01',
    title: 'Enterprise Compliance & Security Audit Check',
    category: 'Risk & Governance',
    description: 'Continuous validation of security boundaries, token integrity, and operational safety rules.',
    assignedAgent: 'Compliance & Security Audit',
    trigger: {
      type: 'THRESHOLD_EVENT',
      name: 'Continuous Security Telemetry',
      description: 'Monitors real-time API logs for unauthorized data access attempts',
      isActive: true
    },
    approvalPolicy: {
      level: 'REQUIRED_IF_HIGH_RISK',
      approverRole: 'Security Administrator',
      riskThresholdPct: 20
    },
    steps: [
      { stepNumber: 1, title: 'Inspect operational endpoints', actionType: 'SECURITY_ISOLATE', assignedAgent: 'Compliance & Security Audit', isCompleted: true },
      { stepNumber: 2, title: 'Compile cryptographic audit verification', actionType: 'GENERATE_REPORT', assignedAgent: 'Compliance & Security Audit', isCompleted: true },
      { stepNumber: 3, title: 'Verify authorization scope integrity', actionType: 'POLICY_EVALUATION', assignedAgent: 'Compliance & Security Audit', isCompleted: false }
    ],
    isActive: true,
    totalExecutions: 43,
    lastRun: '12m ago'
  },
  {
    id: 'wf-02',
    title: 'Autonomous Inbound Call Intake & Speech Transcription',
    category: 'Communications',
    description: 'Automated VoIP answering, real-time speech transcription, sentiment calculation, and task generation.',
    assignedAgent: 'VoIP Telephony Engine',
    trigger: {
      type: 'INCOMING_CALL',
      name: 'VoIP Trunk Listener',
      description: 'Incoming SIP trunk call routed to telephony engine',
      isActive: true
    },
    approvalPolicy: {
      level: 'AUTO_APPROVE',
      approverRole: 'Operations Lead',
      riskThresholdPct: 5
    },
    steps: [
      { stepNumber: 1, title: 'Answer with conversational greeting', actionType: 'VOICE_INTERACTION', assignedAgent: 'VoIP Telephony Engine', isCompleted: true },
      { stepNumber: 2, title: 'Live speech transcription & sentiment scoring', actionType: 'TRANSCRIBE_AUDIO', assignedAgent: 'VoIP Telephony Engine', isCompleted: true },
      { stepNumber: 3, title: 'Extract action items & summarize ticket', actionType: 'CREATE_JOB', assignedAgent: 'AI Workflow Engine', isCompleted: true }
    ],
    isActive: true,
    totalExecutions: 120,
    lastRun: 'Just now'
  },
  {
    id: 'wf-03',
    title: 'Daily Strategic Intelligence Briefing',
    category: 'Executive Strategy',
    description: 'Deep analytical synthesis across operations, agent performance metrics, and system throughput.',
    assignedAgent: 'Enterprise Intelligence',
    trigger: {
      type: 'SCHEDULED_CRON',
      name: 'Daily 06:30 Dispatch',
      description: 'Scheduled multi-agent analysis',
      isActive: true
    },
    approvalPolicy: {
      level: 'AUTO_APPROVE',
      approverRole: 'Executive Director',
      riskThresholdPct: 10
    },
    steps: [
      { stepNumber: 1, title: 'Gather operational metrics & logs', actionType: 'DATA_INGESTION', assignedAgent: 'Enterprise Intelligence', isCompleted: true },
      { stepNumber: 2, title: 'Perform deep reasoning synthesis', actionType: 'DEEP_THINKING', assignedAgent: 'Enterprise Intelligence', isCompleted: true },
      { stepNumber: 3, title: 'Generate executive summary brief', actionType: 'DOCUMENT_RENDER', assignedAgent: 'Master Orchestrator', isCompleted: true }
    ],
    isActive: true,
    totalExecutions: 58,
    lastRun: 'Today, 06:30'
  }
];

// No fictional phone calls with fake people! Starts empty.
export const INITIAL_CALL_RECORDS: CallRecord[] = [];

export const INITIAL_INTELLIGENCE: IntelligenceReport[] = [
  {
    id: 'rep-01',
    title: 'Autonomous Multi-Agent Enterprise Orchestration Standard',
    category: 'Strategic Brief',
    date: 'Sep 10, 2026',
    authorAgent: 'Enterprise Intelligence',
    summary: 'Operational blueprint for coordinated autonomous agents with explicit role-based access control and live telemetry monitoring.',
    keyFindings: [
      'Multi-agent orchestration allows specialized agents to collaborate without shared execution failures.',
      'Explicit risk boundaries prevent unverified actions while maintaining sub-second automated execution.',
      'Direct human steering allows real-time intervention on any in-flight agent workload.'
    ],
    actionItems: [
      'Maintain continuous telemetry heartbeat on active autonomous agents.',
      'Enforce audit logging on all workflow state transitions.',
      'Provide instant voice-to-task conversion via the integrated dialer.'
    ],
    confidential: true
  }
];

export const INITIAL_AUDIT_LOGS: AuditLog[] = [
  {
    id: 'aud-01',
    action: 'SYSTEM_INITIALIZATION',
    actor: 'RCOS Kernel',
    resource: 'RCOS Master App',
    timestamp: '08:00:00 AM',
    status: 'SUCCESS',
    ipAddress: '127.0.0.1 (Local System)',
    details: 'Business Operating System initialized with 5 autonomous agents active'
  },
  {
    id: 'aud-02',
    action: 'POLICY_EVALUATION',
    actor: 'Compliance & Security Audit',
    resource: 'RBAC Policy Engine',
    timestamp: '08:15:22 AM',
    status: 'SUCCESS',
    ipAddress: '10.0.1.5 (Security Gateway)',
    details: 'Role-based access rules and governance parameters verified successfully'
  }
];
