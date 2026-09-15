import React, { useState, useEffect, useRef } from 'react';
import { 
  Send, 
  Paperclip, 
  Briefcase, 
  FileText, 
  Users, 
  Building2, 
  Search, 
  Plus, 
  CheckCircle2, 
  Clock, 
  AlertTriangle, 
  Check, 
  Filter, 
  UserCheck, 
  ArrowRight, 
  ShieldCheck, 
  ChevronRight, 
  Sparkles,
  ExternalLink,
  MessageSquare,
  BadgeAlert,
  XCircle
} from 'lucide-react';
import { 
  Department, 
  Employee, 
  DirectMessage, 
  PaperworkDocument, 
  JobPriority, 
  JobStatus, 
  Job,
  PaperworkStatus
} from '../types';
import { 
  employeeMessagingService, 
  ENTERPRISE_DEPARTMENTS 
} from '../services/employeeMessagingService';
import { DispatchJobModal } from './DispatchJobModal';
import { SendPaperworkModal } from './SendPaperworkModal';
import { DocumentReviewModal } from './DocumentReviewModal';

interface TeamMessagingViewProps {
  onDispatchJobToSystem: (job: Partial<Job>) => void;
  onNavigateToJobs: () => void;
  onLogAuditAction?: (action: string, details: string) => void;
}

