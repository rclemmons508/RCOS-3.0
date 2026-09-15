import React, { useState } from 'react';
import { 
  X, 
  Send, 
  Briefcase, 
  Building2, 
  AlertTriangle, 
  Clock, 
  DollarSign, 
  FileText, 
  UserCheck,
  CheckCircle2
} from 'lucide-react';
import { Department, Employee, JobPriority } from '../types';
import { ENTERPRISE_DEPARTMENTS } from '../services/employeeMessagingService';

interface DispatchJobModalProps {
  isOpen: boolean;
  onClose: () => void;
  employees: Employee[];
  initialDepartment?: Department;
  onDispatch: (payload: {
    title: string;
    department: Department;
    assignedEmployeeId?: string;
    assignedEmployeeName?: string;
    clientName: string;
    priority: JobPriority;
    dueDate: string;
    budget: string;
    summary: string;
  }) => void;
}

export const DispatchJobModal: React.FC<DispatchJobModalProps> = ({
  isOpen,
  onClose,
  employees,
  initialDepartment,
  onDispatch
}) => {
  const [department, setDepartment] = useState<Department>(
    initialDepartment || 'Operations & Field Services'
  );
  const [assignedEmpId, setAssignedEmpId] = useState<string>('');
  const [title, setTitle] = useState<string>('');
  const [clientName, setClientName] = useState<string>('');
  const [priority, setPriority] = useState<JobPriority>('High');
  const [dueDate, setDueDate] = useState<string>('Today by 18:00');
  const [budget, setBudget] = useState<string>('$1,200 Labor / Parts');
  const [summary, setSummary] = useState<string>('');

  if (!isOpen) return null;

  const departmentEmployees = employees.filter(e => e.department === department);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !summary.trim()) return;

    const assignedEmp = employees.find(e => e.id === assignedEmpId);

    onDispatch({
      title: title.trim(),
      department,
      assignedEmployeeId: assignedEmp?.id,
      assignedEmployeeName: assignedEmp ? assignedEmp.name : `${department} Fleet`,
      clientName: clientName.trim() || 'Internal Enterprise Operations',
      priority,
      dueDate: dueDate.trim() || 'Immediate',
      budget: budget.trim() || 'Standard Operating Allocation',
      summary: summary.trim()
    });

    onClose();
  };

  const loadPresetTemplate = (type: 'maintenance' | 'client' | 'urgent_inspection' | 'legal') => {
    if (type === 'maintenance') {
      setTitle('HVAC System Critical Compressor Diagnostics & Filter Replacement');
      setDepartment('Operations & Field Services');
      setPriority('High');
      setDueDate('Today by 16:30');
      setBudget('$1,850 Labor & Equipment');
      setSummary('Dispatching field technician team to inspect unit #4 rooftop vibration alarms and swap secondary filtration matrix.');
      setClientName('Meridian Towers Facility');
    } else if (type === 'urgent_inspection') {
      setTitle('Site Structural & Electrical Grounding Safety Certification');
      setDepartment('Operations & Field Services');
      setPriority('Urgent');
      setDueDate('Immediate (Next 2 Hours)');
      setBudget('$2,500 Field Dispatch');
      setSummary('Perform emergency verification on transformer station tie-in before municipal grid activation.');
      setClientName('Apex Logistics Hub');
    } else if (type === 'client') {
      setTitle('Client Escalation Triage: Contract Addendum & Service SLA Review');
      setDepartment('Client Support & Accounts');
      setPriority('High');
      setDueDate('Tomorrow by 11:00');
      setBudget('$500 Account Review');
      setSummary('Review client-submitted expansion ticket, audit past 30 days uptime logs, and prep executive response memo.');
      setClientName('Vanguard Global Logistics');
    } else if (type === 'legal') {
      setTitle('Regulatory Compliance Sign-Off for Subcontractor Personnel');
      setDepartment('Legal & Compliance');
      setPriority('Normal');
      setDueDate('Within 48 Hours');
      setBudget('$400 Legal Audit');
      setSummary('Verify OSHA 30 certifications, general liability insurance endorsements, and state license status for incoming subcontractor crew.');
      setClientName('Internal Operations');
    }
  };

  return (
    <div 
      id="dispatch-job-modal-overlay"
      className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 animate-in fade-in duration-150"
    >
      <div 
        id="dispatch-job-modal-card"
        className="w-full max-w-lg bg-[#070d09] border border-slate-800 rounded-2xl max-h-[90vh] flex flex-col overflow-hidden shadow-2xl"
        style={{ boxShadow: '0 0 40px rgba(118, 212, 24, 0.15)' }}
      >
        {/* Modal Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800 bg-[#0a150c]">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-[#76d418]/10 border border-[#76d418]/30 flex items-center justify-center text-[#76d418]">
              <Send className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
                Dispatch Job to Department
              </h2>
              <p className="text-xs text-slate-400">
                Direct work order dispatch with live team tracking
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800/80 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-5 space-y-4">
          {/* Quick Presets */}
          <div>
            <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
              Quick Dispatch Templates:
            </div>
            <div className="flex flex-wrap gap-1.5">
              <button
                type="button"
                onClick={() => loadPresetTemplate('maintenance')}
                className="px-2.5 py-1 rounded-md text-[11px] bg-slate-900 hover:bg-[#76d418]/10 hover:text-[#76d418] border border-slate-800 text-slate-300 transition-colors"
              >
                + Field Maintenance
              </button>
              <button
                type="button"
                onClick={() => loadPresetTemplate('urgent_inspection')}
                className="px-2.5 py-1 rounded-md text-[11px] bg-slate-900 hover:bg-rose-500/10 hover:text-rose-400 border border-slate-800 text-slate-300 transition-colors"
              >
                + Urgent Safety Sign-off
              </button>
              <button
                type="button"
                onClick={() => loadPresetTemplate('client')}
                className="px-2.5 py-1 rounded-md text-[11px] bg-slate-900 hover:bg-sky-500/10 hover:text-sky-400 border border-slate-800 text-slate-300 transition-colors"
              >
                + Client Escalation
              </button>
              <button
                type="button"
                onClick={() => loadPresetTemplate('legal')}
                className="px-2.5 py-1 rounded-md text-[11px] bg-slate-900 hover:bg-purple-500/10 hover:text-purple-400 border border-slate-800 text-slate-300 transition-colors"
              >
                + Compliance Audit
              </button>
            </div>
          </div>

          {/* Department Selection */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
                <Building2 className="w-3.5 h-3.5 text-[#76d418]" />
                Target Department *
              </label>
              <select
                value={department}
                onChange={(e) => {
                  setDepartment(e.target.value as Department);
                  setAssignedEmpId('');
                }}
                className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-[#76d418]"
              >
                {ENTERPRISE_DEPARTMENTS.map(dept => (
                  <option key={dept} value={dept}>{dept}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
                <UserCheck className="w-3.5 h-3.5 text-[#76d418]" />
                Assignee Lead / Tech
              </label>
              <select
                value={assignedEmpId}
                onChange={(e) => setAssignedEmpId(e.target.value)}
                className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-[#76d418]"
              >
                <option value="">{department} Team (General Queue)</option>
                {departmentEmployees.map(emp => (
                  <option key={emp.id} value={emp.id}>
                    {emp.name} ({emp.role})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Job Title */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
              <Briefcase className="w-3.5 h-3.5 text-[#76d418]" />
              Work Order / Job Title *
            </label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g., Emergency Transformer Maintenance & Load Balancing"
              className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[#76d418]"
            />
          </div>

          {/* Client & Priority */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Client Organization / Facility
              </label>
              <input
                type="text"
                value={clientName}
                onChange={(e) => setClientName(e.target.value)}
                placeholder="e.g. Apex Logistics Hub or Internal"
                className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[#76d418]"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
                <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
                Dispatch Priority
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as JobPriority)}
                className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-[#76d418]"
              >
                <option value="Urgent">Urgent (Immediate Field Dispatch)</option>
                <option value="High">High Priority</option>
                <option value="Medium">Medium Priority</option>
                <option value="Normal">Normal Standard Queue</option>
              </select>
            </div>
          </div>

          {/* Due Date & Budget */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-slate-400" />
                Target Due Date / SLA
              </label>
              <input
                type="text"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                placeholder="e.g. Today by 17:00"
                className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[#76d418]"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
                <DollarSign className="w-3.5 h-3.5 text-[#76d418]" />
                Budget / Cost Estimate
              </label>
              <input
                type="text"
                value={budget}
                onChange={(e) => setBudget(e.target.value)}
                placeholder="e.g. $1,500 Labor & Parts"
                className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[#76d418]"
              />
            </div>
          </div>

          {/* Scope / Instructions */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5 text-slate-400" />
              Scope of Work & Technical Instructions *
            </label>
            <textarea
              required
              rows={3}
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
              placeholder="Provide exact procedures, entry codes, safety mandates, and deliverables expected..."
              className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[#76d418] resize-none"
            />
          </div>

          {/* Submit */}
          <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-400 hover:text-slate-200 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-[#76d418] hover:bg-[#68bc15] text-slate-950 font-bold text-xs shadow-lg shadow-[#76d418]/20 transition-all cursor-pointer"
            >
              <Send className="w-3.5 h-3.5" />
              <span>Dispatch Job Now</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
