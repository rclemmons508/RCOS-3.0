import React from 'react';
import { 
  Bot, 
  Workflow as WorkflowIcon, 
  Mic, 
  MessageSquare, 
  BrainCircuit, 
  Sliders, 
  Calendar as CalendarIcon, 
  FileText, 
  ShieldCheck, 
  Smartphone, 
  Cloud,
  X,
  ChevronRight
} from 'lucide-react';
import { RcosLogo } from './RcosLogo';

interface MoreMenuModalProps {
  isOpen: boolean;
  onClose: () => void;
  onNavigate: (view: string) => void;
  onOpenSetup: () => void;
}

export const MoreMenuModal: React.FC<MoreMenuModalProps> = ({
  isOpen,
  onClose,
  onNavigate,
  onOpenSetup
}) => {
  if (!isOpen) return null;

  const menuItems = [
    {
      id: 'workspace',
      title: 'Google Workspace Sync',
      desc: 'Drive files, Calendar schedule, and Gmail inbox live synchronization',
      icon: Cloud,
      color: 'text-[#76d418]',
      badge: 'OAuth 2.0'
    },
    {
      id: 'agents',
      title: 'Autonomous Agents Fleet',
      desc: 'Inspect, steer, and provision autonomous agents',
      icon: Bot,
      color: 'text-[#76d418]',
      badge: '5 Active'
    },
    {
      id: 'workflows',
      title: 'AI Workflow Engine',
      desc: 'Pipelines, trigger conditions & automated approval policies',
      icon: WorkflowIcon,
      color: 'text-[#76d418]',
      badge: '3 Pipelines'
    },
    {
      id: 'setup',
      title: 'Enterprise Workspace Setup',
      desc: 'Customize company identity, domain, and governance wizard',
      icon: Sliders,
      color: 'text-purple-400',
      action: () => {
        onClose();
        onOpenSetup();
      }
    },
    {
      id: 'transcribe',
      title: 'Voice Transcriber & Audio Notes',
      desc: 'Real-time microphone speech-to-text to task conversion',
      icon: Mic,
      color: 'text-amber-400'
    },
    {
      id: 'chat',
      title: 'Autonomous Gemini Assistant',
      desc: 'Direct command & conversational steering with fleet agents',
      icon: MessageSquare,
      color: 'text-sky-400'
    },
    {
      id: 'reasoning',
      title: 'Deep Thinking & Strategy Engine',
      desc: 'Multi-factor root cause analysis and scenario stress testing',
      icon: BrainCircuit,
      color: 'text-indigo-400'
    },
    {
      id: 'calendar',
      title: 'Executive Calendar & Milestones',
      desc: 'Autonomous agent scheduled deliverables and meetings',
      icon: CalendarIcon,
      color: 'text-emerald-400'
    },
    {
      id: 'saved',
      title: 'Intelligence Vault',
      desc: 'Encrypted strategic briefings and operational standards',
      icon: FileText,
      color: 'text-teal-400'
    },
    {
      id: 'governance',
      title: 'Security & Governance Audit',
      desc: 'Cryptographic compliance verification and tamper-proof logs',
      icon: ShieldCheck,
      color: 'text-rose-400'
    },
    {
      id: 'apk',
      title: 'Android Mobile APK Center',
      desc: 'Download APK package for Android devices & field ops',
      icon: Smartphone,
      color: 'text-[#76d418]',
      badge: 'v3.4 Ready'
    }
  ];

  return (
    <div 
      id="more-navigation-drawer"
      className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4 animate-in fade-in duration-150"
    >
      <div 
        className="w-full max-w-lg bg-[#070d09] border-t sm:border border-slate-800 rounded-t-3xl sm:rounded-3xl max-h-[85vh] flex flex-col overflow-hidden shadow-2xl"
        style={{
          boxShadow: '0 0 35px rgba(0,0,0,0.9)'
        }}
      >
        {/* Header */}
        <div className="p-4 border-b border-slate-800/80 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <RcosLogo size={32} />
            <div>
              <h3 className="text-sm font-bold text-white">RCOS Applications & Tools</h3>
              <p className="text-[11px] text-slate-400">Select any engine to open immediately</p>
            </div>
          </div>

          <button
            id="btn-close-more-modal"
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable list of modules */}
        <div className="flex-1 overflow-y-auto p-3 space-y-2">
          {menuItems.map((item) => {
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                id={`more-item-${item.id}`}
                onClick={() => {
                  if (item.action) {
                    item.action();
                  } else {
                    onNavigate(item.id);
                    onClose();
                  }
                }}
                className="w-full text-left p-3 rounded-2xl bg-[#0b1318]/90 hover:bg-[#101b22] border border-slate-800/80 hover:border-[#76d418]/50 transition-all flex items-center justify-between group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center flex-shrink-0">
                    <Icon className={`w-5 h-5 ${item.color}`} />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-white group-hover:text-[#76d418] transition-colors">
                        {item.title}
                      </span>
                      {item.badge && (
                        <span className="text-[10px] font-bold px-1.5 py-0.2 rounded-full bg-[#76d418]/15 text-[#76d418] border border-[#76d418]/30">
                          {item.badge}
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-slate-400 line-clamp-1">{item.desc}</p>
                  </div>
                </div>

                <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-[#76d418] transition-transform group-hover:translate-x-0.5" />
              </button>
            );
          })}
        </div>

        <div className="p-3 border-t border-slate-800/80 bg-[#060a08] text-center text-slate-500 text-[11px]">
          RCOS Business Operating System • All engines synchronized
        </div>
      </div>
    </div>
  );
};