export const TeamMessagingView: React.FC<TeamMessagingViewProps> = ({
  onDispatchJobToSystem,
  onNavigateToJobs,
  onLogAuditAction
}) => {
  // Service state
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [currentEmployee, setCurrentEmployee] = useState<Employee>(
    employeeMessagingService.getCurrentEmployee()
  );
  
  // Navigation / Selection state
  const [activeRecipientId, setActiveRecipientId] = useState<string>(
    'dept:Operations & Field Services'
  );
  const [isDepartment, setIsDepartment] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<'chat' | 'paperwork' | 'dispatches'>('chat');
  const [searchQuery, setSearchQuery] = useState<string>('');
  
  // Modals
  const [isDispatchModalOpen, setIsDispatchModalOpen] = useState<boolean>(false);
  const [isPaperworkModalOpen, setIsPaperworkModalOpen] = useState<boolean>(false);
  const [reviewingDoc, setReviewingDoc] = useState<PaperworkDocument | null>(null);
  const [isUserSwitcherOpen, setIsUserSwitcherOpen] = useState<boolean>(false);
  const [isAddEmployeeOpen, setIsAddEmployeeOpen] = useState<boolean>(false);

  // New Employee Form
  const [newEmpName, setNewEmpName] = useState<string>('');
  const [newEmpRole, setNewEmpRole] = useState<string>('');
  const [newEmpDept, setNewEmpDept] = useState<Department>('Operations & Field Services');
  const [newEmpPhone, setNewEmpPhone] = useState<string>('+1 (555) 000-0000');

  // Input
  const [messageText, setMessageText] = useState<string>('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Subscribe to updates
  useEffect(() => {
    const update = () => {
      setEmployees(employeeMessagingService.getEmployees());
      setCurrentEmployee(employeeMessagingService.getCurrentEmployee());
    };
    update();
    const unsub = employeeMessagingService.subscribe(update);
    return unsub;
  }, []);

  // Auto scroll to bottom
  const currentMessages = employeeMessagingService.getMessages(activeRecipientId, isDepartment);
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    // Mark as read
    const unreadIds = currentMessages
      .filter(m => m.senderId !== currentEmployee.id && !m.readBy?.includes(currentEmployee.id))
      .map(m => m.id);
    if (unreadIds.length > 0) {
      employeeMessagingService.markAsRead(unreadIds);
    }
  }, [currentMessages, activeRecipientId, currentEmployee.id]);

  const handleSendMessage = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!messageText.trim()) return;

    let recipientName = activeRecipientId;
    if (isDepartment) {
      recipientName = activeRecipientId.replace('dept:', '');
    } else {
      const emp = employees.find(e => e.id === activeRecipientId);
      recipientName = emp ? emp.name : 'Team Member';
    }

    employeeMessagingService.sendDirectMessage({
      recipientId: activeRecipientId,
      recipientName,
      recipientType: isDepartment ? 'department' : 'direct',
      content: messageText.trim()
    });

    setMessageText('');
  };

  const handleDispatchJob = (payload: {
    title: string;
    department: Department;
    assignedEmployeeId?: string;
    assignedEmployeeName?: string;
    clientName: string;
    priority: JobPriority;
    dueDate: string;
    budget: string;
    summary: string;
  }) => {
    const jobId = `job-disp-${Date.now()}`;

    // 1. Send direct department message with interactive Job Card
    employeeMessagingService.sendDirectMessage({
      recipientId: `dept:${payload.department}`,
      recipientName: payload.department,
      recipientType: 'department',
      content: `Direct Work Order Dispatched to ${payload.department}: ${payload.title}`,
      jobDispatch: {
        jobId,
        title: payload.title,
        targetDepartment: payload.department,
        assignedEmployeeId: payload.assignedEmployeeId,
        assignedEmployeeName: payload.assignedEmployeeName,
        clientName: payload.clientName,
        priority: payload.priority,
        dueDate: payload.dueDate,
        budget: payload.budget,
        summary: payload.summary,
        status: 'In Progress'
      }
    });

    // 2. Add real Job into RCOS core jobs state
    onDispatchJobToSystem({
      id: jobId,
      title: payload.title,
      clientName: payload.clientName,
      assignedAgent: payload.assignedEmployeeName || `${payload.department} Team`,
      priority: payload.priority,
      status: 'In Progress',
      dueDate: payload.dueDate,
      approvalRequired: false,
      isApproved: true,
      progress: 0.15,
      summary: payload.summary,
      budget: payload.budget,
      tags: ['Dispatched Job', payload.department, 'Live Field Work']
    });

    // 3. Log audit event
    if (onLogAuditAction) {
      onLogAuditAction(
        'DISPATCH_JOB',
        `Dispatched work order #${jobId} "${payload.title}" to ${payload.department} (Assignee: ${payload.assignedEmployeeName || 'Department'})`
      );
    }

    // Switch view to that department channel
    setActiveRecipientId(`dept:${payload.department}`);
    setIsDepartment(true);
    setActiveTab('chat');
  };

  const handleSendPaperwork = (docItem: PaperworkDocument, customMsg?: string) => {
    const defaultMsg = customMsg || `Submitted ${docItem.category} for review: "${docItem.title}". Attached: ${docItem.fileName} (${docItem.fileSize}).`;

    // 1. Send to department channel or reviewer
    employeeMessagingService.sendDirectMessage({
      recipientId: `dept:${docItem.targetDepartment}`,
      recipientName: docItem.targetDepartment,
      recipientType: 'department',
      content: defaultMsg,
      paperwork: docItem
    });

    if (onLogAuditAction) {
      onLogAuditAction(
        'SUBMIT_PAPERWORK',
        `Submitted "${docItem.title}" (${docItem.category}) to ${docItem.targetDepartment} for formal review & sign-off`
      );
    }

    // Switch to target department channel
    setActiveRecipientId(`dept:${docItem.targetDepartment}`);
    setIsDepartment(true);
    setActiveTab('chat');
  };

  const handlePaperworkApproval = (
    docId: string, 
    status: PaperworkStatus, 
    feedback?: string, 
    signature?: string
  ) => {
    const result = employeeMessagingService.updatePaperworkApproval(docId, status, feedback, signature);
    
    if (result && result.shouldDispatchJob) {
      // Auto-dispatch job because paperwork was authorized!
      const docItem = result.updatedPaperwork;
      const jobId = `job-auto-${Date.now()}`;

      onDispatchJobToSystem({
        id: jobId,
        title: `Authorized Work Order: ${docItem.title}`,
        clientName: docItem.clientName || 'Internal Enterprise Operations',
        assignedAgent: `${docItem.targetDepartment} Fleet`,
        priority: docItem.urgency === 'Urgent / Immediate' ? 'Urgent' : 'High',
        status: 'In Progress',
        dueDate: 'Within 24 Hours',
        approvalRequired: true,
        isApproved: true,
        progress: 0.1,
        summary: `Authorized on paper approval by ${currentEmployee.name}. Scope: ${docItem.notes}`,
        budget: docItem.estimatedBudget || '$1,500 Authorized',
        tags: ['Paperwork Authorized', docItem.category, docItem.targetDepartment]
      });

      employeeMessagingService.linkDispatchedJobToPaperwork(docId, jobId);

      // Post notification message in channel
      employeeMessagingService.sendDirectMessage({
        recipientId: `dept:${docItem.targetDepartment}`,
        recipientName: docItem.targetDepartment,
        recipientType: 'department',
        content: `APPROVED: "${docItem.title}" was approved by ${currentEmployee.name}. Live job #${jobId} automatically scheduled & dispatched to ${docItem.targetDepartment}.`
      });

      if (onLogAuditAction) {
        onLogAuditAction(
          'PAPERWORK_APPROVED_JOB_DISPATCHED',
          `Paperwork #${docId} approved by ${currentEmployee.name}. Auto-dispatched Job #${jobId} to ${docItem.targetDepartment}`
        );
      }
    } else if (onLogAuditAction) {
      onLogAuditAction(
        'PAPERWORK_STATUS_UPDATE',
        `Paperwork #${docId} status set to ${status} by ${currentEmployee.name}`
      );
    }
  };

  const handleCreateEmployee = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newEmpName.trim()) return;

    const created = employeeMessagingService.addEmployee({
      name: newEmpName.trim(),
      role: newEmpRole.trim() || 'Operations Specialist',
      department: newEmpDept,
      status: 'online',
      avatarSeed: newEmpName.toLowerCase().replace(/\s+/g, ''),
      phone: newEmpPhone.trim(),
      email: `${newEmpName.toLowerCase().replace(/\s+/g, '.')}@rcos.global`
    });

    setNewEmpName('');
    setNewEmpRole('');
    setIsAddEmployeeOpen(false);
    setActiveRecipientId(created.id);
    setIsDepartment(false);
  };

  // Filtered employees for list
  const filteredEmployees = employees.filter(emp => 
    emp.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    emp.role.toLowerCase().includes(searchQuery.toLowerCase()) ||
    emp.department.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const allPaperwork = employeeMessagingService.getAllPaperwork();
  const pendingApprovalsCount = employeeMessagingService.getPendingApprovalCount();
  const allDispatchedJobs = employeeMessagingService.getAllMessages().filter(m => m.jobDispatch);

  // Active chat header info
  let activeTitle = 'Operations & Field Services';
  let activeSub = 'Department Channel';
  let activeStatus: 'online' | 'in_field' | 'busy' | 'offline' = 'online';

  if (isDepartment) {
    activeTitle = activeRecipientId.replace('dept:', '');
    activeSub = `${employees.filter(e => e.department === activeTitle).length} Team Members`;
  } else {
    const emp = employees.find(e => e.id === activeRecipientId);
    if (emp) {
      activeTitle = emp.name;
      activeSub = `${emp.role} • ${emp.department}`;
      activeStatus = emp.status;
    }
  }

  return (
    <div id="team-messaging-container" className="space-y-4">
      {/* Top Banner & Quick Identity Switcher */}
      <div className="p-4 rounded-2xl bg-[#070d09] border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-lg">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-black px-2 py-0.5 rounded-full uppercase tracking-wider bg-[#76d418]/10 text-[#76d418] border border-[#76d418]/30">
              Live Team Comms & Dispatch
            </span>
            {pendingApprovalsCount > 0 && (
              <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1">
                <BadgeAlert className="w-3 h-3" />
                {pendingApprovalsCount} Paperwork Pending
              </span>
            )}
          </div>
          <h1 className="text-lg sm:text-xl font-black text-slate-100 mt-1">
            Employee Direct Messaging & Department Dispatch
          </h1>
          <p className="text-xs text-slate-400">
            Real-time chat, instant departmental job dispatching, and paperwork review & authorization.
          </p>
        </div>

        {/* Current Identity & Switcher */}
        <div className="relative flex items-center gap-2 self-stretch sm:self-auto">
          <div className="flex-1 sm:flex-initial p-2 rounded-xl bg-[#050906] border border-slate-800 flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-[#76d418]/20 border border-[#76d418]/40 flex items-center justify-center font-bold text-[#76d418] text-xs">
              {currentEmployee.name.split(' ').map(n => n[0]).join('')}
            </div>
            <div className="text-left">
              <div className="text-[10px] text-slate-400">Sending As:</div>
              <div className="text-xs font-bold text-slate-200 truncate max-w-[130px]">
                {currentEmployee.name}
              </div>
            </div>
            <button
              type="button"
              onClick={() => setIsUserSwitcherOpen(!isUserSwitcherOpen)}
              className="ml-auto text-[11px] font-semibold text-[#76d418] hover:underline px-2 py-1 rounded bg-slate-900 border border-slate-800 cursor-pointer"
            >
              Switch Identity
            </button>
          </div>

          {/* Identity Dropdown */}
          {isUserSwitcherOpen && (
            <div className="absolute right-0 top-14 z-50 w-72 bg-[#070d09] border border-slate-800 rounded-xl shadow-2xl p-2 space-y-1">
              <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider px-2 py-1">
                Select Team Member:
              </div>
              {employees.map(emp => (
                <button
                  key={emp.id}
                  onClick={() => {
                    employeeMessagingService.setCurrentEmployee(emp.id);
                    setCurrentEmployee(emp);
                    setIsUserSwitcherOpen(false);
                  }}
                  className={`w-full text-left p-2 rounded-lg text-xs flex items-center justify-between transition-colors ${
                    emp.id === currentEmployee.id 
                      ? 'bg-[#76d418]/10 text-[#76d418] border border-[#76d418]/30 font-bold' 
                      : 'hover:bg-slate-900 text-slate-200'
                  }`}
                >
                  <div>
                    <div className="font-bold">{emp.name}</div>
                    <div className="text-[10px] text-slate-400">{emp.role} • {emp.department}</div>
                  </div>
                  {emp.id === currentEmployee.id && <Check className="w-4 h-4 text-[#76d418]" />}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Main Grid: Sidebar + Chat / Paperwork Panels */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-4 min-h-[580px]">
        {/* Left Column: Department Channels & Direct Contacts (4 cols) */}
        <div className="md:col-span-4 bg-[#070d09] border border-slate-800 rounded-2xl flex flex-col overflow-hidden h-full max-h-[640px]">
          {/* Action Buttons: Dispatch Job & Send Paperwork */}
          <div className="p-3 border-b border-slate-800/80 bg-[#050906] space-y-2">
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setIsDispatchModalOpen(true)}
                className="flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl bg-[#76d418] hover:bg-[#68bc15] text-slate-950 font-bold text-xs shadow-md shadow-[#76d418]/20 transition-all cursor-pointer"
              >
                <Briefcase className="w-3.5 h-3.5" />
                <span>Dispatch Job</span>
              </button>

              <button
                type="button"
                onClick={() => setIsPaperworkModalOpen(true)}
                className="flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-[#76d418] border border-[#76d418]/40 font-bold text-xs transition-all cursor-pointer"
              >
                <FileText className="w-3.5 h-3.5" />
                <span>Send Paperwork</span>
              </button>
            </div>

            {/* Navigation Tabs */}
            <div className="flex rounded-lg bg-slate-950 p-1 border border-slate-800 text-xs">
              <button
                type="button"
                onClick={() => setActiveTab('chat')}
                className={`flex-1 py-1 rounded-md font-semibold transition-all ${
                  activeTab === 'chat' ? 'bg-[#76d418] text-slate-950' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Messages
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('paperwork')}
                className={`flex-1 py-1 rounded-md font-semibold transition-all relative ${
                  activeTab === 'paperwork' ? 'bg-[#76d418] text-slate-950' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Paperwork
                {pendingApprovalsCount > 0 && (
                  <span className={`ml-1 text-[10px] px-1 rounded-full ${
                    activeTab === 'paperwork' ? 'bg-slate-950 text-[#76d418]' : 'bg-amber-500 text-slate-950 font-black'
                  }`}>
                    {pendingApprovalsCount}
                  </span>
                )}
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('dispatches')}
                className={`flex-1 py-1 rounded-md font-semibold transition-all ${
                  activeTab === 'dispatches' ? 'bg-[#76d418] text-slate-950' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Dispatches
              </button>
            </div>

            {/* Search */}
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search staff or departments..."
                className="w-full bg-[#050906] border border-slate-800 rounded-xl pl-8 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-[#76d418]"
              />
            </div>
          </div>

          {/* List of Channels & Contacts */}
          <div className="flex-1 overflow-y-auto p-2 space-y-4">
            {/* Department Channels Section */}
            <div>
              <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider px-2 py-1 flex items-center justify-between">
                <span>Department Channels</span>
                <span className="text-slate-500">{ENTERPRISE_DEPARTMENTS.length} Depts</span>
              </div>
              <div className="space-y-1 mt-1">
                {ENTERPRISE_DEPARTMENTS.map(dept => {
                  const deptId = `dept:${dept}`;
                  const isSelected = isDepartment && activeRecipientId === deptId;
                  const deptMsgs = employeeMessagingService.getMessages(deptId, true);
                  const unreadCount = deptMsgs.filter(m => m.senderId !== currentEmployee.id && !m.readBy?.includes(currentEmployee.id)).length;
                  const hasDispatches = deptMsgs.some(m => m.jobDispatch);

                  return (
                    <button
                      key={dept}
                      onClick={() => {
                        setActiveRecipientId(deptId);
                        setIsDepartment(true);
                        setActiveTab('chat');
                      }}
                      className={`w-full text-left p-2 rounded-xl text-xs flex items-center justify-between transition-all cursor-pointer ${
                        isSelected 
                          ? 'bg-[#0a150c] text-[#76d418] border border-[#76d418]/40 shadow-sm' 
                          : 'hover:bg-slate-900/60 text-slate-300 border border-transparent'
                      }`}
                    >
                      <div className="flex items-center gap-2 min-w-0">
                        <div className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 ${
                          isSelected ? 'bg-[#76d418]/20 text-[#76d418]' : 'bg-slate-900 text-slate-400'
                        }`}>
                          <Building2 className="w-3.5 h-3.5" />
                        </div>
                        <div className="min-w-0">
                          <div className="font-bold truncate text-slate-200">
                            #{dept}
                          </div>
                          <div className="text-[10px] text-slate-400 truncate">
                            {hasDispatches ? '⚡ Active Job Dispatches' : `${deptMsgs.length} messages`}
                          </div>
                        </div>
                      </div>

                      {unreadCount > 0 && (
                        <span className="px-1.5 py-0.5 rounded-full bg-[#76d418] text-slate-950 font-black text-[10px]">
                          {unreadCount}
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Direct Employee Contacts Section */}
            <div>
              <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider px-2 py-1 flex items-center justify-between">
                <span>Direct Staff ({filteredEmployees.length})</span>
                <button
                  type="button"
                  onClick={() => setIsAddEmployeeOpen(true)}
                  className="text-[#76d418] hover:underline flex items-center gap-0.5 text-[10px] font-semibold cursor-pointer"
                >
                  <Plus className="w-3 h-3" />
                  Add Staff
                </button>
              </div>
              <div className="space-y-1 mt-1">
                {filteredEmployees.map(emp => {
                  const isSelected = !isDepartment && activeRecipientId === emp.id;
                  const directMsgs = employeeMessagingService.getMessages(emp.id, false);
                  const unreadCount = directMsgs.filter(m => m.senderId === emp.id && !m.readBy?.includes(currentEmployee.id)).length;
                  const isSelf = emp.id === currentEmployee.id;

                  return (
                    <button
                      key={emp.id}
                      onClick={() => {
                        setActiveRecipientId(emp.id);
                        setIsDepartment(false);
                        setActiveTab('chat');
                      }}
                      className={`w-full text-left p-2 rounded-xl text-xs flex items-center justify-between transition-all cursor-pointer ${
                        isSelected 
                          ? 'bg-[#0a150c] text-[#76d418] border border-[#76d418]/40 shadow-sm' 
                          : 'hover:bg-slate-900/60 text-slate-300 border border-transparent'
                      }`}
                    >
                      <div className="flex items-center gap-2 min-w-0">
                        <div className="relative shrink-0">
                          <div className="w-7 h-7 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-center font-bold text-slate-300 text-xs">
                            {emp.name.split(' ').map(n => n[0]).join('')}
                          </div>
                          {/* Status indicator */}
                          <span className={`absolute -bottom-0.5 -right-0.5 w-2 h-2 rounded-full ring-2 ring-[#070d09] ${
                            emp.status === 'online' ? 'bg-[#76d418]' :
                            emp.status === 'in_field' ? 'bg-amber-400' :
                            emp.status === 'busy' ? 'bg-rose-400' : 'bg-slate-500'
                          }`} />
                        </div>
                        <div className="min-w-0">
                          <div className="font-bold truncate text-slate-200 flex items-center gap-1.5">
                            <span>{emp.name}</span>
                            {isSelf && <span className="text-[9px] text-[#76d418] font-normal">(You)</span>}
                          </div>
                          <div className="text-[10px] text-slate-400 truncate">
                            {emp.role}
                          </div>
                        </div>
                      </div>

                      {unreadCount > 0 && (
                        <span className="px-1.5 py-0.5 rounded-full bg-[#76d418] text-slate-950 font-black text-[10px]">
                          {unreadCount}
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Chat Feed OR Paperwork Center (8 cols) */}
        <div className="md:col-span-8 bg-[#070d09] border border-slate-800 rounded-2xl flex flex-col overflow-hidden h-full max-h-[640px]">
          {/* View Tab: CHAT MESSAGES */}
          {activeTab === 'chat' && (
            <>
              {/* Conversation Header */}
              <div className="px-4 py-3 border-b border-slate-800/80 bg-[#050906] flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-[#76d418] font-bold">
                    {isDepartment ? <Building2 className="w-5 h-5" /> : activeTitle[0]}
                  </div>
                  <div>
                    <h2 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                      <span>{isDepartment ? `#${activeTitle}` : activeTitle}</span>
                      {!isDepartment && (
                        <span className={`w-2 h-2 rounded-full ${
                          activeStatus === 'online' ? 'bg-[#76d418]' :
                          activeStatus === 'in_field' ? 'bg-amber-400' :
                          activeStatus === 'busy' ? 'bg-rose-400' : 'bg-slate-500'
                        }`} />
                      )}
                    </h2>
                    <p className="text-[11px] text-slate-400">{activeSub}</p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setIsDispatchModalOpen(true)}
                    className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-slate-900 hover:bg-[#76d418]/10 hover:text-[#76d418] text-slate-300 text-xs font-semibold border border-slate-800 transition-colors"
                  >
                    <Briefcase className="w-3.5 h-3.5 text-[#76d418]" />
                    <span className="hidden sm:inline">Dispatch Job</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsPaperworkModalOpen(true)}
                    className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-slate-900 hover:bg-[#76d418]/10 hover:text-[#76d418] text-slate-300 text-xs font-semibold border border-slate-800 transition-colors"
                  >
                    <FileText className="w-3.5 h-3.5 text-[#76d418]" />
                    <span className="hidden sm:inline">Attach Paperwork</span>
                  </button>
                </div>
              </div>

              {/* Message Feed */}
              <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-[#030604]/40">
                {currentMessages.length === 0 ? (
                  <div className="h-full flex flex-col items-center justify-center text-center p-6 text-slate-400">
                    <MessageSquare className="w-10 h-10 text-[#76d418] opacity-60 mb-2" />
                    <h3 className="text-sm font-bold text-slate-200">No messages yet in this channel</h3>
                    <p className="text-xs text-slate-400 max-w-sm mt-1">
                      Start direct communications, dispatch jobs to the department, or send documents and paperwork for review.
                    </p>
                    <div className="flex gap-2 mt-4">
                      <button
                        onClick={() => setIsDispatchModalOpen(true)}
                        className="px-3 py-1.5 rounded-lg bg-[#76d418] text-slate-950 text-xs font-bold"
                      >
                        Dispatch First Job
                      </button>
                      <button
                        onClick={() => setIsPaperworkModalOpen(true)}
                        className="px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-200 text-xs font-semibold"
                      >
                        Send Paperwork
                      </button>
                    </div>
                  </div>
                ) : (
                  currentMessages.map(msg => {
                    const isMine = msg.senderId === currentEmployee.id;

                    return (
                      <div 
                        key={msg.id}
                        className={`flex flex-col ${isMine ? 'items-end' : 'items-start'}`}
                      >
                        {/* Sender & Timestamp */}
                        <div className="flex items-center gap-2 mb-1 px-1">
                          <span className="text-[10px] font-bold text-slate-400">
                            {msg.senderName} ({msg.senderDepartment})
                          </span>
                          <span className="text-[9px] text-slate-500">
                            {msg.timestamp}
                          </span>
                        </div>

                        {/* Message Bubble Container */}
                        <div className={`max-w-[85%] sm:max-w-[75%] rounded-2xl p-3 text-xs leading-relaxed ${
                          isMine 
                            ? 'bg-[#0f2413] text-slate-100 border border-[#76d418]/30 rounded-tr-none' 
                            : 'bg-[#08120a] text-slate-200 border border-slate-800 rounded-tl-none'
                        }`}>
                          {/* Text Body */}
                          <div className="whitespace-pre-wrap">{msg.content}</div>

                          {/* Interactive Job Dispatch Card (if attached) */}
                          {msg.jobDispatch && (
                            <div className="mt-2.5 p-3 rounded-xl bg-[#050a06] border border-[#76d418]/40 space-y-2">
                              <div className="flex items-center justify-between">
                                <span className="text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded-full bg-[#76d418]/20 text-[#76d418] border border-[#76d418]/40 flex items-center gap-1">
                                  <Briefcase className="w-3 h-3" />
                                  Live Work Order Dispatched
                                </span>
                                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                                  msg.jobDispatch.priority === 'Urgent' ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40' :
                                  msg.jobDispatch.priority === 'High' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' :
                                  'bg-slate-800 text-slate-300'
                                }`}>
                                  {msg.jobDispatch.priority}
                                </span>
                              </div>

                              <div className="font-bold text-slate-100 text-xs">
                                {msg.jobDispatch.title}
                              </div>

                              <div className="grid grid-cols-2 gap-2 text-[11px] text-slate-300">
                                <div>
                                  <span className="text-slate-500 block text-[9px]">Target Dept:</span>
                                  <span className="font-semibold text-[#76d418]">{msg.jobDispatch.targetDepartment}</span>
                                </div>
                                <div>
                                  <span className="text-slate-500 block text-[9px]">Assignee:</span>
                                  <span className="font-semibold">{msg.jobDispatch.assignedEmployeeName || 'Department Team'}</span>
                                </div>
                                <div>
                                  <span className="text-slate-500 block text-[9px]">Target Due Date:</span>
                                  <span className="font-semibold">{msg.jobDispatch.dueDate || 'Today'}</span>
                                </div>
                                <div>
                                  <span className="text-slate-500 block text-[9px]">Budget / Labor:</span>
                                  <span className="font-semibold">{msg.jobDispatch.budget}</span>
                                </div>
                              </div>

                              <div className="text-[11px] text-slate-300 bg-slate-900/80 p-2 rounded-lg border border-slate-800">
                                {msg.jobDispatch.summary}
                              </div>

                              <div className="pt-1 flex items-center justify-between">
                                <span className="text-[10px] text-slate-500 font-mono">Job #{msg.jobDispatch.jobId}</span>
                                <button
                                  type="button"
                                  onClick={onNavigateToJobs}
                                  className="flex items-center gap-1 text-[11px] font-bold text-[#76d418] hover:underline cursor-pointer"
                                >
                                  <span>View in Jobs Hub</span>
                                  <ArrowRight className="w-3 h-3" />
                                </button>
                              </div>
                            </div>
                          )}

                          {/* Interactive Paperwork Document Card (if attached) */}
                          {msg.paperwork && (
                            <div className="mt-2.5 p-3 rounded-xl bg-[#050a06] border border-slate-800 space-y-2">
                              <div className="flex items-center justify-between">
                                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider bg-slate-900 text-slate-300 border border-slate-800">
                                  {msg.paperwork.category}
                                </span>
                                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                                  msg.paperwork.status === 'Approved' ? 'bg-[#76d418]/20 text-[#76d418] border border-[#76d418]/40' :
                                  msg.paperwork.status === 'Changes Requested' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' :
                                  msg.paperwork.status === 'Rejected' ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40' :
                                  'bg-sky-500/20 text-sky-300 border border-sky-500/40 animate-pulse'
                                }`}>
                                  {msg.paperwork.status === 'Approved' && <CheckCircle2 className="w-3 h-3" />}
                                  {msg.paperwork.status}
                                </span>
                              </div>

                              <div className="font-bold text-slate-100 text-xs">
                                {msg.paperwork.title}
                              </div>

                              <div className="flex items-center gap-2 p-2 rounded-lg bg-slate-900/60 border border-slate-800 text-[11px]">
                                <FileText className="w-4 h-4 text-[#76d418] shrink-0" />
                                <span className="truncate text-slate-200">{msg.paperwork.fileName}</span>
                                <span className="text-slate-500 text-[10px] shrink-0">({msg.paperwork.fileSize})</span>
                              </div>

                              {msg.paperwork.notes && (
                                <p className="text-[11px] text-slate-400 line-clamp-2">
                                  "{msg.paperwork.notes}"
                                </p>
                              )}

                              {/* Action Button: Review & Sign off */}
                              <div className="pt-1 flex items-center justify-between">
                                <span className="text-[10px] text-slate-500">
                                  {msg.paperwork.autoDispatchJobOnApproval ? '⚡ Auto-dispatches on sign-off' : 'Review required'}
                                </span>
                                <button
                                  type="button"
                                  onClick={() => setReviewingDoc(msg.paperwork!)}
                                  className="flex items-center gap-1.5 px-3 py-1 rounded-lg bg-[#76d418]/10 hover:bg-[#76d418]/20 text-[#76d418] font-bold text-[11px] border border-[#76d418]/40 transition-colors cursor-pointer"
                                >
                                  <ShieldCheck className="w-3.5 h-3.5" />
                                  <span>{msg.paperwork.status === 'Pending Review' ? 'Review & Sign Off' : 'Inspect Paperwork'}</span>
                                </button>
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })
                )}
                <div ref={messagesEndRef} />
              </div>

              {/* Message Input Bar */}
              <form onSubmit={handleSendMessage} className="p-3 border-t border-slate-800 bg-[#050906] flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsPaperworkModalOpen(true)}
                  title="Attach Paperwork / Document"
                  className="p-2 rounded-xl text-slate-400 hover:text-[#76d418] hover:bg-slate-900 transition-colors"
                >
                  <Paperclip className="w-4 h-4" />
                </button>

                <button
                  type="button"
                  onClick={() => setIsDispatchModalOpen(true)}
                  title="Dispatch Job to this Department"
                  className="p-2 rounded-xl text-slate-400 hover:text-[#76d418] hover:bg-slate-900 transition-colors"
                >
                  <Briefcase className="w-4 h-4" />
                </button>

                <input
                  type="text"
                  value={messageText}
                  onChange={(e) => setMessageText(e.target.value)}
                  placeholder={`Message ${isDepartment ? '#' + activeTitle : activeTitle}... (Enter to send)`}
                  className="flex-1 bg-[#030604] border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[#76d418]"
                />

                <button
                  type="submit"
                  disabled={!messageText.trim()}
                  className="p-2 rounded-xl bg-[#76d418] disabled:bg-slate-800 text-slate-950 disabled:text-slate-500 transition-all font-bold cursor-pointer"
                >
                  <Send className="w-4 h-4" />
                </button>
              </form>
            </>
          )}

          {/* View Tab: PAPERWORK & APPROVALS VAULT */}
          {activeTab === 'paperwork' && (
            <div className="flex-1 flex flex-col overflow-hidden">
              <div className="p-4 border-b border-slate-800 bg-[#050906] flex items-center justify-between">
                <div>
                  <h2 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                    <FileText className="w-4 h-4 text-[#76d418]" />
                    <span>Company Paperwork & Approval Center</span>
                  </h2>
                  <p className="text-[11px] text-slate-400">
                    Inspect, approve, or request revisions for work orders, contracts, and compliance paperwork.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setIsPaperworkModalOpen(true)}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#76d418] text-slate-950 font-bold text-xs"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Submit Paperwork</span>
                </button>
              </div>

              <div className="flex-1 overflow-y-auto p-4 space-y-3">
                {allPaperwork.length === 0 ? (
                  <div className="text-center py-12 text-slate-400">
                    <FileText className="w-10 h-10 text-slate-600 mx-auto mb-2" />
                    <p className="text-xs">No paperwork has been submitted yet.</p>
                  </div>
                ) : (
                  allPaperwork.map(doc => (
                    <div 
                      key={doc.id}
                      className="p-4 rounded-xl bg-[#050906] border border-slate-800 hover:border-slate-700 transition-all flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3"
                    >
                      <div className="space-y-1 min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-900 border border-slate-800 text-slate-300 uppercase">
                            {doc.category}
                          </span>
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                            doc.status === 'Approved' ? 'bg-[#76d418]/20 text-[#76d418] border border-[#76d418]/40' :
                            doc.status === 'Changes Requested' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' :
                            doc.status === 'Rejected' ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40' :
                            'bg-sky-500/20 text-sky-300 border border-sky-500/40'
                          }`}>
                            {doc.status === 'Approved' && <CheckCircle2 className="w-3 h-3" />}
                            {doc.status}
                          </span>
                          <span className="text-[10px] text-slate-500">Submitted: {doc.submissionDate}</span>
                        </div>
                        <div className="font-bold text-slate-100 text-sm">{doc.title}</div>
                        <div className="text-xs text-slate-400">
                          Submitted by <strong>{doc.senderName}</strong> ({doc.senderDepartment}) → Target Dept: <strong className="text-[#76d418]">{doc.targetDepartment}</strong>
                        </div>
                        <div className="text-[11px] text-slate-400 flex items-center gap-2">
                          <span>Attached: {doc.fileName} ({doc.fileSize})</span>
                          {doc.estimatedBudget && <span>• Budget: {doc.estimatedBudget}</span>}
                        </div>
                      </div>

                      <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                        <button
                          type="button"
                          onClick={() => setReviewingDoc(doc)}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#76d418] hover:bg-[#68bc15] text-slate-950 font-bold text-xs transition-colors cursor-pointer"
                        >
                          <ShieldCheck className="w-3.5 h-3.5" />
                          <span>{doc.status === 'Pending Review' ? 'Review & Authorize' : 'Inspect Audit'}</span>
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* View Tab: DISPATCHED JOBS TRACKING */}
          {activeTab === 'dispatches' && (
            <div className="flex-1 flex flex-col overflow-hidden">
              <div className="p-4 border-b border-slate-800 bg-[#050906] flex items-center justify-between">
                <div>
                  <h2 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                    <Briefcase className="w-4 h-4 text-[#76d418]" />
                    <span>Department Job Dispatches</span>
                  </h2>
                  <p className="text-[11px] text-slate-400">
                    Track live work orders routed to field services, technical support, and logistics.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setIsDispatchModalOpen(true)}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#76d418] text-slate-950 font-bold text-xs"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Dispatch New Job</span>
                </button>
              </div>

              <div className="flex-1 overflow-y-auto p-4 space-y-3">
                {allDispatchedJobs.length === 0 ? (
                  <div className="text-center py-12 text-slate-400">
                    <Briefcase className="w-10 h-10 text-slate-600 mx-auto mb-2" />
                    <p className="text-xs">No jobs have been dispatched yet.</p>
                  </div>
                ) : (
                  allDispatchedJobs.map(m => {
                    const disp = m.jobDispatch!;
                    return (
                      <div 
                        key={m.id}
                        className="p-4 rounded-xl bg-[#050906] border border-slate-800 hover:border-[#76d418]/30 transition-all space-y-2"
                      >
                        <div className="flex items-center justify-between">
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-[#76d418]/10 text-[#76d418] border border-[#76d418]/30">
                            #{disp.targetDepartment}
                          </span>
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                            disp.priority === 'Urgent' ? 'bg-rose-500/20 text-rose-300' : 'bg-amber-500/20 text-amber-300'
                          }`}>
                            {disp.priority} Priority
                          </span>
                        </div>

                        <div className="font-bold text-slate-100 text-sm">
                          {disp.title}
                        </div>

                        <div className="text-xs text-slate-400">
                          {disp.summary}
                        </div>

                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1 text-[11px] text-slate-300">
                          <div>
                            <span className="text-slate-500 block text-[9px]">Assignee:</span>
                            <span className="font-semibold">{disp.assignedEmployeeName || 'Department Team'}</span>
                          </div>
                          <div>
                            <span className="text-slate-500 block text-[9px]">Client / Facility:</span>
                            <span className="font-semibold">{disp.clientName || 'Internal'}</span>
                          </div>
                          <div>
                            <span className="text-slate-500 block text-[9px]">Due Date:</span>
                            <span className="font-semibold">{disp.dueDate}</span>
                          </div>
                          <div>
                            <span className="text-slate-500 block text-[9px]">Budget:</span>
                            <span className="font-semibold text-[#76d418]">{disp.budget}</span>
                          </div>
                        </div>

                        <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between">
                          <span className="text-[10px] text-slate-500">Dispatched by {m.senderName} at {m.timestamp}</span>
                          <button
                            type="button"
                            onClick={onNavigateToJobs}
                            className="flex items-center gap-1 text-xs font-bold text-[#76d418] hover:underline"
                          >
                            <span>Open in Jobs View</span>
                            <ArrowRight className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Add Employee Modal */}
      {isAddEmployeeOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-[#070d09] border border-slate-800 rounded-2xl p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                <Users className="w-4 h-4 text-[#76d418]" />
                Add Enterprise Staff Member
              </h3>
              <button onClick={() => setIsAddEmployeeOpen(false)} className="text-slate-400 hover:text-white">
                <XCircle className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateEmployee} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Full Name *</label>
                <input
                  type="text"
                  required
                  value={newEmpName}
                  onChange={(e) => setNewEmpName(e.target.value)}
                  placeholder="e.g. Jordan Mitchell"
                  className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Role / Title *</label>
                <input
                  type="text"
                  required
                  value={newEmpRole}
                  onChange={(e) => setNewEmpRole(e.target.value)}
                  placeholder="e.g. Senior Logistics Dispatcher"
                  className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Department *</label>
                <select
                  value={newEmpDept}
                  onChange={(e) => setNewEmpDept(e.target.value as Department)}
                  className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200"
                >
                  {ENTERPRISE_DEPARTMENTS.map(d => (
                    <option key={d} value={d}>{d}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Phone Number</label>
                <input
                  type="text"
                  value={newEmpPhone}
                  onChange={(e) => setNewEmpPhone(e.target.value)}
                  placeholder="+1 (555) 000-0000"
                  className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsAddEmployeeOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs text-slate-400"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-[#76d418] text-slate-950 font-bold text-xs"
                >
                  Add Staff Member
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Dispatch Job Modal */}
      <DispatchJobModal
        isOpen={isDispatchModalOpen}
        onClose={() => setIsDispatchModalOpen(false)}
        employees={employees}
        initialDepartment={isDepartment ? (activeRecipientId.replace('dept:', '') as Department) : undefined}
        onDispatch={handleDispatchJob}
      />

      {/* Send Paperwork Modal */}
      <SendPaperworkModal
        isOpen={isPaperworkModalOpen}
        onClose={() => setIsPaperworkModalOpen(false)}
        employees={employees}
        initialDepartment={isDepartment ? (activeRecipientId.replace('dept:', '') as Department) : undefined}
        onSendPaperwork={handleSendPaperwork}
      />

      {/* Document Review & Approval Modal */}
      <DocumentReviewModal
        isOpen={!!reviewingDoc}
        onClose={() => setReviewingDoc(null)}
        document={reviewingDoc}
        currentEmployee={currentEmployee}
        onApprove={handlePaperworkApproval}
        onOpenJob={() => {
          setReviewingDoc(null);
          onNavigateToJobs();
        }}
      />
    </div>
  );
};
