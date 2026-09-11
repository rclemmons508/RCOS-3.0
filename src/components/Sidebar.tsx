import React from 'react';
import {
  LayoutDashboard,
  Bot,
  GitBranch,
  Briefcase,
  Users,
  Phone,
  Mic,
  MessageSquareText,
  BrainCircuit,
  Calendar,
  ShieldAlert,
  Archive,
  Smartphone
} from 'lucide-react';

export type NavTabId = 
  | 'dashboard'
  | 'agents'
  | 'workflows'
  | 'jobs'
  | 'clients'
  | 'phone'
  | 'transcribe'
  | 'chat'
  | 'reasoning'
  | 'calendar'
  | 'saved'
  | 'governance'
  | 'apk';

interface SidebarProps {
  activeTab: NavTabId;
  onSelectTab: (tab: NavTabId) => void;
  unreadJobsCount: number;
  activeAgentsCount: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onSelectTab,
  unreadJobsCount,
  activeAgentsCount
}) => {
  const navItems: Array<{
    id: NavTabId;
    label: string;
    icon: React.ComponentType<{ className?: string }>;
    badge?: string | number;
    badgeColor?: string;
    section?: string;
  }> = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard, section: 'Core Operations' },
    { id: 'agents', label: 'Autonomous Agents', icon: Bot, badge: activeAgentsCount, badgeColor: 'bg-emerald-500/20 text-emerald-400' },
    { id: 'workflows', label: 'Workflow Engine', icon: GitBranch },
    { id: 'jobs', label: 'Jobs & Pipeline', icon: Briefcase, badge: unreadJobsCount, badgeColor: 'bg-sky-500/20 text-sky-400' },
    { id: 'clients', label: 'CRM & Clients', icon: Users },
    
    { id: 'phone', label: 'Phone System', icon: Phone, section: 'Communications & AI' },
    { id: 'transcribe', label: 'Voice Transcriber', icon: Mic },
    { id: 'chat', label: 'Gemini Agent Chat', icon: MessageSquareText },
    { id: 'reasoning', label: 'Deep Thinking', icon: BrainCircuit },
    { id: 'calendar', label: 'Calendar & Cron', icon: Calendar },

    { id: 'saved', label: 'Saved Intelligence', icon: Archive, section: 'Governance & Mobile' },
    { id: 'governance', label: 'Governance & RBAC', icon: ShieldAlert },
    { id: 'apk', label: 'Android APK Center', icon: Smartphone, badge: 'Build Ready', badgeColor: 'bg-amber-500/20 text-amber-400' }
  ];

  return (
    <aside id="rcos-sidebar" className="w-full lg:w-64 shrink-0 bg-slate-900/60 border-r border-slate-800/80 p-3 lg:p-4 flex lg:flex-col gap-1 overflow-x-auto lg:overflow-x-visible no-scrollbar">
      {navItems.map((item, idx) => {
        const Icon = item.icon;
        const isActive = activeTab === item.id;
        const showSection = item.section && (idx === 0 || navItems[idx - 1]?.section !== item.section);

        return (
          <React.Fragment key={item.id}>
            {showSection && (
              <div className="hidden lg:block px-3 pt-3 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-500">
                {item.section}
              </div>
            )}
            <button
              id={`nav-btn-${item.id}`}
              onClick={() => onSelectTab(item.id)}
              className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-all whitespace-nowrap lg:whitespace-normal cursor-pointer ${
                isActive
                  ? 'bg-emerald-500/15 text-emerald-400 font-semibold border border-emerald-500/30 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              <Icon className={`w-4 h-4 shrink-0 ${isActive ? 'text-emerald-400' : 'text-slate-400'}`} />
              <span className="flex-1 text-left">{item.label}</span>
              {item.badge !== undefined && (
                <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-tight shrink-0 ${item.badgeColor || 'bg-slate-800 text-slate-300'}`}>
                  {item.badge}
                </span>
              )}
            </button>
          </React.Fragment>
        );
      })}
    </aside>
  );
};
