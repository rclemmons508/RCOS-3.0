import React from 'react';
import { 
  RefreshCw, 
  ChevronDown, 
  Sliders, 
  LogOut,
  Sparkles, 
  MessageSquare,
  LogIn,
  User as UserIcon,
  ShieldCheck
} from 'lucide-react';

interface HeaderProps {
  onOpenSetup: () => void;
  onRefresh?: () => void;
  onOpenWorkspaceSync?: () => void;
  onOpenMessaging?: () => void;
  unreadMessagesCount?: number;
  user?: any;
  onSignIn?: () => void;
  onSignOut?: () => void;
}

export const Header: React.FC<HeaderProps> = ({ 
  onOpenSetup,
  onRefresh,
  onOpenWorkspaceSync,
  onOpenMessaging,
  unreadMessagesCount = 0,
  user,
  onSignIn,
  onSignOut
}) => {
  return (
    <header 
      id="rcos-top-header"
      className="sticky top-0 z-30 bg-[#060b08]/95 backdrop-blur-md border-b border-slate-800/80 px-4 py-2.5 transition-all"
    >
      <div className="max-w-md md:max-w-4xl mx-auto flex items-center justify-between">
        {/* Left Title: "RC Dashboard" */}
        <div className="flex items-center gap-1.5 cursor-pointer select-none">
          <span className="text-[#76d418] font-black text-2xl tracking-tight">RC</span>
          <span className="text-white font-bold text-2xl tracking-tight">Dashboard</span>
        </div>

        {/* Right Actions matching Screenshot 1 */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Google Sign-in with Firebase Auth */}
          {user ? (
            <div className="flex items-center gap-2 px-2.5 py-1 rounded-full bg-[#0a150c] border border-[#76d418]/40">
              {user.photoURL ? (
                <img 
                  src={user.photoURL} 
                  alt={user.displayName || 'User'} 
                  className="w-5 h-5 rounded-full object-cover border border-[#76d418]" 
                  referrerPolicy="no-referrer"
                />
              ) : (
                <div className="w-5 h-5 rounded-full bg-[#76d418] text-slate-950 text-[10px] font-bold flex items-center justify-center">
                  {(user.displayName || user.email || 'U')[0].toUpperCase()}
                </div>
              )}
              <span className="text-xs font-semibold text-slate-200 max-w-[100px] truncate hidden sm:inline">
                {user.displayName || user.email?.split('@')[0]}
              </span>
              <button
                type="button"
                onClick={onSignOut}
                title="Sign out of Firebase"
                className="text-[10px] text-slate-400 hover:text-rose-400 font-bold ml-1 transition-colors cursor-pointer"
              >
                Exit
              </button>
            </div>
          ) : (
            onSignIn && (
              <button
                id="btn-header-google-signin"
                type="button"
                onClick={onSignIn}
                className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#76d418] hover:bg-[#66bd14] text-slate-950 text-xs font-bold transition-all shadow-sm shadow-[#76d418]/20 cursor-pointer"
                title="Sign in with Google (Firebase Auth)"
              >
                <LogIn className="w-3.5 h-3.5" />
                <span>Google Sign-In</span>
              </button>
            )
          )}

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
              if (user && onSignOut) {
                onSignOut();
              } else if (window.confirm('Reset local session state?')) {
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
