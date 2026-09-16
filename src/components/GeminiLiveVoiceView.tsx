import React, { useState, useEffect, useRef } from 'react';
import { 
  Radio, 
  Mic, 
  MicOff, 
  Volume2, 
  Sparkles, 
  AlertCircle, 
  CheckCircle2, 
  PhoneCall, 
  PhoneOff, 
  Activity,
  Cpu,
  Layers,
  MessageSquare
} from 'lucide-react';

interface GeminiLiveVoiceViewProps {
  onDirectTask?: (task: string) => void;
}

export const GeminiLiveVoiceView: React.FC<GeminiLiveVoiceViewProps> = ({ onDirectTask }) => {
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [isConnecting, setIsConnecting] = useState<boolean>(false);
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [isModelSpeaking, setIsModelSpeaking] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [sessionSeconds, setSessionSeconds] = useState<number>(0);
  const [audioLevel, setAudioLevel] = useState<number>(0);

  const [conversationSnippets, setConversationSnippets] = useState<Array<{ sender: 'user' | 'model'; text: string; time: string }>>([
    {
      sender: 'model',
      text: 'Gemini 3.8 Live API voice channel initialized. Press "Start Live Conversation" to talk in real-time.',
      time: 'Ready'
    }
  ]);

  // Audio and WebSocket references
  const wsRef = useRef<WebSocket | null>(null);
  const inputAudioCtxRef = useRef<AudioContext | null>(null);
  const outputAudioCtxRef = useRef<AudioContext | null>(null);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const processorRef = useRef<ScriptProcessorNode | null>(null);
  const nextStartTimeRef = useRef<number>(0);
  const activeSourcesRef = useRef<AudioBufferSourceNode[]>([]);
  const timerRef = useRef<any>(null);
  const isMutedRef = useRef<boolean>(false);

  // Sync ref with state
  useEffect(() => {
    isMutedRef.current = isMuted;
  }, [isMuted]);

  // Session timer
  useEffect(() => {
    if (isConnected) {
      setSessionSeconds(0);
      timerRef.current = setInterval(() => {
        setSessionSeconds(s => s + 1);
      }, 1000);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isConnected]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopLiveSession();
    };
  }, []);

  const playPcmChunk = (base64Audio: string) => {
    try {
      if (!outputAudioCtxRef.current) {
        outputAudioCtxRef.current = new (window.AudioContext || (window as any).webkitAudioContext)({
          sampleRate: 24000
        });
      }
      const audioCtx = outputAudioCtxRef.current;
      if (audioCtx.state === 'suspended') {
        audioCtx.resume();
      }

      const binary = atob(base64Audio);
      const len = binary.length;
      const bytes = new Uint8Array(len);
      for (let i = 0; i < len; i++) {
        bytes[i] = binary.charCodeAt(i);
      }

      const int16 = new Int16Array(bytes.buffer);
      const float32 = new Float32Array(int16.length);
      for (let i = 0; i < int16.length; i++) {
        float32[i] = int16[i] / 32768.0;
      }

      const buffer = audioCtx.createBuffer(1, float32.length, 24000);
      buffer.copyToChannel(float32, 0);

      const source = audioCtx.createBufferSource();
      source.buffer = buffer;
      source.connect(audioCtx.destination);

      const now = audioCtx.currentTime;
      const startTime = Math.max(now, nextStartTimeRef.current);
      source.start(startTime);
      nextStartTimeRef.current = startTime + buffer.duration;

      activeSourcesRef.current.push(source);
      setIsModelSpeaking(true);

      source.onended = () => {
        activeSourcesRef.current = activeSourcesRef.current.filter(s => s !== source);
        if (activeSourcesRef.current.length === 0) {
          setIsModelSpeaking(false);
        }
      };
    } catch (err) {
      console.error('Error playing 24kHz audio chunk:', err);
    }
  };

  const handleInterrupted = () => {
    activeSourcesRef.current.forEach(source => {
      try {
        source.stop();
      } catch {
        // ignore
      }
    });
    activeSourcesRef.current = [];
    if (outputAudioCtxRef.current) {
      nextStartTimeRef.current = outputAudioCtxRef.current.currentTime;
    }
    setIsModelSpeaking(false);
  };

  const startLiveSession = async () => {
    setErrorMessage(null);
    setIsConnecting(true);

    try {
      // 1. Obtain Microphone Access
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
          channelCount: 1,
          sampleRate: 16000
        }
      });
      mediaStreamRef.current = stream;

      // 2. Setup Input AudioContext (16kHz for gemini-3.8-live)
      const inputCtx = new (window.AudioContext || (window as any).webkitAudioContext)({
        sampleRate: 16000
      });
      inputAudioCtxRef.current = inputCtx;
      if (inputCtx.state === 'suspended') {
        await inputCtx.resume();
      }

      // 3. Setup Output AudioContext (24kHz for Live playback)
      const outputCtx = new (window.AudioContext || (window as any).webkitAudioContext)({
        sampleRate: 24000
      });
      outputAudioCtxRef.current = outputCtx;
      nextStartTimeRef.current = outputCtx.currentTime;

      // 4. Connect WebSocket to Server Bridge
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsUrl = `${protocol}//${window.location.host}/api/live`;
      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        setIsConnected(true);
        setIsConnecting(false);
        setConversationSnippets(prev => [
          ...prev,
          {
            sender: 'model',
            text: 'Connected to Gemini 3.8 Live API. You can speak now.',
            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          }
        ]);

        // Connect mic audio pipeline
        const source = inputCtx.createMediaStreamSource(stream);
        const processor = inputCtx.createScriptProcessor(4096, 1, 1);
        processorRef.current = processor;

        source.connect(processor);
        processor.connect(inputCtx.destination);

        processor.onaudioprocess = (e) => {
          if (isMutedRef.current) return;
          if (ws.readyState !== WebSocket.OPEN) return;

          const channel = e.inputBuffer.getChannelData(0);

          // Compute volume meter level
          let sum = 0;
          for (let i = 0; i < channel.length; i++) {
            sum += channel[i] * channel[i];
          }
          const rms = Math.sqrt(sum / channel.length);
          setAudioLevel(Math.min(100, Math.round(rms * 250)));

          // Convert Float32 to 16-bit PCM little-endian
          const buffer = new ArrayBuffer(channel.length * 2);
          const view = new DataView(buffer);
          for (let i = 0; i < channel.length; i++) {
            const s = Math.max(-1, Math.min(1, channel[i]));
            view.setInt16(i * 2, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
          }

          const bytes = new Uint8Array(buffer);
          let binary = '';
          for (let i = 0; i < bytes.byteLength; i++) {
            binary += String.fromCharCode(bytes[i]);
          }

          ws.send(JSON.stringify({
            audio: btoa(binary)
          }));
        };
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          if (data.audio) {
            playPcmChunk(data.audio);
          }
          if (data.interrupted) {
            handleInterrupted();
          }
          if (data.error) {
            setErrorMessage(data.error);
          }
        } catch (err) {
          console.error('Error handling WebSocket message:', err);
        }
      };

      ws.onerror = () => {
        setErrorMessage('Failed to connect to Live API over WebSocket. Please verify the server connection.');
        setIsConnecting(false);
      };

      ws.onclose = () => {
        stopLiveSession();
      };

    } catch (err: any) {
      console.error('Failed to start Live session:', err);
      setErrorMessage(err.message || 'Microphone access denied or connection failed.');
      setIsConnecting(false);
      stopLiveSession();
    }
  };

  const stopLiveSession = () => {
    setIsConnected(false);
    setIsConnecting(false);
    setIsModelSpeaking(false);
    setAudioLevel(0);

    // Stop and cleanup active audio
    activeSourcesRef.current.forEach(s => {
      try { s.stop(); } catch {}
    });
    activeSourcesRef.current = [];

    // Stop mic stream
    if (mediaStreamRef.current) {
      mediaStreamRef.current.getTracks().forEach(t => t.stop());
      mediaStreamRef.current = null;
    }

    // Disconnect processor
    if (processorRef.current) {
      processorRef.current.disconnect();
      processorRef.current = null;
    }

    // Close AudioContexts
    if (inputAudioCtxRef.current) {
      try { inputAudioCtxRef.current.close(); } catch {}
      inputAudioCtxRef.current = null;
    }
    if (outputAudioCtxRef.current) {
      try { outputAudioCtxRef.current.close(); } catch {}
      outputAudioCtxRef.current = null;
    }

    // Close WebSocket
    if (wsRef.current) {
      try { wsRef.current.close(); } catch {}
      wsRef.current = null;
    }
  };

  return (
    <div id="gemini-live-voice-view" className="space-y-4 max-w-4xl mx-auto">
      {/* Top Banner */}
      <div className="bg-[#070e0a] border border-[#76d418]/30 rounded-3xl p-5 shadow-xl shadow-black/50">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className={`w-12 h-12 rounded-2xl flex items-center justify-center border transition-all ${
              isConnected 
                ? 'bg-[#76d418]/20 border-[#76d418] text-[#76d418] shadow-lg shadow-[#76d418]/30 animate-pulse' 
                : 'bg-slate-900 border-slate-800 text-slate-400'
            }`}>
              <Radio className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-white tracking-tight">
                  Gemini 3.8 Live API Voice Engine
                </h2>
                <span className="px-2 py-0.5 rounded-full bg-[#76d418]/15 border border-[#76d418]/40 text-[#76d418] text-[10px] font-bold">
                  gemini-3.8-live
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Low-latency, real-time bidirectional audio streaming directly with Gemini Live
              </p>
            </div>
          </div>

          {/* Connection Status Badge */}
          <div className="flex items-center gap-2">
            <div className={`flex items-center gap-2 px-3 py-1.5 rounded-xl border text-xs font-semibold ${
              isConnected 
                ? 'bg-[#0f2413] border-[#76d418]/50 text-[#76d418]' 
                : 'bg-slate-900 border-slate-800 text-slate-400'
            }`}>
              <div className={`w-2 h-2 rounded-full ${
                isConnected ? 'bg-[#76d418] animate-ping' : 'bg-slate-500'
              }`} />
              <span>{isConnected ? `Live (${String(Math.floor(sessionSeconds / 60)).padStart(2, '0')}:${String(sessionSeconds % 60).padStart(2, '0')})` : 'Standby'}</span>
            </div>
          </div>
        </div>

        {/* Error Notification */}
        {errorMessage && (
          <div className="mt-4 p-3 rounded-xl bg-rose-950/40 border border-rose-800/60 text-xs text-rose-300 flex items-start gap-2.5 animate-in fade-in">
            <AlertCircle className="w-4 h-4 shrink-0 text-rose-400 mt-0.5" />
            <div className="flex-1 leading-relaxed">
              <strong>Notice:</strong> {errorMessage}
            </div>
          </div>
        )}

        {/* Central Visualizer & Large Call Button */}
        <div className="mt-8 flex flex-col items-center justify-center py-6 border-y border-slate-800/80 space-y-6">
          {/* Real-Time Waveform Display */}
          <div className="w-full max-w-md h-16 flex items-center justify-center gap-1.5 px-4">
            {[20, 45, 75, 90, 60, 100, 85, 50, 70, 95, 40, 65, 80, 55, 30].map((h, i) => {
              const dynamicHeight = isConnected
                ? Math.max(15, Math.min(100, Math.round((h * (audioLevel + 20)) / 100)))
                : 12;
              return (
                <div
                  key={i}
                  className={`w-1.5 rounded-full transition-all duration-75 ${
                    isModelSpeaking 
                      ? 'bg-amber-400' 
                      : isConnected 
                        ? 'bg-[#76d418]' 
                        : 'bg-slate-800'
                  }`}
                  style={{
                    height: `${dynamicHeight}%`
                  }}
                />
              );
            })}
          </div>

          {/* Model Status Indicator */}
          <div className="text-xs font-semibold flex items-center gap-2">
            {isModelSpeaking ? (
              <span className="text-amber-400 flex items-center gap-1.5 animate-pulse">
                <Volume2 className="w-4 h-4" />
                Gemini is speaking (24kHz audio)... You can speak to interrupt.
              </span>
            ) : isConnected ? (
              <span className="text-[#76d418] flex items-center gap-1.5">
                <Mic className="w-4 h-4 text-[#76d418]" />
                Listening to your microphone (16kHz PCM)... Speak anytime.
              </span>
            ) : (
              <span className="text-slate-400">
                Click below to start low-latency voice conversation with Gemini 3.8 Live.
              </span>
            )}
          </div>

          {/* Main Action Call Buttons */}
          <div className="flex items-center gap-4">
            {!isConnected ? (
              <button
                id="btn-start-gemini-live-session"
                onClick={startLiveSession}
                disabled={isConnecting}
                className="px-8 py-4 rounded-2xl bg-[#76d418] hover:bg-[#66bd14] disabled:opacity-50 text-slate-950 font-black text-sm tracking-wide flex items-center gap-2.5 transition-all shadow-xl shadow-[#76d418]/25 hover:scale-105 cursor-pointer"
              >
                <PhoneCall className="w-5 h-5 fill-current" />
                <span>{isConnecting ? 'Establishing Live Session...' : 'Start Live Voice Session'}</span>
              </button>
            ) : (
              <div className="flex items-center gap-3">
                <button
                  id="btn-toggle-live-mute"
                  onClick={() => setIsMuted(!isMuted)}
                  className={`p-3.5 rounded-2xl border transition-all cursor-pointer ${
                    isMuted
                      ? 'bg-rose-950/60 border-rose-800 text-rose-300'
                      : 'bg-slate-900 border-slate-700 text-slate-200 hover:text-white'
                  }`}
                  title={isMuted ? 'Unmute microphone' : 'Mute microphone'}
                >
                  {isMuted ? <MicOff className="w-5 h-5 text-rose-400" /> : <Mic className="w-5 h-5 text-[#76d418]" />}
                </button>

                <button
                  id="btn-end-gemini-live-session"
                  onClick={stopLiveSession}
                  className="px-6 py-3.5 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs flex items-center gap-2 transition-all shadow-lg shadow-rose-600/30 cursor-pointer"
                >
                  <PhoneOff className="w-4 h-4 fill-current" />
                  <span>End Session</span>
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Live Audio Telemetry Details */}
        <div className="mt-4 grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80">
            <div className="text-[10px] font-bold text-slate-400 uppercase">Input Audio Spec</div>
            <div className="text-xs font-semibold text-[#76d418] mt-0.5">16kHz 16-bit Little-Endian PCM</div>
          </div>
          <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80">
            <div className="text-[10px] font-bold text-slate-400 uppercase">Output Audio Spec</div>
            <div className="text-xs font-semibold text-amber-400 mt-0.5">24kHz Scheduled Playback</div>
          </div>
          <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80">
            <div className="text-[10px] font-bold text-slate-400 uppercase">Interactive Interruption</div>
            <div className="text-xs font-semibold text-white mt-0.5">Zero-Latency Barge-In Enabled</div>
          </div>
        </div>
      </div>

      {/* Live Transcript / Activity Thread */}
      <div className="bg-[#070e0a] border border-slate-800/80 rounded-3xl p-5 space-y-3 shadow-lg">
        <div className="flex items-center justify-between pb-2 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <MessageSquare className="w-4 h-4 text-[#76d418]" />
            <h3 className="text-xs font-bold text-white uppercase tracking-wider">
              Live Session Activity & Context
            </h3>
          </div>
          <span className="text-[10px] text-slate-500 font-mono">
            Model: gemini-3.8-live
          </span>
        </div>

        <div className="space-y-2.5 max-h-48 overflow-y-auto pr-1">
          {conversationSnippets.map((snip, i) => (
            <div key={i} className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 text-xs flex items-start justify-between gap-3">
              <div className="space-y-0.5 flex-1">
                <span className="text-[10px] font-bold text-[#76d418] uppercase">
                  {snip.sender === 'model' ? 'Gemini 3.8 Live Voice' : 'Executive User'}
                </span>
                <p className="text-slate-200 leading-relaxed">{snip.text}</p>
              </div>
              <span className="text-[10px] text-slate-500 shrink-0">{snip.time}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
