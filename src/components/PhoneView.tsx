import React, { useState, useEffect, useRef } from 'react';
import { 
  Phone, 
  PhoneCall, 
  PhoneOff, 
  PhoneIncoming, 
  Mic, 
  MicOff, 
  Volume2, 
  VolumeX, 
  Bot, 
  User, 
  UserCheck, 
  Building2, 
  Sparkles, 
  Send, 
  MessageSquare, 
  Pause, 
  Play, 
  AlertTriangle, 
  CheckCircle2, 
  Radio, 
  Clock, 
  FileText, 
  PlusCircle, 
  Trash2, 
  Layers, 
  ShieldAlert, 
  Copy, 
  Check, 
  Headphones, 
  ExternalLink,
  ChevronRight,
  Info,
  AlertCircle
} from 'lucide-react';
import { CallRecord, CallTranscriptEntry, Job } from '../types';
import { 
  playPhoneRing, 
  playDtmfTone, 
  playConnectTone, 
  playDisconnectTone, 
  speakAiResponse, 
  stopSpeech,
  isAiSpeaking
} from '../services/telephonyAudio';

interface PhoneViewProps {
  calls: CallRecord[];
  onLogCall: (call: CallRecord) => void;
  onDeleteCall?: (callId: string) => void;
  onCreateJobFromCall?: (job: Partial<Job>) => void;
  orgName?: string;
}

interface ActiveCallState {
  id: string;
  callerName: string;
  phoneNumber: string;
  company: string;
  status: 'ringing' | 'connected_user' | 'connected_ai' | 'on_hold' | 'ended';
  answeredBy: 'human' | 'ai' | null;
  startTime: number;
  transcript: CallTranscriptEntry[];
  whisperDirectives: string[];
}

