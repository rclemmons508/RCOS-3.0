import React, { useState, useRef, useEffect } from 'react';
import { 
  Bot, 
  Send, 
  Sparkles, 
  Cpu, 
  ShieldCheck, 
  BrainCircuit, 
  RefreshCw,
  User
} from 'lucide-react';
import { Agent, ChatMessage } from '../types';

interface GeminiChatViewProps {
  agents: Agent[];
}

export const GeminiChatView: React.FC<GeminiChatViewProps> = ({ agents }) => {
  const [selectedAgent, setSelectedAgent] = useState<Agent>(agents[0]);
  const [inputMessage, setInputMessage] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'msg-1',
      sender: 'agent',
      agentName: 'Aegis Core',
      text: 'Aegis Core autonomous coordination online. How may I orchestrate organizational workloads, dispatch subordinate agents, or review client deliverable velocity today?',
      timestamp: 'Just now'
    }
  ]);
  const chatBottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isTyping]);

  const promptShortcuts = [
    'Generate Q3 autonomous agent velocity briefing',
    'Audit open jobs for compliance & SLA risk',
    'Simulate Apex Logistics tariff classification dispute',
    'Review high-priority task delegation across agents'
  ];

  const handleSend = async (textToSend?: string) => {
    const prompt = textToSend || inputMessage;
    if (!prompt.trim()) return;

    const userMsg: ChatMessage = {
      id: `user-${Date.now()}`,
      sender: 'user',
      text: prompt,
      timestamp: 'Just now'
    };

    setMessages(prev => [...prev, userMsg]);
    setInputMessage('');
    setIsTyping(true);

    // Simulate intelligent multi-agent response
    setTimeout(() => {
      let reply = '';
      let steps: string[] = [];

      if (prompt.toLowerCase().includes('velocity') || prompt.toLowerCase().includes('q3')) {
        reply = `Fleet telemetry analysis compiled for ${selectedAgent.name}: Autonomous workload velocity is up 34% week-over-week. Current bottleneck resides in manual approval gating on contracts exceeding $250k.`;
        steps = [
          'Queried telemetry logs across 6 active agents',
          'Calculated mean task resolution duration: 18.4 minutes',
          'Identified zero compliance threshold violations'
        ];
      } else if (prompt.toLowerCase().includes('audit') || prompt.toLowerCase().includes('compliance') || prompt.toLowerCase().includes('hipaa')) {
        reply = `Compliance scan report: All 42 telemetry channels conform to RCOS RBAC standards. HIPAA token scrubber is active with 0 unhashed PII leaks detected in current buffers.`;
        steps = [
          'Evaluated cryptographic key lifecycles',
          'Verified role permissions for Chief Executive Admin session',
          'Enforced data isolation on Beacon Health buffers'
        ];
      } else if (prompt.toLowerCase().includes('apex') || prompt.toLowerCase().includes('logistics') || prompt.toLowerCase().includes('tariff')) {
        reply = `Apex Global Logistics status update: Vessel ATL-993 manifests cross-referenced. Tariff classification 8471 verified against WTO schedule. Automated Job #101 is currently 72% complete with no blocking exceptions.`;
        steps = [
          'Accessed customs manifest telemetry',
          'Validated bill of lading against automated tariff table',
          'Updated Vanguard CRM client records'
        ];
      } else {
        reply = `Executing command through ${selectedAgent.name} (${selectedAgent.codeName}): The requested workload has been evaluated under policy threshold (<${selectedAgent.riskClassification} Risk). Autonomous state machine is actively executing sub-tasks.`;
        steps = [
          `Dispatched instruction to ${selectedAgent.department}`,
          'Logged event to cryptographic audit ledger',
          'Scheduled follow-up completion notification'
        ];
      }

      const agentReply: ChatMessage = {
        id: `agent-${Date.now()}`,
        sender: 'agent',
        agentName: selectedAgent.name,
        text: reply,
        timestamp: 'Just now',
        reasoningSteps: steps
      };

      setMessages(prev => [...prev, agentReply]);
      setIsTyping(false);
    }, 1000);
  };

  return (
    <div id="gemini-chat-container" className="space-y-4 h-[calc(100vh-140px)] flex flex-col">
      {/* Header & Agent Switcher */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 shrink-0 pb-3 border-b border-slate-800">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <Bot className="w-5 h-5 text-emerald-400" />
            <span>Gemini Autonomous Agent Chat</span>
          </h1>
          <p className="text-xs text-slate-400">
            Real-time conversational interface with any deployed enterprise agent unit
          </p>
        </div>

        {/* Agent Dropdown Switcher */}
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-400">Target Agent:</span>
          <select
            value={selectedAgent.id}
            onChange={(e) => {
              const found = agents.find(a => a.id === e.target.value);
              if (found) setSelectedAgent(found);
            }}
            className="bg-slate-900 border border-slate-700 text-xs font-semibold rounded-lg px-3 py-1.5 text-emerald-400 focus:outline-none cursor-pointer"
          >
            {agents.map(agent => (
              <option key={agent.id} value={agent.id}>
                {agent.name} ({agent.department})
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Messages Stream */}
      <div className="flex-1 overflow-y-auto space-y-4 p-4 rounded-xl bg-slate-950/60 border border-slate-800">
        {messages.map((msg) => {
          const isUser = msg.sender === 'user';

          return (
            <div
              key={msg.id}
              className={`flex gap-3 max-w-2xl ${isUser ? 'ml-auto flex-row-reverse' : ''}`}
            >
              <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 text-xs font-bold ${
                isUser 
                  ? 'bg-sky-600 text-white' 
                  : 'bg-slate-800 text-emerald-400 border border-slate-700'
              }`}>
                {isUser ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
              </div>

              <div className="space-y-1.5 flex-1">
                <div className={`text-[11px] font-semibold ${isUser ? 'text-right text-slate-400' : 'text-emerald-400'}`}>
                  {isUser ? 'You (Executive Admin)' : msg.agentName}
                </div>

                <div className={`p-3.5 rounded-2xl text-xs leading-relaxed ${
                  isUser 
                    ? 'bg-sky-600 text-white rounded-tr-none' 
                    : 'bg-slate-900 border border-slate-800 text-slate-200 rounded-tl-none'
                }`}>
                  {msg.text}

                  {msg.reasoningSteps && msg.reasoningSteps.length > 0 && (
                    <div className="mt-2.5 pt-2 border-t border-slate-800/80 space-y-1">
                      <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                        Reasoning Telemetry:
                      </span>
                      {msg.reasoningSteps.map((step, idx) => (
                        <div key={idx} className="text-[11px] text-emerald-400 font-mono flex items-center gap-1.5">
                          <span>✓</span>
                          <span>{step}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}

        {isTyping && (
          <div className="flex items-center gap-2 text-xs text-slate-400 animate-pulse">
            <Bot className="w-4 h-4 text-emerald-400" />
            <span>{selectedAgent.name} is synthesizing response...</span>
          </div>
        )}

        <div ref={chatBottomRef} />
      </div>

      {/* Prompt Shortcuts */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 shrink-0 no-scrollbar">
        {promptShortcuts.map((sc, i) => (
          <button
            key={i}
            onClick={() => handleSend(sc)}
            className="px-2.5 py-1 rounded-lg bg-slate-900 border border-slate-800 hover:border-slate-700 text-[11px] text-slate-400 hover:text-slate-200 whitespace-nowrap cursor-pointer transition-colors"
          >
            {sc}
          </button>
        ))}
      </div>

      {/* Input Box */}
      <form 
        onSubmit={(e) => {
          e.preventDefault();
          handleSend();
        }}
        className="flex items-center gap-2 shrink-0"
      >
        <input
          id="input-chat-message"
          type="text"
          placeholder={`Direct message to ${selectedAgent.name}...`}
          value={inputMessage}
          onChange={(e) => setInputMessage(e.target.value)}
          className="flex-1 px-4 py-3 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-emerald-500"
        />
        <button
          type="submit"
          disabled={!inputMessage.trim() || isTyping}
          className="px-5 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 disabled:opacity-40 text-white text-xs font-bold flex items-center gap-1.5 transition-colors cursor-pointer"
        >
          <Send className="w-4 h-4" />
          <span>Send</span>
        </button>
      </form>
    </div>
  );
};
