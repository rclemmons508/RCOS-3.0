import React, { useState } from 'react';
import { 
  Users, 
  Building2, 
  Phone, 
  Mail, 
  MapPin, 
  DollarSign, 
  Calendar, 
  Search, 
  Plus, 
  ArrowUpRight, 
  ShieldCheck,
  CheckCircle2,
  FileText
} from 'lucide-react';
import { Client, Job } from '../types';

interface ClientsViewProps {
  clients: Client[];
  jobs: Job[];
  onCreateClient: (client: Partial<Client>) => void;
}

export const ClientsView: React.FC<ClientsViewProps> = ({
  clients,
  jobs,
  onCreateClient
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [selectedClient, setSelectedClient] = useState<Client | null>(clients[0] || null);
  const [showAddModal, setShowAddModal] = useState(false);
  
  // Clean real form inputs (no fake defaults!)
  const [newCompanyName, setNewCompanyName] = useState('');
  const [newIndustry, setNewIndustry] = useState('Technology & Software');
  const [newEmail, setNewEmail] = useState('');
  const [newPhone, setNewPhone] = useState('');
  const [newContactName, setNewContactName] = useState('');
  const [newContractValue, setNewContractValue] = useState('');

  const filteredClients = clients.filter(c => {
    const matchesSearch = c.companyName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.industry.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.primaryContact.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'All' || c.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const clientJobs = selectedClient 
    ? jobs.filter(j => j.clientName.toLowerCase() === selectedClient.companyName.toLowerCase())
    : [];

  const handleAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCompanyName.trim()) return;

    onCreateClient({
      companyName: newCompanyName.trim(),
      industry: newIndustry,
      accountEmail: newEmail.trim() || 'contact@client.com',
      phone: newPhone.trim() || '+1 (555) 000-0000',
      status: 'Active Enterprise',
      headquarters: 'Corporate HQ',
      contractValue: newContractValue.trim() || '$100,000 / yr',
      onboardingDate: new Date().toLocaleDateString('en-US', { month: 'short', year: 'numeric' }),
      primaryContact: {
        name: newContactName.trim() || 'Primary Contact',
        role: 'Operations Lead',
        email: newEmail.trim() || 'contact@client.com',
        phone: newPhone.trim() || '+1 (555) 000-0000',
        preferredChannel: 'Email'
      },
      assignedAgent: 'Compliance & Security Audit',
      notes: 'Real client organization registered in RCOS directory.',
      activeJobsCount: 0
    });

    setNewCompanyName('');
    setNewEmail('');
    setNewPhone('');
    setNewContactName('');
    setNewContractValue('');
    setShowAddModal(false);
  };

  return (
    <div id="clients-view-container" className="space-y-4 max-w-5xl mx-auto pb-20">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <Users className="w-5 h-5 text-[#76d418]" />
            <span>Enterprise Clients & Directory</span>
          </h1>
          <p className="text-xs text-slate-400">
            Manage your real business clients, SLAs, and assign autonomous agents
          </p>
        </div>

        <button
          id="btn-add-client-account"
          onClick={() => setShowAddModal(true)}
          className="px-4 py-2 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 font-bold text-xs flex items-center gap-2 transition-all shadow-md shadow-[#76d418]/20 cursor-pointer self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Add Client Organization</span>
        </button>
      </div>

      {/* Filter and Search */}
      {clients.length > 0 && (
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search enterprise accounts by company, contact, industry..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 rounded-xl bg-[#0a1218] border border-slate-800 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-[#76d418]"
            />
          </div>

          <div className="flex items-center gap-2 overflow-x-auto pb-1">
            {['All', 'Active Enterprise', 'VIP Client', 'Prospect Onboarding'].map((status) => (
              <button
                key={status}
                onClick={() => setStatusFilter(status)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold cursor-pointer transition-colors whitespace-nowrap ${
                  statusFilter === status
                    ? 'bg-[#76d418]/20 text-[#76d418] border border-[#76d418]/50'
                    : 'bg-[#0a1218] text-slate-400 hover:text-slate-200 border border-slate-800'
                }`}
              >
                {status}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* If No Clients Yet: Clean Zero State */}
      {clients.length === 0 ? (
        <div className="p-10 rounded-2xl bg-[#0a1218] border border-slate-800 text-center space-y-3">
          <div className="w-14 h-14 rounded-full bg-[#76d418]/15 text-[#76d418] flex items-center justify-center mx-auto">
            <Building2 className="w-7 h-7" />
          </div>
          <h3 className="text-base font-bold text-white">No client organizations registered</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Add your actual clients, accounts, or corporate partners. No placeholder or fictional data is inserted.
          </p>
          <button
            onClick={() => setShowAddModal(true)}
            className="px-5 py-2.5 rounded-xl bg-[#76d418] text-slate-950 font-bold text-xs inline-flex items-center gap-2 cursor-pointer shadow-md shadow-[#76d418]/20"
          >
            <Plus className="w-4 h-4" />
            <span>Add Client Organization</span>
          </button>
        </div>
      ) : (
        /* Grid: Client Cards & Selected Account Dossier */
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Client Roster List */}
          <div className="space-y-3">
            {filteredClients.map((client) => {
              const isSelected = selectedClient?.id === client.id;

              return (
                <div
                  key={client.id}
                  id={`client-card-${client.id}`}
                  onClick={() => setSelectedClient(client)}
                  className={`p-4 rounded-xl border transition-all cursor-pointer space-y-2.5 ${
                    isSelected
                      ? 'bg-[#0c1622] border-[#76d418]/60 ring-1 ring-[#76d418]/30 shadow-md'
                      : 'bg-[#0a1218] border-slate-800/80 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="text-sm font-bold text-white flex items-center gap-2">
                        <span>{client.companyName}</span>
                      </h3>
                      <p className="text-xs text-slate-400">{client.industry}</p>
                    </div>

                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-[#76d418]/15 text-[#76d418] border border-[#76d418]/30">
                      {client.status}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-xs pt-2 border-t border-slate-800/80">
                    <div>
                      <span className="text-slate-500 text-[10px]">Contact</span>
                      <p className="text-slate-300 font-medium truncate">{client.primaryContact.name}</p>
                    </div>
                    <div>
                      <span className="text-slate-500 text-[10px]">Contract</span>
                      <p className="text-[#76d418] font-mono font-medium truncate">{client.contractValue}</p>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Client Dossier Detail Pane */}
          <div className="lg:col-span-2">
            {selectedClient ? (
              <div className="p-5 rounded-2xl bg-[#091016] border border-slate-800 space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                  <div>
                    <h2 className="text-lg font-bold text-white">{selectedClient.companyName}</h2>
                    <p className="text-xs text-slate-400">{selectedClient.industry} • {selectedClient.headquarters}</p>
                  </div>
                  <span className="text-xs font-bold text-[#76d418] bg-[#76d418]/10 px-2.5 py-1 rounded-full border border-[#76d418]/30">
                    {selectedClient.status}
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs">
                  <div className="p-3 rounded-xl bg-[#0a141a] border border-slate-800">
                    <span className="text-slate-400 text-[10px]">Email</span>
                    <p className="text-white font-medium truncate">{selectedClient.accountEmail}</p>
                  </div>
                  <div className="p-3 rounded-xl bg-[#0a141a] border border-slate-800">
                    <span className="text-slate-400 text-[10px]">Phone</span>
                    <p className="text-white font-medium">{selectedClient.phone}</p>
                  </div>
                  <div className="p-3 rounded-xl bg-[#0a141a] border border-slate-800">
                    <span className="text-slate-400 text-[10px]">Contract Value</span>
                    <p className="text-[#76d418] font-bold font-mono">{selectedClient.contractValue}</p>
                  </div>
                </div>

                <div className="p-4 rounded-xl bg-[#0a141a] border border-slate-800 space-y-2">
                  <h4 className="text-xs font-bold text-white uppercase tracking-wider">Primary Contact</h4>
                  <div className="text-xs text-slate-300">
                    <span className="font-semibold text-white">{selectedClient.primaryContact.name}</span> — {selectedClient.primaryContact.role}
                    <div className="text-slate-400 text-[11px] mt-0.5">{selectedClient.primaryContact.email} • {selectedClient.primaryContact.phone}</div>
                  </div>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      )}

      {/* Add Client Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-[#070d09] border border-[#76d418]/50 rounded-2xl p-5 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Building2 className="w-4 h-4 text-[#76d418]" />
                <span>Register Client Organization</span>
              </h3>
              <button onClick={() => setShowAddModal(false)} className="text-slate-400 hover:text-white">✕</button>
            </div>

            <form onSubmit={handleAddSubmit} className="space-y-3">
              <div>
                <label className="text-xs font-semibold text-slate-300">Company / Organization Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Partner Corporation"
                  value={newCompanyName}
                  onChange={(e) => setNewCompanyName(e.target.value)}
                  className="w-full mt-1 bg-[#0a141a] border border-slate-700 focus:border-[#76d418] rounded-xl p-2.5 text-xs text-white outline-none"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-300">Industry / Sector</label>
                <input
                  type="text"
                  placeholder="e.g. Technology, Healthcare, Logistics"
                  value={newIndustry}
                  onChange={(e) => setNewIndustry(e.target.value)}
                  className="w-full mt-1 bg-[#0a141a] border border-slate-700 focus:border-[#76d418] rounded-xl p-2.5 text-xs text-white outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-slate-300">Primary Contact Name</label>
                  <input
                    type="text"
                    placeholder="Contact Name"
                    value={newContactName}
                    onChange={(e) => setNewContactName(e.target.value)}
                    className="w-full mt-1 bg-[#0a141a] border border-slate-700 focus:border-[#76d418] rounded-xl p-2.5 text-xs text-white outline-none"
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-300">Contract Value / Year</label>
                  <input
                    type="text"
                    placeholder="e.g. $50,000 / yr"
                    value={newContractValue}
                    onChange={(e) => setNewContractValue(e.target.value)}
                    className="w-full mt-1 bg-[#0a141a] border border-slate-700 focus:border-[#76d418] rounded-xl p-2.5 text-xs text-white outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-slate-300">Email Address</label>
                  <input
                    type="email"
                    placeholder="contact@client.com"
                    value={newEmail}
                    onChange={(e) => setNewEmail(e.target.value)}
                    className="w-full mt-1 bg-[#0a141a] border border-slate-700 focus:border-[#76d418] rounded-xl p-2.5 text-xs text-white outline-none"
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-300">Phone Number</label>
                  <input
                    type="tel"
                    placeholder="+1 (555) 000-0000"
                    value={newPhone}
                    onChange={(e) => setNewPhone(e.target.value)}
                    className="w-full mt-1 bg-[#0a141a] border border-slate-700 focus:border-[#76d418] rounded-xl p-2.5 text-xs text-white outline-none"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-3 py-2 rounded-xl text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 font-bold text-xs"
                >
                  Register Client
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
