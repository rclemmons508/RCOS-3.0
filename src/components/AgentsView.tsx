import React, { useState } from 'react';
import { 
  Bot, 
  Cpu, 
  ShieldCheck, 
  Play, 
  Pause, 
  Send, 
  Search, 
  Filter, 
  Plus, 
  CheckCircle2, 
  Activity, 
  Sparkles,
  Zap,
  Sliders
} from 'lucide-react';
import { Agent, RiskLevel } from '../types';

interface AgentsViewProps {
  agents: Agent[];
  onToggleStatus: (agentId: string) => void;
  onDirectTask: (agentId: string, taskDescription: string) => void;
  onOpenCreateModal: () => void;
}

export const AgentsView: React.FC<AgentsViewProps> = ({
  agents,
  onToggleStatus,
  onDirectTask,
  onOpenCreateModal
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('All');
  const [selectedAgent, setSelectedAgent] = useState<Agent | null>(agents[0] || null);
  const [taskInput, setTaskInput] = useState('');

  const departments = ['All', ...Array.from(new Set(agents.map(a => a.department)))];

  const filteredAgents = agents.filter(agent => {
    const matchesSearch = agent.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      agent.role.toLowerCase().includes(searchQuery.toLowerCase()) ||
      agent.codeName.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesDept = departmentFilter === 'All' || agent.department === departmentFilter;
    return matchesSearch && matchesDept;
  });

  const handleDirectTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAgent || !taskInput.trim()) return;
    onDirectTask(selectedAgent.id, taskInput.trim());
    setTaskInput('');
  };

  return (
    <div id="agents-view-container" className="space-y-6">
      {/* Top Header & Provision Button */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <Bot className="w-5 h-5 text-emerald-400" />
            <span>Autonomous Agent Fleet Registry</span>
          </h1>
          <p className="text-xs text-slate-400">
            Provision, monitor, and direct autonomous Gemini-powered organizational units
          </p>
        </div>

        <button
          id="btn-provision-agent"
          onClick={onOpenCreateModal}
          className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold flex items-center gap-2 transition-all shadow-md shadow-emerald-900/30 cursor-pointer self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Provision New Agent</span>
        </button>
      </div>

      {/* Search & Filter bar */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            id="input-agent-search"
            type="text"
            placeholder="Search agents by name, role, codename..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 rounded-xl bg-slate-900/80 border border-slate-800 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-emerald-500/50"
          />
        </div>

        <div className="flex items-center gap-2 overflow-x-auto pb-1 sm:pb-0 no-scrollbar">
          {departments.map((dept) => (
            <button
              key={dept}
              onClick={() => setDepartmentFilter(dept)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-colors cursor-pointer ${
                departmentFilter === dept
                  ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 font-semibold'
                  : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
              }`}
            >
              {dept}
            </button>
          ))}
        </div>
      </div>

      {/* Main Grid: Agents Grid & Live Director Workbench */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Agent Cards Roster (2 columns) */}
        <div className="lg:col-span-2 grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredAgents.map((agent) => {
            const isSelected = selectedAgent?.id === agent.id;
            return (
              <div
                key={agent.id}
                id={`agent-item-${agent.id}`}
                onClick={() => setSelectedAgent(agent)}
                className={`p-4 rounded-xl border transition-all cursor-pointer flex flex-col justify-between gap-3 ${
                  isSelected
                    ? 'bg-slate-900/90 border-emerald-500/50 ring-1 ring-emerald-500/30 shadow-lg'
                    : 'bg-slate-900/60 border-slate-800/80 hover:border-slate-700/80'
                }`}
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div className="flex items-center gap-2.5">
                      <div className="w-10 h-10 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center font-bold text-emerald-400">
                        {agent.name.substring(0, 2)}
                      </div>
                      <div>
                        <h3 className="text-sm font-bold text-white leading-tight">{agent.name}</h3>
                        <span className="text-[11px] text-slate-400 font-mono">{agent.codeName}</span>
                      </div>
                    </div>

                    <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                      agent.status === 'executing'
                        ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 animate-pulse'
                        : agent.status === 'active'
                        ? 'bg-sky-500/20 text-sky-400 border border-sky-500/30'
                        : 'bg-slate-800 text-slate-400'
                    }`}>
                      {agent.status}
                    </span>
                  </div>

                  <p className="text-xs text-slate-300 font-medium line-clamp-1">{agent.role}</p>
                  <p className="text-[11px] text-slate-400 mt-1 line-clamp-2">{agent.capabilityProfile}</p>

                  <div className="mt-3 flex flex-wrap items-center gap-2 text-[10px]">
                    <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">
                      {agent.modelTier}
                    </span>
                    <span className={`px-2 py-0.5 rounded font-semibold ${
                      agent.riskClassification === 'High'
                        ? 'bg-rose-500/15 text-rose-300 border border-rose-500/30'
                        : agent.riskClassification === 'Medium'
                        ? 'bg-amber-500/15 text-amber-300 border border-amber-500/30'
                        : 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30'
                    }`}>
                      Risk: {agent.riskClassification}
                    </span>
                    <span className="text-slate-500">
                      {agent.department}
                    </span>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between">
                  <span className="text-[11px] text-slate-400 font-mono">
                    {agent.tasksCompleted} tasks completed
                  </span>
                  
                  <div className="flex items-center gap-2">
                    <button
                      id={`btn-toggle-status-${agent.id}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        onToggleStatus(agent.id);
                      }}
                      className={`p-1.5 rounded-lg border text-xs cursor-pointer transition-colors ${
                        agent.status === 'offline' || agent.status === 'idle'
                          ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30 hover:bg-emerald-500/30'
                          : 'bg-slate-800 text-slate-400 border-slate-700 hover:bg-slate-700'
                      }`}
                      title={agent.status === 'offline' ? 'Activate Agent' : 'Pause Agent'}
                    >
                      {agent.status === 'offline' || agent.status === 'idle' ? (
                        <Play className="w-3.5 h-3.5" />
                      ) : (
                        <Pause className="w-3.5 h-3.5" />
                      )}
                    </button>
                    <span className="text-xs font-semibold text-emerald-400">
                      Select
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Live Task Director Workbench (1 column) */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4">
          {selectedAgent ? (
            <>
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Live Task Director</span>
                  <h3 className="text-base font-bold text-white">{selectedAgent.name}</h3>
                </div>
                <span className="text-xs font-mono text-emerald-400">{selectedAgent.uptime} uptime</span>
              </div>

              <div className="space-y-2 text-xs">
                <div className="flex justify-between py-1 border-b border-slate-800/60">
                  <span className="text-slate-400">Department:</span>
                  <span className="text-slate-200 font-medium">{selectedAgent.department}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-800/60">
                  <span className="text-slate-400">Model Tier:</span>
                  <span className="text-slate-200 font-mono">{selectedAgent.modelTier}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-800/60">
                  <span className="text-slate-400">Risk Policy:</span>
                  <span className="text-slate-200">{selectedAgent.riskClassification}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-800/60">
                  <span className="text-slate-400">Permission:</span>
                  <span className="text-slate-200">{selectedAgent.permissionLevel}</span>
                </div>
              </div>

              {selectedAgent.currentTask ? (
                <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-xs">
                  <div className="flex items-center gap-2 text-emerald-400 font-semibold mb-1">
                    <Activity className="w-3.5 h-3.5 animate-spin" />
                    <span>In-Flight Workload</span>
                  </div>
                  <p className="text-slate-300 text-xs leading-relaxed">{selectedAgent.currentTask}</p>
                </div>
              ) : (
                <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-400">
                  Agent is currently standing by for dispatch.
                </div>
              )}

              {/* Direct Task Dispatch Form */}
              <form onSubmit={handleDirectTask} className="space-y-2.5 pt-2">
                <label className="text-xs font-bold text-white block">
                  Assign Immediate Autonomous Workload
                </label>
                <textarea
                  id="textarea-direct-task"
                  rows={3}
                  placeholder={`Instruct ${selectedAgent.name} to execute a task (e.g., 'Verify client contract', 'Re-index logistics manifests')...`}
                  value={taskInput}
                  onChange={(e) => setTaskInput(e.target.value)}
                  className="w-full p-3 rounded-lg bg-slate-950 border border-slate-800 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-emerald-500"
                />
                <button
                  type="submit"
                  disabled={!taskInput.trim()}
                  className="w-full py-2 px-3 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white text-xs font-bold flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>Dispatch to {selectedAgent.name}</span>
                </button>
              </form>
            </>
          ) : (
            <div className="py-12 text-center text-xs text-slate-400">
              Select an autonomous agent from the roster to view live controls and dispatch tasks.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
