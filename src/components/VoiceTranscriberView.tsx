import React, { useState, useEffect, useRef } from 'react';
import { 
  Mic, 
  MicOff, 
  Play, 
  Square, 
  CheckCircle2, 
  Sparkles, 
  FileText, 
  ArrowRight,
  Activity,
  Volume2,
  RefreshCw,
  Zap,
  Bot,
  AlertCircle,
  Command,
  History,
  Send,
  Radio,
  ShieldCheck,
  Check
} from 'lucide-react';
import { Agent } from '../types';
import { GeminiLiveVoiceView } from './GeminiLiveVoiceView';

export interface VoiceDirectiveLog {
  id: string;
  timestamp: string;
  rawVoiceInput: string;
  extractedTask: string;
  targetAgentId: string;
  targetAgentName: string;
  status: 'Dispatched' | 'Executing';
}

interface VoiceTranscriberViewProps {
  agents?: Agent[];
  onDirectAgentTask?: (agentId: string, task: string) => void;
  onCreateJobFromVoice?: (title: string, summary: string) => void;
  onNavigate?: (tab: string) => void;
}

export const VoiceTranscriberView: React.FC<VoiceTranscriberViewProps> = ({
  agents = [],
  onDirectAgentTask,
  onCreateJobFromVoice,
  onNavigate
}) => {
  // Web Speech API states
  const [isListening, setIsListening] = useState<boolean>(false);
  const [isSpeechSupported, setIsSpeechSupported] = useState<boolean>(true);
  const [speechError, setSpeechError] = useState<string | null>(null);
  const [interimTranscript, setInterimTranscript] = useState<string>('');
  const [finalTranscript, setFinalTranscript] = useState<string>('');
  const [recordSeconds, setRecordSeconds] = useState<number>(0);

  // Command detection & execution states
  const [activeVoiceTab, setActiveVoiceTab] = useState<'live' | 'commands'>('live');
  const [voiceLogs, setVoiceLogs] = useState<VoiceDirectiveLog[]>([]);
  const [lastExecutedCommand, setLastExecutedCommand] = useState<VoiceDirectiveLog | null>(null);
  const [manualVoiceInput, setManualVoiceInput] = useState<string>('');
  const [voiceFeedbackSpeech, setVoiceFeedbackSpeech] = useState<boolean>(true);

  // General transcription action points
  const [actionPoints, setActionPoints] = useState<string[]>([]);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);

  const recognitionRef = useRef<any>(null);
  const timerRef = useRef<any>(null);
  const recentCommandsRef = useRef<Set<string>>(new Set());

  // Check Web Speech API availability on mount
  useEffect(() => {
    const SpeechRecognition = 
      (window as any).SpeechRecognition || 
      (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setIsSpeechSupported(false);
    }
  }, []);

  // Cleanup timer & recognition on unmount
  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
      if (recognitionRef.current) {
        try {
          recognitionRef.current.stop();
        } catch {
          // ignore
        }
      }
    };
  }, []);

  // Natural Language Voice Command Parser
  const parseAndExecuteVoiceCommand = (spokenPhrase: string): VoiceDirectiveLog | null => {
    const trimmed = spokenPhrase.trim();
    if (!trimmed) return null;

    const lower = trimmed.toLowerCase();

    // Prevent duplicate rapid firing of the exact same command string within a few seconds
    if (recentCommandsRef.current.has(lower)) {
      return null;
    }

    // Command matching patterns (e.g., 'Agent, initiate backup', 'Sentinel, run security audit')
    // Check if the phrase has directive intent
    let extractedTask = '';
    let matchedAgentKey = '';

    // Pattern 1: Starts with 'agent,' or 'agent ' or 'hey agent'
    const agentMatch = lower.match(/^(?:hey\s+)?agent(?:,|:)?\s+(.+)$/i);
    // Pattern 2: Starts with specific agent name/code
    const sentinelMatch = lower.match(/^(?:sentinel|compliance|security)(?:,|:)?\s+(.+)$/i);
    const workflowMatch = lower.match(/^(?:workflow|aegis|pipeline)(?:,|:)?\s+(.+)$/i);
    const pulseMatch = lower.match(/^(?:voip|phone|telephony|pulse)(?:,|:)?\s+(.+)$/i);
    const cognitoMatch = lower.match(/^(?:intelligence|cognito|analyst)(?:,|:)?\s+(.+)$/i);
    const orchestratorMatch = lower.match(/^(?:orchestrator|master|rcos)(?:,|:)?\s+(.+)$/i);

    // Direct action verbs without explicit 'agent' prefix (e.g., 'initiate backup', 'run compliance audit')
    const directActionMatch = lower.match(/^(?:please\s+)?(initiate\s+backup|run\s+(?:security\s+)?audit|trigger\s+(?:workflow|pipeline)|sync\s+agents?|generate\s+report|check\s+fleet\s+health)(.*)$/i);

    if (agentMatch) {
      extractedTask = agentMatch[1];
    } else if (sentinelMatch) {
      extractedTask = sentinelMatch[1];
      matchedAgentKey = 'compliance';
    } else if (workflowMatch) {
      extractedTask = workflowMatch[1];
      matchedAgentKey = 'workflow';
    } else if (pulseMatch) {
      extractedTask = pulseMatch[1];
      matchedAgentKey = 'voip';
    } else if (cognitoMatch) {
      extractedTask = cognitoMatch[1];
      matchedAgentKey = 'intelligence';
    } else if (orchestratorMatch) {
      extractedTask = orchestratorMatch[1];
      matchedAgentKey = 'master';
    } else if (directActionMatch) {
      extractedTask = trimmed;
    } else {
      // If it contains "initiate backup" anywhere in the phrase
      if (lower.includes('initiate backup') || lower.includes('start backup') || lower.includes('run backup')) {
        extractedTask = 'Initiate system backup and encrypted snapshot';
        matchedAgentKey = 'compliance';
      } else {
        return null;
      }
    }

    // Clean up extracted task string
    extractedTask = extractedTask
      .replace(/^[,\s.:]+|[,\s.:]+$/g, '')
      .trim();

    // Capitalize first letter
    if (extractedTask.length > 0) {
      extractedTask = extractedTask.charAt(0).toUpperCase() + extractedTask.slice(1);
    }

    if (!extractedTask) return null;

    // Resolve target agent from available agents
    let targetAgent: Agent | undefined;

    // First check keyword inferred agent
    if (matchedAgentKey === 'compliance' || extractedTask.toLowerCase().includes('backup') || extractedTask.toLowerCase().includes('audit') || extractedTask.toLowerCase().includes('compliance') || extractedTask.toLowerCase().includes('security')) {
      targetAgent = agents.find(a => a.name.toLowerCase().includes('compliance') || a.codeName.includes('SENTINEL'));
    } else if (matchedAgentKey === 'workflow' || extractedTask.toLowerCase().includes('workflow') || extractedTask.toLowerCase().includes('pipeline') || extractedTask.toLowerCase().includes('dispatch')) {
      targetAgent = agents.find(a => a.name.toLowerCase().includes('workflow') || a.codeName.includes('AEGIS'));
    } else if (matchedAgentKey === 'voip' || extractedTask.toLowerCase().includes('voip') || extractedTask.toLowerCase().includes('call') || extractedTask.toLowerCase().includes('phone')) {
      targetAgent = agents.find(a => a.name.toLowerCase().includes('voip') || a.codeName.includes('PULSE'));
    } else if (matchedAgentKey === 'intelligence' || extractedTask.toLowerCase().includes('intelligence') || extractedTask.toLowerCase().includes('report') || extractedTask.toLowerCase().includes('brief')) {
      targetAgent = agents.find(a => a.name.toLowerCase().includes('intelligence') || a.codeName.includes('COGNITO'));
    } else if (matchedAgentKey === 'master' || extractedTask.toLowerCase().includes('orchestrat') || extractedTask.toLowerCase().includes('fleet') || extractedTask.toLowerCase().includes('sync')) {
      targetAgent = agents.find(a => a.name.toLowerCase().includes('orchestrator') || a.codeName.includes('MASTER'));
    }

    // Default fallback agent
    if (!targetAgent) {
      targetAgent = agents.find(a => a.name.toLowerCase().includes('orchestrator')) || agents[0];
    }

    const assignedAgentId = targetAgent ? targetAgent.id : 'ag-01';
    const assignedAgentName = targetAgent ? targetAgent.name : 'Compliance & Security Audit';

    const logEntry: VoiceDirectiveLog = {
      id: `voice-cmd-${Date.now()}`,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      rawVoiceInput: spokenPhrase,
      extractedTask,
      targetAgentId: assignedAgentId,
      targetAgentName: assignedAgentName,
      status: 'Executing'
    };

    // Mark as recently executed to avoid rapid re-trigger loop
    recentCommandsRef.current.add(lower);
    setTimeout(() => {
      recentCommandsRef.current.delete(lower);
    }, 4000);

    // Dispatch directly to the agent system!
    if (onDirectAgentTask) {
      onDirectAgentTask(assignedAgentId, extractedTask);
    }

    // Update UI states
    setVoiceLogs(prev => [logEntry, ...prev]);
    setLastExecutedCommand(logEntry);

    // Synthesize audio confirmation if supported
    if (voiceFeedbackSpeech && 'speechSynthesis' in window) {
      try {
        const speech = new SpeechSynthesisUtterance(
          `Command recognized: ${extractedTask}. Dispatched to ${assignedAgentName}.`
        );
        speech.rate = 1.05;
        speech.pitch = 1.0;
        window.speechSynthesis.speak(speech);
      } catch {
        // silent fail if audio output is blocked
      }
    }

    return logEntry;
  };

  // Toggle Live Web Speech API Recording
  const startSpeechRecognition = () => {
    setSpeechError(null);
    setInterimTranscript('');

    const SpeechRecognition = 
      (window as any).SpeechRecognition || 
      (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setIsSpeechSupported(false);
      setSpeechError("Web Speech API is not natively supported in this browser. You can use the quick commands below to test voice directives.");
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = 'en-US';
      recognition.maxAlternatives = 1;

      recognition.onstart = () => {
        setIsListening(true);
        setRecordSeconds(0);
        if (timerRef.current) clearInterval(timerRef.current);
        timerRef.current = setInterval(() => {
          setRecordSeconds(prev => prev + 1);
        }, 1000);
      };

      recognition.onresult = (event: any) => {
        let currentInterim = '';
        let currentFinal = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          const transcriptChunk = event.results[i][0].transcript;
          if (event.results[i].isFinal) {
            currentFinal += transcriptChunk + ' ';
            // Evaluate voice command immediately upon finalized chunk!
            parseAndExecuteVoiceCommand(transcriptChunk);
          } else {
            currentInterim += transcriptChunk;
            // Also check interim results for immediate responsive command recognition
            if (
              currentInterim.toLowerCase().includes('agent, initiate backup') ||
              currentInterim.toLowerCase().includes('agent initiate backup')
            ) {
              parseAndExecuteVoiceCommand(currentInterim);
            }
          }
        }

        if (currentFinal) {
          setFinalTranscript(prev => (prev ? `${prev} ${currentFinal.trim()}` : currentFinal.trim()));
        }
        setInterimTranscript(currentInterim);
      };

      recognition.onerror = (event: any) => {
        console.warn('Speech recognition error:', event.error);
        if (event.error === 'not-allowed') {
          setSpeechError('Microphone permission was denied. Please allow microphone access in your browser settings.');
        } else if (event.error === 'no-speech') {
          // Normal timeout when quiet, keep continuous recognition alive
        } else {
          setSpeechError(`Speech recognition notice: ${event.error}.`);
        }
      };

      recognition.onend = () => {
        // Only stop if user toggled off
        setIsListening(false);
        if (timerRef.current) clearInterval(timerRef.current);
      };

      recognitionRef.current = recognition;
      recognition.start();
    } catch (err: any) {
      console.error('Failed to start speech recognition:', err);
      setSpeechError(err?.message || 'Could not start speech recognition.');
      setIsListening(false);
    }
  };

  const stopSpeechRecognition = () => {
    if (timerRef.current) clearInterval(timerRef.current);
    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop();
      } catch {
        // ignore
      }
    }
    setIsListening(false);

    // Process general action points from full final transcription
    if (finalTranscript.trim() || interimTranscript.trim()) {
      const fullText = `${finalTranscript} ${interimTranscript}`.trim();
      setIsProcessing(true);
      setTimeout(() => {
        setIsProcessing(false);
        const extracted: string[] = [];
        if (fullText.toLowerCase().includes('backup')) {
          extracted.push('Initiate automated system backup and cryptographic snapshot');
        }
        if (fullText.toLowerCase().includes('audit') || fullText.toLowerCase().includes('compliance')) {
          extracted.push('Execute Sentinel compliance scan on current active workflows');
        }
        if (fullText.toLowerCase().includes('pipeline') || fullText.toLowerCase().includes('workflow')) {
          extracted.push('Trigger Aegis automated pipeline dispatch and evaluation');
        }
        if (extracted.length === 0) {
          extracted.push(`Action item synthesized: ${fullText.slice(0, 70)}...`);
        }
        setActionPoints(extracted);
      }, 600);
    }
  };

  // Sample prompt buttons
  const sampleVoicePrompts = [
    {
      phrase: 'Agent, initiate backup',
      category: 'Security & Snapshot',
      target: 'Compliance & Security Audit'
    },
    {
      phrase: 'Sentinel, run security and compliance audit',
      category: 'Governance Scan',
      target: 'Compliance & Security Audit'
    },
    {
      phrase: 'Workflow, trigger automated pipeline dispatch',
      category: 'Operations',
      target: 'AI Workflow Engine'
    },
    {
      phrase: 'Orchestrator, execute fleet health telemetry check',
      category: 'Master System',
      target: 'Master Orchestrator'
    },
    {
      phrase: 'Intelligence, generate Q4 strategic brief',
      category: 'Strategic Reasoner',
      target: 'Enterprise Intelligence'
    }
  ];

  const handleTestVoiceCommand = (phrase: string) => {
    setFinalTranscript(prev => (prev ? `${prev} "${phrase}"` : `"${phrase}"`));
    parseAndExecuteVoiceCommand(phrase);
  };

  const handleManualCommandSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!manualVoiceInput.trim()) return;
    handleTestVoiceCommand(manualVoiceInput.trim());
    setManualVoiceInput('');
  };

  return (
    <div id="voice-transcriber-container" className="space-y-5 max-w-4xl mx-auto pb-20 animate-in fade-in duration-150">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-2 border-b border-slate-800/80">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <Mic className="w-5 h-5 text-[#76d418]" />
            <span>Voice Command Transcriber & Agent Dispatcher</span>
          </h1>
          <p className="text-xs text-slate-400">
            Powered by Web Speech API: trigger autonomous agent tasks in real time with spoken voice commands
          </p>
        </div>

        {/* Status badges */}
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#0a150c] border border-[#76d418]/40 text-xs font-semibold text-[#76d418]">
            <Radio className={`w-3.5 h-3.5 ${isListening ? 'animate-pulse text-[#76d418]' : 'text-slate-400'}`} />
            <span>{isListening ? 'Live Microphone Stream' : isSpeechSupported ? 'Web Speech API Ready' : 'Voice Command Center'}</span>
          </div>

          <button
            onClick={() => setVoiceFeedbackSpeech(!voiceFeedbackSpeech)}
            title={voiceFeedbackSpeech ? 'Voice synthesis reply enabled' : 'Voice synthesis reply disabled'}
            className={`p-1.5 rounded-lg border text-xs flex items-center gap-1 transition-colors cursor-pointer ${
              voiceFeedbackSpeech 
                ? 'bg-[#76d418]/15 border-[#76d418]/40 text-[#76d418]' 
                : 'bg-slate-900 border-slate-800 text-slate-400'
            }`}
          >
            <Volume2 className="w-3.5 h-3.5" />
            <span className="hidden sm:inline text-[11px]">{voiceFeedbackSpeech ? 'TTS Reply On' : 'Muted'}</span>
          </button>
        </div>
      </div>

      {/* Voice Mode Selector Tabs */}
      <div className="flex items-center gap-2 p-1 rounded-2xl bg-slate-950/80 border border-slate-800/80 w-fit">
        <button
          type="button"
          id="tab-voice-live-api"
          onClick={() => setActiveVoiceTab('live')}
          className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition-all cursor-pointer ${
            activeVoiceTab === 'live'
              ? 'bg-[#76d418] text-slate-950 shadow-md shadow-[#76d418]/20'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <Radio className="w-4 h-4" />
          <span>Gemini 3.8 Live Voice Conversation</span>
        </button>

        <button
          type="button"
          id="tab-voice-commands"
          onClick={() => setActiveVoiceTab('commands')}
          className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition-all cursor-pointer ${
            activeVoiceTab === 'commands'
              ? 'bg-[#76d418] text-slate-950 shadow-md shadow-[#76d418]/20'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <Command className="w-4 h-4" />
          <span>Directive Dispatcher & Transcriber</span>
        </button>
      </div>

      {activeVoiceTab === 'live' ? (
        <GeminiLiveVoiceView 
          onDirectTask={(task) => {
            if (agents.length > 0 && onDirectAgentTask) {
              onDirectAgentTask(agents[0].id, task);
            }
          }}
        />
      ) : (
        <>
          {/* Active Trigger Announcement Banner */}
      {lastExecutedCommand && (
        <div 
          id="voice-command-alert-banner"
          className="p-3.5 rounded-2xl bg-[#0d1c10] border-2 border-[#76d418] shadow-lg shadow-[#76d418]/15 flex items-center justify-between gap-3 animate-in fade-in slide-in-from-top-2 duration-200"
        >
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-9 h-9 rounded-xl bg-[#76d418] text-slate-950 flex items-center justify-center flex-shrink-0 font-bold shadow-md">
              <Zap className="w-5 h-5 fill-current" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-xs font-black uppercase tracking-wider text-[#76d418]">
                  Voice Command Executed
                </span>
                <span className="text-[10px] text-slate-400">
                  {lastExecutedCommand.timestamp}
                </span>
              </div>
              <p className="text-sm font-bold text-white truncate">
                &ldquo;{lastExecutedCommand.extractedTask}&rdquo;
              </p>
              <p className="text-[11px] text-slate-300 flex items-center gap-1.5 mt-0.5">
                <Bot className="w-3 h-3 text-[#76d418]" />
                <span>Assigned to: <strong className="text-white">{lastExecutedCommand.targetAgentName}</strong></span>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 flex-shrink-0">
            {onNavigate && (
              <button
                id="btn-banner-view-jobs"
                onClick={() => onNavigate('jobs')}
                className="px-3 py-1.5 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 font-bold text-xs flex items-center gap-1 transition-all shadow cursor-pointer"
              >
                <span>View in Jobs</span>
                <ArrowRight className="w-3 h-3" />
              </button>
            )}
          </div>
        </div>
      )}

      {/* Microphone Error Notice if needed */}
      {speechError && (
        <div className="p-3 rounded-xl bg-rose-950/40 border border-rose-800 text-rose-300 text-xs flex items-start gap-2.5">
          <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
          <div className="flex-1">
            <span className="font-semibold">{speechError}</span>
            <p className="text-rose-400/80 text-[11px] mt-0.5">
              You can still trigger agent tasks using the voice prompt chips below or direct text input.
            </p>
          </div>
        </div>
      )}

      {/* Main Console: Live Mic Recorder + Transcription Stream */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* Left Column: Live Audio Capture Panel */}
        <div className="bg-[#091016] border border-slate-800 rounded-2xl p-5 flex flex-col items-center justify-between text-center space-y-4 shadow-xl">
          <div className="space-y-1 w-full">
            <div className="flex items-center justify-between text-[11px] font-bold text-slate-400 uppercase tracking-wider">
              <span>Speech Recognition</span>
              <span className={`px-2 py-0.5 rounded-full text-[10px] ${isListening ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30 animate-pulse' : 'bg-slate-800 text-slate-300'}`}>
                {isListening ? 'RECORDING' : 'IDLE'}
              </span>
            </div>
            
            <div className="text-3xl font-mono font-bold text-white pt-1">
              {String(Math.floor(recordSeconds / 60)).padStart(2, '0')}:
              {String(recordSeconds % 60).padStart(2, '0')}
            </div>
          </div>

          {/* Large Recording Pulse Button */}
          <div className="relative py-2">
            {/* Pulsing ring animation when active */}
            {isListening && (
              <div className="absolute inset-0 rounded-full bg-rose-600/30 animate-ping -m-2 pointer-events-none" />
            )}

            <button
              id="btn-voice-record-toggle"
              onClick={isListening ? stopSpeechRecognition : startSpeechRecognition}
              className={`w-24 h-24 rounded-full flex flex-col items-center justify-center transition-all shadow-2xl cursor-pointer relative z-10 ${
                isListening
                  ? 'bg-rose-600 hover:bg-rose-500 text-white ring-8 ring-rose-500/30'
                  : 'bg-[#76d418] hover:bg-[#66bd14] text-slate-950 ring-8 ring-[#76d418]/20 hover:scale-105'
              }`}
            >
              {isListening ? (
                <>
                  <Square className="w-8 h-8 fill-current" />
                  <span className="text-[10px] font-bold uppercase mt-1">Stop</span>
                </>
              ) : (
                <>
                  <Mic className="w-8 h-8 text-slate-950 stroke-[2.5]" />
                  <span className="text-[10px] font-black uppercase mt-1">Speak</span>
                </>
              )}
            </button>
          </div>

          {/* Audio Wave Visualizer Simulation when listening */}
          {isListening ? (
            <div className="w-full flex items-center justify-center gap-1 h-8 py-1">
              {[40, 70, 95, 60, 85, 100, 75, 45, 90, 65, 80, 50].map((h, i) => (
                <div
                  key={i}
                  className="w-1 bg-[#76d418] rounded-full transition-all duration-150 animate-pulse"
                  style={{
                    height: `${h}%`,
                    animationDelay: `${(i * 70) % 500}ms`
                  }}
                />
              ))}
            </div>
          ) : (
            <p className="text-xs text-slate-400 max-w-xs leading-relaxed">
              Say e.g. <strong className="text-white">&ldquo;Agent, initiate backup&rdquo;</strong> to trigger an autonomous task instantly.
            </p>
          )}

          {/* Direct Text-to-Voice Command Input */}
          <form onSubmit={handleManualCommandSubmit} className="w-full pt-3 border-t border-slate-800/80 space-y-2">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block text-left">
              Or Type Voice Directive:
            </span>
            <div className="flex items-center gap-1.5">
              <input
                id="input-manual-voice-command"
                type="text"
                value={manualVoiceInput}
                onChange={(e) => setManualVoiceInput(e.target.value)}
                placeholder="e.g. Agent, initiate backup"
                className="flex-1 bg-[#060a08] border border-slate-700/80 focus:border-[#76d418] rounded-xl px-3 py-2 text-xs text-white placeholder:text-slate-500 outline-none"
              />
              <button
                id="btn-dispatch-manual-voice-command"
                type="submit"
                disabled={!manualVoiceInput.trim()}
                className="p-2 rounded-xl bg-[#76d418] hover:bg-[#66bd14] disabled:opacity-40 text-slate-950 font-bold cursor-pointer transition-colors shadow-sm"
                title="Dispatch Directive"
              >
                <Send className="w-3.5 h-3.5" />
              </button>
            </div>
          </form>
        </div>

        {/* Right Column: Live Transcription Stream & Command Execution Feed */}
        <div className="lg:col-span-2 space-y-4">
          {/* Live Transcript Box */}
          <div className="bg-[#091016] border border-slate-800 rounded-2xl p-5 space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <FileText className="w-4 h-4 text-[#76d418]" />
                <h3 className="text-sm font-bold text-white">Live Audio Transcription Stream</h3>
              </div>

              <div className="flex items-center gap-2">
                {(finalTranscript || interimTranscript) && (
                  <button
                    onClick={() => {
                      setFinalTranscript('');
                      setInterimTranscript('');
                      setActionPoints([]);
                    }}
                    className="text-[11px] text-slate-400 hover:text-white cursor-pointer"
                  >
                    Clear Stream
                  </button>
                )}
                {isListening && (
                  <span className="text-xs text-[#76d418] flex items-center gap-1 animate-pulse">
                    <span className="w-2 h-2 rounded-full bg-[#76d418]" />
                    <span>Listening...</span>
                  </span>
                )}
              </div>
            </div>

            {/* Transcription Box with Real-time styling */}
            <div 
              id="voice-transcription-output-box"
              className="p-4 rounded-xl bg-[#060a08] border border-slate-800/80 min-h-[130px] max-h-[220px] overflow-y-auto text-xs text-slate-200 leading-relaxed font-mono"
            >
              {finalTranscript || interimTranscript ? (
                <div>
                  <span>{finalTranscript}</span>
                  {interimTranscript && (
                    <span className="text-[#76d418] italic font-semibold ml-1 bg-[#76d418]/10 px-1 rounded">
                      {interimTranscript}
                    </span>
                  )}
                </div>
              ) : (
                <div className="h-24 flex flex-col items-center justify-center text-center text-slate-500 font-sans space-y-1">
                  <Mic className="w-5 h-5 text-slate-600 mb-1" />
                  <p className="text-xs">Microphone transcript will appear here in real time as you speak.</p>
                  <p className="text-[11px] text-slate-600">Try saying: &ldquo;Agent, initiate backup&rdquo;</p>
                </div>
              )}
            </div>

            {/* Synthesized Action Items if present */}
            {actionPoints.length > 0 && (
              <div className="pt-2 border-t border-slate-800/80 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                    <span>Synthesized Action Items ({actionPoints.length})</span>
                  </span>

                  {onCreateJobFromVoice && (
                    <button
                      id="btn-convert-voice-to-job"
                      onClick={() => {
                        onCreateJobFromVoice(
                          'Voice Audio Intake: Action Items',
                          actionPoints.join('. ')
                        );
                      }}
                      className="text-xs font-bold text-[#76d418] hover:text-[#88f020] flex items-center gap-1 cursor-pointer"
                    >
                      <span>Convert to Job Ticket</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>

                <div className="space-y-1.5">
                  {actionPoints.map((item, idx) => (
                    <div key={idx} className="flex items-start gap-2 p-2.5 rounded-lg bg-[#0c141a] border border-slate-800 text-xs text-slate-300">
                      <CheckCircle2 className="w-4 h-4 text-[#76d418] shrink-0 mt-0.5" />
                      <span>{item}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Quick Voice Command Chips (Click to test / execute) */}
          <div className="bg-[#091016] border border-slate-800 rounded-2xl p-4 space-y-2.5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-1.5">
                <Command className="w-3.5 h-3.5 text-[#76d418]" />
                <span>Test Direct Voice Commands</span>
              </span>
              <span className="text-[11px] text-slate-500">Tap any phrase to dispatch</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {sampleVoicePrompts.map((item, idx) => (
                <button
                  key={idx}
                  id={`btn-sample-voice-prompt-${idx}`}
                  onClick={() => handleTestVoiceCommand(item.phrase)}
                  className="p-2.5 rounded-xl bg-[#060a08] border border-slate-800 hover:border-[#76d418]/60 hover:bg-[#0c1620] text-left transition-all cursor-pointer group flex items-center justify-between"
                >
                  <div className="min-w-0 pr-2">
                    <div className="text-xs font-bold text-white group-hover:text-[#76d418] transition-colors truncate">
                      &ldquo;{item.phrase}&rdquo;
                    </div>
                    <div className="text-[10px] text-slate-400 flex items-center gap-1 mt-0.5 truncate">
                      <span>→ {item.target}</span>
                    </div>
                  </div>
                  <div className="w-6 h-6 rounded-lg bg-slate-900 group-hover:bg-[#76d418] group-hover:text-slate-950 text-[#76d418] flex items-center justify-center flex-shrink-0 transition-colors">
                    <Zap className="w-3.5 h-3.5" />
                  </div>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Dispatched Voice Directives History */}
      <div className="bg-[#091016] border border-slate-800 rounded-2xl p-5 space-y-3">
        <div className="flex items-center justify-between pb-2 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <History className="w-4 h-4 text-[#76d418]" />
            <h3 className="text-sm font-bold text-white">Dispatched Voice Directives Telemetry</h3>
          </div>
          <span className="text-xs font-mono text-slate-400">
            {voiceLogs.length} Directives Fired
          </span>
        </div>

        {voiceLogs.length === 0 ? (
          <div className="py-6 text-center text-xs text-slate-500 space-y-1">
            <Bot className="w-6 h-6 text-slate-600 mx-auto mb-1" />
            <p>No voice commands executed yet during this session.</p>
            <p className="text-[11px] text-slate-600">
              Speak into the microphone or tap any quick prompt to issue autonomous commands.
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {voiceLogs.map((log) => (
              <div 
                key={log.id} 
                id={`voice-directive-log-${log.id}`}
                className="p-3 rounded-xl bg-[#060a08] border border-slate-800 flex items-center justify-between gap-3 text-xs"
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <div className="w-7 h-7 rounded-lg bg-[#76d418]/15 border border-[#76d418]/30 flex items-center justify-center text-[#76d418] shrink-0 font-bold text-[10px]">
                    <Zap className="w-3.5 h-3.5" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-white truncate">{log.extractedTask}</span>
                      <span className="text-[10px] text-slate-500 font-mono">{log.timestamp}</span>
                    </div>
                    <div className="text-[11px] text-slate-400 flex items-center gap-1.5 mt-0.5 truncate">
                      <span>Heard: &ldquo;{log.rawVoiceInput}&rdquo;</span>
                      <span>•</span>
                      <span className="text-[#76d418] font-medium">Assigned: {log.targetAgentName}</span>
                    </div>
                  </div>
                </div>

                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#76d418]/15 text-[#76d418] border border-[#76d418]/30 shrink-0">
                  {log.status}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
        </>
      )}
    </div>
  );
};
