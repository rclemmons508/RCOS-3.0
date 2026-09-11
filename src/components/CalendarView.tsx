import React, { useState } from 'react';
import { 
  Calendar as CalendarIcon, 
  Clock, 
  Plus, 
  CheckCircle2, 
  Bot, 
  Users, 
  AlertCircle,
  Cloud
} from 'lucide-react';

interface CalendarViewProps {
  onOpenWorkspaceSync?: () => void;
}

export const CalendarView: React.FC<CalendarViewProps> = ({ onOpenWorkspaceSync }) => {
  const [events, setEvents] = useState([
    {
      id: 'ev-1',
      title: 'Daily Strategic Intelligence Briefing (06:30 Cron)',
      type: 'Automated Cron',
      assignedAgent: 'Cognito Deep',
      time: 'Today, 06:30 AM',
      attendees: 'All Executive Admins',
      status: 'Completed'
    },
    {
      id: 'ev-2',
      title: 'Apex Logistics Quarterly SLA Review Call',
      type: 'Executive Client Meeting',
      assignedAgent: 'Vanguard CRM',
      time: 'Today, 14:00 PM',
      attendees: 'Marcus Vance, VP Operations',
      status: 'Upcoming'
    },
    {
      id: 'ev-3',
      title: 'Automated HIPAA Key Rollover & Telemetry Audit',
      type: 'Security Sentry Execution',
      assignedAgent: 'Sentinel Risk',
      time: 'Tomorrow, 00:00 AM',
      attendees: 'Automated System Service',
      status: 'Scheduled'
    },
    {
      id: 'ev-4',
      title: 'Beacon Health CMIO Check-In (Dr. Reed)',
      type: 'Voice Consultation',
      assignedAgent: 'Pulse Voice',
      time: 'Thursday, 11:30 AM',
      attendees: 'Dr. Evelyn Reed',
      status: 'Scheduled'
    }
  ]);

  const [showAddModal, setShowAddModal] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newTime, setNewTime] = useState('Tomorrow, 15:00 PM');
  const [newType, setNewType] = useState('Client Meeting');
  const [newAgent, setNewAgent] = useState('Aegis Core');

  const handleAddEvent = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;

    setEvents(prev => [
      ...prev,
      {
        id: `ev-${Date.now()}`,
        title: newTitle.trim(),
        type: newType,
        assignedAgent: newAgent,
        time: newTime,
        attendees: 'Executive Staff',
        status: 'Scheduled'
      }
    ]);

    setNewTitle('');
    setShowAddModal(false);
  };

  return (
    <div id="calendar-view-container" className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <CalendarIcon className="w-5 h-5 text-emerald-400" />
            <span>Calendar & Autonomous Cron Dispatcher</span>
          </h1>
          <p className="text-xs text-slate-400">
            Automated schedule coordination between executive meetings, cron jobs, and agent syncs
          </p>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          {onOpenWorkspaceSync && (
            <button
              onClick={onOpenWorkspaceSync}
              className="px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 hover:border-[#76d418] text-slate-200 hover:text-white text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
              title="View Google Calendar Live Sync"
            >
              <Cloud className="w-3.5 h-3.5 text-[#76d418]" />
              <span>Google Calendar Sync</span>
            </button>
          )}

          <button
            onClick={() => setShowAddModal(true)}
            className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold flex items-center gap-2 transition-all shadow-md shadow-emerald-900/30 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Schedule Event / Cron</span>
          </button>
        </div>
      </div>

      {/* Events List */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {events.map((ev) => (
          <div
            key={ev.id}
            className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-3"
          >
            <div className="flex items-start justify-between gap-2">
              <div>
                <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-400">
                  {ev.type}
                </span>
                <h3 className="text-sm font-bold text-white leading-snug">{ev.title}</h3>
              </div>

              <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase ${
                ev.status === 'Completed'
                  ? 'bg-emerald-500/20 text-emerald-400'
                  : 'bg-sky-500/20 text-sky-400'
              }`}>
                {ev.status}
              </span>
            </div>

            <div className="flex items-center justify-between text-xs text-slate-300">
              <span className="flex items-center gap-1.5 text-slate-400">
                <Clock className="w-3.5 h-3.5" />
                <span>{ev.time}</span>
              </span>
              <span className="text-slate-400">{ev.attendees}</span>
            </div>

            <div className="pt-2 border-t border-slate-800/70 flex items-center justify-between text-[11px] text-slate-400">
              <span>Agent: <strong className="text-emerald-400">{ev.assignedAgent}</strong></span>
              <span className="text-slate-500">Sync: Realtime</span>
            </div>
          </div>
        ))}
      </div>

      {/* Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <h3 className="text-base font-bold text-white">Schedule Calendar Event or Cron</h3>
            <form onSubmit={handleAddEvent} className="space-y-3">
              <div>
                <label className="text-xs text-slate-400 block mb-1">Event Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Apex Logistics Q4 Review"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-xs text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 block mb-1">Type</label>
                  <select
                    value={newType}
                    onChange={(e) => setNewType(e.target.value)}
                    className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-xs text-white"
                  >
                    <option value="Client Meeting">Client Meeting</option>
                    <option value="Automated Cron">Automated Cron</option>
                    <option value="Executive Briefing">Executive Briefing</option>
                    <option value="Audit Check">Audit Check</option>
                  </select>
                </div>

                <div>
                  <label className="text-xs text-slate-400 block mb-1">Assigned Agent</label>
                  <select
                    value={newAgent}
                    onChange={(e) => setNewAgent(e.target.value)}
                    className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-xs text-white"
                  >
                    <option value="Aegis Core">Aegis Core</option>
                    <option value="Sentinel Risk">Sentinel Risk</option>
                    <option value="Pulse Voice">Pulse Voice</option>
                    <option value="Vanguard CRM">Vanguard CRM</option>
                    <option value="Cognito Deep">Cognito Deep</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs text-slate-400 block mb-1">Time / Schedule</label>
                <input
                  type="text"
                  value={newTime}
                  onChange={(e) => setNewTime(e.target.value)}
                  className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-xs text-white"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 rounded-xl bg-slate-800 text-xs font-semibold text-slate-300 hover:bg-slate-700 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-emerald-600 text-xs font-semibold text-white hover:bg-emerald-500 cursor-pointer"
                >
                  Save Event
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
