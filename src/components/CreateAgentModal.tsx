import React, { useState } from 'react';
import { 
  Bot, 
  X, 
  Sparkles, 
  ShieldCheck, 
  Cpu, 
  CheckCircle2 
} from 'lucide-react';
import { Agent, RiskLevel, AccessLevel } from '../types';

interface CreateAgentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (agent: Partial<Agent>) => void;
}

export const CreateAgentModal: React.FC<CreateAgentModalProps> = ({
  isOpen,
  onClose,
  onCreate
}) => {
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [department, setDepartment] = useState('Executive Operations');
  const [modelTier, setModelTier] = useState<'gemini-3.7-flash' | 'gemini-2.5-pro' | 'gemini-2.5-flash'>('gemini-3.7-flash');
  const [riskClassification, setRiskClassification] = useState<RiskLevel>('Low');
  const [permissionLevel, setPermissionLevel] = useState<AccessLevel>('Regular Staff');
  const [capabilities, setCapabilities] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !role.trim()) return;

    const code = `${name.toUpperCase().replace(/\s+/g, '-')}-${Math.floor(10 + Math.random() * 90)}`;

    onCreate({
      name: name.trim(),
      codeName: code,
      role: role.trim(),
      department,
      modelTier,
      riskClassification,
      permissionLevel,
      capabilityProfile: capabilities.trim() || 'Autonomous enterprise agent executing task pipelines.',
      status: 'active',
      tasksCompleted: 0,
      uptime: '100%',
      lastActive: 'Just now',
      avatarSeed: name.toLowerCase()
    });

    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full p-6 space-y-5 shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <Bot className="w-5 h-5 text-emerald-400" />
            <h3 className="text-base font-bold text-white">Provision Autonomous Agent</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="text-slate-300 font-semibold block mb-1">Agent Name</label>
              <input
                type="text"
                required
                placeholder="e.g. Nexus Auditor"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-white placeholder:text-slate-500"
              />
            </div>

            <div>
              <label className="text-slate-300 font-semibold block mb-1">Department</label>
              <select
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-white"
              >
                <option value="Executive Operations">Executive Operations</option>
                <option value="Risk & Governance">Risk & Governance</option>
                <option value="Communications">Communications</option>
                <option value="Client Success">Client Success</option>
                <option value="Project Management">Project Management</option>
                <option value="Intelligence & Research">Intelligence & Research</option>
              </select>
            </div>
          </div>

          <div>
            <label className="text-slate-300 font-semibold block mb-1">Operational Role & Scope</label>
            <input
              type="text"
              required
              placeholder="e.g. Automated Invoice Reconciliation Specialist"
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-white placeholder:text-slate-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="text-slate-300 font-semibold block mb-1">Model Tier</label>
              <select
                value={modelTier}
                onChange={(e) => setModelTier(e.target.value as any)}
                className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-white font-mono text-[11px]"
              >
                <option value="gemini-3.7-flash">gemini-3.7-flash</option>
                <option value="gemini-2.5-pro">gemini-2.5-pro</option>
                <option value="gemini-2.5-flash">gemini-2.5-flash</option>
              </select>
            </div>

            <div>
              <label className="text-slate-300 font-semibold block mb-1">Risk Policy</label>
              <select
                value={riskClassification}
                onChange={(e) => setRiskClassification(e.target.value as any)}
                className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-white"
              >
                <option value="Low">Low Risk</option>
                <option value="Medium">Medium Risk</option>
                <option value="High">High Risk</option>
              </select>
            </div>

            <div>
              <label className="text-slate-300 font-semibold block mb-1">Permission Level</label>
              <select
                value={permissionLevel}
                onChange={(e) => setPermissionLevel(e.target.value as any)}
                className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-white"
              >
                <option value="Regular Staff">Regular Staff</option>
                <option value="Department Lead">Department Lead</option>
                <option value="Executive / Admin">Executive / Admin</option>
              </select>
            </div>
          </div>

          <div>
            <label className="text-slate-300 font-semibold block mb-1">Capability Profile & System Directives</label>
            <textarea
              rows={2}
              placeholder="Detail the specialized guidelines, automated API integrations, and safety limits for this unit..."
              value={capabilities}
              onChange={(e) => setCapabilities(e.target.value)}
              className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-white placeholder:text-slate-500"
            />
          </div>

          <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition-all shadow-md shadow-emerald-900/40 cursor-pointer"
            >
              Provision Agent
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
