import React, { useState } from 'react';
import { 
  ShieldAlert, 
  ShieldCheck, 
  Lock, 
  Key, 
  Sliders, 
  Search, 
  CheckCircle2, 
  AlertTriangle,
  Layers,
  Database
} from 'lucide-react';
import { AuditLog } from '../types';

interface GovernanceViewProps {
  auditLogs: AuditLog[];
}

export const GovernanceView: React.FC<GovernanceViewProps> = ({ auditLogs }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [riskThreshold, setRiskThreshold] = useState(20);
  const [dollarLimit, setDollarLimit] = useState(5000);

  const filteredLogs = auditLogs.filter(log =>
    log.action.toLowerCase().includes(searchQuery.toLowerCase()) ||
    log.actor.toLowerCase().includes(searchQuery.toLowerCase()) ||
    log.resource.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div id="governance-view-container" className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-emerald-400" />
            <span>Multi-Workspace Governance, RBAC & Audit Ledger</span>
          </h1>
          <p className="text-xs text-slate-400">
            Cryptographic access policies, role permissions, and immutable audit logs
          </p>
        </div>

        <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-xs font-semibold text-emerald-400">
          <ShieldCheck className="w-3.5 h-3.5" />
          <span>Active Workspace: ws_rcos_master</span>
        </div>
      </div>

      {/* Governance & Risk Threshold Policies */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-white uppercase tracking-wider">
              Autonomous Auto-Approval Risk Ceiling
            </span>
            <span className="text-sm font-bold text-emerald-400 font-mono">&lt; {riskThreshold}% Risk</span>
          </div>
          <p className="text-xs text-slate-400">
            Workflows and agent actions below this calculated risk threshold execute autonomously without blocking for executive signature.
          </p>
          <input
            type="range"
            min="5"
            max="50"
            step="5"
            value={riskThreshold}
            onChange={(e) => setRiskThreshold(Number(e.target.value))}
            className="w-full accent-emerald-500 cursor-pointer"
          />
        </div>

        <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-white uppercase tracking-wider">
              Autonomous Financial Dollar Limit
            </span>
            <span className="text-sm font-bold text-emerald-400 font-mono">${dollarLimit.toLocaleString()}</span>
          </div>
          <p className="text-xs text-slate-400">
            Maximum single expenditure an autonomous agent (e.g. cloud compute, API token reloads) can dispatch without manual gating.
          </p>
          <input
            type="range"
            min="1000"
            max="25000"
            step="1000"
            value={dollarLimit}
            onChange={(e) => setDollarLimit(Number(e.target.value))}
            className="w-full accent-emerald-500 cursor-pointer"
          />
        </div>
      </div>

      {/* RBAC Role Matrix */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-3">
        <h3 className="text-sm font-bold text-white">Enterprise Role-Based Access Control (RBAC)</h3>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 text-[10px] uppercase tracking-wider">
                <th className="py-2.5 px-3">Role Tier</th>
                <th className="py-2.5 px-3">Model Access</th>
                <th className="py-2.5 px-3">Agent Dispatch</th>
                <th className="py-2.5 px-3">Financial Limit</th>
                <th className="py-2.5 px-3">Vault Security</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              <tr>
                <td className="py-2.5 px-3 font-semibold text-emerald-400">Chief Executive Admin</td>
                <td className="py-2.5 px-3">Gemini 2.5 Pro & 3.7 Flash</td>
                <td className="py-2.5 px-3 font-mono">Full Fleet</td>
                <td className="py-2.5 px-3">Unlimited</td>
                <td className="py-2.5 px-3 text-emerald-400">Read / Write / Delete</td>
              </tr>
              <tr>
                <td className="py-2.5 px-3 font-semibold text-white">Department Lead</td>
                <td className="py-2.5 px-3">Gemini 3.7 Flash</td>
                <td className="py-2.5 px-3 font-mono">Departmental Units</td>
                <td className="py-2.5 px-3">$10,000 / day</td>
                <td className="py-2.5 px-3 text-slate-400">Read Only</td>
              </tr>
              <tr>
                <td className="py-2.5 px-3 font-semibold text-white">Autonomous Agent Sentry</td>
                <td className="py-2.5 px-3">Scoped API Key</td>
                <td className="py-2.5 px-3 font-mono">Workflow Gated</td>
                <td className="py-2.5 px-3">&lt; ${dollarLimit.toLocaleString()}</td>
                <td className="py-2.5 px-3 text-slate-500">Append Only (Audit)</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Cryptographic Audit Log Table */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h3 className="text-sm font-bold text-white">Cryptographic Audit Ledger</h3>
            <p className="text-xs text-slate-400">Immutable trace logs for all autonomous actions and permission gates</p>
          </div>

          <div className="relative w-full sm:w-64">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search audit logs..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-8 pr-3 py-1.5 rounded-lg bg-slate-950 border border-slate-800 text-xs text-white"
            />
          </div>
        </div>

        <div className="space-y-2">
          {filteredLogs.map((log) => (
            <div
              key={log.id}
              className="p-3.5 rounded-lg bg-slate-950/70 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs"
            >
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <span className="font-mono font-bold text-emerald-400">{log.action}</span>
                  <span className="text-slate-500 font-mono text-[10px]">[{log.timestamp}]</span>
                  <span className="px-1.5 py-0.2 rounded text-[10px] font-bold uppercase bg-emerald-500/20 text-emerald-400">
                    {log.status}
                  </span>
                </div>
                <p className="text-slate-300">{log.details}</p>
              </div>

              <div className="text-right text-[11px] text-slate-400 shrink-0">
                <div>Actor: <strong className="text-slate-200">{log.actor}</strong></div>
                <div className="font-mono text-[10px] text-slate-500">{log.ipAddress}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
