import React from 'react';
import { 
  RefreshCw, 
  ChevronDown, 
  Sliders, 
  LogOut,
  Sparkles,
  MessageSquare
} from 'lucide-react';

interface HeaderProps {
  onOpenSetup: () => void;
  onRefresh?: () => void;
  onOpenWorkspaceSync?: () => void;
  onOpenMessaging?: () => void;
  unreadMessagesCount?: number;
}

export const Header: React.FC<HeaderProps> = ({ 
  onOpenSetup,
  onRefresh,
  onOpenWorkspaceSync,
  onOpenMessaging,
  unreadMessagesCount = 0
}) => {
  return (
    <header 
      id="rcos-top-header"
      className="sticky top-0 z-30 bg-[#060b08]/95 backdrop-blur-md border-b border-slate-800/80 px-4 py-2.5 transition-all"
    >
      <div className="max-w-md md:max-w-3xl mx-auto flex items-center justify-between">
        {/* Left Title: "RC Dashboard" */}
        <div className="flex items-center gap-1.5 cursor-pointer select-none">
          <span className="text-[#76d418] font-black text-2xl tracking-tight">RC</span>
          <span className="text-white font-bold text-2xl tracking-tight">Dashboard</span>
        </div>

        {/* Right Actions matching Screenshot 1 */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Team Direct Messaging & Dispatch Button */}
          {onOpenMessaging && (
            <button
              id="btn-header-team-messaging"
              onClick={onOpenMessaging}
              title="Employee Messaging & Job Dispatch"
              className="relative p-1.5 rounded-lg text-slate-300 hover:text-[#76d418] hover:bg-slate-900 transition-colors cursor-pointer"
            >
              <MessageSquare className="w-5 h-5 text-[#76d418]" />
              {unreadMessagesCount > 0 && (
                <span className="absolute -top-0.5 -right-0.5 w-4 h-4 rounded-full bg-[#76d418] text-slate-950 text-[9px] font-black flex items-center justify-center animate-pulse">
                  {unreadMessagesCount}
                </span>
              )}
            </button>
          )}

          {/* "Auto" pill with sync icon and dropdown - links directly to Workspace Sync */}
          <button
            id="btn-header-auto-sync"
            onClick={onOpenWorkspaceSync || onRefresh}
            title="Google Workspace & Autonomous Sync Console"
            className="flex items-center gap-1.5 px-3 py-1 rounded-full border border-[#76d418]/70 bg-[#0a150c] text-[#76d418] text-xs font-semibold hover:bg-[#0f2212] transition-colors cursor-pointer shadow-sm shadow-[#76d418]/10"
          >
            <RefreshCw className="w-3.5 h-3.5 animate-[spin_10s_linear_infinite]" />
            <span>Auto</span>
            <ChevronDown className="w-3 h-3 text-[#76d418]" />
          </button>

          {/* Equalizer / Sliders Icon matching Screenshot 1 (opens Enterprise Setup) */}
          <button
            id="btn-header-settings"
            onClick={onOpenSetup}
            title="Enterprise Workspace Setup"
            className="p-1.5 rounded-lg text-[#76d418] hover:text-[#88f020] hover:bg-slate-900 transition-colors cursor-pointer"
          >
            <Sliders className="w-5 h-5 text-[#76d418]" />
          </button>

          {/* Log out / Exit Icon matching Screenshot 1 */}
          <button
            id="btn-header-logout"
            onClick={() => {
              if (window.confirm('Reset local session state?')) {
                window.location.reload();
              }
            }}
            title="Session Management"
            className="p-1.5 rounded-lg text-rose-400 hover:text-rose-300 hover:bg-slate-900 transition-colors cursor-pointer"
          >
            <LogOut className="w-5 h-5 text-rose-400" />
          </button>
        </div>
      </div>
    </header>
  );
};
