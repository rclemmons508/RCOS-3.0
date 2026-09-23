import { WebSocketServer, WebSocket } from "ws";
import type { IncomingMessage } from "http";
import type { Duplex } from "stream";
import { GoogleGenAI, Modality, LiveServerMessage } from "@google/genai";

// ============================================================================
// AUDIO CODEC & RESAMPLING UTILITIES (G.711 Mu-law <-> Linear PCM)
// ============================================================================

/**
 * Pre-computed G.711 mu-law to 16-bit linear PCM lookup table (256 entries).
 */
export const ULAW_TO_PCM16: Int16Array = new Int16Array(256);

(function initUlawTable() {
  for (let i = 0; i < 256; i++) {
    const ulaw = ~i & 0xff;
    const sign = ulaw & 0x80;
    const exponent = (ulaw >> 4) & 0x07;
    const mantissa = ulaw & 0x0f;
    let sample = ((mantissa << 3) + 0x84) << exponent;
    sample -= 0x84;
    ULAW_TO_PCM16[i] = sign !== 0 ? -sample : sample;
  }
})();

const BIAS = 0x84;
const CLIP = 32635;

/**
 * Compresses a 16-bit signed linear PCM sample into an 8-bit G.711 mu-law byte.
 */
export function linearToUlaw(sample: number): number {
  let sign = 0;
  if (sample < 0) {
    sign = 0x80;
    sample = -sample;
  }
  if (sample > CLIP) sample = CLIP;
  sample += BIAS;

  let exponent = 7;
  for (let expMask = 0x4000; (sample & expMask) === 0 && exponent > 0; expMask >>= 1) {
    exponent--;
  }
  const mantissa = (sample >> (exponent + 3)) & 0x0f;
  return ~(sign | (exponent << 4) | mantissa) & 0xff;
}

/**
 * Decodes an 8kHz G.711 mu-law buffer into 16-bit linear PCM (8kHz).
 */
export function decodeUlawToPcm16(ulawBuffer: Uint8Array): Int16Array {
  const pcm = new Int16Array(ulawBuffer.length);
  for (let i = 0; i < ulawBuffer.length; i++) {
    pcm[i] = ULAW_TO_PCM16[ulawBuffer[i]];
  }
  return pcm;
}

/**
 * Encodes a 16-bit linear PCM buffer into 8-bit G.711 mu-law.
 */
export function encodePcm16ToUlaw(pcmBuffer: Int16Array): Uint8Array {
  const ulaw = new Uint8Array(pcmBuffer.length);
  for (let i = 0; i < pcmBuffer.length; i++) {
    ulaw[i] = linearToUlaw(pcmBuffer[i]);
  }
  return ulaw;
}

/**
 * Resamples 8kHz linear PCM to 16kHz linear PCM via linear interpolation.
 */
export function resample8To16(pcm8: Int16Array): Int16Array {
  const pcm16 = new Int16Array(pcm8.length * 2);
  for (let i = 0; i < pcm8.length; i++) {
    const current = pcm8[i];
    const next = i + 1 < pcm8.length ? pcm8[i + 1] : current;
    pcm16[i * 2] = current;
    pcm16[i * 2 + 1] = Math.round((current + next) / 2);
  }
  return pcm16;
}

/**
 * Resamples 24kHz linear PCM (Gemini Live output default) down to 8kHz (Twilio standard).
 */
export function resample24To8(pcm24: Int16Array): Int16Array {
  const outLen = Math.floor(pcm24.length / 3);
  const pcm8 = new Int16Array(outLen);
  for (let i = 0; i < outLen; i++) {
    const idx = i * 3;
    const avg = Math.round((pcm24[idx] + (pcm24[idx + 1] || pcm24[idx]) + (pcm24[idx + 2] || pcm24[idx])) / 3);
    pcm8[i] = avg;
  }
  return pcm8;
}

/**
 * Resamples 16kHz linear PCM down to 8kHz (2:1 decimation with anti-aliasing average).
 */
export function resample16To8(pcm16: Int16Array): Int16Array {
  const outLen = Math.floor(pcm16.length / 2);
  const pcm8 = new Int16Array(outLen);
  for (let i = 0; i < outLen; i++) {
    const idx = i * 2;
    pcm8[i] = Math.round((pcm16[idx] + (pcm16[idx + 1] || pcm16[idx])) / 2);
  }
  return pcm8;
}

