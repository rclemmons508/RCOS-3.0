import React, { useState, useRef, useEffect } from 'react';
import { 
  Bot, 
  Send, 
  Sparkles, 
  Search, 
  RefreshCw, 
  User, 
  Globe, 
  ExternalLink, 
  Sliders, 
  Zap, 
  ShieldCheck,
  CheckCircle2,
  Trash2,
  Cpu
} from 'lucide-react';
import { Agent } from '../types';
import { geminiClientService, GeminiModelType, ChatHistoryMessage } from '../services/geminiClientService';

interface GeminiChatViewProps {
  agents: Agent[];
}

interface ChatRolePreset {
  id: string;
  name: string;
  department: string;
  systemInstruction: string;
  recommendedModel: GeminiModelType;
}

const ROLE_PRESETS: ChatRolePreset[] = [
  {
    id: 'executive',
    name: 'Executive Fleet Orchestrator',
    department: 'Command',
    systemInstruction: 'You are the Chief Autonomous Operations Orchestrator for RCOS. You synthesize enterprise telemetry, triage organizational bottlenecks, and delegate directives across subordinate agent fleets with maximum precision.',
    recommendedModel: 'gemini-3.5-flash'
  },
  {
    id: 'compliance',
    name: 'Compliance & SLA Auditor',
    department: 'Governance',
    systemInstruction: 'You are the RCOS Regulatory Compliance and SLA Risk Auditor. You analyze contracts, HIPAA/PII scrubbers, cryptographic audit logs, and enforce strict governance policies across all enterprise workflows.',
    recommendedModel: 'gemini-3.1-pro-preview'
  },
  {
    id: 'dispatch',
    name: 'Real-Time Dispatch Officer',
    department: 'Field Ops',
    systemInstruction: 'You are the rapid job dispatch assistant for field operations and logistics. You provide instantaneous work order allocations, crew assignments, and priority triage.',
    recommendedModel: 'gemini-3.1-flash-lite'
  },
  {
    id: 'research',
    name: 'Global Market Intelligence',
    department: 'Intelligence',
    systemInstruction: 'You are the Market Intelligence Agent. You perform real-time web search grounding to provide verified, up-to-date facts, tariff changes, vendor benchmarks, and market shifts.',
    recommendedModel: 'gemini-3.5-flash'
  }
];

