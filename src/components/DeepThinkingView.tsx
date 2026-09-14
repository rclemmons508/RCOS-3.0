import React, { useState } from 'react';
import { 
  BrainCircuit, 
  Sparkles, 
  CheckCircle2, 
  ArrowRight, 
  Activity, 
  Sliders, 
  ShieldCheck, 
  Clock 
} from 'lucide-react';

export const DeepThinkingView: React.FC = () => {
  const [problemStatement, setProblemStatement] = useState(
    'Simulate global logistics freight cost spikes of +25% across maritime corridors and design an autonomous mitigation strategy utilizing air-freight consolidation and dynamic carrier SLA bidding.'
  );
  const [isSolving, setIsSolving] = useState(false);
  const [thoughtSteps, setThoughtSteps] = useState<Array<{ stage: string; summary: string; detail: string }>>([
    {
      stage: 'Phase 1: Multi-Factor Constraint Extraction',
      summary: 'Isolated 3 primary cost drivers across Atlantic & Pacific shipping lanes.',
      detail: 'Base fuel indexing (+12%), port congestion demurrage penalties in Long Beach, and carrier vessel allocation caps.'
    },
    {
      stage: 'Phase 2: Sub-Agent Cross-Dependency Evaluation',
      summary: 'Synthesized telemetry between Vanguard CRM and Pulse Voice.',
      detail: 'Client contracts with Apex Logistics specify a maximum allowable delay of 72 hours before contractual liquidated damages apply ($45,000/day).'
    },
    {
      stage: 'Phase 3: Autonomous Strategy Synthesis',
      summary: 'Formulated dynamic split-routing protocol with secondary rail intermodal carriers.',
      detail: 'Recommendation: Route 35% of high-margin cargo via expedited Pacific air freight and route remaining 65% through Vancouver rail hub.'
    }
  ]);

  const handleRunThinking = () => {
    if (!problemStatement.trim()) return;
    setIsSolving(true);

    setTimeout(() => {
      setIsSolving(false);
      setThoughtSteps([
        {
          stage: 'Phase 1: Deep Enterprise Constraint Ingestion',
          summary: 'Extracted semantic boundaries and regulatory guidelines.',
          detail: 'Evaluated budget constraints, compliance barriers, and historical resolution paths under SOC2 guidelines.'
        },
        {
          stage: 'Phase 2: Multi-Agent Probability Modeling',
          summary: 'Simulated 10,000 execution paths with Gemini Reasoning.',
          detail: 'Optimal policy path selected with 98.4% confidence rating and zero high-risk permission escalations.'
        },
        {
          stage: 'Phase 3: Orchestrated Action Plan',
          summary: 'Generated phased rollout schedule for RCOS agents.',
          detail: 'Automated triggers queued in Workflow Engine; assigned leads: Aegis Core, Sentinel Risk, and Vanguard CRM.'
        }
      ]);
    }, 1500);
  };

  return (
    <div id="deep-thinking-container" className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <BrainCircuit className="w-5 h-5 text-emerald-400" />
            <span>Deep Thinking Strategic Engine</span>
          </h1>
          <p className="text-xs text-slate-400">
            Multi-stage reasoning workbench powered by Gemini for high-stakes operational planning
          </p>
        </div>

        <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-xs font-semibold text-emerald-400">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Reasoning Engine: Active</span>
        </div>
      </div>

      {/* Input Workbench */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4">
        <label className="text-xs font-bold text-white block">
          Enterprise Scenario / Complex Strategic Directive
        </label>
        <textarea
          id="textarea-deep-thinking"
          rows={3}
          value={problemStatement}
          onChange={(e) => setProblemStatement(e.target.value)}
          placeholder="Describe an enterprise problem for multi-step reasoning..."
          className="w-full p-3.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-emerald-500"
        />

        <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
          <div className="flex items-center gap-3 text-xs text-slate-400">
            <span>Model: <strong className="text-white font-mono">Gemini 2.5 Pro (Thinking Budget: 8k)</strong></span>
            <span>•</span>
            <span>Risk Gating: <strong className="text-emerald-400">Autonomous &lt;20%</strong></span>
          </div>

          <button
            id="btn-run-deep-thinking"
            onClick={handleRunThinking}
            disabled={isSolving || !problemStatement.trim()}
            className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 disabled:opacity-40 text-white text-xs font-bold flex items-center gap-2 transition-all shadow-md shadow-emerald-900/30 cursor-pointer"
          >
            <BrainCircuit className={`w-4 h-4 ${isSolving ? 'animate-spin' : ''}`} />
            <span>{isSolving ? 'Synthesizing Thought Vectors...' : 'Execute Deep Reasoning'}</span>
          </button>
        </div>
      </div>

      {/* Thought Process Chain */}
      <div className="space-y-4">
        <h3 className="text-sm font-bold text-white flex items-center gap-2">
          <span>Decomposed Logical Steps & Synthesis</span>
        </h3>

        <div className="space-y-3">
          {thoughtSteps.map((step, idx) => (
            <div
              key={idx}
              className="p-4 rounded-xl bg-slate-900/90 border border-slate-800 space-y-2 relative overflow-hidden"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-emerald-400 uppercase tracking-wider">
                  {step.stage}
                </span>
                <span className="text-[11px] font-mono text-slate-500">Step {idx + 1} of {thoughtSteps.length}</span>
              </div>

              <h4 className="text-xs font-semibold text-white">{step.summary}</h4>
              <p className="text-xs text-slate-300 leading-relaxed font-mono bg-slate-950/60 p-3 rounded-lg border border-slate-800/80">
                {step.detail}
              </p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
