import React, { useState } from 'react';
import { 
  Phone, 
  PhoneCall, 
  PhoneOff, 
  Mic, 
  MicOff, 
  Volume2, 
  Play, 
  Pause, 
  Clock, 
  User, 
  Building, 
  CheckCircle2, 
  AlertCircle,
  Hash,
  Delete
} from 'lucide-react';
import { CallRecord } from '../types';

interface PhoneViewProps {
  calls: CallRecord[];
  onLogCall: (call: CallRecord) => void;
  orgName?: string;
}

export const PhoneView: React.FC<PhoneViewProps> = ({ calls, onLogCall, orgName }) => {
  const [dialedNumber, setDialedNumber] = useState('');
  const [activeCall, setActiveCall] = useState<CallRecord | null>(null);
  const [callDuration, setCallDuration] = useState(0);
  const [isMuted, setIsMuted] = useState(false);

  // Keypad click handler
  const handleKeypadPress = (val: string) => {
    if (dialedNumber.length < 16) {
      setDialedNumber(prev => prev + val);
    }
  };

  const handleBackspace = () => {
    setDialedNumber(prev => prev.slice(0, -1));
  };

  const handleStartCall = () => {
    if (!dialedNumber.trim()) return;

    const newCall: CallRecord = {
      id: `call-${Date.now()}`,
      callerName: 'Direct Outbound Telephony',
      company: orgName || 'Enterprise Workspace',
      phoneNumber: dialedNumber,
      timestamp: 'Just now',
      duration: '00:00',
      sentiment: 'Positive',
      summary: `Autonomous agent connected via SIP trunk to ${dialedNumber}. Real-time audio stream initialized.`,
      agentRoutedTo: 'VoIP Telephony Engine',
      status: 'In Progress'
    };

    setActiveCall(newCall);
    setCallDuration(0);

    // Increment duration timer
    const interval = setInterval(() => {
      setCallDuration(prev => prev + 1);
    }, 1000);

    (window as any).__rcosCallTimer = interval;
  };

  const handleEndCall = () => {
    if ((window as any).__rcosCallTimer) {
      clearInterval((window as any).__rcosCallTimer);
    }

    if (activeCall) {
      const minutes = Math.floor(callDuration / 60);
      const seconds = callDuration % 60;
      const formattedDuration = `${String(minutes).padStart(2, '0')}m ${String(seconds).padStart(2, '0')}s`;

      const completedCall: CallRecord = {
        ...activeCall,
        duration: formattedDuration,
        status: 'Completed',
        summary: `Call to ${dialedNumber} completed (${formattedDuration}). Real-time audio transcribed by VoIP Telephony Engine.`
      };

      onLogCall(completedCall);
    }

    setActiveCall(null);
    setCallDuration(0);
  };

  const formatTimer = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  return (
    <div id="phone-view-container" className="space-y-4 max-w-5xl mx-auto pb-20">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <PhoneCall className="w-5 h-5 text-[#76d418]" />
            <span>VoIP Telephony & Call System</span>
          </h1>
          <p className="text-xs text-slate-400">
            Autonomous SIP trunk answering, live speech-to-text, and outbound direct dialer
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#76d418]/15 border border-[#76d418]/40 text-[#76d418] text-xs font-semibold">
            <span className="w-2 h-2 rounded-full bg-[#76d418] animate-pulse" />
            <span>SIP Trunk Connected</span>
          </div>
        </div>
      </div>

      {/* Grid: Dialer Column + Stream Column */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Interactive Dialer (1 col) */}
        <div className="bg-[#091016] border border-slate-800 rounded-2xl p-5 flex flex-col items-center justify-between space-y-4 shadow-xl">
          <div className="w-full">
            <div className="text-xs font-semibold text-slate-400 mb-1.5 flex items-center justify-between">
              <span>Target Phone Line</span>
              <span className="text-[#76d418] font-mono text-[11px]">DID Active</span>
            </div>

            {/* Dialed Number Display */}
            <div className="w-full h-12 bg-[#060a08] border border-slate-700/80 rounded-xl px-4 flex items-center justify-between text-lg font-mono text-white tracking-widest overflow-hidden">
              <span className="truncate">{dialedNumber || 'Enter number...'}</span>
              {dialedNumber && (
                <button
                  onClick={() => setDialedNumber('')}
                  className="text-slate-500 hover:text-slate-300 text-xs font-sans ml-2"
                >
                  Clear
                </button>
              )}
            </div>
          </div>

          {/* Active Call UI or Keypad */}
          {activeCall ? (
            <div className="w-full py-6 flex flex-col items-center justify-center space-y-4 bg-slate-950/80 rounded-xl border border-[#76d418]/40 p-4 animate-in fade-in">
              <div className="w-16 h-16 rounded-full bg-[#76d418]/20 border border-[#76d418] flex items-center justify-center text-[#76d418] animate-pulse">
                <Volume2 className="w-8 h-8" />
              </div>

              <div className="text-center">
                <h4 className="text-sm font-bold text-white">{activeCall.phoneNumber}</h4>
                <div className="text-lg font-mono text-[#76d418] font-bold mt-1">
                  {formatTimer(callDuration)}
                </div>
                <span className="text-[11px] text-slate-400">Autonomous VoIP Telephony Engine Streaming</span>
              </div>

              {/* Call Controls */}
              <div className="flex items-center gap-3 pt-2">
                <button
                  onClick={() => setIsMuted(!isMuted)}
                  className={`p-3 rounded-full border transition-colors cursor-pointer ${
                    isMuted
                      ? 'bg-rose-500/20 border-rose-500 text-rose-400'
                      : 'bg-slate-800 border-slate-700 text-slate-300 hover:text-white'
                  }`}
                  title={isMuted ? 'Unmute' : 'Mute'}
                >
                  {isMuted ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
                </button>
                <button
                  onClick={handleEndCall}
                  className="px-6 py-3 rounded-full bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold flex items-center gap-2 transition-all cursor-pointer shadow-lg shadow-rose-900/40"
                >
                  <PhoneOff className="w-4 h-4" />
                  <span>End Call</span>
                </button>
              </div>
            </div>
          ) : (
            <>
              {/* Keypad Buttons */}
              <div className="grid grid-cols-3 gap-3 w-full max-w-[260px]">
                {['1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#'].map((digit) => (
                  <button
                    key={digit}
                    onClick={() => handleKeypadPress(digit)}
                    className="h-12 rounded-xl bg-[#060a08] border border-slate-800 hover:bg-slate-800 text-sm font-bold text-white transition-colors cursor-pointer flex flex-col items-center justify-center shadow-sm"
                  >
                    <span>{digit}</span>
                  </button>
                ))}
              </div>

              {/* Action Buttons */}
              <div className="flex items-center justify-between w-full max-w-[260px] pt-2">
                <button
                  onClick={handleBackspace}
                  disabled={!dialedNumber}
                  className="p-3 rounded-xl bg-slate-800 hover:bg-slate-700 disabled:opacity-30 text-slate-300 transition-colors cursor-pointer"
                  title="Backspace"
                >
                  <Delete className="w-4 h-4" />
                </button>

                <button
                  onClick={handleStartCall}
                  disabled={!dialedNumber.trim()}
                  className="px-6 py-3 rounded-xl bg-[#76d418] hover:bg-[#66bd14] disabled:opacity-40 text-slate-950 text-xs font-bold flex items-center gap-2 transition-all shadow-md shadow-[#76d418]/25 cursor-pointer"
                >
                  <PhoneCall className="w-4 h-4" />
                  <span>Dial Direct</span>
                </button>
              </div>
            </>
          )}
        </div>

        {/* Call Records & Telephony Stream (2 cols) */}
        <div className="lg:col-span-2 bg-[#091016] border border-slate-800 rounded-2xl p-5 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800">
            <div>
              <h3 className="text-sm font-bold text-white">SIP Call Logs & AI Audio Transcriptions</h3>
              <p className="text-xs text-slate-400">Automated intake logs with sentiment scoring and transcriptions</p>
            </div>
            <span className="text-xs font-mono text-slate-400">{calls.length} Total Calls</span>
          </div>

          {calls.length === 0 ? (
            <div className="h-48 flex flex-col items-center justify-center text-center p-6 text-slate-500 text-xs border border-dashed border-slate-800 rounded-xl space-y-2">
              <Phone className="w-6 h-6 text-slate-600" />
              <p className="text-slate-400 font-medium">No recorded calls in history.</p>
              <p className="text-[11px] text-slate-500 max-w-xs">
                Use the keypad dialer to simulate an inbound or outbound call handled by the autonomous VoIP engine.
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {calls.map((call) => (
                <div
                  key={call.id}
                  id={`call-card-${call.id}`}
                  className="p-4 rounded-xl bg-[#060a08] border border-slate-800 space-y-2.5"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-lg bg-slate-800 text-[#76d418] flex items-center justify-center font-bold text-xs border border-slate-700">
                        <Phone className="w-4 h-4" />
                      </div>
                      <div>
                        <h4 className="text-xs font-bold text-white">{call.callerName}</h4>
                        <span className="text-[11px] text-slate-400">{call.phoneNumber}</span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase bg-[#76d418]/15 text-[#76d418] border border-[#76d418]/30">
                        {call.sentiment}
                      </span>
                      <span className="text-xs font-mono text-slate-400">{call.duration}</span>
                    </div>
                  </div>

                  <p className="text-xs text-slate-300 leading-relaxed pl-10">
                    {call.summary}
                  </p>

                  <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-500 pl-10">
                    <span>Timestamp: <strong className="text-slate-400">{call.timestamp}</strong></span>
                    <span>Engine: <strong className="text-[#76d418]">{call.agentRoutedTo}</strong></span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
