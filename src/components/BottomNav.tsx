import React from 'react';
import { 
  Gauge, 
  PhoneCall, 
  Briefcase, 
  Users, 
  MoreHorizontal 
} from 'lucide-react';

export type MainNavTab = 'dashboard' | 'phone' | 'jobs' | 'clients' | 'more';

interface BottomNavProps {
  activeTab: string;
  onSelectTab: (tab: MainNavTab) => void;
  jobsBadgeCount?: number;
}

export const BottomNav: React.FC<BottomNavProps> = ({
  activeTab,
  onSelectTab,
  jobsBadgeCount = 0
}) => {
  const isDashboardActive = activeTab === 'dashboard';
  const isPhoneActive = activeTab === 'phone';
  const isJobsActive = activeTab === 'jobs';
  const isClientsActive = activeTab === 'clients';
  const isMoreActive = activeTab === 'more' || ['agents', 'workflows', 'transcribe', 'chat', 'reasoning', 'calendar', 'saved', 'governance', 'apk'].includes(activeTab);

  return (
    <nav 
      id="rcos-bottom-navigation"
      aria-label="Main Navigation"
      className="fixed bottom-0 left-0 right-0 z-40 bg-[#050907]/95 backdrop-blur-lg border-t border-slate-800/90 px-2 py-1.5 sm:py-2 transition-all"
      style={{
        boxShadow: '0 -4px 20px rgba(0,0,0,0.7)'
      }}
    >
      <div className="max-w-md mx-auto flex items-center justify-around">
        {/* Tab 1: Dashboard */}
        <button
          id="nav-tab-dashboard"
          onClick={() => onSelectTab('dashboard')}
          className={`flex flex-col items-center justify-center flex-1 py-1 transition-all cursor-pointer ${
            isDashboardActive 
              ? 'text-[#76d418]' 
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Gauge className={`w-5 h-5 transition-transform ${isDashboardActive ? 'scale-110 text-[#76d418]' : ''}`} />
          <span className={`text-[11px] mt-0.5 tracking-tight ${isDashboardActive ? 'font-bold text-[#76d418]' : 'font-medium'}`}>
            Dashboard
          </span>
        </button>

        {/* Tab 2: Phone Sy... */}
        <button
          id="nav-tab-phone"
          onClick={() => onSelectTab('phone')}
          className={`flex flex-col items-center justify-center flex-1 py-1 transition-all cursor-pointer ${
            isPhoneActive 
              ? 'text-[#76d418]' 
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <PhoneCall className={`w-5 h-5 transition-transform ${isPhoneActive ? 'scale-110 text-[#76d418]' : ''}`} />
          <span className={`text-[11px] mt-0.5 tracking-tight ${isPhoneActive ? 'font-bold text-[#76d418]' : 'font-medium'}`}>
            Phone Sy...
          </span>
        </button>

        {/* Tab 3: Jobs */}
        <button
          id="nav-tab-jobs"
          onClick={() => onSelectTab('jobs')}
          className={`flex flex-col items-center justify-center flex-1 py-1 relative transition-all cursor-pointer ${
            isJobsActive 
              ? 'text-[#76d418]' 
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <div className="relative">
            <Briefcase className={`w-5 h-5 transition-transform ${isJobsActive ? 'scale-110 text-[#76d418]' : ''}`} />
            {jobsBadgeCount > 0 && (
              <span className="absolute -top-1 -right-2 w-4 h-4 rounded-full bg-[#76d418] text-slate-950 text-[9px] font-black flex items-center justify-center">
                {jobsBadgeCount}
              </span>
            )}
          </div>
          <span className={`text-[11px] mt-0.5 tracking-tight ${isJobsActive ? 'font-bold text-[#76d418]' : 'font-medium'}`}>
            Jobs
          </span>
        </button>

        {/* Tab 4: Clients */}
        <button
          id="nav-tab-clients"
          onClick={() => onSelectTab('clients')}
          className={`flex flex-col items-center justify-center flex-1 py-1 transition-all cursor-pointer ${
            isClientsActive 
              ? 'text-[#76d418]' 
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Users className={`w-5 h-5 transition-transform ${isClientsActive ? 'scale-110 text-[#76d418]' : ''}`} />
          <span className={`text-[11px] mt-0.5 tracking-tight ${isClientsActive ? 'font-bold text-[#76d418]' : 'font-medium'}`}>
            Clients
          </span>
        </button>

        {/* Tab 5: More */}
        <button
          id="nav-tab-more"
          onClick={() => onSelectTab('more')}
          className={`flex flex-col items-center justify-center flex-1 py-1 transition-all cursor-pointer ${
            isMoreActive 
              ? 'text-[#76d418]' 
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <MoreHorizontal className={`w-5 h-5 transition-transform ${isMoreActive ? 'scale-110 text-[#76d418]' : ''}`} />
          <span className={`text-[11px] mt-0.5 tracking-tight ${isMoreActive ? 'font-bold text-[#76d418]' : 'font-medium'}`}>
            More
          </span>
        </button>
      </div>
    </nav>
  );
};
