// Telephony Web Audio and Speech Synthesis Engine
// Generates realistic telephony signaling tones (Ring, DTMF, Connect, Busy) and synthesizes AI Receptionist speech

let audioCtx: AudioContext | null = null;

function getAudioContext(): AudioContext {
  if (!audioCtx || audioCtx.state === 'closed') {
    const AudioCtxClass = window.AudioContext || (window as any).webkitAudioContext;
    audioCtx = new AudioCtxClass();
  }
  if (audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  return audioCtx;
}

// DTMF Frequency Map (Standard Telephony Frequencies in Hz)
const DTMF_FREQS: Record<string, [number, number]> = {
  '1': [697, 1209], '2': [697, 1336], '3': [697, 1477],
  '4': [770, 1209], '5': [770, 1336], '6': [770, 1477],
  '7': [852, 1209], '8': [852, 1336], '9': [852, 1477],
  '*': [941, 1209], '0': [941, 1336], '#': [941, 1477]
};

// Play DTMF Touch-tone when pressing phone keypad
export function playDtmfTone(char: string, durationMs = 120): void {
  try {
    const freqs = DTMF_FREQS[char];
    if (!freqs) return;

    const ctx = getAudioContext();
    const now = ctx.currentTime;

    const osc1 = ctx.createOscillator();
    const osc2 = ctx.createOscillator();
    const gain = ctx.createGain();

    osc1.frequency.value = freqs[0];
    osc2.frequency.value = freqs[1];

    gain.gain.setValueAtTime(0.08, now);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + durationMs / 1000);

    osc1.connect(gain);
    osc2.connect(gain);
    gain.connect(ctx.destination);

    osc1.start(now);
    osc2.start(now);
    osc1.stop(now + durationMs / 1000);
    osc2.stop(now + durationMs / 1000);
  } catch (err) {
    console.warn('Audio DTMF playback error:', err);
  }
}

// Play Standard Telephony Ringing Tone (440Hz + 480Hz dual cadence)
export function playPhoneRing(): () => void {
  let isStopped = false;
  let timeoutId: number | null = null;

  try {
    const ctx = getAudioContext();

    const ringPulse = () => {
      if (isStopped) return;

      try {
        const now = ctx.currentTime;
        const osc1 = ctx.createOscillator();
        const osc2 = ctx.createOscillator();
        const gain = ctx.createGain();

        osc1.frequency.value = 440;
        osc2.frequency.value = 480;

        gain.gain.setValueAtTime(0, now);
        gain.gain.linearRampToValueAtTime(0.1, now + 0.05);
        gain.gain.setValueAtTime(0.1, now + 1.8);
        gain.gain.linearRampToValueAtTime(0, now + 2.0);

        osc1.connect(gain);
        osc2.connect(gain);
        gain.connect(ctx.destination);

        osc1.start(now);
        osc2.start(now);
        osc1.stop(now + 2.0);
        osc2.stop(now + 2.0);

        // Standard ring cycle: 2 seconds on, 4 seconds off
        timeoutId = window.setTimeout(ringPulse, 6000);
      } catch (e) {
        console.warn('Ring pulse error:', e);
      }
    };

    ringPulse();
  } catch (err) {
    console.warn('Ring tone error:', err);
  }

  return () => {
    isStopped = true;
    if (timeoutId) clearTimeout(timeoutId);
  };
}

// Play Call Connected Acoustic Chirp
export function playConnectTone(): void {
  try {
    const ctx = getAudioContext();
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.frequency.setValueAtTime(520, now);
    osc.frequency.exponentialRampToValueAtTime(880, now + 0.15);

    gain.gain.setValueAtTime(0.12, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.2);

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.start(now);
    osc.stop(now + 0.2);
  } catch (err) {
    console.warn('Connect tone error:', err);
  }
}

// Play Disconnect Busy Signal (480Hz + 620Hz dual cadence)
export function playDisconnectTone(): void {
  try {
    const ctx = getAudioContext();
    const now = ctx.currentTime;
    const osc1 = ctx.createOscillator();
    const osc2 = ctx.createOscillator();
    const gain = ctx.createGain();

    osc1.frequency.value = 480;
    osc2.frequency.value = 620;

    gain.gain.setValueAtTime(0.1, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.4);

    osc1.connect(gain);
    osc2.connect(gain);
    gain.connect(ctx.destination);

    osc1.start(now);
    osc2.start(now);
    osc1.stop(now + 0.4);
    osc2.stop(now + 0.4);
  } catch (err) {
    console.warn('Disconnect tone error:', err);
  }
}

let activeUtterance: SpeechSynthesisUtterance | null = null;
let speechWatchdogTimeout: number | null = null;

// Speech Synthesis for AI Receptionist (Aegis voice)
export function speakAiResponse(text: string, onEnd?: () => void): void {
  if (!('speechSynthesis' in window)) {
    if (onEnd) onEnd();
    return;
  }

  try {
    if (speechWatchdogTimeout) {
      clearTimeout(speechWatchdogTimeout);
      speechWatchdogTimeout = null;
    }
    window.speechSynthesis.cancel();

    // Clean markdown/bullet fragments
    const cleanText = text
      .replace(/[*_#`]/g, '')
      .replace(/\[.*?\]/g, '')
      .trim();

    if (!cleanText) {
      if (onEnd) onEnd();
      return;
    }

    const utterance = new SpeechSynthesisUtterance(cleanText);
    utterance.rate = 1.05;
    utterance.pitch = 1.0;

    // Pick best English voice if available
    const voices = window.speechSynthesis.getVoices();
    const preferredVoice = voices.find(v => 
      (v.name.includes('Google') || v.name.includes('Natural') || v.name.includes('Samantha') || v.name.includes('Daniel')) &&
      v.lang.startsWith('en')
    ) || voices.find(v => v.lang.startsWith('en'));

    if (preferredVoice) {
      utterance.voice = preferredVoice;
    }

    let hasEnded = false;
    const finish = () => {
      if (hasEnded) return;
      hasEnded = true;
      if (speechWatchdogTimeout) {
        clearTimeout(speechWatchdogTimeout);
        speechWatchdogTimeout = null;
      }
      activeUtterance = null;
      if (onEnd) onEnd();
    };

    utterance.onend = finish;
    utterance.onerror = finish;

    // Keep reference in module scope so Chrome doesn't garbage collect mid-speech
    activeUtterance = utterance;

    // Safety watchdog: estimated duration based on text length
    const wordCount = cleanText.split(/\s+/).length;
    const maxDurationMs = Math.max(3000, wordCount * 500);
    speechWatchdogTimeout = window.setTimeout(() => {
      if (!hasEnded) {
        finish();
      }
    }, maxDurationMs);

    window.speechSynthesis.speak(utterance);
  } catch (err) {
    console.warn('Speech synthesis error:', err);
    if (onEnd) onEnd();
  }
}

export function stopSpeech(): void {
  if (speechWatchdogTimeout) {
    clearTimeout(speechWatchdogTimeout);
    speechWatchdogTimeout = null;
  }
  activeUtterance = null;
  if ('speechSynthesis' in window) {
    window.speechSynthesis.cancel();
  }
}

export function isAiSpeaking(): boolean {
  return Boolean(activeUtterance) || ('speechSynthesis' in window && window.speechSynthesis.speaking);
}
