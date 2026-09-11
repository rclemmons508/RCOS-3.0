import React, { useState } from 'react';
import { 
  Archive, 
  FileText, 
  Download, 
  CheckCircle2, 
  Lock, 
  Share2, 
  Search,
  Sparkles,
  Copy
} from 'lucide-react';
import { IntelligenceReport } from '../types';

interface IntelligenceVaultViewProps {
  reports: IntelligenceReport[];
}

export const IntelligenceVaultView: React.FC<IntelligenceVaultViewProps> = ({ reports }) => {
  const [selectedReport, setSelectedReport] = useState<IntelligenceReport | null>(reports[0] || null);
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    if (!selectedReport) return;
    navigator.clipboard.writeText(
      `${selectedReport.title}\nDate: ${selectedReport.date}\nAuthor: ${selectedReport.authorAgent}\n\nSummary:\n${selectedReport.summary}\n\nKey Findings:\n${selectedReport.keyFindings.join('\n')}\n\nAction Items:\n${selectedReport.actionItems.join('\n')}`
    );
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div id="intelligence-vault-container" className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <Archive className="w-5 h-5 text-emerald-400" />
            <span>Saved Intelligence & Executive Vault</span>
          </h1>
          <p className="text-xs text-slate-400">
            Archived autonomous research briefs, strategic memos, and market telemetry
          </p>
        </div>

        <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-xs font-semibold text-emerald-400">
          <Lock className="w-3.5 h-3.5" />
          <span>Encrypted Storage: SOC2 Compliant</span>
        </div>
      </div>

      {/* Grid: Reports list & Detailed viewer */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Reports list (1 col) */}
        <div className="space-y-3">
          {reports.map((rep) => {
            const isSelected = selectedReport?.id === rep.id;

            return (
              <div
                key={rep.id}
                onClick={() => setSelectedReport(rep)}
                className={`p-4 rounded-xl border transition-all cursor-pointer space-y-2 ${
                  isSelected
                    ? 'bg-slate-900/90 border-emerald-500/50 ring-1 ring-emerald-500/30 shadow-md'
                    : 'bg-slate-900/60 border-slate-800/80 hover:border-slate-700'
                }`}
              >
                <div className="flex items-start justify-between gap-2">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-400">
                    {rep.category}
                  </span>
                  {rep.confidential && (
                    <span className="text-[9px] px-1.5 py-0.5 rounded font-bold uppercase bg-rose-500/20 text-rose-400 border border-rose-500/30 flex items-center gap-1">
                      <Lock className="w-2.5 h-2.5" />
                      <span>Confidential</span>
                    </span>
                  )}
                </div>

                <h3 className="text-xs font-bold text-white leading-snug line-clamp-2">
                  {rep.title}
                </h3>

                <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1 border-t border-slate-800/60">
                  <span>Author: <strong className="text-slate-300">{rep.authorAgent}</strong></span>
                  <span>{rep.date}</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Selected Report Reader (2 cols) */}
        <div className="lg:col-span-2 bg-slate-900/80 border border-slate-800 rounded-xl p-6 space-y-5">
          {selectedReport ? (
            <>
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 pb-4 border-b border-slate-800">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold uppercase tracking-wider text-emerald-400">
                      {selectedReport.category}
                    </span>
                    <span className="text-slate-500">•</span>
                    <span className="text-xs text-slate-400">{selectedReport.date}</span>
                  </div>
                  <h2 className="text-lg font-bold text-white leading-snug">
                    {selectedReport.title}
                  </h2>
                  <span className="text-xs text-slate-400 block">
                    Author: <strong className="text-emerald-400">{selectedReport.authorAgent}</strong>
                  </span>
                </div>

                <button
                  onClick={handleCopy}
                  className="px-3.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer shrink-0"
                >
                  <Copy className="w-3.5 h-3.5" />
                  <span>{copied ? 'Copied!' : 'Copy Memo'}</span>
                </button>
              </div>

              {/* Executive Summary */}
              <div className="space-y-2">
                <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                  Executive Briefing Summary
                </h4>
                <p className="text-xs text-slate-200 leading-relaxed bg-slate-950/70 p-4 rounded-xl border border-slate-800/80">
                  {selectedReport.summary}
                </p>
              </div>

              {/* Key Findings */}
              <div className="space-y-2.5">
                <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                  Key Strategic Findings
                </h4>
                <div className="space-y-2">
                  {selectedReport.keyFindings.map((finding, idx) => (
                    <div key={idx} className="flex items-start gap-2.5 p-3 rounded-lg bg-slate-950/50 border border-slate-800 text-xs text-slate-300">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                      <span>{finding}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Recommended Action Items */}
              <div className="space-y-2.5">
                <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                  Recommended Organizational Actions
                </h4>
                <div className="space-y-2">
                  {selectedReport.actionItems.map((action, idx) => (
                    <div key={idx} className="flex items-start gap-2.5 p-3 rounded-lg bg-emerald-950/20 border border-emerald-500/20 text-xs text-emerald-300">
                      <Sparkles className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                      <span>{action}</span>
                    </div>
                  ))}
                </div>
              </div>
            </>
          ) : (
            <div className="py-12 text-center text-xs text-slate-400">
              Select an intelligence brief to inspect full analytical synthesis and recommendations.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
