import React, { useState } from 'react';
import { 
  X, 
  UploadCloud, 
  FileText, 
  ShieldCheck, 
  Building2, 
  UserCheck, 
  AlertCircle, 
  Check, 
  Paperclip,
  CheckCircle2,
  Sparkles
} from 'lucide-react';
import { 
  Department, 
  Employee, 
  PaperworkCategory, 
  PaperworkDocument, 
  PaperworkUrgency 
} from '../types';
import { ENTERPRISE_DEPARTMENTS } from '../services/employeeMessagingService';

interface SendPaperworkModalProps {
  isOpen: boolean;
  onClose: () => void;
  employees: Employee[];
  initialDepartment?: Department;
  onSendPaperwork: (doc: PaperworkDocument, customMessage?: string) => void;
}

export const SendPaperworkModal: React.FC<SendPaperworkModalProps> = ({
  isOpen,
  onClose,
  employees,
  initialDepartment,
  onSendPaperwork
}) => {
  const [title, setTitle] = useState<string>('');
  const [category, setCategory] = useState<PaperworkCategory>('Work Order');
  const [targetDepartment, setTargetDepartment] = useState<Department>(
    initialDepartment || 'Legal & Compliance'
  );
  const [assignedReviewerId, setAssignedReviewerId] = useState<string>('');
  const [urgency, setUrgency] = useState<PaperworkUrgency>('High Priority');
  const [fileName, setFileName] = useState<string>('Standard_Work_Order_2026.pdf');
  const [fileSize, setFileSize] = useState<string>('1.4 MB');
  const [fileType, setFileType] = useState<string>('application/pdf');
  const [fileDataUrl, setFileDataUrl] = useState<string | undefined>(undefined);
  const [notes, setNotes] = useState<string>('');
  const [autoDispatch, setAutoDispatch] = useState<boolean>(true);
  const [estimatedBudget, setEstimatedBudget] = useState<string>('$1,500');
  const [clientName, setClientName] = useState<string>('Apex Logistics Hub');
  const [customMessage, setCustomMessage] = useState<string>('');

  if (!isOpen) return null;

  const departmentEmployees = employees.filter(e => e.department === targetDepartment);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setFileName(file.name);
      setFileSize(`${(file.size / (1024 * 1024)).toFixed(2)} MB`);
      setFileType(file.type || 'application/octet-stream');
      
      const reader = new FileReader();
      reader.onload = (event) => {
        setFileDataUrl(event.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const loadPaperworkTemplate = (type: PaperworkCategory) => {
    setCategory(type);
    if (type === 'Work Order') {
      setTitle('Subcontractor Fiber Splicing Work Order Authorization');
      setFileName('Subcontractor_Fiber_WO_Auth_v3.pdf');
      setFileSize('1.2 MB');
      setTargetDepartment('Operations & Field Services');
      setUrgency('High Priority');
      setNotes('Requires departmental approval before technicians enter active trench zone. Includes safety checklist, contractor licenses, and emergency contact list.');
      setAutoDispatch(true);
      setEstimatedBudget('$2,200 Labor');
      setClientName('Metropolitan Fiber Ring');
    } else if (type === 'Change Order') {
      setTitle('Change Order #04: Additional Sub-Panel Conduit Installation');
      setFileName('CO_04_SubPanel_Extension.pdf');
      setFileSize('850 KB');
      setTargetDepartment('Legal & Compliance');
      setUrgency('Routine');
      setNotes('Client requested 40ft conduit reroute to clear structural HVAC duct. $1,400 additional billable scope.');
      setAutoDispatch(true);
      setEstimatedBudget('$1,400 Billable Scope');
      setClientName('Apex Logistics Hub');
    } else if (type === 'Site Inspection') {
      setTitle('High-Voltage Transformer Grounding & Arc-Flash Safety Sign-Off');
      setFileName('Arc_Flash_Safety_Clearance_2026.pdf');
      setFileSize('2.6 MB');
      setTargetDepartment('Legal & Compliance');
      setUrgency('Urgent / Immediate');
      setNotes('Certified engineer field readings verified. Requires supervisor and compliance counter-signature before re-energizing substation.');
      setAutoDispatch(false);
      setEstimatedBudget('$800 Audit');
      setClientName('North Campus Industrial');
    } else if (type === 'Invoice & Billing') {
      setTitle('Milestone 2 Milestone Completion & Client Invoice Authorization');
      setFileName('Invoice_Milestone2_Release.pdf');
      setFileSize('420 KB');
      setTargetDepartment('Executive Leadership');
      setUrgency('High Priority');
      setNotes('Milestone 2 acceptance signed by client site director. Requesting release of invoice #INV-9021 for $18,500.');
      setAutoDispatch(false);
      setEstimatedBudget('$18,500 Billed');
      setClientName('Vanguard Global Logistics');
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !notes.trim()) return;

    const assignedReviewer = employees.find(e => e.id === assignedReviewerId);

    const docItem: PaperworkDocument = {
      id: `doc-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
      title: title.trim(),
      category,
      fileName,
      fileSize,
      fileType,
      fileDataUrl,
      senderId: 'emp-01',
      senderName: 'Alex Mercer',
      senderDepartment: 'Executive Leadership',
      targetDepartment,
      assignedReviewerId: assignedReviewer?.id,
      assignedReviewerName: assignedReviewer ? assignedReviewer.name : `${targetDepartment} Approver`,
      submissionDate: 'Just now',
      urgency,
      status: 'Pending Review',
      notes: notes.trim(),
      autoDispatchJobOnApproval: autoDispatch,
      estimatedBudget: estimatedBudget.trim() || undefined,
      clientName: clientName.trim() || undefined
    };

    onSendPaperwork(docItem, customMessage.trim() || undefined);
    onClose();
  };

  return (
    <div 
      id="send-paperwork-modal-overlay"
      className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 animate-in fade-in duration-150"
    >
      <div 
        id="send-paperwork-modal-card"
        className="w-full max-w-lg bg-[#070d09] border border-slate-800 rounded-2xl max-h-[90vh] flex flex-col overflow-hidden shadow-2xl"
        style={{ boxShadow: '0 0 40px rgba(118, 212, 24, 0.15)' }}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800 bg-[#0a150c]">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-[#76d418]/10 border border-[#76d418]/30 flex items-center justify-center text-[#76d418]">
              <FileText className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
                Send Document / Paperwork for Review
              </h2>
              <p className="text-xs text-slate-400">
                Direct paperwork submission with signature & approval workflow
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

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-5 space-y-4">
          {/* Quick Paperwork Presets */}
          <div>
            <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
              Load Preset Paperwork Template:
            </div>
            <div className="flex flex-wrap gap-1.5">
              <button
                type="button"
                onClick={() => loadPaperworkTemplate('Work Order')}
                className="px-2.5 py-1 rounded-md text-[11px] bg-slate-900 hover:bg-[#76d418]/10 hover:text-[#76d418] border border-slate-800 text-slate-300 transition-colors"
              >
                + Work Order
              </button>
              <button
                type="button"
                onClick={() => loadPaperworkTemplate('Change Order')}
                className="px-2.5 py-1 rounded-md text-[11px] bg-slate-900 hover:bg-amber-500/10 hover:text-amber-400 border border-slate-800 text-slate-300 transition-colors"
              >
                + Change Order
              </button>
              <button
                type="button"
                onClick={() => loadPaperworkTemplate('Site Inspection')}
                className="px-2.5 py-1 rounded-md text-[11px] bg-slate-900 hover:bg-rose-500/10 hover:text-rose-400 border border-slate-800 text-slate-300 transition-colors"
              >
                + Site Inspection Sign-off
              </button>
              <button
                type="button"
                onClick={() => loadPaperworkTemplate('Invoice & Billing')}
                className="px-2.5 py-1 rounded-md text-[11px] bg-slate-900 hover:bg-emerald-500/10 hover:text-emerald-400 border border-slate-800 text-slate-300 transition-colors"
              >
                + Billing Release
              </button>
            </div>
          </div>

          {/* Paperwork Title */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5 text-[#76d418]" />
              Document / Paperwork Title *
            </label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Hazardous Materials Site Clearance Sign-Off"
              className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[#76d418]"
            />
          </div>

          {/* Category & Urgency */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Category
              </label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value as PaperworkCategory)}
                className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-[#76d418]"
              >
                <option value="Work Order">Work Order</option>
                <option value="Change Order">Change Order</option>
                <option value="Site Inspection">Site Inspection / Safety</option>
                <option value="Subcontractor Agreement">Subcontractor Agreement</option>
                <option value="Invoice & Billing">Invoice & Billing Approval</option>
                <option value="Safety Sign-Off">Safety Sign-Off</option>
                <option value="General Document">General Document</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
                <AlertCircle className="w-3.5 h-3.5 text-amber-400" />
                Urgency
              </label>
              <select
                value={urgency}
                onChange={(e) => setUrgency(e.target.value as PaperworkUrgency)}
                className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-[#76d418]"
              >
                <option value="Urgent / Immediate">Urgent / Immediate (Blocking Operations)</option>
                <option value="High Priority">High Priority (Within 4 Hours)</option>
                <option value="Routine">Routine Review (Standard SLA)</option>
              </select>
            </div>
          </div>

          {/* Target Department & Reviewer */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
                <Building2 className="w-3.5 h-3.5 text-[#76d418]" />
                Target Review Department *
              </label>
              <select
                value={targetDepartment}
                onChange={(e) => {
                  setTargetDepartment(e.target.value as Department);
                  setAssignedReviewerId('');
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
                Assigned Reviewer (Optional)
              </label>
              <select
                value={assignedReviewerId}
                onChange={(e) => setAssignedReviewerId(e.target.value)}
                className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-[#76d418]"
              >
                <option value="">Any Department Reviewer</option>
                {departmentEmployees.map(emp => (
                  <option key={emp.id} value={emp.id}>
                    {emp.name} ({emp.role})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* File Upload Box */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
              <Paperclip className="w-3.5 h-3.5 text-[#76d418]" />
              Attach Document / File (PDF, DOC, Images, Sheets)
            </label>
            <div className="border-2 border-dashed border-slate-800 hover:border-[#76d418]/60 bg-[#050906] rounded-xl p-4 text-center transition-colors">
              <input 
                type="file" 
                id="paperwork-file-input"
                className="hidden" 
                onChange={handleFileUpload}
                accept=".pdf,.doc,.docx,.png,.jpg,.jpeg,.xlsx,.txt"
              />
              <label htmlFor="paperwork-file-input" className="cursor-pointer block">
                <UploadCloud className="w-8 h-8 text-[#76d418] mx-auto mb-2 opacity-80" />
                <div className="text-xs font-bold text-slate-200">
                  {fileName ? (
                    <span className="text-[#76d418] flex items-center justify-center gap-1.5">
                      <CheckCircle2 className="w-3.5 h-3.5 text-[#76d418]" />
                      Attached: {fileName} ({fileSize})
                    </span>
                  ) : (
                    'Click to upload paperwork file or drag here'
                  )}
                </div>
                <div className="text-[11px] text-slate-400 mt-1">
                  Supports PDF, Work Orders, Inspection Images, Excel Sheets
                </div>
              </label>
            </div>
          </div>

          {/* Review Notes / Summary */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              Submission Scope & Review Instructions *
            </label>
            <textarea
              required
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Explain what needs to be verified, signatures required, or special precautions..."
              className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[#76d418] resize-none"
            />
          </div>

          {/* Auto-Dispatch on Approval Toggle */}
          <div className="p-3 rounded-xl bg-[#0a150c] border border-[#76d418]/30">
            <label className="flex items-start gap-2.5 cursor-pointer">
              <input
                type="checkbox"
                checked={autoDispatch}
                onChange={(e) => setAutoDispatch(e.target.checked)}
                className="mt-0.5 w-4 h-4 rounded text-[#76d418] accent-[#76d418] cursor-pointer"
              />
              <div>
                <div className="text-xs font-bold text-slate-100 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-[#76d418]" />
                  Auto-Dispatch Job to Department Upon Approval
                </div>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  When the reviewer clicks "Approve", RCOS will automatically generate and dispatch an active job in the system.
                </p>
              </div>
            </label>

            {autoDispatch && (
              <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2 border-t border-slate-800/80">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 mb-1">
                    Client / Facility
                  </label>
                  <input
                    type="text"
                    value={clientName}
                    onChange={(e) => setClientName(e.target.value)}
                    placeholder="e.g. Apex Logistics Hub"
                    className="w-full bg-[#050906] border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-200"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 mb-1">
                    Allocated Budget
                  </label>
                  <input
                    type="text"
                    value={estimatedBudget}
                    onChange={(e) => setEstimatedBudget(e.target.value)}
                    placeholder="e.g. $1,500"
                    className="w-full bg-[#050906] border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-200"
                  />
                </div>
              </div>
            )}
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
              <FileText className="w-3.5 h-3.5" />
              <span>Send Paperwork for Review</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
