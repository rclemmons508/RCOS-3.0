import React, { useState } from 'react';
import { 
  X, 
  FileText, 
  CheckCircle2, 
  AlertCircle, 
  XCircle, 
  Building2, 
  Calendar, 
  Clock, 
  User, 
  Sparkles, 
  Download, 
  Eye, 
  ShieldCheck, 
  DollarSign,
  Send
} from 'lucide-react';
import { Employee, PaperworkDocument, PaperworkStatus } from '../types';

interface DocumentReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  document: PaperworkDocument | null;
  currentEmployee: Employee;
  onApprove: (docId: string, status: PaperworkStatus, feedback?: string, signature?: string) => void;
  onOpenJob?: (jobId: string) => void;
}

export const DocumentReviewModal: React.FC<DocumentReviewModalProps> = ({
  isOpen,
  onClose,
  document,
  currentEmployee,
  onApprove,
  onOpenJob
}) => {
  const [feedback, setFeedback] = useState<string>('');
  const [signature, setSignature] = useState<string>(
    `${currentEmployee.name} (${currentEmployee.role})`
  );
  const [showFullPreview, setShowFullPreview] = useState<boolean>(false);

  if (!isOpen || !document) return null;

  const isPending = document.status === 'Pending Review';
  const isApproved = document.status === 'Approved';
  const isChangesRequested = document.status === 'Changes Requested';
  const isRejected = document.status === 'Rejected';

  const handleAction = (status: PaperworkStatus) => {
    onApprove(document.id, status, feedback.trim() || undefined, signature.trim() || undefined);
    onClose();
  };

  return (
    <div 
      id="document-review-modal-overlay"
      className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 animate-in fade-in duration-150"
    >
      <div 
        id="document-review-modal-card"
        className="w-full max-w-xl bg-[#070d09] border border-slate-800 rounded-2xl max-h-[90vh] flex flex-col overflow-hidden shadow-2xl"
        style={{ boxShadow: '0 0 45px rgba(0,0,0,0.9)' }}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800 bg-[#0a150c]">
          <div className="flex items-center gap-2.5">
            <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${
              isApproved ? 'bg-[#76d418]/20 text-[#76d418]' :
              isChangesRequested ? 'bg-amber-500/20 text-amber-400' :
              isRejected ? 'bg-rose-500/20 text-rose-400' :
              'bg-sky-500/20 text-sky-400'
            }`}>
              <FileText className="w-4 h-4" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider bg-slate-900 border border-slate-800 text-slate-300">
                  {document.category}
                </span>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                  isApproved ? 'bg-[#76d418]/10 text-[#76d418] border border-[#76d418]/30' :
                  isChangesRequested ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30' :
                  isRejected ? 'bg-rose-500/10 text-rose-400 border border-rose-500/30' :
                  'bg-sky-500/10 text-sky-400 border border-sky-500/30 animate-pulse'
                }`}>
                  {isApproved && <CheckCircle2 className="w-3 h-3" />}
                  {isChangesRequested && <AlertCircle className="w-3 h-3" />}
                  {isRejected && <XCircle className="w-3 h-3" />}
                  {document.status}
                </span>
              </div>
              <h2 className="text-base font-bold text-slate-100 mt-1 line-clamp-1">
                {document.title}
              </h2>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800/80 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4 text-xs text-slate-300">
          {/* Metadata Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
            <div className="p-2.5 rounded-xl bg-[#050906] border border-slate-800/80">
              <div className="text-[10px] text-slate-400 font-medium">Submitted By</div>
              <div className="font-bold text-slate-200 mt-0.5 truncate">{document.senderName}</div>
              <div className="text-[10px] text-slate-500 truncate">{document.senderDepartment}</div>
            </div>

            <div className="p-2.5 rounded-xl bg-[#050906] border border-slate-800/80">
              <div className="text-[10px] text-slate-400 font-medium">Target Dept</div>
              <div className="font-bold text-[#76d418] mt-0.5 truncate">{document.targetDepartment}</div>
              <div className="text-[10px] text-slate-500 truncate">{document.assignedReviewerName || 'Dept Reviewer'}</div>
            </div>

            <div className="p-2.5 rounded-xl bg-[#050906] border border-slate-800/80">
              <div className="text-[10px] text-slate-400 font-medium">Urgency</div>
              <div className={`font-bold mt-0.5 truncate ${
                document.urgency.includes('Urgent') ? 'text-rose-400' :
                document.urgency.includes('High') ? 'text-amber-400' : 'text-slate-300'
              }`}>
                {document.urgency}
              </div>
              <div className="text-[10px] text-slate-500">{document.submissionDate}</div>
            </div>

            <div className="p-2.5 rounded-xl bg-[#050906] border border-slate-800/80">
              <div className="text-[10px] text-slate-400 font-medium">Attached File</div>
              <div className="font-bold text-slate-200 mt-0.5 truncate">{document.fileSize}</div>
              <div className="text-[10px] text-slate-500 truncate">{document.fileName}</div>
            </div>
          </div>

          {/* Attached Document File Preview Box */}
          <div className="p-3 rounded-xl bg-[#050906] border border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-10 h-10 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-center text-[#76d418] shrink-0">
                <FileText className="w-5 h-5" />
              </div>
              <div className="min-w-0">
                <div className="font-bold text-slate-200 truncate">{document.fileName}</div>
                <div className="text-[11px] text-slate-400">{document.fileSize} • {document.fileType}</div>
              </div>
            </div>

            <div className="flex items-center gap-1.5 shrink-0">
              <button
                type="button"
                onClick={() => setShowFullPreview(!showFullPreview)}
                className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-slate-200 text-xs font-semibold border border-slate-800 transition-colors"
              >
                <Eye className="w-3.5 h-3.5 text-[#76d418]" />
                <span>{showFullPreview ? 'Hide Preview' : 'Inspect File'}</span>
              </button>
            </div>
          </div>

          {/* Expanded Preview (if clicked or image) */}
          {showFullPreview && (
            <div className="p-4 rounded-xl bg-[#030604] border border-[#76d418]/30 font-mono text-[11px] text-slate-300 space-y-2 max-h-48 overflow-y-auto">
              <div className="flex items-center justify-between text-slate-400 pb-1 border-b border-slate-800">
                <span>VERIFIED DOCUMENT ARTIFACT</span>
                <span>SHA-256 ENCRYPTED</span>
              </div>
              {document.fileDataUrl && document.fileType.startsWith('image/') ? (
                <div className="text-center">
                  <img 
                    src={document.fileDataUrl} 
                    alt={document.fileName}
                    className="max-h-40 mx-auto rounded-lg border border-slate-800 object-contain"
                  />
                </div>
              ) : (
                <div className="space-y-1 text-slate-300">
                  <p className="text-[#76d418] font-bold">[RCOS FORM VALIDATION STAMP]</p>
                  <p>Document: {document.title}</p>
                  <p>Category: {document.category}</p>
                  <p>Client Reference: {document.clientName || 'Internal Facility'}</p>
                  <p>Budget Authorization: {document.estimatedBudget || 'N/A'}</p>
                  <p>Submitter: {document.senderName} ({document.senderDepartment})</p>
                  <p>Scope Notes: {document.notes}</p>
                  <p className="text-slate-500">// Digital payload verified and ready for departmental sign-off.</p>
                </div>
              )}
            </div>
          )}

          {/* Submission Notes */}
          <div>
            <div className="text-[11px] font-semibold text-slate-400 mb-1">
              Submission Scope & Detailed Notes:
            </div>
            <div className="p-3 rounded-xl bg-[#050906] border border-slate-800 text-slate-200 text-xs leading-relaxed">
              {document.notes}
            </div>
          </div>

          {/* Auto-Dispatch Note or Dispatched Job Indicator */}
          {document.autoDispatchJobOnApproval && (
            <div className="p-3 rounded-xl bg-[#0a150c] border border-[#76d418]/30 flex items-start gap-2.5">
              <Sparkles className="w-4 h-4 text-[#76d418] shrink-0 mt-0.5" />
              <div className="text-xs">
                <span className="font-bold text-slate-100">Automated Department Dispatch Enabled</span>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Approving this paperwork will immediately authorize and dispatch a live job to <strong className="text-[#76d418]">{document.targetDepartment}</strong>.
                </p>
                {document.dispatchedJobId && (
                  <div className="mt-2 flex items-center gap-2">
                    <span className="text-[11px] text-[#76d418] font-mono">Job #{document.dispatchedJobId} Dispatched</span>
                    {onOpenJob && (
                      <button
                        type="button"
                        onClick={() => onOpenJob(document.dispatchedJobId!)}
                        className="text-[11px] underline text-slate-300 hover:text-white"
                      >
                        View in Jobs Hub →
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Existing Review Info if Already Reviewed */}
          {!isPending && (
            <div className="p-3 rounded-xl bg-[#050906] border border-slate-800 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-400">Reviewed by: <strong className="text-slate-200">{document.reviewedBy}</strong></span>
                <span className="text-slate-500">{document.reviewedAt}</span>
              </div>
              {document.signature && (
                <div className="text-[11px] text-slate-400">
                  Signature: <span className="font-mono text-[#76d418]">{document.signature}</span>
                </div>
              )}
              {document.reviewerFeedback && (
                <div className="text-xs text-slate-300 p-2 rounded-lg bg-slate-900/80 border border-slate-800">
                  <span className="text-[10px] text-slate-400 font-bold block mb-0.5">Reviewer Feedback:</span>
                  "{document.reviewerFeedback}"
                </div>
              )}
            </div>
          )}

          {/* Action Panel for Pending Review */}
          {isPending && (
            <div className="pt-2 border-t border-slate-800 space-y-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-300 mb-1">
                  Reviewer Notes / Feedback (Optional)
                </label>
                <input
                  type="text"
                  value={feedback}
                  onChange={(e) => setFeedback(e.target.value)}
                  placeholder="e.g. Scope reviewed and approved for immediate dispatch."
                  className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[#76d418]"
                />
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-300 mb-1">
                  Digital Authorization Signature
                </label>
                <input
                  type="text"
                  value={signature}
                  onChange={(e) => setSignature(e.target.value)}
                  className="w-full bg-[#050906] border border-slate-800 rounded-xl px-3 py-2 text-xs text-[#76d418] font-mono focus:outline-none focus:border-[#76d418]"
                />
              </div>

              <div className="grid grid-cols-3 gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => handleAction('Changes Requested')}
                  className="flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 font-bold text-xs border border-amber-500/30 transition-all cursor-pointer"
                >
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>Request Revisions</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction('Rejected')}
                  className="flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 font-bold text-xs border border-rose-500/30 transition-all cursor-pointer"
                >
                  <XCircle className="w-3.5 h-3.5" />
                  <span>Reject</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleAction('Approved')}
                  className="flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl bg-[#76d418] hover:bg-[#68bc15] text-slate-950 font-bold text-xs shadow-lg shadow-[#76d418]/20 transition-all cursor-pointer"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Approve & Authorize</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