// ============================================================================
// TWIML XML BUILDER UTILITIES
// ============================================================================

export function escapeXml(unsafe: string): string {
  if (!unsafe) return "";
  return unsafe
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

export interface BuildTwiMLStreamOptions {
  wsUrl: string;
  callId: string;
  direction: "inbound" | "outbound";
  greetingText?: string;
  customParameters?: Record<string, string>;
}

/**
 * Generates production TwiML containing the <Connect><Stream> directive
 * to route the bidirectional audio channel directly to our Gemini Media Stream handler.
 */
export function buildTwiMLWithMediaStream(options: BuildTwiMLStreamOptions): string {
  const { wsUrl, callId, direction, greetingText, customParameters = {} } = options;

  let paramsXml = `      <Parameter name="callId" value="${escapeXml(callId)}" />\n`;
  paramsXml += `      <Parameter name="direction" value="${escapeXml(direction)}" />\n`;
  paramsXml += `      <Parameter name="agent" value="gemini-multimodal" />\n`;

  for (const [key, value] of Object.entries(customParameters)) {
    paramsXml += `      <Parameter name="${escapeXml(key)}" value="${escapeXml(value)}" />\n`;
  }

  const sayTag = greetingText
    ? `  <Say voice="Polly.Matthew">${escapeXml(greetingText)}</Say>\n`
    : "";

  return `<?xml version="1.0" encoding="UTF-8"?>
<Response>
${sayTag}  <Connect>
    <Stream url="${escapeXml(wsUrl)}">
${paramsXml}    </Stream>
  </Connect>
</Response>`;
}

// ============================================================================
// TYPES & INTERFACES
// ============================================================================

export interface TelephonyCallSession {
  id: string;
  callerName: string;
  phoneNumber: string;
  company: string;
  status: "ringing" | "connected_user" | "connected_ai" | "on_hold" | "ended";
  answeredBy: "human" | "ai" | null;
  startTime: number;
  transcript: { speaker: "customer" | "operator" | "ai" | "system"; text: string; time: string }[];
  whisperDirectives: string[];
  summary?: string;
  sentiment?: string;
  actionItems?: string[];
  isTwilioStreamActive?: boolean;
  twilioStreamSid?: string;
  twilioCallSid?: string;
}

export interface TwilioVoiceConfig {
  accountSid?: string;
  authToken?: string;
  phoneNumber?: string;
  geminiApiKey?: string;
}

// ============================================================================
// TWILIO VOICE SERVICE LAYER
// ============================================================================

export class TwilioVoiceService {
  private wssMediaStream: WebSocketServer;
  private activeCalls: Map<string, TelephonyCallSession>;
  private broadcastToOperators: (msg: any) => void;
  private getGeminiClient: () => GoogleGenAI | null;

  constructor(
    activeCalls: Map<string, TelephonyCallSession>,
    broadcastToOperators: (msg: any) => void,
    getGeminiClient: () => GoogleGenAI | null
  ) {
    this.activeCalls = activeCalls;
    this.broadcastToOperators = broadcastToOperators;
    this.getGeminiClient = getGeminiClient;

    this.wssMediaStream = new WebSocketServer({ noServer: true });
    this.setupMediaStreamHandlers();
  }

  /**
   * Upgrades incoming WebSocket connections targeting /api/telephony/media-stream
   */
  public handleUpgrade(request: IncomingMessage, socket: Duplex, head: Buffer) {
    this.wssMediaStream.handleUpgrade(request, socket, head, (ws) => {
      this.wssMediaStream.emit("connection", ws, request);
    });
  }

  /**
   * Helper to derive the public WebSocket URL from the incoming HTTP request.
   */
  public getMediaStreamWsUrl(req: { get: (name: string) => string | undefined; protocol?: string }): string {
    const host = req.get("host") || "localhost:3000";
    const isHttps = req.protocol === "https" || req.get("x-forwarded-proto") === "https";
    const wsProto = isHttps ? "wss" : "ws";
    return `${wsProto}://${host}/api/telephony/media-stream`;
  }

  /**
   * Handles Inbound TwiML generation with the Gemini Multimodal Media Stream
   */
  public createInboundTwiML(req: any, callId: string, callerNumber: string): string {
    const wsUrl = this.getMediaStreamWsUrl(req);
    return buildTwiMLWithMediaStream({
      wsUrl,
      callId,
      direction: "inbound",
      greetingText: "Thank you for calling RCOS Enterprise Solutions. Connecting you directly to Aegis, our real-time Gemini AI agent.",
      customParameters: {
        callerNumber,
        provider: "twilio-bidirectional-stream"
      }
    });
  }

  /**
   * Handles Outbound TwiML generation to connect placed calls directly to the Gemini Agent
   */
  public createOutboundTwiML(req: any, callId: string, destinationNumber: string, purpose?: string): string {
    const wsUrl = this.getMediaStreamWsUrl(req);
    return buildTwiMLWithMediaStream({
      wsUrl,
      callId,
      direction: "outbound",
      greetingText: "Hello. This is Aegis connecting on behalf of RCOS Enterprise Solutions.",
      customParameters: {
        destinationNumber,
        purpose: purpose || "Outbound Operational Telephony Dispatch",
        provider: "twilio-bidirectional-stream"
      }
    });
  }

  /**
   * Dispatches an actual outbound call using Twilio REST API if credentials are provided.
   * If credentials are not configured, returns instructions and provisions a live stream session.
   */
  public async initiateOutboundCall(options: {
    destinationNumber: string;
    callerName?: string;
    purpose?: string;
    req: any;
  }): Promise<{
    success: boolean;
    callId: string;
    callSid?: string;
    mode: "twilio_carrier_placed" | "virtual_stream_prepared";
    message: string;
    twimlUrl: string;
    mediaStreamWsUrl: string;
    call: TelephonyCallSession;
  }> {
    const { destinationNumber, callerName = "Outbound Call", purpose, req } = options;

    const host = req.get("host") || "localhost:3000";
    const protocol = req.protocol === "https" || req.get("x-forwarded-proto") === "https" ? "https" : "http";
    const callId = `outbound-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`;
    const twimlUrl = `${protocol}://${host}/api/telephony/twiml/outbound?callId=${encodeURIComponent(callId)}&to=${encodeURIComponent(destinationNumber)}`;
    const mediaStreamWsUrl = this.getMediaStreamWsUrl(req);

    const newCall: TelephonyCallSession = {
      id: callId,
      callerName,
      phoneNumber: destinationNumber,
      company: "Outbound Telephony Dispatch",
      status: "connected_ai",
      answeredBy: "ai",
      startTime: Date.now(),
      transcript: [
        {
          speaker: "system",
          text: `Outbound call initiated to ${destinationNumber} via Gemini multi-modal stream. Purpose: ${purpose || "Direct Telephony Dispatch"}.`,
          time: new Date().toLocaleTimeString()
        }
      ],
      whisperDirectives: [],
      isTwilioStreamActive: false
    };

    this.activeCalls.set(callId, newCall);

    // Check for real Twilio credentials in environment
    const accountSid = process.env.TWILIO_ACCOUNT_SID;
    const authToken = process.env.TWILIO_AUTH_TOKEN;
    const twilioPhoneNumber = process.env.TWILIO_PHONE_NUMBER || "+18885507267";

    if (accountSid && authToken && accountSid.startsWith("AC")) {
      try {
        const auth = Buffer.from(`${accountSid}:${authToken}`).toString("base64");
        const bodyParams = new URLSearchParams();
        bodyParams.append("To", destinationNumber);
        bodyParams.append("From", twilioPhoneNumber);
        bodyParams.append("Url", twimlUrl);
        bodyParams.append("StatusCallback", `${protocol}://${host}/api/telephony/status?callId=${encodeURIComponent(callId)}`);
        bodyParams.append("StatusCallbackEvent", "initiated");
        bodyParams.append("StatusCallbackEvent", "ringing");
        bodyParams.append("StatusCallbackEvent", "answered");
        bodyParams.append("StatusCallbackEvent", "completed");

        const twilioRes = await fetch(
          `https://api.twilio.com/2010-04-01/Accounts/${accountSid}/Calls.json`,
          {
            method: "POST",
            headers: {
              Authorization: `Basic ${auth}`,
              "Content-Type": "application/x-www-form-urlencoded"
            },
            body: bodyParams.toString()
          }
        );

        const twilioData = (await twilioRes.json()) as any;

        if (twilioRes.ok && twilioData.sid) {
          newCall.twilioCallSid = twilioData.sid;
          this.activeCalls.set(twilioData.sid, newCall);

          this.broadcastToOperators({
            type: "OUTBOUND_CALL_STARTED",
            call: newCall,
            twilioCallSid: twilioData.sid
          });

          return {
            success: true,
            callId,
            callSid: twilioData.sid,
            mode: "twilio_carrier_placed",
            message: `Outbound carrier call placed to ${destinationNumber} via Twilio. TwiML media stream attached to Gemini multi-modal agent.`,
            twimlUrl,
            mediaStreamWsUrl,
            call: newCall
          };
        } else {
          console.warn("Twilio API returned error, falling back to virtual stream:", twilioData);
        }
      } catch (carrierErr: any) {
        console.warn("Failed to place call via Twilio carrier REST API:", carrierErr.message);
      }
    }

    // Virtual / simulation stream prepared
    this.broadcastToOperators({
      type: "OUTBOUND_CALL_STARTED",
      call: newCall
    });

    return {
      success: true,
      callId,
      mode: "virtual_stream_prepared",
      message: `Outbound call registered. Ready to stream bidirectional audio with Gemini multi-modal AI agent via TwiML stream. (Add TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN in environment to route through physical PSTN carriers).`,
      twimlUrl,
      mediaStreamWsUrl,
      call: newCall
    };
  }

  /**
   * Sets up the WebSocket connection handlers for Twilio Bidirectional Media Streams
   */
  private setupMediaStreamHandlers() {
    this.wssMediaStream.on("connection", async (twilioWs: WebSocket) => {
      let streamSid = "";
      let callSid = "";
      let callId = "";
      let direction = "inbound";
      let geminiLiveSession: any = null;
      let activeCall: TelephonyCallSession | null = null;
      let isGeminiConnected = false;
      let speechAccumulator = "";
      let lastTranscriptTime = 0;

      const ai = this.getGeminiClient();

      // Clean up helper
      const cleanup = () => {
        if (geminiLiveSession) {
          try {
            geminiLiveSession.close();
          } catch {}
          geminiLiveSession = null;
        }
        if (activeCall) {
          activeCall.isTwilioStreamActive = false;
        }
      };

      twilioWs.on("message", async (data: any) => {
        try {
          const msg = JSON.parse(data.toString());

          switch (msg.event) {
            case "connected": {
              // Initial connection handshake from Twilio Media Streams
              break;
            }

            case "start": {
              streamSid = msg.start?.streamSid || "";
              callSid = msg.start?.callSid || "";
              const customParams = msg.start?.customParameters || {};
              callId = customParams.callId || callSid || `call-${Date.now()}`;
              direction = customParams.direction || "inbound";

              // Link to active call in memory
              activeCall = this.activeCalls.get(callId) || this.activeCalls.get(callSid) || null;
              if (activeCall) {
                activeCall.twilioStreamSid = streamSid;
                activeCall.twilioCallSid = callSid;
                activeCall.isTwilioStreamActive = true;
                activeCall.status = "connected_ai";
                activeCall.answeredBy = "ai";
              }

              this.broadcastToOperators({
                type: "TWILIO_STREAM_CONNECTED",
                callId,
                streamSid,
                callSid,
                direction
              });

              // Initialize Gemini Live Multimodal Session
              if (ai) {
                try {
                  geminiLiveSession = await ai.live.connect({
                    model: "gemini-3.8-live",
                    config: {
                      responseModalities: [Modality.AUDIO],
                      speechConfig: {
                        voiceConfig: {
                          prebuiltVoiceConfig: { voiceName: "Puck" }
                        }
                      },
                      systemInstruction: `You are Aegis, the real-time voice and multi-modal AI agent for RCOS Enterprise Solutions. 
You are speaking directly with a customer on a live telephone line (via Twilio Media Stream).
Key instructions:
- Speak naturally, professionally, and warmly.
- Keep each spoken response short (1 to 2 sentences) so the conversation feels snappy, interactive, and human.
- Handle customer inquiries, appointments, job requests, and routing.
- Do not use markdown, emojis, asterisks, or bullet points in spoken output.
- If the customer pauses, wait politely.
${activeCall?.whisperDirectives?.length ? `Current operator directives: ${activeCall.whisperDirectives.join("; ")}` : ""}`
                    },
                    callbacks: {
                      onmessage: (serverMsg: LiveServerMessage) => {
                        // Handle audio returned from Gemini
                        const audioBase64 = serverMsg.serverContent?.modelTurn?.parts?.[0]?.inlineData?.data;
                        if (audioBase64 && twilioWs.readyState === WebSocket.OPEN && streamSid) {
                          try {
                            // Gemini returns 24kHz linear PCM base64.
                            // Convert to 8kHz G.711 mu-law for Twilio.
                            const pcmBuffer = Buffer.from(audioBase64, "base64");
                            const pcm24 = new Int16Array(
                              pcmBuffer.buffer,
                              pcmBuffer.byteOffset,
                              pcmBuffer.byteLength / 2
                            );

                            // Resample from 24kHz to 8kHz
                            const pcm8 = resample24To8(pcm24);

                            // Encode to 8-bit G.711 mu-law
                            const ulaw = encodePcm16ToUlaw(pcm8);
                            const mulawBase64 = Buffer.from(ulaw.buffer, ulaw.byteOffset, ulaw.byteLength).toString("base64");

                            // Send media chunk to Twilio
                            twilioWs.send(
                              JSON.stringify({
                                event: "media",
                                streamSid,
                                media: {
                                  payload: mulawBase64
                                }
                              })
                            );
                          } catch (transcodeErr) {
                            console.error("Transcoding error from Gemini to Twilio:", transcodeErr);
                          }
                        }

                        // Interruption handling (caller barged in / started speaking while Gemini was talking)
                        if (serverMsg.serverContent?.interrupted && twilioWs.readyState === WebSocket.OPEN && streamSid) {
                          // Clear Twilio's audio playback buffer immediately
                          twilioWs.send(
                            JSON.stringify({
                              event: "clear",
                              streamSid
                            })
                          );
                        }

                        // Capture transcript text if provided
                        const modelText = serverMsg.serverContent?.modelTurn?.parts?.[0]?.text;
                        if (modelText && activeCall) {
                          const time = new Date().toLocaleTimeString();
                          activeCall.transcript.push({
                            speaker: "ai",
                            text: modelText,
                            time
                          });
                          this.broadcastToOperators({
                            type: "TRANSCRIPT_UPDATE",
                            callId: activeCall.id,
                            speaker: "ai",
                            text: modelText,
                            time
                          });
                        }
                      },
                      onclose: () => {
                        isGeminiConnected = false;
                      }
                    }
                  });

                  isGeminiConnected = true;

                  // If outbound call, have Aegis speak initial greeting
                  if (direction === "outbound") {
                    geminiLiveSession.sendClientContent({
                      turns: [
                        {
                          role: "user",
                          parts: [
                            {
                              text: `[SYSTEM] The outbound telephone call has just connected to the recipient. Greet them warmly as Aegis from RCOS Enterprise Solutions and ask how you can assist them today.`
                            }
                          ]
                        }
                      ],
                      turnComplete: true
                    });
                  }
                } catch (liveErr: any) {
                  console.warn("Could not connect to Gemini Live API for Twilio Stream:", liveErr.message);
                }
              }
              break;
            }

            case "media": {
              // Incoming audio from caller (8kHz G.711 mu-law base64)
              const payload = msg.media?.payload;
              if (payload && geminiLiveSession && isGeminiConnected) {
                try {
                  const ulawBuffer = Buffer.from(payload, "base64");
                  // Decode 8kHz mu-law to 8kHz linear PCM
                  const pcm8 = decodeUlawToPcm16(ulawBuffer);
                  // Resample 8kHz to 16kHz linear PCM for Gemini
                  const pcm16 = resample8To16(pcm8);

                  const pcm16Buffer = Buffer.from(pcm16.buffer, pcm16.byteOffset, pcm16.byteLength);
                  const base64Pcm16 = pcm16Buffer.toString("base64");

                  // Stream real-time audio chunk into Gemini Live Session
                  geminiLiveSession.sendRealtimeInput({
                    audio: {
                      data: base64Pcm16,
                      mimeType: "audio/pcm;rate=16000"
                    }
                  });
                } catch (inputErr) {
                  console.error("Error processing Twilio media input chunk:", inputErr);
                }
              }
              break;
            }

            case "mark": {
              // Audio marker acknowledged by Twilio
              break;
            }

            case "stop": {
              cleanup();
              this.broadcastToOperators({
                type: "TWILIO_STREAM_STOPPED",
                callId,
                streamSid
              });
              break;
            }
          }
        } catch (msgErr) {
          console.error("Twilio media stream message error:", msgErr);
        }
      });

      twilioWs.on("close", () => {
        cleanup();
      });

      twilioWs.on("error", (err) => {
        console.error("Twilio media stream socket error:", err);
        cleanup();
      });
    });
  }
}
