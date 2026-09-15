export type AgentStatus = 'active' | 'idle' | 'executing' | 'offline';
export type RiskLevel = 'Low' | 'Medium' | 'High';
export type AccessLevel = 'Regular Staff' | 'Department Lead' | 'Executive / Admin';

export interface Agent {
  id: string;
  name: string;
  codeName: string;
  role: string;
  department: string;
  status: AgentStatus;
  modelTier: 'gemini-3.7-flash' | 'gemini-2.5-pro' | 'gemini-2.5-flash';
  riskClassification: RiskLevel;
  permissionLevel: AccessLevel;
  capabilityProfile: string;
  tasksCompleted: number;
  uptime: string;
  lastActive: string;
  currentTask?: string;
  avatarSeed: string;
}

export interface ContactPerson {
  name: string;
  role: string;
  email: string;
  phone: string;
  preferredChannel: string;
}

export interface Client {
  id: string;
  companyName: string;
  industry: string;
  accountEmail: string;
  phone: string;
  status: 'Active Enterprise' | 'VIP Client' | 'Prospect Onboarding';
  headquarters: string;
  contractValue: string;
  onboardingDate: string;
  primaryContact: ContactPerson;
  assignedAgent: string;
  notes: string;
  activeJobsCount: number;
}

export type JobStatus = 'Pending' | 'In Progress' | 'Queued' | 'Completed' | 'Archived';
export type JobPriority = 'Urgent' | 'High' | 'Medium' | 'Normal';

export interface Job {
  id: string;
  title: string;
  clientName: string;
  status: JobStatus;
  assignedAgent: string;
  priority: JobPriority;
  dueDate: string;
  approvalRequired: boolean;
  isApproved: boolean;
  progress: number;
  summary: string;
  budget: string;
  tags: string[];
}

export type TriggerType = 'INCOMING_CALL' | 'EMAIL_RECEIVED' | 'SCHEDULED_CRON' | 'THRESHOLD_EVENT' | 'MANUAL_DISPATCH';
export type ApprovalLevel = 'AUTO_APPROVE' | 'REQUIRED_IF_HIGH_RISK' | 'ALWAYS_REQUIRED';

export interface WorkflowStep {
  stepNumber: number;
  title: string;
  actionType: string;
  assignedAgent: string;
  isCompleted: boolean;
}

export interface Workflow {
  id: string;
  title: string;
  category: string;
  description: string;
  assignedAgent: string;
  trigger: {
    type: TriggerType;
    name: string;
    description: string;
    isActive: boolean;
  };
  approvalPolicy: {
    level: ApprovalLevel;
    approverRole: string;
    riskThresholdPct: number;
  };
  steps: WorkflowStep[];
  isActive: boolean;
  totalExecutions: number;
  lastRun: string;
}

export interface CallRecord {
  id: string;
  callerName: string;
  company: string;
  phoneNumber: string;
  timestamp: string;
  duration: string;
  sentiment: 'Positive' | 'Neutral' | 'Action Required';
  summary: string;
  agentRoutedTo: string;
  status: 'Completed' | 'Missed' | 'Voicemail' | 'In Progress';
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'agent' | 'system';
  agentName?: string;
  text: string;
  timestamp: string;
  reasoningSteps?: string[];
  suggestedActions?: string[];
}

export interface IntelligenceReport {
  id: string;
  title: string;
  category: 'Strategic Brief' | 'Risk Assessment' | 'Market Intelligence' | 'Operational Audit';
  date: string;
  authorAgent: string;
  summary: string;
  keyFindings: string[];
  actionItems: string[];
  confidential: boolean;
}

export interface AuditLog {
  id: string;
  action: string;
  actor: string;
  resource: string;
  timestamp: string;
  status: 'SUCCESS' | 'FLAGGED' | 'BLOCKED';
  ipAddress: string;
  details: string;
}

// Enterprise Department Definitions
export type Department = 
  | 'Operations & Field Services'
  | 'Dispatch & Logistics'
  | 'Client Support & Accounts'
  | 'Technical & Engineering'
  | 'Legal & Compliance'
  | 'Executive Leadership';

export type EmployeeStatus = 'online' | 'in_field' | 'busy' | 'offline';

export interface Employee {
  id: string;
  name: string;
  email: string;
  role: string;
  department: Department;
  status: EmployeeStatus;
  avatarSeed: string;
  phone: string;
  isCurrentUser?: boolean;
}

export type PaperworkCategory = 
  | 'Work Order'
  | 'Change Order'
  | 'Site Inspection'
  | 'Subcontractor Agreement'
  | 'Invoice & Billing'
  | 'Safety Sign-Off'
  | 'General Document';

export type PaperworkStatus = 'Pending Review' | 'Approved' | 'Changes Requested' | 'Rejected';

export type PaperworkUrgency = 'Routine' | 'High Priority' | 'Urgent / Immediate';

export interface PaperworkDocument {
  id: string;
  title: string;
  category: PaperworkCategory;
  fileName: string;
  fileSize: string;
  fileType: string;
  fileDataUrl?: string;
  senderId: string;
  senderName: string;
  senderDepartment: Department;
  targetDepartment: Department;
  assignedReviewerId?: string;
  assignedReviewerName?: string;
  submissionDate: string;
  urgency: PaperworkUrgency;
  status: PaperworkStatus;
  notes: string;
  reviewerFeedback?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  signature?: string;
  autoDispatchJobOnApproval: boolean;
  dispatchedJobId?: string;
  estimatedBudget?: string;
  clientName?: string;
}

export interface JobDispatchPayload {
  jobId: string;
  title: string;
  targetDepartment: Department;
  assignedEmployeeId?: string;
  assignedEmployeeName?: string;
  clientName?: string;
  priority: JobPriority;
  dueDate: string;
  budget: string;
  summary: string;
  status: JobStatus;
  paperworkAttachment?: {
    documentId: string;
    title: string;
    category: PaperworkCategory;
    status: PaperworkStatus;
  };
}

export interface DirectMessage {
  id: string;
  senderId: string;
  senderName: string;
  senderRole: string;
  senderDepartment: Department;
  recipientId: string; // employee ID or 'dept:<Department>'
  recipientType: 'direct' | 'department';
  recipientName: string;
  content: string;
  timestamp: string;
  paperwork?: PaperworkDocument;
  jobDispatch?: JobDispatchPayload;
  readBy: string[];
}

