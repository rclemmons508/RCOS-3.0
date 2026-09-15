import React, { useState } from 'react';
import { 
  Briefcase, 
  Plus, 
  CheckCircle2, 
  Clock, 
  AlertCircle, 
  ArrowRight, 
  Filter, 
  Search,
  Zap,
  Bot,
  Send
} from 'lucide-react';
import { Job, JobStatus } from '../types';

interface JobsViewProps {
  jobs: Job[];
  onUpdateJobStatus: (jobId: string, status: JobStatus) => void;
  onCreateJob: (job: Partial<Job>) => void;
  orgName?: string;
  onOpenMessaging?: () => void;
}

export const JobsView: React.FC<JobsViewProps> = ({
  jobs,
  onUpdateJobStatus,
  onCreateJob,
  orgName,
  onOpenMessaging
}) => {
  const [filterPriority, setFilterPriority] = useState<string>('All');
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newClient, setNewClient] = useState(orgName || 'Internal Operations');
  const [newAgent, setNewAgent] = useState('Compliance & Security Audit');
  const [newPriority, setNewPriority] = useState<'Normal' | 'Medium' | 'High' | 'Urgent'>('High');

  const columns: JobStatus[] = ['Pending', 'Queued', 'In Progress', 'Completed'];

  const filteredJobs = jobs.filter(job => {
    const matchesPriority = filterPriority === 'All' || job.priority === filterPriority;
    const matchesSearch = job.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      job.clientName.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesPriority && matchesSearch;
  });

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;

    onCreateJob({
      title: newTitle.trim(),
      clientName: newClient.trim() || 'Internal Operations',
      assignedAgent: newAgent,
      priority: newPriority,
      status: 'In Progress',
      dueDate: 'Today',
      approvalRequired: false,
      isApproved: true,
      progress: 0.2,
      summary: 'Task initialized through RCOS executive pipeline.',
      budget: '$500 compute',
      tags: ['Autonomous', 'New']
    });

    setNewTitle('');
    setShowCreateModal(false);
  };

  return (
    <div id="jobs-view-container" className="space-y-4 max-w-5xl mx-auto pb-20">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <Briefcase className="w-5 h-5 text-[#76d418]" />
            <span>Task Deliverables & Kanban</span>
          </h1>
          <p className="text-xs text-slate-400">
            Enterprise workload deliverables assigned to autonomous agents
          </p>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          {onOpenMessaging && (
            <button
              type="button"
              onClick={onOpenMessaging}
              className="px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-[#76d418] border border-[#76d418]/40 font-bold text-xs flex items-center gap-2 transition-all cursor-pointer"
            >
              <Send className="w-3.5 h-3.5" />
              <span>Department Dispatch</span>
            </button>
          )}

          <button
            id="btn-create-new-job"
            onClick={() => setShowCreateModal(true)}
            className="px-4 py-2 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 font-bold text-xs flex items-center gap-2 transition-all shadow-md shadow-[#76d418]/20 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>New Task Deliverable</span>
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search tasks and deliverables..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 rounded-xl bg-[#0a1218] border border-slate-800 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-[#76d418]"
          />
        </div>

        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          {['All', 'Urgent', 'High', 'Medium', 'Normal'].map((p) => (
            <button
              key={p}
              onClick={() => setFilterPriority(p)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold cursor-pointer transition-colors whitespace-nowrap ${
                filterPriority === p
                  ? 'bg-[#76d418]/20 text-[#76d418] border border-[#76d418]/50'
                  : 'bg-[#0a1218] text-slate-400 hover:text-slate-200 border border-slate-800'
              }`}
            >
              {p}
            </button>
          ))}
        </div>
      </div>

      {/* Kanban Board Columns */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-3.5">
        {columns.map((status) => {
          const colJobs = filteredJobs.filter(j => j.status === status);

          return (
            <div
              key={status}
              id={`kanban-col-${status.toLowerCase().replace(' ', '-')}`}
              className="rounded-2xl bg-[#091016]/90 border border-slate-800/90 p-3.5 flex flex-col min-h-[380px]"
            >
              {/* Column Header */}
              <div className="flex items-center justify-between pb-3 border-b border-slate-800/80 mb-3">
                <span className="text-xs font-bold text-white uppercase tracking-wider">
                  {status}
                </span>
                <span className="text-xs font-black px-2 py-0.5 rounded-full bg-slate-800 text-slate-300">
                  {colJobs.length}
                </span>
              </div>

              {/* Tasks in column */}
              <div className="space-y-2.5 flex-1 overflow-y-auto">
                {colJobs.length === 0 ? (
                  <div className="h-32 flex flex-col items-center justify-center text-center p-3 text-slate-500 text-xs border border-dashed border-slate-800 rounded-xl">
                    <span>No {status.toLowerCase()} tasks</span>
                  </div>
                ) : (
                  colJobs.map((job) => (
                    <div
                      key={job.id}
                      id={`job-card-${job.id}`}
                      className="p-3 rounded-xl bg-[#0c1622] border border-slate-800 hover:border-[#76d418]/50 transition-all space-y-2"
                    >
                      <div className="flex items-start justify-between gap-1.5">
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase ${
                          job.priority === 'Urgent'
                            ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                            : job.priority === 'High'
                            ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                            : 'bg-slate-800 text-slate-300'
                        }`}>
                          {job.priority}
                        </span>

                        <span className="text-[11px] font-mono text-slate-400">{job.dueDate}</span>
                      </div>

                      <h4 className="text-xs font-bold text-white leading-snug">
                        {job.title}
                      </h4>

                      <p className="text-[11px] text-slate-400 line-clamp-2">
                        {job.summary}
                      </p>

                      <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-[11px]">
                        <span className="text-[#76d418] font-medium flex items-center gap-1 truncate max-w-[120px]">
                          <Bot className="w-3 h-3" />
                          <span className="truncate">{job.assignedAgent}</span>
                        </span>

                        {/* Status advance button */}
                        {status !== 'Completed' && (
                          <button
                            onClick={() => {
                              const nextStatus: JobStatus = 
                                status === 'Pending' ? 'Queued' :
                                status === 'Queued' ? 'In Progress' : 'Completed';
                              onUpdateJobStatus(job.id, nextStatus);
                            }}
                            className="text-[10px] text-slate-400 hover:text-[#76d418] flex items-center gap-0.5 cursor-pointer"
                          >
                            <span>Advance</span>
                            <ArrowRight className="w-3 h-3" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Zero State if no jobs at all */}
      {jobs.length === 0 && (
        <div className="p-8 rounded-2xl bg-[#091016] border border-slate-800 text-center space-y-3">
          <div className="w-12 h-12 rounded-full bg-[#76d418]/15 text-[#76d418] flex items-center justify-center mx-auto">
            <Briefcase className="w-6 h-6" />
          </div>
          <h3 className="text-sm font-bold text-white">No active tasks in queue</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Create deliverables for autonomous agents or direct tasks straight from the Live Agent Fleet Pulse on the dashboard.
          </p>
          <button
            onClick={() => setShowCreateModal(true)}
            className="px-4 py-2 rounded-xl bg-[#76d418] text-slate-950 font-bold text-xs inline-flex items-center gap-2 cursor-pointer shadow-md shadow-[#76d418]/20"
          >
            <Plus className="w-4 h-4" />
            <span>Create First Task</span>
          </button>
        </div>
      )}

      {/* Create Task Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-[#070d09] border border-[#76d418]/50 rounded-2xl p-5 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Briefcase className="w-4 h-4 text-[#76d418]" />
                <span>Initialize Deliverable</span>
              </h3>
              <button onClick={() => setShowCreateModal(false)} className="text-slate-400 hover:text-white">✕</button>
            </div>

            <form onSubmit={handleCreateSubmit} className="space-y-3">
              <div>
                <label className="text-xs font-semibold text-slate-300">Deliverable Title / Goal</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Audit API credentials or Reconcile billing"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  className="w-full mt-1 bg-[#0a141a] border border-slate-700 focus:border-[#76d418] rounded-xl p-2.5 text-xs text-white outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-slate-300">Assign Autonomous Agent</label>
                  <select
                    value={newAgent}
                    onChange={(e) => setNewAgent(e.target.value)}
                    className="w-full mt-1 bg-[#0a141a] border border-slate-700 rounded-xl p-2.5 text-xs text-white outline-none cursor-pointer"
                  >
                    <option value="Compliance & Security Audit">Compliance & Security Audit</option>
                    <option value="AI Workflow Engine">AI Workflow Engine</option>
                    <option value="VoIP Telephony Engine">VoIP Telephony Engine</option>
                    <option value="Enterprise Intelligence">Enterprise Intelligence</option>
                    <option value="Master Orchestrator">Master Orchestrator</option>
                  </select>
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-300">Priority Level</label>
                  <select
                    value={newPriority}
                    onChange={(e) => setNewPriority(e.target.value as any)}
                    className="w-full mt-1 bg-[#0a141a] border border-slate-700 rounded-xl p-2.5 text-xs text-white outline-none cursor-pointer"
                  >
                    <option value="Urgent">Urgent</option>
                    <option value="High">High</option>
                    <option value="Medium">Medium</option>
                    <option value="Normal">Normal</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-300">Enterprise / Client Association</label>
                <input
                  type="text"
                  placeholder="Internal Operations or Client Name"
                  value={newClient}
                  onChange={(e) => setNewClient(e.target.value)}
                  className="w-full mt-1 bg-[#0a141a] border border-slate-700 focus:border-[#76d418] rounded-xl p-2.5 text-xs text-white outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-3 py-2 rounded-xl text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 font-bold text-xs"
                >
                  Create Deliverable
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