export const PhoneView: React.FC<PhoneViewProps> = ({ 
  calls, 
  onLogCall, 
  onDeleteCall, 
  onCreateJobFromCall, 
  orgName 
}) => {
  // Navigation tabs inside Phone System
  const [activeSubTab, setActiveSubTab] = useState<'switchboard' | 'customer_portal' | 'logs'>('switchboard');

  // Keypad state
  const [dialedNumber, setDialedNumber] = useState('');
  const [outboundCallerName, setOutboundCallerName] = useState('Direct Outbound Client');

  // Twilio Service Layer & Real-time Stream Config
  const [telephonyConfig, setTelephonyConfig] = useState<{
    phoneNumber: string;
    phoneDidFormatted: string;
    companyName: string;
    receptionistName: string;
    webhookUrl: string;
    outboundTwimlUrl: string;
    mediaStreamWsUrl: string;
    statusUrl: string;
    hasTwilioCarrier: boolean;
    hasGeminiApiKey: boolean;
  } | null>(null);
  const [showTwilioModal, setShowTwilioModal] = useState(false);
  const [outboundStatusNote, setOutboundStatusNote] = useState<string | null>(null);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  // Active call state
  const [activeCall, setActiveCall] = useState<ActiveCallState | null>(null);
  const [incomingCall, setIncomingCall] = useState<ActiveCallState | null>(null);
  const [callDuration, setCallDuration] = useState(0);
  const [isMuted, setIsMuted] = useState(false);
  const [operatorSpeechInput, setOperatorSpeechInput] = useState('');
  const [whisperInput, setWhisperInput] = useState('');
  const [showWhisperBox, setShowWhisperBox] = useState(false);
  const [selectedCallDetails, setSelectedCallDetails] = useState<CallRecord | null>(null);
  const [isSummarizing, setIsSummarizing] = useState(false);
  const [hasCopiedWebhook, setHasCopiedWebhook] = useState(false);

  // Live Voice Transcription & Web Speech Recognition State
  const [interimUserSpeech, setInterimUserSpeech] = useState('');
  const [isMicListening, setIsMicListening] = useState(false);
  const [speechError, setSpeechError] = useState<string | null>(null);
  const [isAiResponding, setIsAiResponding] = useState(false);

  const recognitionRef = useRef<any>(null);
  const isAiSpeakingRef = useRef<boolean>(false);
  const activeCallRef = useRef<ActiveCallState | null>(null);
  const isMutedRef = useRef<boolean>(false);
  const activeSubTabRef = useRef<'switchboard' | 'customer_portal' | 'logs'>('switchboard');
  const isHandlingSpeechRef = useRef<boolean>(false);

  useEffect(() => {
    activeCallRef.current = activeCall;
  }, [activeCall]);

  useEffect(() => {
    isMutedRef.current = isMuted;
  }, [isMuted]);

  useEffect(() => {
    activeSubTabRef.current = activeSubTab;
  }, [activeSubTab]);

  // Fetch dynamic telephony configuration & Twilio media stream routes
  useEffect(() => {
    fetch('/api/telephony/config')
      .then(res => res.json())
      .then(data => setTelephonyConfig(data))
      .catch(err => console.warn('Failed to load telephony config:', err));
  }, []);

  // Customer Portal simulator state
  const [customerName, setCustomerName] = useState('');
  const [customerCompany, setCustomerCompany] = useState('');
  const [customerNumber, setCustomerNumber] = useState('');
  const [customerIssuePreset, setCustomerIssuePreset] = useState('');
  const [customerSpeechInput, setCustomerSpeechInput] = useState('');
  const [isCustomerCalling, setIsCustomerCalling] = useState(false);
  const [customerCallState, setCustomerCallState] = useState<'idle' | 'calling' | 'connected' | 'ended'>('idle');

  // WebSocket & Ring Audio refs
  const wsRef = useRef<WebSocket | null>(null);
  const stopRingRef = useRef<(() => void) | null>(null);
  const timerRef = useRef<number | null>(null);
  const transcriptEndRef = useRef<HTMLDivElement | null>(null);

  // Auto-scroll transcript to bottom
  useEffect(() => {
    transcriptEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [activeCall?.transcript, interimUserSpeech]);

  // Play AI response through speech synthesis and protect mic from self-hearing
  const playAiSpeech = (text: string) => {
    isAiSpeakingRef.current = true;
    speakAiResponse(text, () => {
      setTimeout(() => {
        isAiSpeakingRef.current = false;
      }, 500);
    });
  };

  // Process finalized user speech (from either live microphone or manual text submit)
  const handleUserSpokenFinalText = async (text: string) => {
    const call = activeCallRef.current;
    if (!text.trim() || !call || isHandlingSpeechRef.current) return;

    // Check if AI is currently talking (suppress speaker audio feedback)
    if (isAiSpeakingRef.current || isAiSpeaking()) {
      return;
    }

    const cleanSpoken = text.trim();
    isHandlingSpeechRef.current = true;
    const isCustomerView = activeSubTabRef.current === 'customer_portal';

    // In operator view with human answered, user is operator.
    // In all AI answered calls or customer simulator, user is customer.
    const speaker: 'customer' | 'operator' = (call.status === 'connected_user' && !isCustomerView)
      ? 'operator'
      : 'customer';

    const timestamp = new Date().toLocaleTimeString();

    // Append to live transcript immediately
    setActiveCall(prev => {
      if (!prev) return null;
      return {
        ...prev,
        transcript: [
          ...prev.transcript,
          {
            speaker,
            text: cleanSpoken,
            time: timestamp
          }
        ]
      };
    });

    if (speaker === 'operator') {
      // Send operator speech over WebSocket
      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({
          type: 'OPERATOR_SPEECH',
          callId: call.id,
          text: cleanSpoken
        }));
      }
      isHandlingSpeechRef.current = false;
    } else {
      // Caller spoke to AI or Operator
      if (call.status === 'connected_ai') {
        setIsAiResponding(true);
        try {
          const res = await fetch('/api/telephony/ai-respond', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              callId: call.id,
              customerText: cleanSpoken,
              conversationHistory: call.transcript,
              whisperDirectives: call.whisperDirectives
            })
          });

          const data = await res.json();
          if (data.aiText) {
            playAiSpeech(data.aiText);
          }
        } catch (err) {
          console.error('Error generating AI response for customer:', err);
        } finally {
          setIsAiResponding(false);
          isHandlingSpeechRef.current = false;
        }
      } else {
        // Connected to human operator
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
          wsRef.current.send(JSON.stringify({
            type: 'CUSTOMER_SPEECH',
            callId: call.id,
            text: cleanSpoken
          }));
        }
        isHandlingSpeechRef.current = false;
      }
    }
  };

  // Continuous Live Speech Recognition Controller
  useEffect(() => {
    if (!activeCall || isMuted) {
      if (recognitionRef.current) {
        try {
          recognitionRef.current.stop();
        } catch (e) {}
        recognitionRef.current = null;
      }
      setIsMicListening(false);
      setInterimUserSpeech('');
      return;
    }

    const SpeechRecognition = 
      (window as any).SpeechRecognition || 
      (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setSpeechError('Web Speech API is not supported in this browser. You can type in the speech console.');
      return;
    }

    let isDestroyed = false;

    const startRecognitionInstance = () => {
      if (isDestroyed || !activeCallRef.current || isMutedRef.current) return;

      try {
        const recognition = new SpeechRecognition();
        recognition.continuous = true;
        recognition.interimResults = true;
        recognition.lang = 'en-US';
        recognition.maxAlternatives = 1;

        recognition.onstart = () => {
          if (!isDestroyed) {
            setIsMicListening(true);
            setSpeechError(null);
          }
        };

        let speechAccumulator = '';
        let speechFinalizeTimeout: any = null;

        recognition.onresult = (event: any) => {
          if (isDestroyed || isMutedRef.current) return;

          // If AI is currently speaking, ignore audio to avoid self-echoing
          if (isAiSpeakingRef.current || isAiSpeaking()) {
            setInterimUserSpeech('');
            speechAccumulator = '';
            return;
          }

          let currentInterim = '';
          let newlyFinalized = '';

          for (let i = event.resultIndex; i < event.results.length; ++i) {
            const transcriptChunk = event.results[i][0]?.transcript || '';
            if (event.results[i].isFinal) {
              newlyFinalized += transcriptChunk + ' ';
            } else {
              currentInterim += transcriptChunk;
            }
          }

          if (newlyFinalized) {
            speechAccumulator = (speechAccumulator + ' ' + newlyFinalized).trim();
          }

          const combinedPreview = (speechAccumulator ? speechAccumulator + ' ' : '') + currentInterim;
          if (combinedPreview.trim()) {
            setInterimUserSpeech(combinedPreview.trim());
          }

          // Debounce finalization: wait 750ms of silence before submitting the full sentence/thought
          // This prevents chopping sentences into single isolated words!
          if (speechFinalizeTimeout) {
            clearTimeout(speechFinalizeTimeout);
          }

          speechFinalizeTimeout = setTimeout(() => {
            const fullUtterance = (speechAccumulator || currentInterim).trim();
            if (fullUtterance && !isDestroyed && !isMutedRef.current) {
              setInterimUserSpeech('');
              speechAccumulator = '';
              handleUserSpokenFinalText(fullUtterance);
            }
          }, 750);
        };

        recognition.onerror = (event: any) => {
          if (isDestroyed) return;
          console.warn('Telephony speech recognition error event:', event.error);
          if (event.error === 'not-allowed') {
            setSpeechError('Microphone permission required. Please allow microphone access in your browser to speak directly on the call.');
            setIsMicListening(false);
          } else if (event.error === 'no-speech') {
            // Normal pause in speech, will auto-cycle
          }
        };

        recognition.onend = () => {
          if (isDestroyed) return;
          setIsMicListening(false);
          // Restart continuous recognition if call is ongoing and unmuted
          if (activeCallRef.current && !isMutedRef.current) {
            setTimeout(() => {
              if (!isDestroyed && activeCallRef.current && !isMutedRef.current) {
                startRecognitionInstance();
              }
            }, 300);
          }
        };

        recognitionRef.current = recognition;
        recognition.start();
      } catch (err: any) {
        console.warn('Failed to start telephony speech recognition instance:', err);
        setIsMicListening(false);
      }
    };

    startRecognitionInstance();

    return () => {
      isDestroyed = true;
      if (recognitionRef.current) {
        try {
          recognitionRef.current.stop();
        } catch (e) {}
        recognitionRef.current = null;
      }
      setIsMicListening(false);
      setInterimUserSpeech('');
    };
  }, [Boolean(activeCall), isMuted]);

  // Connect to Telephony WebSocket Gateway
  useEffect(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/api/telephony/stream`;

    let ws: WebSocket;
    try {
      ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        // Register this browser client as an operator console
        ws.send(JSON.stringify({ type: 'REGISTER_OPERATOR' }));
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);

          switch (data.type) {
            case 'INCOMING_CALL': {
              const newIncoming: ActiveCallState = data.call;
              setIncomingCall(newIncoming);
              // Start audible ring
              if (!stopRingRef.current) {
                stopRingRef.current = playPhoneRing();
              }
              break;
            }

            case 'CALL_ANSWERED': {
              // Stop ringing
              if (stopRingRef.current) {
                stopRingRef.current();
                stopRingRef.current = null;
              }
              playConnectTone();

              setActiveCall(prev => {
                const base = prev || incomingCall;
                if (!base) return null;
                return {
                  ...base,
                  status: data.answeredBy === 'ai' ? 'connected_ai' : 'connected_user',
                  answeredBy: data.answeredBy,
                  transcript: [
                    ...base.transcript,
                    {
                      speaker: 'system',
                      text: data.answeredBy === 'ai' 
                        ? 'Aegis Autonomous AI Receptionist answered the line.' 
                        : 'Human Operator connected to live call.',
                      time: new Date().toLocaleTimeString()
                    },
                    ...(data.greeting ? [{
                      speaker: 'ai' as const,
                      text: data.greeting,
                      time: new Date().toLocaleTimeString()
                    }] : [])
                  ]
                };
              });

              setIncomingCall(null);

              // If AI spoke greeting, synthesize speech
              if (data.greeting) {
                playAiSpeech(data.greeting);
              }
              break;
            }

            case 'AI_SPEECH': {
              const text = data.text;
              setActiveCall(prev => {
                if (!prev) return null;
                const last = prev.transcript[prev.transcript.length - 1];
                if (last && last.speaker === 'ai' && last.text === text) {
                  return prev;
                }
                return {
                  ...prev,
                  transcript: [
                    ...prev.transcript,
                    {
                      speaker: 'ai',
                      text,
                      time: new Date().toLocaleTimeString()
                    }
                  ]
                };
              });
              // Synthesize voice
              playAiSpeech(text);
              break;
            }

            case 'CUSTOMER_SPEECH': {
              const text = data.text;
              setActiveCall(prev => {
                if (!prev) return null;
                const last = prev.transcript[prev.transcript.length - 1];
                if (last && last.speaker === 'customer' && last.text === text) {
                  return prev;
                }
                return {
                  ...prev,
                  transcript: [
                    ...prev.transcript,
                    {
                      speaker: 'customer',
                      text,
                      time: new Date().toLocaleTimeString()
                    }
                  ]
                };
              });
              break;
            }

            case 'OPERATOR_SPEECH': {
              const text = data.text;
              setActiveCall(prev => {
                if (!prev) return null;
                const last = prev.transcript[prev.transcript.length - 1];
                if (last && last.speaker === 'operator' && last.text === text) {
                  return prev;
                }
                return {
                  ...prev,
                  transcript: [
                    ...prev.transcript,
                    {
                      speaker: 'operator',
                      text,
                      time: new Date().toLocaleTimeString()
                    }
                  ]
                };
              });
              break;
            }

            case 'TRANSCRIPT_UPDATE': {
              setActiveCall(prev => {
                if (!prev) return null;
                const last = prev.transcript[prev.transcript.length - 1];
                if (last && last.speaker === data.speaker && last.text === data.text) {
                  return prev;
                }
                return {
                  ...prev,
                  transcript: [
                    ...prev.transcript,
                    {
                      speaker: data.speaker,
                      text: data.text,
                      time: data.time || new Date().toLocaleTimeString()
                    }
                  ]
                };
              });
              break;
            }

            case 'CALL_TAKEN_OVER': {
              setActiveCall(prev => {
                if (!prev) return null;
                return {
                  ...prev,
                  status: 'connected_user',
                  answeredBy: 'human',
                  transcript: [
                    ...prev.transcript,
                    {
                      speaker: 'system',
                      text: 'Human Operator took over call from AI receptionist.',
                      time: new Date().toLocaleTimeString()
                    }
                  ]
                };
              });
              stopSpeech();
              break;
            }

            case 'CALL_HELD': {
              setActiveCall(prev => prev ? { ...prev, status: 'on_hold' } : null);
              break;
            }

            case 'CALL_UNHELD': {
              setActiveCall(prev => prev ? { ...prev, status: data.status } : null);
              break;
            }

            case 'CALL_ENDED': {
              handleTerminateCall(false);
              break;
            }
          }
        } catch (e) {
          console.error('Error handling telephony WebSocket message:', e);
        }
      };

      ws.onerror = (err) => {
        console.warn('Telephony WebSocket warning:', err);
      };
    } catch (e) {
      console.warn('Telephony WebSocket connection error:', e);
    }

    return () => {
      if (stopRingRef.current) {
        stopRingRef.current();
      }
      stopSpeech();
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, []);

  // Duration Timer for active call
  useEffect(() => {
    if (activeCall && (activeCall.status === 'connected_user' || activeCall.status === 'connected_ai' || activeCall.status === 'on_hold')) {
      timerRef.current = window.setInterval(() => {
        setCallDuration(prev => prev + 1);
      }, 1000);
    } else {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    }

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, [activeCall?.status]);

  // Keypad Touch Tones
  const handleKeyPress = (digit: string) => {
    playDtmfTone(digit);
    if (dialedNumber.length < 18) {
      setDialedNumber(prev => prev + digit);
    }
  };

  // 1. Answer Call Personally (Operator / Human)
  const handleAnswerPersonally = () => {
    if (!incomingCall) return;

    if (stopRingRef.current) {
      stopRingRef.current();
      stopRingRef.current = null;
    }

    playConnectTone();

    const answeredCall: ActiveCallState = {
      ...incomingCall,
      status: 'connected_user',
      answeredBy: 'human',
      transcript: [
        ...incomingCall.transcript,
        {
          speaker: 'system',
          text: 'Operator answered call on console.',
          time: new Date().toLocaleTimeString()
        }
      ]
    };

    setActiveCall(answeredCall);
    setIncomingCall(null);
    setCallDuration(0);

    // Notify server over WebSocket
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: 'ANSWER_CALL',
        callId: answeredCall.id
      }));
    }
  };

  // 2. Dispatch AI Receptionist to Answer Call
  const handleDispatchAiAnswer = () => {
    if (!incomingCall) return;

    if (stopRingRef.current) {
      stopRingRef.current();
      stopRingRef.current = null;
    }

    playConnectTone();

    const greeting = `Thank you for calling ${orgName || 'RCOS Enterprise Solutions'}. My name is Aegis, your AI receptionist. How may I assist you today?`;

    const aiCall: ActiveCallState = {
      ...incomingCall,
      status: 'connected_ai',
      answeredBy: 'ai',
      transcript: [
        ...incomingCall.transcript,
        {
          speaker: 'system',
          text: 'Aegis Autonomous AI Receptionist connected.',
          time: new Date().toLocaleTimeString()
        },
        {
          speaker: 'ai',
          text: greeting,
          time: new Date().toLocaleTimeString()
        }
      ]
    };

    setActiveCall(aiCall);
    setIncomingCall(null);
    setCallDuration(0);

    playAiSpeech(greeting);

    // Notify server
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: 'AI_ANSWER_CALL',
        callId: aiCall.id
      }));
    }
  };

  // 3. Take Over Call from AI (Barge In)
  const handleTakeOverCall = () => {
    if (!activeCall) return;
    stopSpeech();
    isAiSpeakingRef.current = false;

    setActiveCall(prev => {
      if (!prev) return null;
      return {
        ...prev,
        status: 'connected_user',
        answeredBy: 'human',
        transcript: [
          ...prev.transcript,
          {
            speaker: 'system',
            text: 'Operator barged in and took over call.',
            time: new Date().toLocaleTimeString()
          }
        ]
      };
    });

    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: 'TAKE_OVER_CALL',
        callId: activeCall.id
      }));
    }
  };

  // 4. Hand Back to AI
  const handleHandBackToAi = () => {
    if (!activeCall) return;

    setActiveCall(prev => {
      if (!prev) return null;
      return {
        ...prev,
        status: 'connected_ai',
        answeredBy: 'ai',
        transcript: [
          ...prev.transcript,
          {
            speaker: 'system',
            text: 'Call handed back to Aegis AI Receptionist.',
            time: new Date().toLocaleTimeString()
          }
        ]
      };
    });

    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: 'AI_ANSWER_CALL',
        callId: activeCall.id
      }));
    }
  };

  // 5. Send Operator Whisper to AI
  const handleSendWhisper = () => {
    if (!whisperInput.trim() || !activeCall) return;
    const directive = whisperInput.trim();

    setActiveCall(prev => {
      if (!prev) return null;
      return {
        ...prev,
        whisperDirectives: [...prev.whisperDirectives, directive],
        transcript: [
          ...prev.transcript,
          {
            speaker: 'system',
            text: `[Private Operator Directive to AI]: "${directive}"`,
            time: new Date().toLocaleTimeString()
          }
        ]
      };
    });

    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: 'WHISPER_DIRECTIVE',
        callId: activeCall.id,
        directive
      }));
    }

    setWhisperInput('');
  };

  // 6. Operator Speaks / Sends Text to Caller
  const handleOperatorSendSpeech = (textToSend?: string) => {
    const text = (textToSend || operatorSpeechInput).trim();
    if (!text || !activeCall) return;
    setOperatorSpeechInput('');
    handleUserSpokenFinalText(text);
  };

  // 7. Toggle Hold
  const handleToggleHold = () => {
    if (!activeCall) return;

    if (activeCall.status === 'on_hold') {
      const nextStatus = activeCall.answeredBy === 'ai' ? 'connected_ai' : 'connected_user';
      setActiveCall(prev => prev ? { ...prev, status: nextStatus } : null);
      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({ type: 'UNHOLD_CALL', callId: activeCall.id }));
      }
    } else {
      setActiveCall(prev => prev ? { ...prev, status: 'on_hold' } : null);
      stopSpeech();
      isAiSpeakingRef.current = false;
      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({ type: 'HOLD_CALL', callId: activeCall.id }));
      }
    }
  };

  // 8. Terminate / End Call & Trigger AI Summary
  const handleTerminateCall = async (notifyServer = true) => {
    if (stopRingRef.current) {
      stopRingRef.current();
      stopRingRef.current = null;
    }
    stopSpeech();
    isAiSpeakingRef.current = false;
    playDisconnectTone();

    const currentCall = activeCall || incomingCall;
    if (!currentCall) {
      setActiveCall(null);
      setIncomingCall(null);
      return;
    }

    if (notifyServer && wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: 'END_CALL',
        callId: currentCall.id
      }));
    }

    const minutes = Math.floor(callDuration / 60);
    const seconds = callDuration % 60;
    const formattedDuration = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

    setIsSummarizing(true);

    try {
      // Request AI summarization from backend
      const res = await fetch('/api/telephony/summarize', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          callId: currentCall.id,
          transcript: currentCall.transcript
        })
      });

      const summaryData = await res.json();

      const completedRecord: CallRecord = {
        id: currentCall.id,
        callerName: currentCall.callerName,
        company: currentCall.company,
        phoneNumber: currentCall.phoneNumber,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        duration: formattedDuration,
        sentiment: summaryData.sentiment || 'Neutral',
        summary: summaryData.summary || `Call with ${currentCall.callerName} ended. Duration: ${formattedDuration}.`,
        agentRoutedTo: currentCall.answeredBy === 'ai' ? 'Aegis AI Receptionist' : 'Human Operator',
        status: 'Completed',
        answeredBy: currentCall.answeredBy || 'ai',
        transcript: currentCall.transcript,
        actionItems: summaryData.actionItems || [],
        audioDurationSec: callDuration
      };

      onLogCall(completedRecord);

      // Offer to auto-create job if suggested
      if (summaryData.suggestedJob && onCreateJobFromCall) {
        // user can click to convert from logs
      }
    } catch (err) {
      console.warn('Call summarization fallback:', err);
      const fallbackRecord: CallRecord = {
        id: currentCall.id,
        callerName: currentCall.callerName,
        company: currentCall.company,
        phoneNumber: currentCall.phoneNumber,
        timestamp: 'Just now',
        duration: formattedDuration,
        sentiment: 'Neutral',
        summary: `Call completed with ${currentCall.callerName}. Handled by ${currentCall.answeredBy || 'System'}.`,
        agentRoutedTo: 'VoIP Telephony Engine',
        status: 'Completed',
        answeredBy: currentCall.answeredBy || 'ai',
        transcript: currentCall.transcript,
        actionItems: ['Follow up with customer inquiry']
      };
      onLogCall(fallbackRecord);
    } finally {
      setIsSummarizing(false);
      setActiveCall(null);
      setIncomingCall(null);
      setCallDuration(0);
      setCustomerCallState('ended');
    }
  };

  // 9. Outbound Dialing (from Keypad via Twilio Voice Service & Gemini Media Stream)
  const handleDialOutbound = async (dispatchAi = false) => {
    if (!dialedNumber.trim()) return;

    try {
      setOutboundStatusNote("Initiating call and connecting TwiML stream to Gemini multi-modal AI agent...");
      const res = await fetch('/api/telephony/outbound', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phoneNumber: dialedNumber,
          callerName: outboundCallerName,
          dispatchAi,
          purpose: 'Executive Direct Telephony Call'
        })
      });

      const data = await res.json();
      if (data.call) {
        playConnectTone();
        setActiveCall({
          ...data.call,
          status: dispatchAi ? 'connected_ai' : 'connected_user',
          answeredBy: dispatchAi ? 'ai' : 'human'
        });
        setCallDuration(0);

        if (data.message) {
          setOutboundStatusNote(data.message);
          setTimeout(() => setOutboundStatusNote(null), 9000);
        }

        if (dispatchAi) {
          const greeting = `Hello, this is Aegis calling on behalf of ${orgName || 'RCOS Enterprise Solutions'}. How can we support your team today?`;
          playAiSpeech(greeting);
        }
      } else if (data.error) {
        setOutboundStatusNote(`Call failed: ${data.error}`);
      }
    } catch (e: any) {
      console.error('Outbound dial error:', e);
      setOutboundStatusNote(`Dialing error: ${e.message || 'Failed to place call'}`);
    }
  };

  // 10. Customer Portal: Simulate Customer Calling the Business Line
  const handleCustomerDialBusiness = async () => {
    setCustomerCallState('calling');
    setIsCustomerCalling(true);

    try {
      const res = await fetch('/api/telephony/incoming', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phoneNumber: customerNumber,
          callerName: customerName,
          company: customerCompany,
          issueSummary: customerIssuePreset
        })
      });

      const data = await res.json();
      if (data.call) {
        setCustomerCallState('calling');
      }
    } catch (e) {
      console.error('Customer call simulation error:', e);
    }
  };

  // 11. Customer Speaks in Customer Portal
  const handleCustomerSpeak = async () => {
    if (!customerSpeechInput.trim() || !activeCall) return;
    const text = customerSpeechInput.trim();
    setCustomerSpeechInput('');
    await handleUserSpokenFinalText(text);
  };

  // Copy Webhook URL
  const handleCopyWebhook = () => {
    const webhookUrl = `${window.location.origin}/api/telephony/incoming`;
    navigator.clipboard.writeText(webhookUrl);
    setHasCopiedWebhook(true);
    setTimeout(() => setHasCopiedWebhook(false), 2000);
  };

  const formatTimer = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  return (
    <div id="telephony-root" className="space-y-6 max-w-6xl mx-auto pb-24 animate-in fade-in duration-200">
      
      {/* 1. Header & Live Telephony Telemetry */}
      <div className="bg-[#091016] border border-slate-800 rounded-2xl p-5 shadow-xl">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-[#76d418]/15 border border-[#76d418]/30 flex items-center justify-center text-[#76d418] shadow-sm shadow-[#76d418]/20">
                <PhoneCall className="w-5 h-5" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
                  <span>VoIP Telephony & Dual Answering Switchboard</span>
                  <span className="text-[11px] px-2 py-0.5 rounded-full font-mono bg-[#76d418]/15 text-[#76d418] border border-[#76d418]/40">
                    SIP GATEWAY ACTIVE
                  </span>
                </h1>
                <p className="text-xs text-slate-400">
                  Direct phone line with autonomous Gemini AI Receptionist or human executive pickup
                </p>
              </div>
            </div>
          </div>

          {/* Quick Line Stats */}
          <div className="flex flex-wrap items-center gap-2.5">
            <div className="px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 flex items-center gap-2">
              <span className="text-slate-400 text-xs">Direct Inbound DID:</span>
              <span className="font-mono text-xs font-bold text-white tracking-wider">
                {telephonyConfig?.phoneNumber || "+1 (888) 550-RCOS"}
              </span>
            </div>

            <button
              onClick={() => setShowTwilioModal(true)}
              className="px-3 py-1.5 rounded-xl bg-cyan-950/60 hover:bg-cyan-900/80 border border-cyan-800/80 text-xs font-medium text-cyan-300 flex items-center gap-1.5 transition-colors cursor-pointer"
              title="Twilio Voice Webhook & TwiML Media Streams Setup"
            >
              <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
              <span>Twilio & TwiML Streams</span>
            </button>

            <button
              onClick={handleCopyWebhook}
              className="px-3 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-xs font-medium text-slate-300 flex items-center gap-1.5 transition-colors cursor-pointer"
              title="Copy Twilio/SIP Webhook URL"
            >
              {hasCopiedWebhook ? <Check className="w-3.5 h-3.5 text-[#76d418]" /> : <Copy className="w-3.5 h-3.5 text-slate-400" />}
              <span>{hasCopiedWebhook ? 'Copied' : 'Webhook URL'}</span>
            </button>
          </div>
        </div>

        {/* Outbound Status Notification Banner */}
        {outboundStatusNote && (
          <div className="mt-4 p-3 rounded-xl bg-cyan-950/70 border border-cyan-500/40 text-cyan-200 text-xs flex items-center justify-between gap-3 animate-in fade-in">
            <div className="flex items-center gap-2">
              <Radio className="w-4 h-4 text-cyan-400 animate-pulse flex-shrink-0" />
              <span>{outboundStatusNote}</span>
            </div>
            <button
              onClick={() => setOutboundStatusNote(null)}
              className="text-cyan-400 hover:text-white font-bold text-xs cursor-pointer ml-2"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Navigation Tabs */}
        <div className="flex items-center gap-2 pt-4 mt-4 border-t border-slate-800/80">
          <button
            onClick={() => setActiveSubTab('switchboard')}
            className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition-all cursor-pointer ${
              activeSubTab === 'switchboard'
                ? 'bg-[#76d418] text-slate-950 shadow-md shadow-[#76d418]/20'
                : 'bg-slate-900/80 text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <Radio className="w-3.5 h-3.5" />
            <span>Operator Switchboard</span>
            {incomingCall && (
              <span className="w-2 h-2 rounded-full bg-rose-500 animate-ping" />
            )}
          </button>

          <button
            onClick={() => setActiveSubTab('customer_portal')}
            className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition-all cursor-pointer ${
              activeSubTab === 'customer_portal'
                ? 'bg-[#76d418] text-slate-950 shadow-md shadow-[#76d418]/20'
                : 'bg-slate-900/80 text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <User className="w-3.5 h-3.5" />
            <span>Customer Caller Line (Simulator & Testing)</span>
          </button>

          <button
            onClick={() => setActiveSubTab('logs')}
            className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition-all cursor-pointer ${
              activeSubTab === 'logs'
                ? 'bg-[#76d418] text-slate-950 shadow-md shadow-[#76d418]/20'
                : 'bg-slate-900/80 text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <FileText className="w-3.5 h-3.5" />
            <span>Call Logs & Intelligence ({calls.length})</span>
          </button>
        </div>
      </div>

      {/* 2. INCOMING CALL BANNER (Rings if someone calls) */}
      {incomingCall && !activeCall && (
        <div 
          id="incoming-call-alert" 
          className="bg-gradient-to-r from-amber-950/80 via-slate-900 to-amber-950/80 border-2 border-amber-500/80 rounded-2xl p-5 shadow-2xl animate-pulse"
        >
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-4 text-center md:text-left">
              <div className="w-14 h-14 rounded-full bg-amber-500/20 border-2 border-amber-400 flex items-center justify-center text-amber-400 animate-bounce">
                <PhoneIncoming className="w-7 h-7" />
              </div>
              <div>
                <div className="flex items-center gap-2 justify-center md:justify-start">
                  <span className="text-xs font-bold uppercase tracking-wider text-amber-400 bg-amber-500/20 px-2 py-0.5 rounded-full border border-amber-500/40">
                    Live Inbound Call Ringing
                  </span>
                  <span className="text-xs text-slate-400 font-mono">DID Line 1</span>
                </div>
                <h3 className="text-lg font-bold text-white mt-1">
                  {incomingCall.callerName} <span className="text-sm font-normal text-slate-300">({incomingCall.company})</span>
                </h3>
                <p className="text-xs font-mono text-slate-400">{incomingCall.phoneNumber}</p>
              </div>
            </div>

            {/* Answer Choice Buttons */}
            <div className="flex items-center gap-3">
              <button
                onClick={handleAnswerPersonally}
                className="px-5 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 text-xs font-bold flex items-center gap-2 shadow-lg shadow-emerald-900/40 transition-transform active:scale-95 cursor-pointer"
              >
                <Phone className="w-4 h-4" />
                <span>Answer Personally (Human)</span>
              </button>

              <button
                onClick={handleDispatchAiAnswer}
                className="px-5 py-3 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 text-xs font-bold flex items-center gap-2 shadow-lg shadow-[#76d418]/30 transition-transform active:scale-95 cursor-pointer"
              >
                <Bot className="w-4 h-4" />
                <span>Let AI Answer (Aegis)</span>
              </button>

              <button
                onClick={() => handleTerminateCall(true)}
                className="p-3 rounded-xl bg-slate-800 hover:bg-rose-950 hover:text-rose-400 border border-slate-700 text-slate-400 transition-colors cursor-pointer"
                title="Decline / Send to Voicemail"
              >
                <PhoneOff className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 3. ACTIVE CALL CONSOLE (If call is ongoing) */}
      {activeCall && (
        <div id="active-call-console" className="bg-[#091016] border-2 border-[#76d418]/60 rounded-2xl p-6 shadow-2xl space-y-5">
          {/* Active Call Header */}
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-[#76d418]/20 border border-[#76d418] flex items-center justify-center text-[#76d418] animate-pulse">
                <Volume2 className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold uppercase tracking-wider text-[#76d418] bg-[#76d418]/15 px-2 py-0.5 rounded-full border border-[#76d418]/40">
                    Call In Progress
                  </span>
                  <span className="text-xs text-slate-400">
                    {activeCall.status === 'connected_ai' ? 'Handled by Aegis AI Receptionist' : 'Connected to Human Operator'}
                  </span>
                </div>
                <h3 className="text-base font-bold text-white mt-0.5">
                  {activeCall.callerName} <span className="text-xs text-slate-400 font-normal">({activeCall.company})</span>
                </h3>
                <div className="flex flex-wrap items-center gap-2 mt-1">
                  <span className="text-xs font-mono text-slate-400">{activeCall.phoneNumber}</span>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded-md bg-cyan-950/80 text-cyan-300 border border-cyan-800/60 flex items-center gap-1">
                    <Radio className="w-2.5 h-2.5 text-cyan-400 animate-pulse" />
                    TwiML Media Stream: Gemini Multi-Modal
                  </span>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded-md bg-slate-900 text-slate-400 border border-slate-800">
                    Bidirectional 8kHz PCMU &hArr; Gemini Live 16kHz
                  </span>
                </div>
              </div>
            </div>

            {/* Timer & Handler Pill */}
            <div className="flex items-center gap-3">
              <div className="text-right">
                <div className="text-2xl font-mono font-bold text-white tracking-wider">
                  {formatTimer(callDuration)}
                </div>
                <span className="text-[11px] text-slate-400">
                  {activeCall.status === 'on_hold' ? 'Call on hold' : 'Two-way audio live'}
                </span>
              </div>

              {/* Action Buttons */}
              <button
                onClick={handleToggleHold}
                className={`px-3 py-2 rounded-xl border text-xs font-medium flex items-center gap-1.5 transition-colors cursor-pointer ${
                  activeCall.status === 'on_hold'
                    ? 'bg-amber-500/20 border-amber-500 text-amber-300'
                    : 'bg-slate-900 border-slate-800 text-slate-300 hover:text-white'
                }`}
              >
                {activeCall.status === 'on_hold' ? <Play className="w-3.5 h-3.5" /> : <Pause className="w-3.5 h-3.5" />}
                <span>{activeCall.status === 'on_hold' ? 'Resume' : 'Hold'}</span>
              </button>

              <button
                onClick={() => handleTerminateCall(true)}
                disabled={isSummarizing}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white text-xs font-bold flex items-center gap-2 transition-all shadow-lg shadow-rose-900/40 cursor-pointer"
              >
                <PhoneOff className="w-4 h-4" />
                <span>{isSummarizing ? 'Summarizing...' : 'End Call'}</span>
              </button>
            </div>
          </div>

          {/* Intervention & Supervision Bar */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 p-3 bg-slate-950/60 rounded-xl border border-slate-800/80">
            {/* Take Over / Barge In */}
            <div className="flex items-center justify-between p-2 rounded-lg bg-slate-900/80 border border-slate-800">
              <div className="flex items-center gap-2">
                {activeCall.status === 'connected_ai' ? (
                  <Bot className="w-4 h-4 text-cyan-400" />
                ) : (
                  <UserCheck className="w-4 h-4 text-emerald-400" />
                )}
                <div>
                  <span className="text-xs font-bold text-white block">
                    {activeCall.status === 'connected_ai' ? 'Aegis Answering' : 'Operator Answering'}
                  </span>
                  <span className="text-[10px] text-slate-400">
                    {activeCall.status === 'connected_ai' ? 'Gemini 3.5 Flash Voice' : 'Human Voice Channel'}
                  </span>
                </div>
              </div>

              {activeCall.status === 'connected_ai' ? (
                <button
                  onClick={handleTakeOverCall}
                  className="px-3 py-1 rounded-lg bg-amber-500 hover:bg-amber-400 text-slate-950 text-[11px] font-bold transition-colors cursor-pointer"
                >
                  Take Over (Barge In)
                </button>
              ) : (
                <button
                  onClick={handleHandBackToAi}
                  className="px-3 py-1 rounded-lg bg-[#76d418] hover:bg-[#66bd14] text-slate-950 text-[11px] font-bold transition-colors cursor-pointer"
                >
                  Hand to AI
                </button>
              )}
            </div>

            {/* Secret Whisper to AI */}
            <div className="flex items-center justify-between p-2 rounded-lg bg-slate-900/80 border border-slate-800">
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-[#76d418]" />
                <div>
                  <span className="text-xs font-bold text-white block">Whisper Directive</span>
                  <span className="text-[10px] text-slate-400">Guide AI response privately</span>
                </div>
              </div>

              <button
                onClick={() => setShowWhisperBox(!showWhisperBox)}
                className="px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-[11px] font-semibold transition-colors cursor-pointer"
              >
                {showWhisperBox ? 'Hide Whisper' : 'Whisper'}
              </button>
            </div>

            {/* Mic Toggle & Real-time Channel Status */}
            <div className="flex items-center justify-between p-2 rounded-lg bg-slate-900/80 border border-slate-800">
              <div className="flex items-center gap-2.5">
                {isMuted ? (
                  <MicOff className="w-4 h-4 text-rose-400" />
                ) : (isAiSpeakingRef.current || isAiSpeaking()) ? (
                  <Volume2 className="w-4 h-4 text-amber-400 animate-pulse" />
                ) : isMicListening ? (
                  <div className="relative flex items-center justify-center">
                    <Mic className="w-4 h-4 text-[#76d418]" />
                    <span className="absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full bg-[#76d418] animate-ping" />
                  </div>
                ) : (
                  <Mic className="w-4 h-4 text-slate-400" />
                )}
                <div>
                  <span className="text-xs font-bold text-white block">
                    {activeCall.status === 'connected_user' && activeSubTab !== 'customer_portal' 
                      ? 'Operator Live Microphone' 
                      : 'Live Voice Channel'}
                  </span>
                  <span className="text-[10px] text-slate-400">
                    {isMuted 
                      ? 'Muted (Audio input paused)' 
                      : (isAiSpeakingRef.current || isAiSpeaking()) 
                      ? 'Aegis Speaking (Speaker suppression)' 
                      : isMicListening 
                      ? '● Live mic active — speak anytime' 
                      : 'Connecting microphone...'}
                  </span>
                </div>
              </div>

              <button
                onClick={() => setIsMuted(!isMuted)}
                className={`px-3 py-1 rounded-lg text-[11px] font-bold transition-colors cursor-pointer ${
                  isMuted ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40' : 'bg-slate-800 text-slate-300 hover:text-white'
                }`}
              >
                {isMuted ? 'Unmute' : 'Mute'}
              </button>
            </div>
          </div>

          {/* Speech API Warning Banner */}
          {speechError && (
            <div className="p-3 bg-amber-950/40 border border-amber-500/30 rounded-xl text-xs text-amber-300 flex items-start justify-between gap-2">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-amber-400" />
                <span>{speechError}</span>
              </div>
              <button 
                onClick={() => setSpeechError(null)} 
                className="text-amber-400 hover:text-amber-200 text-xs font-bold cursor-pointer"
              >
                Dismiss
              </button>
            </div>
          )}

          {/* Whisper Drawer */}
          {showWhisperBox && (
            <div className="p-3 bg-slate-950 rounded-xl border border-[#76d418]/30 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="text-[#76d418] font-bold flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>Private Operator Whisper (Caller cannot hear this)</span>
                </span>
                <span className="text-[10px] text-slate-400">Directs AI's next spoken sentence</span>
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={whisperInput}
                  onChange={(e) => setWhisperInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSendWhisper()}
                  placeholder="e.g. 'Offer them a 15% enterprise onboarding discount' or 'Schedule for 3pm'..."
                  className="flex-1 h-9 bg-[#060a08] border border-slate-800 rounded-lg px-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-[#76d418]"
                />
                <button
                  onClick={handleSendWhisper}
                  disabled={!whisperInput.trim()}
                  className="h-9 px-4 rounded-lg bg-[#76d418] hover:bg-[#66bd14] disabled:opacity-40 text-slate-950 text-xs font-bold flex items-center gap-1.5 transition-colors cursor-pointer"
                >
                  <Send className="w-3 h-3" />
                  <span>Send</span>
                </button>
              </div>
            </div>
          )}

          {/* Live Audio Transcription Stream */}
          <div className="space-y-2">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span className="font-semibold flex items-center gap-1.5">
                <MessageSquare className="w-3.5 h-3.5 text-[#76d418]" />
                <span>Live Real-Time Call Transcription</span>
              </span>
              <span className="text-[11px] text-slate-500 flex items-center gap-1.5">
                {isMicListening && <span className="w-2 h-2 rounded-full bg-[#76d418] animate-pulse" />}
                <span>{isMicListening ? 'Live mic stream synced' : 'Auto-transcribed stream'}</span>
              </span>
            </div>

            <div className="h-64 overflow-y-auto bg-[#060a08] border border-slate-800 rounded-xl p-4 space-y-3 font-sans text-xs">
              {activeCall.transcript.map((entry, idx) => (
                <div 
                  key={idx} 
                  className={`flex flex-col ${
                    entry.speaker === 'customer' 
                      ? 'items-start' 
                      : entry.speaker === 'operator' 
                      ? 'items-end' 
                      : entry.speaker === 'ai' 
                      ? 'items-center text-center' 
                      : 'items-center text-center'
                  }`}
                >
                  <div className="flex items-center gap-1.5 text-[10px] text-slate-500 mb-1">
                    <span className="font-bold uppercase tracking-wider">
                      {entry.speaker === 'customer' 
                        ? activeCall.callerName 
                        : entry.speaker === 'operator' 
                        ? 'Operator (You)' 
                        : entry.speaker === 'ai' 
                        ? 'Aegis AI Receptionist' 
                        : 'Telephony System'}
                    </span>
                    <span>•</span>
                    <span>{entry.time || ''}</span>
                  </div>

                  <div className={`p-3 rounded-xl max-w-lg leading-relaxed ${
                    entry.speaker === 'customer'
                      ? 'bg-slate-900 border border-slate-800 text-slate-200'
                      : entry.speaker === 'operator'
                      ? 'bg-emerald-950/60 border border-emerald-500/40 text-emerald-100'
                      : entry.speaker === 'ai'
                      ? 'bg-cyan-950/40 border border-cyan-500/30 text-cyan-100'
                      : 'bg-amber-950/20 border border-amber-500/20 text-amber-300 font-mono text-[11px]'
                  }`}>
                    {entry.text}
                  </div>
                </div>
              ))}

              {/* Interim Real-Time Transcription Bubble (Operator / Caller Speaking) */}
              {interimUserSpeech && (
                <div 
                  className={`flex flex-col ${
                    activeCall.status === 'connected_user' && activeSubTab !== 'customer_portal' 
                      ? 'items-end' 
                      : 'items-start'
                  } animate-pulse`}
                >
                  <div className="flex items-center gap-1.5 text-[10px] text-[#76d418] mb-1 font-bold">
                    <span className="w-2 h-2 rounded-full bg-[#76d418] animate-ping" />
                    <span>
                      {activeCall.status === 'connected_user' && activeSubTab !== 'customer_portal' 
                        ? 'Operator (You) speaking...' 
                        : `${activeCall.callerName || 'Caller'} speaking...`}
                    </span>
                  </div>
                  <div className="p-3 rounded-xl max-w-lg bg-[#76d418]/10 border border-[#76d418]/40 text-[#76d418] text-xs italic">
                    "{interimUserSpeech}..."
                  </div>
                </div>
              )}

              {/* AI Generating Indicator */}
              {isAiResponding && (
                <div className="flex flex-col items-center text-center animate-pulse py-1">
                  <div className="flex items-center gap-2 text-[11px] text-cyan-400 font-bold bg-cyan-950/40 border border-cyan-500/30 px-3 py-1.5 rounded-full">
                    <Bot className="w-3.5 h-3.5 text-cyan-400 animate-spin" />
                    <span>Aegis AI Receptionist is formulating voice response...</span>
                  </div>
                </div>
              )}

              <div ref={transcriptEndRef} />
            </div>
          </div>

          {/* Operator Direct Speak / Reply Input */}
          <div className="flex items-center gap-2 pt-2">
            <input
              type="text"
              value={operatorSpeechInput}
              onChange={(e) => setOperatorSpeechInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleOperatorSendSpeech()}
              placeholder={activeCall.status === 'connected_user' 
                ? "Speak to customer (live microphone active, or type message here)..." 
                : "Type message as operator (or click 'Take Over' to speak directly)..."}
              className="flex-1 h-10 bg-[#060a08] border border-slate-800 rounded-xl px-4 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-[#76d418]"
            />

            <button
              onClick={() => handleOperatorSendSpeech()}
              disabled={!operatorSpeechInput.trim()}
              className="h-10 px-5 rounded-xl bg-[#76d418] hover:bg-[#66bd14] disabled:opacity-40 text-slate-950 text-xs font-bold flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <Send className="w-3.5 h-3.5" />
              <span>Send Speech</span>
            </button>
          </div>
        </div>
      )}

      {/* 4. SUB-TAB VIEWS: Switchboard / Customer Simulator / Logs */}
      {activeSubTab === 'switchboard' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Keypad Dialer Column (1 col) */}
          <div className="bg-[#091016] border border-slate-800 rounded-2xl p-5 flex flex-col items-center justify-between space-y-4 shadow-xl">
            <div className="w-full">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-400 mb-1.5">
                <span>Target Phone Line</span>
                <span className="text-[#76d418] font-mono text-[11px]">SIP Trunk Active</span>
              </div>

              {/* Number display */}
              <div className="w-full h-12 bg-[#060a08] border border-slate-700/80 rounded-xl px-4 flex items-center justify-between text-base font-mono text-white tracking-widest overflow-hidden">
                <span className="truncate">{dialedNumber || 'Enter number...'}</span>
                {dialedNumber && (
                  <button
                    onClick={() => setDialedNumber('')}
                    className="text-slate-500 hover:text-slate-300 text-xs font-sans ml-2 cursor-pointer"
                  >
                    Clear
                  </button>
                )}
              </div>

              <div className="mt-2">
                <input
                  type="text"
                  value={outboundCallerName}
                  onChange={(e) => setOutboundCallerName(e.target.value)}
                  placeholder="Target Contact / Entity"
                  className="w-full h-8 bg-[#060a08] border border-slate-800 rounded-lg px-2.5 text-[11px] text-slate-300 placeholder-slate-600 focus:outline-none focus:border-[#76d418]"
                />
              </div>
            </div>

            {/* Keypad Buttons with DTMF sound */}
            <div className="grid grid-cols-3 gap-2.5 w-full max-w-[260px]">
              {['1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#'].map((digit) => (
                <button
                  key={digit}
                  onClick={() => handleKeyPress(digit)}
                  className="h-12 rounded-xl bg-[#060a08] border border-slate-800 hover:bg-slate-800 active:scale-95 text-sm font-bold text-white transition-all cursor-pointer flex flex-col items-center justify-center shadow-sm"
                >
                  <span>{digit}</span>
                </button>
              ))}
            </div>

            {/* Dial Action Buttons */}
            <div className="w-full max-w-[260px] space-y-2 pt-1">
              <button
                onClick={() => handleDialOutbound(false)}
                disabled={!dialedNumber.trim() || Boolean(activeCall)}
                className="w-full h-11 rounded-xl bg-emerald-500 hover:bg-emerald-400 disabled:opacity-40 text-slate-950 text-xs font-bold flex items-center justify-center gap-2 transition-all shadow-md shadow-emerald-900/20 cursor-pointer"
              >
                <PhoneCall className="w-4 h-4" />
                <span>Call Direct (Operator)</span>
              </button>

              <button
                onClick={() => handleDialOutbound(true)}
                disabled={!dialedNumber.trim() || Boolean(activeCall)}
                className="w-full h-11 rounded-xl bg-[#76d418] hover:bg-[#66bd14] disabled:opacity-40 text-slate-950 text-xs font-bold flex items-center justify-center gap-2 transition-all shadow-md shadow-[#76d418]/25 cursor-pointer"
              >
                <Bot className="w-4 h-4" />
                <span>Dispatch AI Calling Agent</span>
              </button>
            </div>
          </div>

          {/* Switchboard Management & Quick Call Simulator (2 cols) */}
          <div className="lg:col-span-2 space-y-5">
            {/* Quick Inbound Trigger */}
            <div className="bg-[#091016] border border-slate-800 rounded-2xl p-5 space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                <div>
                  <h3 className="text-sm font-bold text-white flex items-center gap-2">
                    <PhoneIncoming className="w-4 h-4 text-[#76d418]" />
                    <span>Inbound Telephony Dispatch Test</span>
                  </h3>
                  <p className="text-xs text-slate-400">
                    Test customer inbound calls to verify either human answering or AI receptionist responses
                  </p>
                </div>
                <span className="text-[11px] px-2.5 py-1 rounded-full bg-slate-800 text-slate-300 font-mono">
                  Line 1 Ready
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <button
                  onClick={() => {
                    setIncomingCall({
                      id: `call-${Date.now()}`,
                      callerName: 'Customer Inbound Line',
                      phoneNumber: '+1 (800) 555-0199',
                      company: 'Customer Inbound Service',
                      status: 'ringing',
                      answeredBy: null,
                      startTime: Date.now(),
                      transcript: [
                        { speaker: 'system', text: 'Inbound line diagnostic test call connected.', time: new Date().toLocaleTimeString() }
                      ],
                      whisperDirectives: []
                    });
                    if (!stopRingRef.current) stopRingRef.current = playPhoneRing();
                  }}
                  className="p-3.5 rounded-xl bg-[#060a08] border border-slate-800 hover:border-amber-500/60 text-left transition-all cursor-pointer group"
                >
                  <span className="text-[10px] font-bold uppercase text-amber-400 block mb-1">Standard Inbound</span>
                  <h4 className="text-xs font-bold text-white group-hover:text-amber-300 transition-colors">
                    Customer Inbound Line
                  </h4>
                  <p className="text-[11px] text-slate-400 mt-0.5">+1 (800) 555-0199 • General Line</p>
                </button>

                <button
                  onClick={() => {
                    setIncomingCall({
                      id: `call-${Date.now()}`,
                      callerName: 'Enterprise Client Line',
                      phoneNumber: '+1 (888) 555-0144',
                      company: 'Enterprise Client Inbound',
                      status: 'ringing',
                      answeredBy: null,
                      startTime: Date.now(),
                      transcript: [
                        { speaker: 'system', text: 'Enterprise client inbound line diagnostic test call initiated.', time: new Date().toLocaleTimeString() }
                      ],
                      whisperDirectives: []
                    });
                    if (!stopRingRef.current) stopRingRef.current = playPhoneRing();
                  }}
                  className="p-3.5 rounded-xl bg-[#060a08] border border-slate-800 hover:border-[#76d418]/60 text-left transition-all cursor-pointer group"
                >
                  <span className="text-[10px] font-bold uppercase text-[#76d418] block mb-1">Enterprise Line</span>
                  <h4 className="text-xs font-bold text-white group-hover:text-[#76d418] transition-colors">
                    Enterprise Client Line
                  </h4>
                  <p className="text-[11px] text-slate-400 mt-0.5">+1 (888) 555-0144 • Dedicated Trunk</p>
                </button>

                <button
                  onClick={() => {
                    setIncomingCall({
                      id: `call-${Date.now()}`,
                      callerName: 'Priority Operations Line',
                      phoneNumber: '+1 (877) 555-0122',
                      company: 'Priority Operations Inbound',
                      status: 'ringing',
                      answeredBy: null,
                      startTime: Date.now(),
                      transcript: [
                        { speaker: 'system', text: 'Priority operations line diagnostic test call initiated.', time: new Date().toLocaleTimeString() }
                      ],
                      whisperDirectives: []
                    });
                    if (!stopRingRef.current) stopRingRef.current = playPhoneRing();
                  }}
                  className="p-3.5 rounded-xl bg-[#060a08] border border-slate-800 hover:border-cyan-500/60 text-left transition-all cursor-pointer group"
                >
                  <span className="text-[10px] font-bold uppercase text-cyan-400 block mb-1">Priority Line</span>
                  <h4 className="text-xs font-bold text-white group-hover:text-cyan-300 transition-colors">
                    Priority Operations Line
                  </h4>
                  <p className="text-[11px] text-slate-400 mt-0.5">+1 (877) 555-0122 • Direct Dispatch</p>
                </button>
              </div>
            </div>

            {/* Telephony SIP & Webhook Integration Specs */}
            <div className="bg-[#091016] border border-slate-800 rounded-2xl p-5 space-y-4">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Layers className="w-4 h-4 text-[#76d418]" />
                <span>Twilio / Carrier SIP Trunk Integration Details</span>
              </h3>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                <div className="p-3.5 rounded-xl bg-[#060a08] border border-slate-800 space-y-1.5">
                  <span className="text-slate-400 font-semibold block text-[11px]">Inbound Voice Webhook</span>
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-mono text-slate-200 truncate">{window.location.origin}/api/telephony/incoming</span>
                    <button
                      onClick={handleCopyWebhook}
                      className="text-[#76d418] hover:text-white p-1"
                    >
                      <Copy className="w-3.5 h-3.5" />
                    </button>
                  </div>
                  <p className="text-[10px] text-slate-500">Attach to Twilio Console phone number under "Voice & Fax &gt; A Call Comes In"</p>
                </div>

                <div className="p-3.5 rounded-xl bg-[#060a08] border border-slate-800 space-y-1.5">
                  <span className="text-slate-400 font-semibold block text-[11px]">AI Receptionist Persona</span>
                  <div className="flex items-center gap-2">
                    <Bot className="w-4 h-4 text-[#76d418]" />
                    <span className="text-slate-200 font-bold">Aegis Executive Receptionist</span>
                  </div>
                  <p className="text-[10px] text-slate-500">Powered by Gemini 3.5 Flash server-side with natural conversational voice</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 5. SUB-TAB: CUSTOMER CALLER LINE PORTAL (Simulator) */}
      {activeSubTab === 'customer_portal' && (
        <div className="bg-[#091016] border border-slate-800 rounded-2xl p-6 shadow-xl space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800">
            <div>
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <Headphones className="w-5 h-5 text-[#76d418]" />
                <span>Customer Calling Experience Simulator</span>
              </h2>
              <p className="text-xs text-slate-400">
                Experience what your customers hear when they dial your business number. Speak or type to converse with your AI Receptionist or human operator.
              </p>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-400 font-mono">Dialing:</span>
              <span className="px-3 py-1 rounded-full bg-[#76d418]/15 border border-[#76d418]/40 text-[#76d418] text-xs font-mono font-bold">
                +1 (888) 550-RCOS
              </span>
            </div>
          </div>

          {/* Customer Setup Form */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="text-xs font-semibold text-slate-400 mb-1 block">Your Name (Customer)</label>
              <input
                type="text"
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                className="w-full h-10 bg-[#060a08] border border-slate-800 rounded-xl px-3 text-xs text-white focus:outline-none focus:border-[#76d418]"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-slate-400 mb-1 block">Company / Organization</label>
              <input
                type="text"
                value={customerCompany}
                onChange={(e) => setCustomerCompany(e.target.value)}
                className="w-full h-10 bg-[#060a08] border border-slate-800 rounded-xl px-3 text-xs text-white focus:outline-none focus:border-[#76d418]"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-slate-400 mb-1 block">Caller Phone Number</label>
              <input
                type="text"
                value={customerNumber}
                onChange={(e) => setCustomerNumber(e.target.value)}
                className="w-full h-10 bg-[#060a08] border border-slate-800 rounded-xl px-3 text-xs text-white focus:outline-none focus:border-[#76d418]"
              />
            </div>
          </div>

          {/* Call Trigger Button */}
          <div className="flex items-center justify-between p-4 bg-[#060a08] border border-slate-800 rounded-xl">
            <div className="space-y-0.5">
              <h4 className="text-xs font-bold text-white">Place Call to {orgName || 'RCOS Enterprise'} Line</h4>
              <p className="text-[11px] text-slate-400">
                This triggers the switchboard telephone ring. You or Aegis AI can answer.
              </p>
            </div>

            {!activeCall ? (
              <button
                onClick={handleCustomerDialBusiness}
                className="px-6 py-2.5 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 text-xs font-bold flex items-center gap-2 shadow-lg shadow-[#76d418]/25 transition-all cursor-pointer"
              >
                <PhoneCall className="w-4 h-4" />
                <span>Dial Business Line</span>
              </button>
            ) : (
              <div className="flex items-center gap-3">
                <span className="text-xs text-[#76d418] font-bold flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-[#76d418] animate-ping" />
                  <span>Call Connected ({activeCall.answeredBy === 'ai' ? 'Aegis AI' : 'Human Operator'})</span>
                </span>
                <button
                  onClick={() => handleTerminateCall(true)}
                  className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold flex items-center gap-1.5 transition-colors cursor-pointer"
                >
                  <PhoneOff className="w-3.5 h-3.5" />
                  <span>Hang Up</span>
                </button>
              </div>
            )}
          </div>

          {/* Interactive Customer Voice / Speech Console */}
          {activeCall && (
            <div className="p-5 bg-slate-950 rounded-2xl border border-slate-800 space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs">
                <div className="flex items-center gap-2">
                  {isMuted ? (
                    <MicOff className="w-4 h-4 text-rose-400" />
                  ) : (isAiSpeakingRef.current || isAiSpeaking()) ? (
                    <Volume2 className="w-4 h-4 text-amber-400 animate-pulse" />
                  ) : isMicListening ? (
                    <div className="relative flex items-center justify-center">
                      <Mic className="w-4 h-4 text-[#76d418]" />
                      <span className="absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full bg-[#76d418] animate-ping" />
                    </div>
                  ) : (
                    <Mic className="w-4 h-4 text-slate-400" />
                  )}
                  <span className="font-bold text-white">Customer Live Audio Channel</span>
                  <span className="text-[11px] text-[#76d418] font-mono">
                    {isMuted 
                      ? '(Muted)' 
                      : (isAiSpeakingRef.current || isAiSpeaking()) 
                      ? '(Aegis Speaking)' 
                      : isMicListening 
                      ? '(Live Mic Listening)' 
                      : ''}
                  </span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-slate-400 font-mono text-[11px]">
                    {activeCall.answeredBy === 'ai' ? 'Talking with Aegis AI Receptionist' : 'Talking with Operator'}
                  </span>
                  <button
                    onClick={() => setIsMuted(!isMuted)}
                    className={`px-2.5 py-1 rounded-lg text-[10px] font-bold transition-colors cursor-pointer ${
                      isMuted ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40' : 'bg-slate-900 border border-slate-800 text-slate-300 hover:text-white'
                    }`}
                  >
                    {isMuted ? 'Unmute Mic' : 'Mute Mic'}
                  </button>
                </div>
              </div>

              {/* Live interim speech preview */}
              {interimUserSpeech && (
                <div className="p-3 bg-[#76d418]/10 border border-[#76d418]/30 rounded-xl text-xs text-[#76d418] flex items-center gap-2 animate-pulse">
                  <span className="w-2 h-2 rounded-full bg-[#76d418] animate-ping" />
                  <span className="font-mono text-[11px] font-bold">Transcribing Live:</span>
                  <span className="italic">"{interimUserSpeech}..."</span>
                </div>
              )}

              {/* Speech input */}
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={customerSpeechInput}
                  onChange={(e) => setCustomerSpeechInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleCustomerSpeak()}
                  placeholder={isMicListening 
                    ? "Speak into your microphone hands-free, or type customer statement here..." 
                    : "Type what you would say as a customer (e.g., 'Hello, I need to schedule a meeting')..."}
                  className="flex-1 h-11 bg-[#060a08] border border-slate-800 rounded-xl px-4 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-[#76d418]"
                />

                <button
                  onClick={handleCustomerSpeak}
                  disabled={!customerSpeechInput.trim()}
                  className="h-11 px-6 rounded-xl bg-[#76d418] hover:bg-[#66bd14] disabled:opacity-40 text-slate-950 text-xs font-bold flex items-center gap-2 transition-colors cursor-pointer"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>Speak</span>
                </button>
              </div>

              {/* Quick Customer Prompts */}
              <div className="flex flex-wrap items-center gap-2 pt-1">
                <span className="text-[11px] text-slate-400">Quick speech prompts:</span>
                {[
                  "Can I speak with a human operator?",
                  "What services does RCOS provide?",
                  "We have an urgent database deployment incident.",
                  "Can you send me an enterprise quote?"
                ].map((prompt, i) => (
                  <button
                    key={i}
                    onClick={() => {
                      setCustomerSpeechInput(prompt);
                    }}
                    className="px-2.5 py-1 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-800 text-[11px] text-slate-300 transition-colors cursor-pointer"
                  >
                    "{prompt}"
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* 6. SUB-TAB: CALL LOGS & AI INTELLIGENCE */}
      {activeSubTab === 'logs' && (
        <div className="bg-[#091016] border border-slate-800 rounded-2xl p-6 shadow-xl space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800">
            <div>
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <FileText className="w-5 h-5 text-[#76d418]" />
                <span>Call Logs, Transcripts & AI Action Item Extraction</span>
              </h2>
              <p className="text-xs text-slate-400">
                Every call is automatically logged, sentiment scored, transcribed, and summarized by Gemini
              </p>
            </div>
            <span className="text-xs font-mono text-slate-400 bg-slate-900 px-3 py-1 rounded-full border border-slate-800">
              {calls.length} Total Records
            </span>
          </div>

          {calls.length === 0 ? (
            <div className="h-48 flex flex-col items-center justify-center text-center p-6 text-slate-500 text-xs border border-dashed border-slate-800 rounded-xl space-y-2">
              <Phone className="w-6 h-6 text-slate-600" />
              <p className="text-slate-400 font-medium">No recorded calls yet.</p>
              <p className="text-[11px] text-slate-500 max-w-sm">
                Use the Switchboard or Customer Simulator to place or answer a call. All transcripts and action items will be stored here.
              </p>
            </div>
          ) : (
            <div className="space-y-3.5">
              {calls.map((call) => (
                <div
                  key={call.id}
                  id={`call-log-${call.id}`}
                  className="p-4 rounded-xl bg-[#060a08] border border-slate-800 hover:border-slate-700 transition-colors space-y-3"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className={`w-9 h-9 rounded-xl flex items-center justify-center font-bold text-xs border ${
                        call.answeredBy === 'human'
                          ? 'bg-emerald-950/60 border-emerald-500/40 text-emerald-400'
                          : 'bg-cyan-950/60 border-cyan-500/40 text-cyan-400'
                      }`}>
                        {call.answeredBy === 'human' ? <UserCheck className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h4 className="text-xs font-bold text-white">{call.callerName}</h4>
                          <span className="text-[11px] text-slate-400 font-normal">({call.company})</span>
                          <span className={`text-[10px] px-2 py-0.2 rounded-full font-bold uppercase ${
                            call.sentiment === 'Positive'
                              ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                              : call.sentiment === 'Action Required'
                              ? 'bg-rose-500/15 text-rose-400 border border-rose-500/30'
                              : 'bg-slate-800 text-slate-300 border border-slate-700'
                          }`}>
                            {call.sentiment}
                          </span>
                        </div>
                        <span className="text-[11px] font-mono text-slate-400">{call.phoneNumber}</span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="text-xs font-mono text-slate-400">{call.duration}</span>
                      <span className="text-xs text-slate-500">•</span>
                      <span className="text-xs text-slate-400">{call.timestamp}</span>

                      {onDeleteCall && (
                        <button
                          onClick={() => onDeleteCall(call.id)}
                          className="p-1.5 text-slate-500 hover:text-rose-400 transition-colors ml-2 cursor-pointer"
                          title="Delete Call"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Summary */}
                  <p className="text-xs text-slate-300 leading-relaxed pl-12">
                    {call.summary}
                  </p>

                  {/* Action items extracted by Gemini */}
                  {call.actionItems && call.actionItems.length > 0 && (
                    <div className="pl-12 pt-1 flex flex-wrap items-center gap-2">
                      <span className="text-[10px] font-semibold uppercase text-slate-400">Action Items:</span>
                      {call.actionItems.map((item, idx) => (
                        <span 
                          key={idx} 
                          className="text-[11px] px-2.5 py-0.5 rounded-md bg-slate-900 border border-slate-800 text-slate-300 flex items-center gap-1"
                        >
                          <CheckCircle2 className="w-3 h-3 text-[#76d418]" />
                          <span>{item}</span>
                        </span>
                      ))}
                    </div>
                  )}

                  {/* Bottom Footer: Transcript View & Create Job */}
                  <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-400 pl-12">
                    <span>Handled By: <strong className="text-white">{call.agentRoutedTo}</strong></span>

                    <div className="flex items-center gap-2">
                      {onCreateJobFromCall && (
                        <button
                          onClick={() => onCreateJobFromCall({
                            title: `Follow Up: ${call.callerName} (${call.company})`,
                            summary: call.summary,
                            priority: call.sentiment === 'Action Required' ? 'Urgent' : 'High',
                            status: 'In Progress'
                          })}
                          className="px-3 py-1 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-800 text-[#76d418] hover:text-white text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
                        >
                          <PlusCircle className="w-3.5 h-3.5" />
                          <span>Convert to RCOS Job</span>
                        </button>
                      )}

                      {call.transcript && call.transcript.length > 0 && (
                        <button
                          onClick={() => setSelectedCallDetails(call)}
                          className="px-3 py-1 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 hover:text-white text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
                        >
                          <MessageSquare className="w-3.5 h-3.5" />
                          <span>View Full Transcript</span>
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 7. MODAL: Full Call Transcript Viewer */}
      {selectedCallDetails && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#091016] border border-slate-800 rounded-2xl max-w-2xl w-full p-6 space-y-4 shadow-2xl max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div>
                <h3 className="text-sm font-bold text-white">Call Transcript Record</h3>
                <span className="text-xs text-slate-400">
                  {selectedCallDetails.callerName} • {selectedCallDetails.phoneNumber} • {selectedCallDetails.duration}
                </span>
              </div>
              <button
                onClick={() => setSelectedCallDetails(null)}
                className="text-slate-400 hover:text-white text-xs font-semibold p-1"
              >
                Close
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-3 p-3 bg-[#060a08] border border-slate-800 rounded-xl">
              {selectedCallDetails.transcript && selectedCallDetails.transcript.length > 0 ? (
                selectedCallDetails.transcript.map((item, idx) => (
                  <div key={idx} className="space-y-1">
                    <div className="flex items-center gap-2 text-[10px] text-slate-500">
                      <span className="font-bold uppercase tracking-wider text-slate-400">
                        {item.speaker === 'customer' 
                          ? selectedCallDetails.callerName 
                          : item.speaker === 'operator' 
                          ? 'Operator' 
                          : item.speaker === 'ai' 
                          ? 'Aegis AI' 
                          : 'System'}
                      </span>
                      <span>•</span>
                      <span>{item.time || ''}</span>
                    </div>
                    <div className={`p-2.5 rounded-lg text-xs ${
                      item.speaker === 'customer'
                        ? 'bg-slate-900 text-slate-200'
                        : item.speaker === 'operator'
                        ? 'bg-emerald-950/40 text-emerald-200 border border-emerald-500/20'
                        : item.speaker === 'ai'
                        ? 'bg-cyan-950/40 text-cyan-200 border border-cyan-500/20'
                        : 'bg-slate-950 text-slate-400 font-mono text-[11px]'
                    }`}>
                      {item.text}
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-xs text-slate-500 text-center py-6">No transcript lines recorded.</p>
              )}
            </div>

            <div className="pt-2 flex justify-end">
              <button
                onClick={() => setSelectedCallDetails(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-bold transition-colors cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 8. MODAL: Twilio Voice & TwiML Media Stream Setup Guide */}
      {showTwilioModal && (
        <div className="fixed inset-0 bg-slate-950/85 backdrop-blur-md z-50 flex items-center justify-center p-4">
          <div className="bg-[#091016] border border-cyan-500/40 rounded-2xl max-w-3xl w-full p-6 space-y-5 shadow-2xl max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            {/* Modal Header */}
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-cyan-950/80 border border-cyan-700/60 flex items-center justify-center text-cyan-400">
                  <Sparkles className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Twilio Voice & Gemini Multi-Modal Stream Architecture</h3>
                  <p className="text-xs text-slate-400">Real-time bidirectional audio pipeline between Twilio PSTN and Gemini Live API</p>
                </div>
              </div>
              <button
                onClick={() => setShowTwilioModal(false)}
                className="text-slate-400 hover:text-white text-xs font-semibold px-2 py-1 rounded-lg bg-slate-900 border border-slate-800 transition-colors cursor-pointer"
              >
                Close
              </button>
            </div>

            {/* Modal Body */}
            <div className="flex-1 overflow-y-auto space-y-4 pr-1 text-xs">
              {/* Architecture Status Badge */}
              <div className="p-3.5 rounded-xl bg-[#060a08] border border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-slate-300">Carrier Service Status:</span>
                  <span className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold font-mono border ${
                    telephonyConfig?.hasTwilioCarrier
                      ? 'bg-emerald-950/60 text-emerald-400 border-emerald-500/40'
                      : 'bg-cyan-950/60 text-cyan-300 border-cyan-500/40'
                  }`}>
                    {telephonyConfig?.hasTwilioCarrier ? 'TWILIO CARRIER LINKED' : 'TWIML STREAM READY (VIRTUAL / LIVE)'}
                  </span>
                </div>
                <p className="text-slate-400 text-[11px] leading-relaxed">
                  The service layer routes both inbound and outbound voice calls using Twilio's bidirectional <code>&lt;Connect&gt;&lt;Stream&gt;</code> protocol. 
                  Incoming 8kHz mu-law audio is transcoded into 16kHz linear PCM for the Gemini Live agent, and Gemini's responses are streamed back to the phone line with real-time barge-in support.
                </p>
              </div>

              {/* Endpoint URLs */}
              <div className="space-y-3">
                <h4 className="font-bold text-white text-xs uppercase tracking-wider text-cyan-400">Webhooks & Media Stream URLs</h4>

                {/* 1. Outbound TwiML Stream */}
                <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-white">1. Outbound TwiML Stream Endpoint:</span>
                    <button
                      onClick={() => {
                        const url = telephonyConfig?.outboundTwimlUrl || `${window.location.origin}/api/telephony/twiml/outbound`;
                        navigator.clipboard.writeText(url);
                        setCopiedKey('outbound');
                        setTimeout(() => setCopiedKey(null), 2000);
                      }}
                      className="text-cyan-400 hover:text-cyan-300 flex items-center gap-1 font-mono text-[10px] cursor-pointer"
                    >
                      {copiedKey === 'outbound' ? <Check className="w-3 h-3 text-[#76d418]" /> : <Copy className="w-3 h-3" />}
                      <span>{copiedKey === 'outbound' ? 'Copied' : 'Copy'}</span>
                    </button>
                  </div>
                  <div className="font-mono text-[11px] text-cyan-300 bg-[#060a08] p-2 rounded-lg break-all border border-slate-800">
                    {telephonyConfig?.outboundTwimlUrl || `${window.location.origin}/api/telephony/twiml/outbound`}
                  </div>
                  <span className="text-[10px] text-slate-500">Twilio calls this URL to fetch the TwiML Stream attaching the outbound recipient to the Gemini agent.</span>
                </div>

                {/* 2. Inbound Voice Webhook */}
                <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-white">2. Inbound Voice Webhook ("A Call Comes In"):</span>
                    <button
                      onClick={() => {
                        const url = telephonyConfig?.webhookUrl || `${window.location.origin}/api/telephony/incoming`;
                        navigator.clipboard.writeText(url);
                        setCopiedKey('inbound');
                        setTimeout(() => setCopiedKey(null), 2000);
                      }}
                      className="text-cyan-400 hover:text-cyan-300 flex items-center gap-1 font-mono text-[10px] cursor-pointer"
                    >
                      {copiedKey === 'inbound' ? <Check className="w-3 h-3 text-[#76d418]" /> : <Copy className="w-3 h-3" />}
                      <span>{copiedKey === 'inbound' ? 'Copied' : 'Copy'}</span>
                    </button>
                  </div>
                  <div className="font-mono text-[11px] text-cyan-300 bg-[#060a08] p-2 rounded-lg break-all border border-slate-800">
                    {telephonyConfig?.webhookUrl || `${window.location.origin}/api/telephony/incoming`}
                  </div>
                  <span className="text-[10px] text-slate-500">Set this in your Twilio Console under your Phone Number's Voice configuration (HTTP POST).</span>
                </div>

                {/* 3. Bidirectional WebSocket Media Stream */}
                <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-white">3. Bidirectional Audio WebSocket (Media Stream):</span>
                    <button
                      onClick={() => {
                        const wsUrl = telephonyConfig?.mediaStreamWsUrl || `${window.location.origin.replace('http', 'ws')}/api/telephony/media-stream`;
                        navigator.clipboard.writeText(wsUrl);
                        setCopiedKey('ws');
                        setTimeout(() => setCopiedKey(null), 2000);
                      }}
                      className="text-cyan-400 hover:text-cyan-300 flex items-center gap-1 font-mono text-[10px] cursor-pointer"
                    >
                      {copiedKey === 'ws' ? <Check className="w-3 h-3 text-[#76d418]" /> : <Copy className="w-3 h-3" />}
                      <span>{copiedKey === 'ws' ? 'Copied' : 'Copy'}</span>
                    </button>
                  </div>
                  <div className="font-mono text-[11px] text-emerald-400 bg-[#060a08] p-2 rounded-lg break-all border border-slate-800">
                    {telephonyConfig?.mediaStreamWsUrl || `${window.location.origin.replace('http', 'ws')}/api/telephony/media-stream`}
                  </div>
                  <span className="text-[10px] text-slate-500">Embedded automatically inside the generated TwiML &lt;Stream&gt; tags.</span>
                </div>
              </div>

              {/* TwiML Stream XML Sample */}
              <div className="space-y-1.5 pt-1">
                <h4 className="font-bold text-white text-xs uppercase tracking-wider text-slate-300">Generated TwiML Stream Specification</h4>
                <div className="p-3 rounded-xl bg-[#060a08] border border-slate-800 font-mono text-[10px] text-slate-300 overflow-x-auto">
                  <pre>{`<?xml version="1.0" encoding="UTF-8"?>
<Response>
  <Say voice="Polly.Matthew">Connecting you to Aegis Gemini Multi-Modal AI Agent.</Say>
  <Connect>
    <Stream url="${telephonyConfig?.mediaStreamWsUrl || 'wss://your-domain/api/telephony/media-stream'}">
      <Parameter name="callId" value="outbound-1727038..." />
      <Parameter name="direction" value="outbound" />
      <Parameter name="agent" value="gemini-multimodal" />
    </Stream>
  </Connect>
</Response>`}</pre>
                </div>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="pt-3 border-t border-slate-800 flex items-center justify-between">
              <span className="text-[11px] text-slate-500">
                To route calls to external cellular phones, set <code>TWILIO_ACCOUNT_SID</code> and <code>TWILIO_AUTH_TOKEN</code> in your environment.
              </span>
              <button
                onClick={() => setShowTwilioModal(false)}
                className="px-4 py-2 rounded-xl bg-[#76d418] hover:bg-[#66bd14] text-slate-950 font-bold text-xs transition-colors cursor-pointer"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