export const GeminiChatView: React.FC<GeminiChatViewProps> = ({ agents }) => {
  const [selectedModel, setSelectedModel] = useState<GeminiModelType>('gemini-3.5-flash');
  const [selectedRole, setSelectedRole] = useState<ChatRolePreset>(ROLE_PRESETS[0]);
  const [customSystemInstruction, setCustomSystemInstruction] = useState<string>(ROLE_PRESETS[0].systemInstruction);
  const [showRoleConfig, setShowRoleConfig] = useState<boolean>(false);
  const [enableSearchGrounding, setEnableSearchGrounding] = useState<boolean>(false);

  const [inputMessage, setInputMessage] = useState<string>('');
  const [isGenerating, setIsGenerating] = useState<boolean>(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const [conversationHistory, setConversationHistory] = useState<ChatHistoryMessage[]>([
    {
      role: 'model',
      content: `Greetings Executive. RCOS Autonomous Multi-Turn Agent Console is active. Powered by Gemini, I can assist with enterprise fleet orchestration, governance audits, or real-time search-grounded research. What directive shall we evaluate?`,
      timestamp: 'Online',
      modelUsed: 'gemini-3.5-flash'
    }
  ]);

  const chatBottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [conversationHistory, isGenerating]);

  const handleSelectRole = (preset: ChatRolePreset) => {
    setSelectedRole(preset);
    setCustomSystemInstruction(preset.systemInstruction);
    setSelectedModel(preset.recommendedModel);
    if (preset.id === 'research') {
      setEnableSearchGrounding(true);
    }
  };

  const handleClearHistory = () => {
    if (window.confirm('Clear conversation history?')) {
      setConversationHistory([
        {
          role: 'model',
          content: `New multi-turn conversation started with ${selectedRole.name}. Ready for directives.`,
          timestamp: 'Just now',
          modelUsed: selectedModel
        }
      ]);
      setApiError(null);
    }
  };

  const handleSendMessage = async (customPrompt?: string) => {
    const textToSend = (customPrompt || inputMessage).trim();
    if (!textToSend || isGenerating) return;

    setApiError(null);
    setInputMessage('');

    const newUserMsg: ChatHistoryMessage = {
      role: 'user',
      content: textToSend,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    const updatedHistory = [...conversationHistory, newUserMsg];
    setConversationHistory(updatedHistory);
    setIsGenerating(true);

    try {
      // Send entire conversation history to maintain multi-turn context
      const response = await geminiClientService.sendChatMessage(
        updatedHistory,
        selectedModel,
        customSystemInstruction,
        enableSearchGrounding
      );

      const modelReplyMsg: ChatHistoryMessage = {
        role: 'model',
        content: response.text,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        sources: response.sources,
        modelUsed: response.modelUsed || selectedModel
      };

      setConversationHistory(prev => [...prev, modelReplyMsg]);
    } catch (err: any) {
      console.error('Chat error:', err);
      setApiError(err.message || 'Failed to generate response. Please verify the server configuration.');
      
      const errorMsg: ChatHistoryMessage = {
        role: 'model',
        content: `Operational Notice: ${err.message || 'Unable to complete response. Please check server connection.'}`,
        timestamp: 'Error',
        modelUsed: selectedModel
      };
      setConversationHistory(prev => [...prev, errorMsg]);
    } finally {
      setIsGenerating(false);
    }
  };

  const promptShortcuts = [
    { label: 'Q3 Telemetry Velocity', query: 'Summarize our autonomous agent velocity and identify key bottlenecks in fleet operations.' },
    { label: 'Compliance & HIPAA Audit', query: 'Audit open jobs for HIPAA compliance risks and verify PII scrubber thresholds.' },
    { label: 'Search Real-Time AI News', query: 'What are the latest enterprise multi-agent system advancements published this week?', search: true },
    { label: 'High Priority Delegation', query: 'Break down an optimal task distribution schedule for 6 autonomous agents.' }
  ];

  return (
    <div id="gemini-chat-view" className="flex flex-col h-[calc(100vh-145px)] max-w-4xl mx-auto space-y-3">
      {/* Top Controls & Configuration Bar */}
      <div className="bg-[#070e0a] border border-[#76d418]/30 rounded-2xl p-3.5 shrink-0 shadow-lg shadow-black/40">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
          {/* Title & Role */}
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#76d418]/15 border border-[#76d418]/40 flex items-center justify-center text-[#76d418]">
              <Bot className="w-5 h-5 text-[#76d418]" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-sm font-bold text-white tracking-tight">
                  Gemini Autonomous Chat
                </h1>
                <span className="px-2 py-0.5 rounded-full bg-[#76d418]/20 border border-[#76d418]/40 text-[#76d418] text-[10px] font-bold">
                  Multi-Turn
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Active Role: <span className="text-[#76d418] font-semibold">{selectedRole.name}</span>
              </p>
            </div>
          </div>

          {/* Model Selector Pills */}
          <div className="flex flex-wrap items-center gap-1.5">
            <button
              type="button"
              id="model-gemini-3-5-flash"
              onClick={() => setSelectedModel('gemini-3.5-flash')}
              className={`px-2.5 py-1 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer ${
                selectedModel === 'gemini-3.5-flash'
                  ? 'bg-[#76d418] text-slate-950 shadow-sm shadow-[#76d418]/30'
                  : 'bg-slate-900 text-slate-400 border border-slate-800 hover:text-white'
              }`}
              title="gemini-3.5-flash: Balanced for general enterprise tasks and search grounding"
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>3.5 Flash (General)</span>
            </button>

            <button
              type="button"
              id="model-gemini-3-1-pro-preview"
              onClick={() => setSelectedModel('gemini-3.1-pro-preview')}
              className={`px-2.5 py-1 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer ${
                selectedModel === 'gemini-3.1-pro-preview'
                  ? 'bg-amber-400 text-slate-950 shadow-sm shadow-amber-400/30'
                  : 'bg-slate-900 text-slate-400 border border-slate-800 hover:text-white'
              }`}
              title="gemini-3.1-pro-preview: Deep reasoning for particularly complex tasks"
            >
              <Cpu className="w-3.5 h-3.5" />
              <span>3.1 Pro (Complex)</span>
            </button>

            <button
              type="button"
              id="model-gemini-3-1-flash-lite"
              onClick={() => setSelectedModel('gemini-3.1-flash-lite')}
              className={`px-2.5 py-1 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer ${
                selectedModel === 'gemini-3.1-flash-lite'
                  ? 'bg-sky-400 text-slate-950 shadow-sm shadow-sky-400/30'
                  : 'bg-slate-900 text-slate-400 border border-slate-800 hover:text-white'
              }`}
              title="gemini-3.1-flash-lite: Low-latency ultra fast responses"
            >
              <Zap className="w-3.5 h-3.5" />
              <span>3.1 Lite (Fast)</span>
            </button>

            {/* Role Config Toggle */}
            <button
              type="button"
              id="btn-toggle-role-config"
              onClick={() => setShowRoleConfig(!showRoleConfig)}
              className={`p-1.5 rounded-lg border transition-colors cursor-pointer ${
                showRoleConfig
                  ? 'bg-[#76d418]/20 border-[#76d418] text-[#76d418]'
                  : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-white'
              }`}
              title="Customize chatbot role & system instruction"
            >
              <Sliders className="w-3.5 h-3.5" />
            </button>

            {/* Clear History */}
            <button
              type="button"
              onClick={handleClearHistory}
              className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-rose-400 transition-colors cursor-pointer"
              title="Clear conversation"
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Search Grounding Bar */}
        <div className="mt-2.5 pt-2 border-t border-slate-800/80 flex flex-wrap items-center justify-between gap-2">
          <label className="flex items-center gap-2 cursor-pointer select-none">
            <input
              type="checkbox"
              id="toggle-search-grounding"
              checked={enableSearchGrounding}
              onChange={(e) => setEnableSearchGrounding(e.target.checked)}
              className="w-4 h-4 rounded border-slate-700 text-[#76d418] accent-[#76d418] focus:ring-0 cursor-pointer"
            />
            <div className="flex items-center gap-1.5 text-xs text-slate-300">
              <Globe className="w-3.5 h-3.5 text-[#76d418]" />
              <span className="font-semibold">Google Search Grounding</span>
              <span className="text-[10px] text-slate-400 font-normal">
                (Up-to-date real-time web citations via gemini-3.5-flash)
              </span>
            </div>
          </label>

          <span className="text-[11px] font-mono text-slate-500">
            History: {conversationHistory.length} turns
          </span>
        </div>

        {/* Collapsible System Instruction & Role Presets */}
        {showRoleConfig && (
          <div className="mt-3 pt-3 border-t border-slate-800 space-y-2.5 animate-in fade-in duration-150">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-300">Preset Chatbot Roles:</span>
              <span className="text-[11px] text-slate-400">Select a role to adapt the system instruction</span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {ROLE_PRESETS.map((preset) => (
                <button
                  key={preset.id}
                  type="button"
                  onClick={() => handleSelectRole(preset)}
                  className={`p-2 rounded-xl text-left border transition-all cursor-pointer ${
                    selectedRole.id === preset.id
                      ? 'bg-[#0e2113] border-[#76d418] text-white shadow-sm shadow-[#76d418]/20'
                      : 'bg-slate-900 border-slate-800 text-slate-300 hover:border-slate-700'
                  }`}
                >
                  <div className="text-xs font-bold truncate text-white">{preset.name}</div>
                  <div className="text-[10px] text-[#76d418]">{preset.department}</div>
                </button>
              ))}
            </div>

            <div className="space-y-1">
              <label className="text-[11px] font-semibold text-slate-400">
                System Instruction (Gives chatbot its specific role and operational scope):
              </label>
              <textarea
                value={customSystemInstruction}
                onChange={(e) => setCustomSystemInstruction(e.target.value)}
                rows={2}
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-[#76d418] resize-none"
                placeholder="Define role, boundaries, tone, or specific responsibilities..."
              />
            </div>
          </div>
        )}
      </div>

      {/* Scrollable Conversation Thread */}
      <div 
        id="chat-messages-container"
        className="flex-1 overflow-y-auto space-y-3.5 p-4 rounded-2xl bg-[#050b07] border border-slate-800/80 shadow-inner"
      >
        {conversationHistory.map((msg, index) => {
          const isUser = msg.role === 'user';

          return (
            <div
              key={`msg-${index}`}
              className={`flex gap-3 max-w-3xl ${isUser ? 'ml-auto flex-row-reverse' : ''}`}
            >
              {/* Avatar Icon */}
              <div
                className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 text-xs font-bold border ${
                  isUser
                    ? 'bg-[#76d418] text-slate-950 border-[#76d418]/60 shadow-md shadow-[#76d418]/20'
                    : 'bg-slate-900 text-[#76d418] border-slate-800'
                }`}
              >
                {isUser ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4 text-[#76d418]" />}
              </div>

              {/* Message Bubble */}
              <div className="space-y-1.5 flex-1">
                <div className={`flex items-center gap-2 text-[11px] font-semibold ${
                  isUser ? 'justify-end text-slate-400' : 'text-[#76d418]'
                }`}>
                  <span>{isUser ? 'Executive You' : selectedRole.name}</span>
                  {msg.modelUsed && !isUser && (
                    <span className="text-[9px] px-1.5 py-0.5 rounded bg-slate-900 border border-slate-800 text-slate-400 font-mono">
                      {msg.modelUsed}
                    </span>
                  )}
                  <span className="text-[10px] text-slate-500 font-normal">{msg.timestamp}</span>
                </div>

                <div
                  className={`p-4 rounded-2xl text-xs leading-relaxed whitespace-pre-wrap ${
                    isUser
                      ? 'bg-[#76d418] text-slate-950 font-medium rounded-tr-none shadow-md shadow-[#76d418]/15'
                      : 'bg-[#08120b] border border-[#76d418]/25 text-slate-200 rounded-tl-none shadow-sm'
                  }`}
                >
                  {msg.content}

                  {/* Google Search Grounding Sources */}
                  {msg.sources && msg.sources.length > 0 && (
                    <div className="mt-3 pt-2.5 border-t border-[#76d418]/20 space-y-1.5">
                      <div className="flex items-center gap-1.5 text-[10px] font-bold text-[#76d418] uppercase tracking-wider">
                        <Globe className="w-3.5 h-3.5" />
                        <span>Google Search Grounding Sources ({msg.sources.length}):</span>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {msg.sources.map((src, srcIdx) => (
                          <a
                            key={srcIdx}
                            href={src.uri}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-900/90 border border-slate-800 hover:border-[#76d418] text-[10px] text-slate-300 hover:text-[#76d418] transition-colors"
                          >
                            <ExternalLink className="w-3 h-3" />
                            <span className="max-w-[200px] truncate">{src.title || src.uri}</span>
                          </a>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}

        {isGenerating && (
          <div className="flex items-center gap-2 text-xs text-[#76d418] animate-pulse bg-[#08120b] border border-[#76d418]/20 p-3 rounded-2xl w-fit">
            <Bot className="w-4 h-4 text-[#76d418]" />
            <span>{selectedRole.name} is evaluating and generating with {selectedModel}...</span>
          </div>
        )}

        <div ref={chatBottomRef} />
      </div>

      {/* Suggested Prompt Shortcuts */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 shrink-0 no-scrollbar">
        {promptShortcuts.map((sc, i) => (
          <button
            key={i}
            type="button"
            onClick={() => {
              if (sc.search) setEnableSearchGrounding(true);
              handleSendMessage(sc.query);
            }}
            className="px-2.5 py-1 rounded-lg bg-slate-900/90 border border-slate-800 hover:border-[#76d418]/60 text-[11px] text-slate-300 hover:text-white whitespace-nowrap cursor-pointer transition-colors"
          >
            {sc.label}
          </button>
        ))}
      </div>

      {/* Message Input & Send Form */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          handleSendMessage();
        }}
        className="flex items-center gap-2 shrink-0"
      >
        <input
          id="input-gemini-chat"
          type="text"
          placeholder={`Direct message to ${selectedRole.name} (Enter prompt)...`}
          value={inputMessage}
          onChange={(e) => setInputMessage(e.target.value)}
          disabled={isGenerating}
          className="flex-1 px-4 py-3 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-[#76d418] transition-colors disabled:opacity-50"
        />

        <button
          id="btn-send-gemini-chat"
          type="submit"
          disabled={!inputMessage.trim() || isGenerating}
          className="px-5 py-3 rounded-xl bg-[#76d418] hover:bg-[#66bd14] disabled:opacity-40 text-slate-950 font-bold text-xs flex items-center gap-1.5 transition-all shadow-md shadow-[#76d418]/20 cursor-pointer"
        >
          <Send className="w-4 h-4" />
          <span>Send</span>
        </button>
      </form>
    </div>
  );
};
