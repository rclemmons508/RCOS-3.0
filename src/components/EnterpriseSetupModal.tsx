import React, { useState } from 'react';
import { 
  Building2, 
  Globe, 
  Users, 
  Layers, 
  ArrowRight, 
  X, 
  Check, 
  Sparkles,
  Bot,
  Zap,
  ShieldCheck
} from 'lucide-react';
import { RcosLogo } from './RcosLogo';

interface EnterpriseSetupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onComplete: (data: EnterpriseProfile) => void;
  currentProfile?: EnterpriseProfile;
}

export interface EnterpriseProfile {
  name: string;
  domain: string;
  headcount: string;
  industry: string;
  bottleneck: string;
}

export const EnterpriseSetupModal: React.FC<EnterpriseSetupModalProps> = ({
  isOpen,
  onClose,
  onComplete,
  currentProfile
}) => {
  const [step, setStep] = useState<number>(1);
  const [profile, setProfile] = useState<EnterpriseProfile>({
    name: currentProfile?.name || '',
    domain: currentProfile?.domain || 'company.global',
    headcount: currentProfile?.headcount || '50-250 Employees',
    industry: currentProfile?.industry || 'Technology & Software',
    bottleneck: currentProfile?.bottleneck || 'Repetitive cross-team manual triage and compliance auditing'
  });

  if (!isOpen) return null;

  const handleNext = () => {
    if (step < 4) {
      setStep(step + 1);
    } else {
      onComplete(profile);
      onClose();
    }
  };

  return (
    <div 
      id="enterprise-setup-modal"
      className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-3 sm:p-4 overflow-y-auto"
    >
      <div 
        className="w-full max-w-lg bg-[#070d09] border border-[#76d418]/40 rounded-3xl overflow-hidden shadow-2xl shadow-black relative my-auto animate-in fade-in zoom-in-95 duration-200"
        style={{
          boxShadow: '0 0 40px rgba(118, 212, 24, 0.15)'
        }}
      >
        {/* Top Header Row matching Photo 2 */}
        <div className="p-4 pb-2 border-b border-slate-800/80">
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <RcosLogo size={34} />
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-black tracking-wider text-[#76d418]">STEP {step} OF 4</span>
                  <span className="text-sm font-bold text-white">
                    {step === 1 && 'Enterprise Identity'}
                    {step === 2 && 'Workflows & Policy'}
                    {step === 3 && 'Agent Fleet Allocation'}
                    {step === 4 && 'Launch System'}
                  </span>
                </div>
                <p className="text-[11px] text-slate-400 line-clamp-1 mt-0.5">
                  Configure enterprise workflow governance & agent autonomy before launching
                </p>
              </div>
            </div>

            <button 
              id="btn-close-setup-modal"
              onClick={onClose}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Stepper Tabs matching Photo 2 */}
          <div className="flex items-center justify-between mt-3 text-xs font-semibold text-slate-400 border-t border-slate-800/60 pt-2.5 px-1">
            <div className={`cursor-pointer pb-1 transition-all ${step === 1 ? 'text-[#76d418] border-b-2 border-[#76d418]' : 'hover:text-slate-200'}`} onClick={() => setStep(1)}>
              1. Profile
            </div>
            <div className={`cursor-pointer pb-1 transition-all ${step === 2 ? 'text-[#76d418] border-b-2 border-[#76d418]' : 'hover:text-slate-200'}`} onClick={() => setStep(2)}>
              2. Workflows
            </div>
            <div className={`cursor-pointer pb-1 transition-all ${step === 3 ? 'text-[#76d418] border-b-2 border-[#76d418]' : 'hover:text-slate-200'}`} onClick={() => setStep(3)}>
              3. Agents
            </div>
            <div className={`cursor-pointer pb-1 transition-all ${step === 4 ? 'text-[#76d418] border-b-2 border-[#76d418]' : 'hover:text-slate-200'}`} onClick={() => setStep(4)}>
              4. Launch
            </div>
          </div>
        </div>

        {/* Purple Cyber Mesh Banner matching Photo 2 */}
        <div className="px-4 py-3">
          <div className="rounded-2xl p-4 bg-gradient-to-r from-purple-950 via-indigo-950 to-slate-950 border border-purple-800/40 relative overflow-hidden shadow-inner">
            <div className="absolute inset-0 opacity-20 bg-[radial-gradient(#a855f7_1px,transparent_1px)] [background-size:12px_12px] pointer-events-none" />
            <div className="relative z-10">
              <h3 className="text-sm font-bold text-white tracking-wide">
                Enterprise Workspace Setup
              </h3>
              <p className="text-xs text-purple-200/90 mt-1 leading-relaxed">
                Tailor autonomous agents & workflows to your corporate infrastructure
              </p>
            </div>
          </div>
        </div>

        {/* Step 1: Profile Form */}
        {step === 1 && (
          <div className="p-4 space-y-4">
            {/* Enterprise / Organization Name */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                <Building2 className="w-3.5 h-3.5 text-slate-400" />
                Enterprise / Organization Name
              </label>
              <input
                id="input-org-name"
                type="text"
                placeholder="e.g. Acme Global or Your Business Name"
                value={profile.name}
                onChange={(e) => setProfile({ ...profile, name: e.target.value })}
                className="w-full bg-[#0a141a] border border-slate-700/80 focus:border-[#76d418] rounded-xl p-3 text-white text-sm outline-none transition-all placeholder:text-slate-500"
              />
            </div>

            {/* Corporate Domain & Headcount Row */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* Corporate Domain with active green border like in Photo 2! */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-[#76d418] flex items-center gap-1.5">
                  <Globe className="w-3.5 h-3.5 text-[#76d418]" />
                  Corporate Domain
                </label>
                <div className="flex items-center gap-2 border-2 border-[#76d418] rounded-xl bg-[#09120b] p-3 shadow-sm shadow-[#76d418]/10">
                  <Globe className="w-4 h-4 text-slate-400 flex-shrink-0" />
                  <input
                    id="input-domain"
                    type="text"
                    value={profile.domain}
                    onChange={(e) => setProfile({ ...profile, domain: e.target.value })}
                    className="w-full bg-transparent text-white text-sm font-medium outline-none"
                    placeholder="company.global"
                  />
                </div>
              </div>

              {/* Headcount Scale */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                  <Users className="w-3.5 h-3.5 text-slate-400" />
                  Headcount Scale
                </label>
                <div className="flex items-center gap-2 border border-slate-700/80 rounded-xl bg-[#0a141a] p-3">
                  <Users className="w-4 h-4 text-slate-400 flex-shrink-0" />
                  <select
                    id="select-headcount"
                    value={profile.headcount}
                    onChange={(e) => setProfile({ ...profile, headcount: e.target.value })}
                    className="w-full bg-transparent text-white text-sm outline-none cursor-pointer"
                  >
                    <option value="1-10 Employees" className="bg-slate-900">1-10 Employees</option>
                    <option value="10-50 Employees" className="bg-slate-900">10-50 Employees</option>
                    <option value="50-250 Employees" className="bg-slate-900">50-250 Employees</option>
                    <option value="250-1000 Employees" className="bg-slate-900">250-1000 Employees</option>
                    <option value="1000+ Enterprise" className="bg-slate-900">1000+ Enterprise</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Target Industry Dropdown */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                <Layers className="w-3.5 h-3.5 text-slate-400" />
                Target Industry / Operations Domain
              </label>
              <div className="border border-slate-700/80 rounded-xl bg-[#0a141a] p-3">
                <select
                  id="select-industry"
                  value={profile.industry}
                  onChange={(e) => setProfile({ ...profile, industry: e.target.value })}
                  className="w-full bg-transparent text-white text-sm outline-none cursor-pointer"
                >
                  <option value="Technology & Software" className="bg-slate-900">Technology & Software</option>
                  <option value="Manufacturing & Logistics" className="bg-slate-900">Manufacturing & Logistics</option>
                  <option value="Finance & Professional Services" className="bg-slate-900">Finance & Professional Services</option>
                  <option value="Healthcare & Life Sciences" className="bg-slate-900">Healthcare & Life Sciences</option>
                  <option value="Telecommunications & Media" className="bg-slate-900">Telecommunications & Media</option>
                  <option value="General Business Operations" className="bg-slate-900">General Business Operations</option>
                </select>
              </div>
            </div>

            {/* Primary Operational Bottleneck */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300">
                Primary Operational Bottleneck
              </label>
              <textarea
                id="input-bottleneck"
                rows={2}
                value={profile.bottleneck}
                onChange={(e) => setProfile({ ...profile, bottleneck: e.target.value })}
                placeholder="Describe your current manual delays, call triage loads, or compliance bottlenecks..."
                className="w-full bg-[#0a141a] border border-slate-700/80 focus:border-[#76d418] rounded-xl p-3 text-white text-sm outline-none transition-all placeholder:text-slate-500 resize-none"
              />
            </div>
          </div>
        )}

        {/* Step 2: Workflows Setup */}
        {step === 2 && (
          <div className="p-4 space-y-3">
            <h4 className="text-xs font-bold text-[#76d418] uppercase tracking-wider">Automated Enterprise Workflows</h4>
            <div className="space-y-2 text-xs">
              <div className="p-3 rounded-xl bg-[#0a141a] border border-slate-800 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-white">Security & Regulatory Policy Guardrails</div>
                  <div className="text-slate-400 text-[11px]">Continuous authorization checks and audit logs</div>
                </div>
                <span className="text-[#76d418] font-bold">Enabled</span>
              </div>
              <div className="p-3 rounded-xl bg-[#0a141a] border border-slate-800 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-white">Autonomous Call Intake & Speech Transcription</div>
                  <div className="text-slate-400 text-[11px]">Voice SIP trunk routing & automated task creation</div>
                </div>
                <span className="text-[#76d418] font-bold">Enabled</span>
              </div>
              <div className="p-3 rounded-xl bg-[#0a141a] border border-slate-800 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-white">Autonomous Dispatch Pipeline</div>
                  <div className="text-slate-400 text-[11px]">Task priority triage and agent assignment</div>
                </div>
                <span className="text-[#76d418] font-bold">Enabled</span>
              </div>
            </div>
          </div>
        )}

        {/* Step 3: Agents Allocation */}
        {step === 3 && (
          <div className="p-4 space-y-3">
            <h4 className="text-xs font-bold text-[#76d418] uppercase tracking-wider">5 Autonomous RCOS Agents Ready</h4>
            <div className="grid grid-cols-1 gap-2 text-xs">
              <div className="p-2.5 rounded-xl bg-[#0a141a] border border-slate-800 flex items-center gap-2.5">
                <Bot className="w-4 h-4 text-[#76d418]" />
                <div>
                  <span className="font-semibold text-white">Compliance & Security Audit</span>
                  <span className="text-slate-400 ml-2">Legal & Compliance</span>
                </div>
              </div>
              <div className="p-2.5 rounded-xl bg-[#0a141a] border border-slate-800 flex items-center gap-2.5">
                <Zap className="w-4 h-4 text-[#76d418]" />
                <div>
                  <span className="font-semibold text-white">AI Workflow Engine</span>
                  <span className="text-slate-400 ml-2">Operations & Automation</span>
                </div>
              </div>
              <div className="p-2.5 rounded-xl bg-[#0a141a] border border-slate-800 flex items-center gap-2.5">
                <Bot className="w-4 h-4 text-[#76d418]" />
                <div>
                  <span className="font-semibold text-white">VoIP Telephony Engine</span>
                  <span className="text-slate-400 ml-2">Communications</span>
                </div>
              </div>
              <div className="p-2.5 rounded-xl bg-[#0a141a] border border-slate-800 flex items-center gap-2.5">
                <Bot className="w-4 h-4 text-[#76d418]" />
                <div>
                  <span className="font-semibold text-white">Enterprise Intelligence</span>
                  <span className="text-slate-400 ml-2">Strategic Analysis</span>
                </div>
              </div>
              <div className="p-2.5 rounded-xl bg-[#0a141a] border border-slate-800 flex items-center gap-2.5">
                <ShieldCheck className="w-4 h-4 text-[#76d418]" />
                <div>
                  <span className="font-semibold text-white">Master Orchestrator</span>
                  <span className="text-slate-400 ml-2">Executive Operations</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 4: Launch Confirmation */}
        {step === 4 && (
          <div className="p-4 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-[#76d418]/20 border border-[#76d418] mx-auto flex items-center justify-center text-[#76d418]">
              <Check className="w-6 h-6" />
            </div>
            <h4 className="text-base font-bold text-white">Ready to Deploy Enterprise Workspace</h4>
            <p className="text-xs text-slate-300 max-w-sm mx-auto">
              {profile.name ? `Workspace for ${profile.name}` : 'Enterprise workspace'} with domain <span className="text-[#76d418] font-mono">{profile.domain}</span> will be initialized with continuous agent monitoring.
            </p>
          </div>
        )}

        {/* Footer Actions matching Photo 2 */}
        <div className="p-4 pt-2 border-t border-slate-800/80 flex items-center justify-between">
          <button
            id="btn-cancel-setup"
            onClick={onClose}
            className="text-sm font-semibold text-[#76d418] hover:underline px-3 py-2 cursor-pointer"
          >
            Cancel
          </button>

          <button
            id="btn-next-step"
            onClick={handleNext}
            className="px-5 py-2.5 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 font-bold text-sm transition-all flex items-center gap-2 shadow-lg shadow-[#76d418]/25 cursor-pointer"
          >
            <span>
              {step === 1 && 'Next: Workflows'}
              {step === 2 && 'Next: Agents'}
              {step === 3 && 'Review & Launch'}
              {step === 4 && 'Launch Workspace'}
            </span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};
