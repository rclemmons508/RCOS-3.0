import React, { useState } from 'react';
import { 
  LayoutGrid, 
  Bot, 
  Zap, 
  Workflow as WorkflowIcon, 
  ChevronRight, 
  CheckCircle2, 
  Sliders, 
  PhoneCall, 
  Briefcase, 
  Users, 
  Plus, 
  Sparkles,
  Send,
  Cloud,
  X,
  MessageSquare
} from 'lucide-react';
import { Agent, Job, Workflow, CallRecord } from '../types';
import { RcosLogo } from './RcosLogo';

interface DashboardViewProps {
  agents: Agent[];
  jobs: Job[];
  workflows: Workflow[];
  calls: CallRecord[];
  onNavigate: (tab: string) => void;
  onSelectAgent: (agentId: string) => void;
  onDirectTask: (agentId: string, task: string) => void;
  onOpenSetup: () => void;
  orgName?: string;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  agents,
  jobs,
  workflows,
  calls,
  onNavigate,
  onSelectAgent,
  onDirectTask,
  onOpenSetup,
  orgName
}) => {
  const [selectedAgentForDirective, setSelectedAgentForDirective] = useState<Agent | null>(null);
  const [directiveText, setDirectiveText] = useState<string>('');
  const [overviewSubTab, setOverviewSubTab] = useState<'overview' | 'agents'>('overview');

  const activeAgentsCount = agents.filter(a => a.status === 'active' || a.status === 'executing').length;

  const handleSendDirective = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAgentForDirective || !directiveText.trim()) return;
    onDirectTask(selectedAgentForDirective.id, directiveText.trim());
    setDirectiveText('');
    setSelectedAgentForDirective(null);
  };

  return (
    <div id="rcos-dashboard-container" className="space-y-4 pb-20 max-w-md md:max-w-xl mx-auto">
      {/* Subheader Status matching Screenshot 1 */}
      <div className="flex items-center justify-between pt-1 px-1">
        <div className="flex items-center gap-2.5">
          <RcosLogo size={38} />
          <div className="flex items-center gap-1.5">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#76d418] opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-[#76d418]"></span>
            </span>
            <span className="text-xs font-semibold text-slate-300">Operating System Active</span>
          </div>
        </div>

        {orgName && (
          <span className="text-xs font-semibold text-[#76d418] bg-[#0a150c] border border-[#76d418]/40 px-2.5 py-0.5 rounded-full">
            {orgName}
          </span>
        )}
      </div>

      {/* Hero Title Section matching Screenshot 1 */}
      <div className="text-center py-2">
        <h1 
          className="text-5xl sm:text-6xl font-black tracking-tight text-[#76d418] select-none"
          style={{
            textShadow: '0 0 25px rgba(118, 212, 24, 0.35)'
          }}
        >
          RCOS
        </h1>
        <h2 className="text-lg sm:text-xl font-black tracking-wider text-white uppercase mt-0.5">
          RCOS MASTER APP
        </h2>
        <p className="text-xs sm:text-sm text-slate-400 font-medium mt-0.5">
          Business Operating System
        </p>
      </div>

      {/* Switcher Pills matching Screenshot 1 */}
      <div className="flex items-center justify-center gap-2.5 px-1">
        {/* "Executive Overview" active solid lime button */}
        <button
          id="btn-tab-executive-overview"
          onClick={() => setOverviewSubTab('overview')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all cursor-pointer ${
            overviewSubTab === 'overview'
              ? 'bg-[#76d418] text-slate-950 shadow-lg shadow-[#76d418]/25'
              : 'bg-[#0c141d] text-white border border-slate-800 hover:border-slate-700'
          }`}
        >
          <LayoutGrid className="w-4 h-4" />
          <span>Executive Overview</span>
        </button>

        {/* "Autonomous Agents (5)" pill */}
        <button
          id="btn-tab-autonomous-agents"
          onClick={() => onNavigate('agents')}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold bg-[#0c141d] text-white border border-slate-800 hover:border-[#76d418]/50 transition-all cursor-pointer"
        >
          <Bot className="w-4 h-4 text-[#76d418]" />
          <span>Autonomous Agents</span>
          <span className="w-5 h-5 rounded-full bg-[#76d418] text-slate-950 text-[11px] font-black flex items-center justify-center">
            {agents.length}
          </span>
        </button>
      </div>

      {/* LIVE AGENT FLEET PULSE Container matching Screenshot 1 */}
      <div 
        id="live-agent-fleet-pulse-card"
        className="rounded-2xl p-4 bg-[#0a1218]/90 border border-slate-800/90 shadow-xl"
        style={{
          boxShadow: '0 4px 20px rgba(0,0,0,0.6)'
        }}
      >
        {/* Header Row */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#76d418] animate-pulse" />
            <span className="text-xs font-bold uppercase tracking-wider text-[#76d418]">
              LIVE AGENT FLEET PULSE
            </span>
          </div>

          <button
            id="btn-view-all-agents"
            onClick={() => onNavigate('agents')}
            className="text-xs font-semibold text-cyan-400 hover:text-cyan-300 transition-colors cursor-pointer"
          >
            View All ({agents.length})
          </button>
        </div>

        {/* Description */}
        <p className="text-xs text-slate-400 mt-2 mb-3 leading-relaxed">
          Click on any agent to inspect live in-flight task execution and issue real-time steering directives:
        </p>

        {/* Horizontal scroll of agent cards */}
        <div className="flex items-stretch gap-3 overflow-x-auto pb-2 scrollbar-thin">
          {agents.map((agent) => (
            <div
              key={agent.id}
              id={`pulse-agent-${agent.id}`}
              onClick={() => setSelectedAgentForDirective(agent)}
              className="w-64 flex-shrink-0 rounded-xl p-3 bg-[#0c1520] hover:bg-[#101c2a] border border-slate-800 hover:border-[#76d418]/60 transition-all cursor-pointer flex flex-col justify-between group"
            >
              <div>
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-emerald-950/80 border border-[#76d418]/30 flex items-center justify-center text-[#76d418]">
                    <Bot className="w-4 h-4 text-[#76d418]" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="text-xs font-bold text-white truncate group-hover:text-[#76d418] transition-colors">
                      {agent.name}
                    </h4>
                    <p className="text-[11px] text-slate-400 truncate">
                      {agent.department}
                    </p>
                  </div>
                </div>

                {/* Status Pill */}
                <div className="mt-2.5 px-2.5 py-1 rounded-md bg-slate-900/90 border border-slate-800/80 flex items-center gap-1.5 text-[11px] text-slate-200">
                  <Zap className="w-3 h-3 text-[#76d418]" />
                  <span className="truncate">{agent.currentTask || 'Executing Workflow Pipeline'}</span>
                </div>
              </div>

              {/* Bottom action indicator */}
              <div className="mt-3 flex items-center justify-between text-[11px] font-semibold text-[#76d418]">
                <span className="flex items-center gap-1 text-[11px]">
                  <span className="w-1.5 h-1.5 rounded-full bg-[#76d418]" />
                  Live • Tap to Direct
                </span>
                <ChevronRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Big Action Buttons matching Screenshot 1 */}
      <div className="space-y-2.5">
        {/* Button 1: Active Autonomous Agents */}
        <div
          id="btn-card-active-agents"
          onClick={() => onNavigate('agents')}
          className="rounded-2xl p-4 bg-[#0e1620] hover:bg-[#121c29] border border-slate-800 hover:border-[#76d418]/60 transition-all flex items-center justify-between cursor-pointer group shadow-lg"
        >
          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-emerald-950/70 border border-[#76d418]/30 flex items-center justify-center text-[#76d418]">
              <Bot className="w-5 h-5 text-[#76d418]" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white group-hover:text-[#76d418] transition-colors">
                Active Autonomous Agents
              </h3>
              <p className="text-xs text-slate-400">
                {activeAgentsCount} of {agents.length} agents executing tasks
              </p>
            </div>
          </div>

          <ChevronRight className="w-5 h-5 text-slate-500 group-hover:text-[#76d418] transition-transform group-hover:translate-x-1" />
        </div>

        {/* Button 2: AI Workflow Engine */}
        <div
          id="btn-card-ai-workflow-engine"
          onClick={() => onNavigate('workflows')}
          className="rounded-2xl p-4 bg-[#0e1620] hover:bg-[#121c29] border border-slate-800 hover:border-[#76d418]/60 transition-all flex items-center justify-between cursor-pointer group shadow-lg"
        >
          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-emerald-950/70 border border-[#76d418]/30 flex items-center justify-center text-[#76d418]">
              <WorkflowIcon className="w-5 h-5 text-[#76d418]" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white group-hover:text-[#76d418] transition-colors">
                AI Workflow Engine
              </h3>
              <p className="text-xs text-slate-400">
                {workflows.length} pipelines active with automated triggers
              </p>
            </div>
          </div>

          <ChevronRight className="w-5 h-5 text-slate-500 group-hover:text-[#76d418] transition-transform group-hover:translate-x-1" />
        </div>

        {/* Button 3: Google Workspace Sync */}
        <div
          id="btn-card-google-workspace-sync"
          onClick={() => onNavigate('workspace')}
          className="rounded-2xl p-4 bg-[#0a1510] hover:bg-[#0e1e16] border border-[#76d418]/40 hover:border-[#76d418] transition-all flex items-center justify-between cursor-pointer group shadow-lg shadow-[#76d418]/5"
        >
          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-[#76d418]/15 border border-[#76d418]/40 flex items-center justify-center text-[#76d418]">
              <Cloud className="w-5 h-5 text-[#76d418]" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-white group-hover:text-[#76d418] transition-colors">
                  Google Workspace Sync
                </h3>
                <span className="px-2 py-0.5 rounded-full bg-[#76d418]/20 border border-[#76d418]/40 text-[#76d418] text-[10px] font-bold">
                  OAuth 2.0
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Google Drive files, Calendar schedule & Gmail live synchronization
              </p>
            </div>
          </div>

          <ChevronRight className="w-5 h-5 text-slate-500 group-hover:text-[#76d418] transition-transform group-hover:translate-x-1" />
        </div>

        {/* Button 4: Employee Direct Messaging & Job Dispatch */}
        <div
          id="btn-card-team-messaging"
          onClick={() => onNavigate('messaging')}
          className="rounded-2xl p-4 bg-[#0a1510] hover:bg-[#0e1e16] border border-[#76d418]/40 hover:border-[#76d418] transition-all flex items-center justify-between cursor-pointer group shadow-lg shadow-[#76d418]/5"
        >
          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-[#76d418]/15 border border-[#76d418]/40 flex items-center justify-center text-[#76d418]">
              <MessageSquare className="w-5 h-5 text-[#76d418]" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-white group-hover:text-[#76d418] transition-colors">
                  Team Comms & Department Dispatch
                </h3>
                <span className="px-2 py-0.5 rounded-full bg-[#76d418]/20 border border-[#76d418]/40 text-[#76d418] text-[10px] font-bold">
                  Direct Comms
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Direct staff messaging, departmental job dispatching & paperwork approvals
              </p>
            </div>
          </div>

          <ChevronRight className="w-5 h-5 text-slate-500 group-hover:text-[#76d418] transition-transform group-hover:translate-x-1" />
        </div>
      </div>

      {/* Quick Access Tiles to Jobs, Clients, Phone System */}
      <div className="grid grid-cols-3 gap-2.5 pt-1">
        {/* Jobs tile */}
        <button
          id="tile-jobs-dashboard"
          onClick={() => onNavigate('jobs')}
          className="p-3 rounded-2xl bg-[#0a1218] border border-slate-800/80 hover:border-[#76d418]/50 text-left transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between">
            <Briefcase className="w-4 h-4 text-[#76d418]" />
            <span className="text-[11px] font-bold text-slate-400">{jobs.length}</span>
          </div>
          <div className="mt-2 text-xs font-bold text-white group-hover:text-[#76d418]">
            Tasks & Jobs
          </div>
          <p className="text-[10px] text-slate-400 line-clamp-1">Deliverables</p>
        </button>

        {/* Clients tile */}
        <button
          id="tile-clients-dashboard"
          onClick={() => onNavigate('clients')}
          className="p-3 rounded-2xl bg-[#0a1218] border border-slate-800/80 hover:border-[#76d418]/50 text-left transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between">
            <Users className="w-4 h-4 text-[#76d418]" />
            <span className="text-[11px] font-bold text-slate-400">{calls.length}</span>
          </div>
          <div className="mt-2 text-xs font-bold text-white group-hover:text-[#76d418]">
            Clients
          </div>
          <p className="text-[10px] text-slate-400 line-clamp-1">Directory</p>
        </button>

        {/* Phone tile */}
        <button
          id="tile-phone-dashboard"
          onClick={() => onNavigate('phone')}
          className="p-3 rounded-2xl bg-[#0a1218] border border-slate-800/80 hover:border-[#76d418]/50 text-left transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between">
            <PhoneCall className="w-4 h-4 text-[#76d418]" />
            <span className="text-[10px] font-bold text-[#76d418] bg-[#76d418]/10 px-1 rounded">Live</span>
          </div>
          <div className="mt-2 text-xs font-bold text-white group-hover:text-[#76d418]">
            Phone System
          </div>
          <p className="text-[10px] text-slate-400 line-clamp-1">VoIP Intake</p>
        </button>
      </div>

      {/* Modal for Directing Agent Workload */}
      {selectedAgentForDirective && (
        <div 
          id="agent-directive-dialog"
          className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-150"
        >
          <div className="w-full max-w-sm bg-[#070d09] border border-[#76d418]/50 rounded-2xl p-4 shadow-2xl space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <Bot className="w-4 h-4 text-[#76d418]" />
                <span className="text-xs font-bold text-white">{selectedAgentForDirective.name}</span>
              </div>
              <button
                onClick={() => setSelectedAgentForDirective(null)}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="text-xs text-slate-300">
              Department: <span className="font-semibold text-[#76d418]">{selectedAgentForDirective.department}</span>
              <p className="text-slate-400 text-[11px] mt-1">{selectedAgentForDirective.capabilityProfile}</p>
            </div>

            <form onSubmit={handleSendDirective} className="space-y-2.5">
              <label className="text-xs font-semibold text-slate-200">
                Direct Workload or Task:
              </label>
              <textarea
                rows={3}
                required
                value={directiveText}
                onChange={(e) => setDirectiveText(e.target.value)}
                placeholder={`Tell ${selectedAgentForDirective.name} what to execute...`}
                className="w-full bg-[#0c141a] border border-slate-700 focus:border-[#76d418] rounded-xl p-2.5 text-xs text-white outline-none resize-none"
              />

              <div className="flex items-center justify-end gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => setSelectedAgentForDirective(null)}
                  className="px-3 py-1.5 rounded-lg text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 rounded-lg bg-[#76d418] hover:bg-[#66bd14] text-slate-950 font-bold text-xs flex items-center gap-1.5 shadow-md shadow-[#76d418]/20"
                >
                  <Send className="w-3 h-3" />
                  <span>Dispatch Directive</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
