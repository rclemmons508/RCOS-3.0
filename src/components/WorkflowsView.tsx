import React, { useState } from 'react';
import { 
  GitBranch, 
  Play, 
  CheckCircle2, 
  Clock, 
  ShieldAlert, 
  Plus, 
  ArrowRight, 
  Zap, 
  Settings2,
  RefreshCw,
  SlidersHorizontal
} from 'lucide-react';
import { Workflow } from '../types';

interface WorkflowsViewProps {
  workflows: Workflow[];
  onTriggerWorkflow: (workflowId: string) => void;
  onCreateWorkflow: (wf: Partial<Workflow>) => void;
}

export const WorkflowsView: React.FC<WorkflowsViewProps> = ({
  workflows,
  onTriggerWorkflow,
  onCreateWorkflow
}) => {
  const [selectedWorkflow, setSelectedWorkflow] = useState<Workflow | null>(workflows[0] || null);
  const [triggeringId, setTriggeringId] = useState<string | null>(null);

  const handleTrigger = (id: string) => {
    setTriggeringId(id);
    onTriggerWorkflow(id);
    setTimeout(() => {
      setTriggeringId(null);
    }, 1500);
  };

  return (
    <div id="workflows-view-container" className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <GitBranch className="w-5 h-5 text-purple-400" />
            <span>Workflow Automation Engine</span>
          </h1>
          <p className="text-xs text-slate-400">
            Multi-agent state machines with continuous trigger evaluation and risk approval gating
          </p>
        </div>

        <button
          onClick={() => {
            const newWf: Partial<Workflow> = {
              title: 'Automated Client Health Check & SLA Audit',
              category: 'Client Success',
              description: 'Weekly automated polling of client API error rates and SLA performance.',
              assignedAgent: 'Vanguard CRM',
              steps: [
                { stepNumber: 1, title: 'Extract API response latency', actionType: 'METRIC_POLL', assignedAgent: 'Vanguard CRM', isCompleted: true },
                { stepNumber: 2, title: 'Compile SLA compliance score', actionType: 'CALCULATE_SLA', assignedAgent: 'Sentinel Risk', isCompleted: true },
                { stepNumber: 3, title: 'Send proactive mitigation advice', actionType: 'NOTIFY_CLIENT', assignedAgent: 'Aegis Core', isCompleted: false }
              ]
            };
            onCreateWorkflow(newWf);
          }}
          className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-semibold flex items-center gap-2 transition-all shadow-md shadow-purple-900/30 cursor-pointer self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>New Workflow Pipeline</span>
        </button>
      </div>

      {/* Grid: Workflow List & Detail Visualizer */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Workflow Templates (1.2 cols) */}
        <div className="space-y-3">
          {workflows.map((wf) => {
            const isSelected = selectedWorkflow?.id === wf.id;
            const isTriggering = triggeringId === wf.id;

            return (
              <div
                key={wf.id}
                id={`wf-card-${wf.id}`}
                onClick={() => setSelectedWorkflow(wf)}
                className={`p-4 rounded-xl border transition-all cursor-pointer space-y-3 ${
                  isSelected
                    ? 'bg-slate-900/90 border-purple-500/50 ring-1 ring-purple-500/30'
                    : 'bg-slate-900/60 border-slate-800/80 hover:border-slate-700'
                }`}
              >
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <span className="text-[10px] font-bold uppercase tracking-wider text-purple-400">
                      {wf.category}
                    </span>
                    <h3 className="text-sm font-bold text-white leading-snug">{wf.title}</h3>
                  </div>
                  <button
                    id={`btn-trigger-wf-${wf.id}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleTrigger(wf.id);
                    }}
                    disabled={isTriggering}
                    className="p-1.5 rounded-lg bg-purple-500/10 hover:bg-purple-500/20 text-purple-300 border border-purple-500/30 text-xs font-semibold flex items-center gap-1 cursor-pointer transition-all"
                    title="Execute Workflow Now"
                  >
                    <Play className={`w-3.5 h-3.5 ${isTriggering ? 'animate-spin' : ''}`} />
                    <span className="text-[11px]">{isTriggering ? 'Running...' : 'Run'}</span>
                  </button>
                </div>

                <p className="text-xs text-slate-400 line-clamp-2">{wf.description}</p>

                <div className="flex items-center justify-between text-[11px] text-slate-500 pt-2 border-t border-slate-800/60">
                  <span>Lead: <strong className="text-slate-300">{wf.assignedAgent}</strong></span>
                  <span className="font-mono">{wf.totalExecutions} runs</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Workflow Detail & Step Execution Visualizer (1.8 cols) */}
        <div className="lg:col-span-2 bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-5">
          {selectedWorkflow ? (
            <>
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-4 border-b border-slate-800">
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-purple-400">
                    {selectedWorkflow.category}
                  </span>
                  <h2 className="text-lg font-bold text-white">{selectedWorkflow.title}</h2>
                  <p className="text-xs text-slate-400 mt-0.5">{selectedWorkflow.description}</p>
                </div>
                
                <button
                  onClick={() => handleTrigger(selectedWorkflow.id)}
                  disabled={triggeringId === selectedWorkflow.id}
                  className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold flex items-center gap-2 transition-all shadow-md cursor-pointer self-start sm:self-auto shrink-0"
                >
                  <Play className={`w-3.5 h-3.5 ${triggeringId === selectedWorkflow.id ? 'animate-spin' : ''}`} />
                  <span>{triggeringId === selectedWorkflow.id ? 'Executing Pipeline...' : 'Dispatch Pipeline'}</span>
                </button>
              </div>

              {/* Trigger & Policy Info */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="p-3 rounded-lg bg-slate-950/70 border border-slate-800 space-y-1">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-amber-400 flex items-center gap-1.5">
                    <Zap className="w-3 h-3" />
                    <span>Active Trigger</span>
                  </span>
                  <h4 className="text-xs font-semibold text-white">{selectedWorkflow.trigger.name}</h4>
                  <p className="text-[11px] text-slate-400">{selectedWorkflow.trigger.description}</p>
                </div>

                <div className="p-3 rounded-lg bg-slate-950/70 border border-slate-800 space-y-1">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-400 flex items-center gap-1.5">
                    <ShieldAlert className="w-3 h-3" />
                    <span>Risk & Approval Gate</span>
                  </span>
                  <h4 className="text-xs font-semibold text-white">Policy: {selectedWorkflow.approvalPolicy.level}</h4>
                  <p className="text-[11px] text-slate-400">
                    Approver: {selectedWorkflow.approvalPolicy.approverRole} (Threshold: &lt;{selectedWorkflow.approvalPolicy.riskThresholdPct}%)
                  </p>
                </div>
              </div>

              {/* Step Sequence Flow */}
              <div className="space-y-3">
                <h4 className="text-xs font-bold text-white uppercase tracking-wider">
                  Sequential Execution Steps ({selectedWorkflow.steps.length})
                </h4>

                <div className="space-y-2.5">
                  {selectedWorkflow.steps.map((step, i) => (
                    <div
                      key={step.stepNumber}
                      className="flex items-center gap-3 p-3 rounded-xl bg-slate-950/80 border border-slate-800/90 text-xs"
                    >
                      <div className="w-6 h-6 rounded-full bg-purple-500/20 text-purple-300 flex items-center justify-center font-bold text-[11px] shrink-0 border border-purple-500/30">
                        {step.stepNumber}
                      </div>

                      <div className="flex-1">
                        <div className="font-semibold text-white">{step.title}</div>
                        <div className="text-[11px] text-slate-400 flex items-center gap-2 mt-0.5">
                          <span className="font-mono text-purple-300">{step.actionType}</span>
                          <span>•</span>
                          <span>Assigned to: <strong className="text-slate-300">{step.assignedAgent}</strong></span>
                        </div>
                      </div>

                      {step.isCompleted ? (
                        <span className="flex items-center gap-1 text-[11px] text-emerald-400 font-semibold shrink-0">
                          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                          <span>Ready</span>
                        </span>
                      ) : (
                        <span className="flex items-center gap-1 text-[11px] text-slate-500 shrink-0">
                          <Clock className="w-3.5 h-3.5" />
                          <span>Pending</span>
                        </span>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </>
          ) : (
            <div className="py-12 text-center text-xs text-slate-400">
              Select a workflow template to inspect its trigger conditions, approval thresholds, and automated agent steps.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
