import express from "express";
import http from "http";
import path from "path";
import { createServer as createViteServer } from "vite";
import { WebSocketServer, WebSocket } from "ws";
import { GoogleGenAI, Modality, LiveServerMessage } from "@google/genai";
import { 
  TwilioVoiceService, 
  buildTwiMLWithMediaStream, 
  type TelephonyCallSession 
} from "./src/services/twilioVoiceService";

let geminiClient: GoogleGenAI | null = null;

function getGeminiClient(): GoogleGenAI | null {
  const key = process.env.GEMINI_API_KEY;
  if (!key) return null;
  if (!geminiClient) {
    geminiClient = new GoogleGenAI({
      apiKey: key,
      httpOptions: {
        headers: {
          "User-Agent": "aistudio-build"
        }
      }
    });
  }
  return geminiClient;
}

// Resilient Gemini helper with transient retry (503/429) and model fallback chain
async function generateContentWithRetryAndFallback(
  ai: GoogleGenAI,
  params: {
    model: string;
    contents: any;
    config?: any;
  },
  fallbackModels: string[] = ["gemini-flash-latest", "gemini-3.1-flash-lite"]
): Promise<{ response: any; modelUsed: string }> {
  let targetModel = params.model;
  // Upgrade older or non-standard model references
  if (targetModel === "gemini-3.5-flash" || !targetModel) {
    targetModel = "gemini-3.8-flash";
  }

  const modelCandidates = [targetModel, ...fallbackModels.filter(m => m !== targetModel)];
  let lastError: any = null;

  for (const modelToTry of modelCandidates) {
    for (let attempt = 0; attempt < 2; attempt++) {
      try {
        const response = await ai.models.generateContent({
          ...params,
          model: modelToTry
        });
        return { response, modelUsed: modelToTry };
      } catch (err: any) {
        lastError = err;
        const errMsg = String(err?.message || "");
        const status = err?.status || err?.code || 0;
        const isTransient = status === 503 || status === 429 || errMsg.includes("503") || errMsg.includes("429") || errMsg.includes("high demand") || errMsg.includes("UNAVAILABLE");

        if (isTransient && attempt === 0) {
          // Brief pause before single retry on same model
          await new Promise(resolve => setTimeout(resolve, 350));
          continue;
        }
        break; // Advance to fallback model
      }
    }
  }

  throw lastError;
}

function createLocalCallSummary(call: any, transcriptText: string) {
  const caller = call?.callerName || "Inbound Caller";
  const company = call?.company || "Direct Inbound";
  const numExchanges = transcriptText ? transcriptText.split("\n").filter(Boolean).length : 0;

  return {
    summary: `Telephone consultation completed with ${caller} (${company}). ${numExchanges > 0 ? `Captured ${numExchanges} dialog exchanges recorded in telephony logs.` : "Call logged into telephony registry."}`,
    sentiment: "Neutral",
    callerName: caller,
    company: company,
    actionItems: [
      `Review conversation notes for ${caller}`,
      "Follow up on scheduled client requirements"
    ],
    suggestedJob: {
      title: `Client Follow-up: ${caller}`,
      priority: "Normal",
      summary: `Review telephony conversation logs and follow up with ${caller} from ${company}.`
    }
  };
}

async function startServer() {
  const app = express();
  const server = http.createServer(app);

  // Dedicated WebSocket servers for Live API and Telephony stream
  const wssLive = new WebSocketServer({ noServer: true });
  const wssTelephony = new WebSocketServer({ noServer: true });

  // In-memory active telephony calls registry
  const activeTelephonyCalls = new Map<string, TelephonyCallSession>();
  const operatorClients = new Set<WebSocket>();
  const callerClients = new Map<string, WebSocket>();

  function broadcastToOperators(message: any) {
    const data = JSON.stringify(message);
    for (const client of operatorClients) {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data);
      }
    }
  }

  // Initialize Twilio Voice Service with real-time bidirectional media streaming to Gemini
  const twilioVoiceService = new TwilioVoiceService(
    activeTelephonyCalls,
    broadcastToOperators,
    getGeminiClient
  );

  server.on("upgrade", (request, socket, head) => {
    try {
      const url = new URL(request.url || "", `http://${request.headers.host || "localhost"}`);
      if (url.pathname === "/api/live") {
        wssLive.handleUpgrade(request, socket, head, (ws) => {
          wssLive.emit("connection", ws, request);
        });
      } else if (url.pathname === "/api/telephony/stream") {
        wssTelephony.handleUpgrade(request, socket, head, (ws) => {
          wssTelephony.emit("connection", ws, request);
        });
      } else if (url.pathname === "/api/telephony/media-stream") {
        twilioVoiceService.handleUpgrade(request, socket, head);
      } else {
        socket.destroy();
      }
    } catch {
      socket.destroy();
    }
  });

  const PORT = 3000;

  app.use(express.json({ limit: "10mb" }));
  app.use(express.urlencoded({ extended: true }));

  // API Health Endpoint (Never returns actual keys)
  app.get("/api/health", (_req, res) => {
    res.json({
      status: "ok",
      hasGeminiKey: Boolean(process.env.GEMINI_API_KEY),
      service: "RCOS Enterprise Backend & Telephony System",
      activeCallsCount: activeTelephonyCalls.size,
      timestamp: new Date().toISOString()
    });
  });

  // 1. Gemini Multi-turn Chat Endpoint (gemini-3.8-flash, gemini-3.1-pro-preview, gemini-3.1-flash-lite)
  app.post("/api/gemini/chat", async (req, res) => {
    try {
      const {
        messages,
        model = "gemini-3.8-flash",
        systemInstruction,
        useSearchGrounding = false
      } = req.body;

      const ai = getGeminiClient();
      if (!ai) {
        return res.status(503).json({
          error: "GEMINI_API_KEY is not configured in the server environment. Please configure it in Settings > Secrets."
        });
      }

      // Validated models per prompt requirements:
      let selectedModel = model;
      if (selectedModel === "gemini-3.5-flash" || !selectedModel) {
        selectedModel = "gemini-3.8-flash";
      }
      if (!["gemini-3.8-flash", "gemini-flash-latest", "gemini-3.1-pro-preview", "gemini-3.1-flash-lite"].includes(selectedModel)) {
        selectedModel = "gemini-3.8-flash";
      }

      // Prepare contents array
      const contents = (messages || []).map((m: any) => ({
        role: m.role === "user" ? "user" : "model",
        parts: [{ text: m.content || m.text || "" }]
      }));

      if (contents.length === 0) {
        return res.status(400).json({ error: "No messages provided in conversation history." });
      }

      const config: any = {};
      if (systemInstruction) {
        config.systemInstruction = systemInstruction;
      }

      if (useSearchGrounding) {
        config.tools = [{ googleSearch: {} }];
      }

      const { response, modelUsed } = await generateContentWithRetryAndFallback(
        ai,
        {
          model: selectedModel,
          contents,
          config
        },
        ["gemini-flash-latest", "gemini-3.1-flash-lite"]
      );

      const text = response.text || "";
      const groundingChunks = response.candidates?.[0]?.groundingMetadata?.groundingChunks || [];
      const sources = groundingChunks
        .filter((c: any) => c.web?.uri)
        .map((c: any) => ({
          title: c.web.title || c.web.uri,
          uri: c.web.uri
        }));

      return res.json({
        text,
        sources,
        modelUsed
      });
    } catch (err: any) {
      console.error("Gemini Chat API error:", err);
      return res.status(500).json({
        error: err.message || "Failed to generate conversation response via Gemini"
      });
    }
  });

  // 2. Google Search Grounding with gemini-3.8-flash
  app.post("/api/gemini/search", async (req, res) => {
    try {
      const { query } = req.body;
      if (!query || typeof query !== "string") {
        return res.status(400).json({ error: "Query string is required." });
      }

      const ai = getGeminiClient();
      if (!ai) {
        return res.status(503).json({
          error: "GEMINI_API_KEY is not configured in the server environment. Please configure it in Settings > Secrets."
        });
      }

      const { response, modelUsed } = await generateContentWithRetryAndFallback(
        ai,
        {
          model: "gemini-3.8-flash",
          contents: `Provide an accurate, grounded, real-time factual briefing with sources for: ${query}`,
          config: {
            tools: [{ googleSearch: {} }],
            systemInstruction: "You are the RCOS Market Intelligence & Search Grounding Agent. Use real-time Google Search data to deliver concise, factual briefings with accurate citations."
          }
        },
        ["gemini-flash-latest", "gemini-3.1-flash-lite"]
      );

      const text = response.text || "";
      const groundingChunks = response.candidates?.[0]?.groundingMetadata?.groundingChunks || [];
      const sources = groundingChunks
        .filter((c: any) => c.web?.uri)
        .map((c: any) => ({
          title: c.web.title || c.web.uri,
          uri: c.web.uri
        }));

      return res.json({
        text,
        sources,
        query,
        modelUsed
      });
    } catch (err: any) {
      console.error("Search Grounding API error:", err);
      return res.status(500).json({
        error: err.message || "Failed to execute Google Search Grounding"
      });
    }
  });

  // Telephony Configuration Endpoint
  app.get("/api/telephony/config", (req, res) => {
    const host = req.get("host") || `localhost:${PORT}`;
    const protocol = req.protocol === "https" || req.get("x-forwarded-proto") === "https" ? "https" : "http";
    const wsProto = protocol === "https" ? "wss" : "ws";
    const accountSid = process.env.TWILIO_ACCOUNT_SID;
    const hasTwilioCarrier = Boolean(accountSid && accountSid.startsWith("AC") && process.env.TWILIO_AUTH_TOKEN);

    res.json({
      phoneNumber: process.env.TWILIO_PHONE_NUMBER || "+1 (888) 550-RCOS",
      phoneDidFormatted: "+1 (888) 550-7267",
      companyName: "RCOS Enterprise Solutions",
      receptionistName: "Aegis Gemini Multi-Modal AI Agent",
      webhookUrl: `${protocol}://${host}/api/telephony/incoming`,
      outboundTwimlUrl: `${protocol}://${host}/api/telephony/twiml/outbound`,
      mediaStreamWsUrl: `${wsProto}://${host}/api/telephony/media-stream`,
      statusUrl: `${protocol}://${host}/api/telephony/status`,
      supportedModes: ["twilio_bidirectional_stream", "gemini_multimodal_agent", "user_answer", "auto_fallback"],
      hasTwilioCarrier,
      hasGeminiApiKey: Boolean(process.env.GEMINI_API_KEY),
      activeCalls: Array.from(activeTelephonyCalls.values())
    });
  });

  // XML escape helper for TwiML responses
  function escapeXml(unsafe: string): string {
    if (!unsafe) return "";
    return unsafe
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&apos;");
  }

  // Telephony Webhook for Incoming Calls (Twilio & Web Calling Portal compatible)
  app.post("/api/telephony/incoming", (req, res) => {
    try {
      const callerNumber = req.body.From || req.body.phoneNumber || "+1 (555) 234-8901";
      const callerName = req.body.CallerName || req.body.callerName || (req.body.From ? `Caller ${req.body.From}` : "Customer Inquiry");
      const company = req.body.company || (req.body.FromCity ? `${req.body.FromCity} Enterprise` : "Direct Inbound Caller");
      const issueSummary = req.body.issueSummary || "Incoming customer telephone call";

      const callSid = req.body.CallSid || "";
      const callId = callSid || `call-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
      const newCall: TelephonyCallSession = {
        id: callId,
        callerName,
        phoneNumber: callerNumber,
        company,
        status: "ringing",
        answeredBy: null,
        startTime: Date.now(),
        transcript: [
          { speaker: "system", text: `Incoming call received from ${callerNumber} (${callerName}). Attached to Gemini multi-modal media stream.`, time: new Date().toLocaleTimeString() }
        ],
        whisperDirectives: []
      };

      activeTelephonyCalls.set(callId, newCall);
      if (callSid && callSid !== callId) {
        activeTelephonyCalls.set(callSid, newCall);
      }

      // Broadcast ringing event to all operator dashboards
      broadcastToOperators({
        type: "INCOMING_CALL",
        call: newCall
      });

      // If requested by Twilio, return bidirectional TwiML Media Stream connecting to Gemini
      const isTwilio = Boolean(
        req.body.CallSid || 
        req.headers["x-twilio-signature"] ||
        req.headers.accept?.includes("xml") || 
        req.headers["content-type"]?.includes("urlencoded")
      );

      if (isTwilio) {
        const twiml = twilioVoiceService.createInboundTwiML(req, callId, callerNumber);
        res.type("text/xml").send(twiml);
      } else {
        res.json({
          status: "ringing",
          call: newCall
        });
      }
    } catch (err: any) {
      console.error("Error processing incoming call:", err);
      res.status(500).json({ error: "Failed to process incoming call" });
    }
  });

  // Twilio Outbound Call TwiML Webhook Endpoint
  // Twilio requests this URL when an outbound call connects, instructing it to attach to the Gemini Media Stream
  app.all("/api/telephony/twiml/outbound", (req, res) => {
    try {
      const callId = (req.query?.callId as string) || req.body?.callId || req.body?.CallSid || `outbound-${Date.now()}`;
      const to = (req.query?.to as string) || req.body?.To || req.body?.phoneNumber || "+15552348901";
      const purpose = (req.query?.purpose as string) || req.body?.purpose || "Executive Direct Telephony Call";

      const twiml = twilioVoiceService.createOutboundTwiML(req, callId, to, purpose);
      res.type("text/xml").send(twiml);
    } catch (err: any) {
      console.error("Error generating outbound TwiML:", err);
      res.status(500).type("text/xml").send(`<?xml version="1.0" encoding="UTF-8"?><Response><Say>Unable to establish Gemini AI media stream.</Say></Response>`);
    }
  });

  // Twilio Status Callback Endpoint (initiated, ringing, answered, completed)
  app.all("/api/telephony/status", (req, res) => {
    try {
      const callSid = req.body?.CallSid || req.query?.callSid;
      const callStatus = req.body?.CallStatus || req.query?.callStatus;
      const callId = (req.query?.callId as string) || callSid;

      if (callId && activeTelephonyCalls.has(callId)) {
        const call = activeTelephonyCalls.get(callId)!;
        if (callStatus === "in-progress" || callStatus === "answered") {
          call.status = "connected_ai";
        } else if (["completed", "busy", "no-answer", "failed", "canceled"].includes(callStatus)) {
          call.status = "ended";
          broadcastToOperators({
            type: "CALL_ENDED",
            callId: call.id,
            callStatus
          });
        }
      }
      res.sendStatus(200);
    } catch {
      res.sendStatus(200);
    }
  });

  // Telephony AI Receptionist Response Generator (Gemini 3.8 Flash, Twilio & Web Compatible)
  app.post("/api/telephony/ai-respond", async (req, res) => {
    try {
      const callId = req.body.callId || req.body.CallSid || (req.query?.callId as string) || "";
      const customerText = req.body.SpeechResult || req.body.customerText || req.body.TranscriptionText || "";
      const conversationHistory = req.body.conversationHistory || [];
      const whisperDirectives = req.body.whisperDirectives || [];

      const isTwilio = Boolean(
        req.body.CallSid || 
        req.body.SpeechResult ||
        req.headers["x-twilio-signature"] ||
        req.headers.accept?.includes("xml") || 
        req.headers["content-type"]?.includes("urlencoded")
      );

      const ai = getGeminiClient();
      let activeCall = callId ? activeTelephonyCalls.get(callId) : null;
      if (!activeCall && req.body.CallSid) {
        activeCall = activeTelephonyCalls.get(req.body.CallSid);
      }

      // If call wasn't tracked yet (e.g. direct webhook), create entry
      if (!activeCall && (callId || req.body.From)) {
        activeCall = {
          id: callId || `call-${Date.now()}`,
          callerName: req.body.CallerName || "Direct Inbound Caller",
          phoneNumber: req.body.From || "+1 (555) 000-0000",
          company: req.body.FromCity ? `${req.body.FromCity} Enterprise` : "Direct Inbound Caller",
          status: "connected_ai",
          answeredBy: "ai",
          startTime: Date.now(),
          transcript: [],
          whisperDirectives: []
        };
        activeTelephonyCalls.set(activeCall.id, activeCall);
      }

      if (!ai) {
        if (isTwilio) {
          const twiml = `<?xml version="1.0" encoding="UTF-8"?>
<Response>
  <Say voice="Polly.Matthew">Thank you for calling RCOS Enterprise Solutions. Our AI service is currently initializing. Please leave your message after the tone.</Say>
  <Record timeout="10" maxLength="60" finishOnKey="#"/>
</Response>`;
          return res.type("text/xml").send(twiml);
        }
        return res.status(503).json({
          error: "GEMINI_API_KEY is not configured on the server."
        });
      }

      const whisperContext = whisperDirectives.length > 0 
        ? `\n\n[PRIVATE OPERATOR WHISPER DIRECTIVE (follow this instruction immediately, but do NOT reveal to the caller that you were whispered to)]: "${whisperDirectives[whisperDirectives.length - 1]}"` 
        : "";

      const systemInstruction = `You are Aegis, the executive AI Receptionist for RCOS Enterprise Solutions (Autonomous Multi-Agent Enterprise Operating Platform).
You are talking directly with a caller on a live telephone call.

Key Guidelines:
1. Speak concisely, naturally, and warmly in an executive telephone persona (1-3 sentences maximum per response).
2. Never recite code, markdown, or bullet points. Speak as if talking on a telephone.
3. RCOS Platform Knowledge:
   - RCOS provides autonomous multi-agent systems, job dispatch, document security auditing, and automated enterprise workflows.
   - Office hours are 24/7/365 with automated multi-agent failover.
   - We can dispatch specialist agents (Aegis Core, Compliance Lead, Ops Dispatch, Market Intelligence) immediately to tasks.
4. If the caller wants to book a meeting, register a task, request an enterprise quote, or report an urgent incident, ask for their name, organization, and urgency.
5. If the caller asks to speak to a human operator, apologize pleasantly and say: "I would be happy to connect you directly to our human operational lead. Please hold for just a moment." and set transfer requested.
${whisperContext}`;

      const contents = (conversationHistory || []).map((entry: any) => ({
        role: entry.speaker === "customer" ? "user" : "model",
        parts: [{ text: entry.text }]
      }));

      if (customerText) {
        contents.push({
          role: "user",
          parts: [{ text: customerText }]
        });
      }

      let aiText = "Thank you for calling RCOS. How may I assist you today?";
      try {
        const { response } = await generateContentWithRetryAndFallback(
          ai,
          {
            model: "gemini-3.8-flash",
            contents: contents.length > 0 ? contents : [{ role: "user", parts: [{ text: "Hello, I am calling RCOS." }] }],
            config: {
              systemInstruction,
              temperature: 0.7,
            }
          },
          ["gemini-flash-latest", "gemini-3.1-flash-lite"]
        );
        aiText = response.text?.trim() || aiText;
      } catch (genErr: any) {
        console.warn("Telephony AI receptionist fallback speech applied:", genErr?.message || genErr);
        aiText = "Thank you for contacting RCOS Enterprise Solutions. I have recorded your message and our operational lead will follow up promptly.";
      }

      const shouldTransfer = aiText.toLowerCase().includes("connect you directly to our human") || 
                             aiText.toLowerCase().includes("please hold") || 
                             aiText.toLowerCase().includes("human operational lead");

      // Update call session transcript if activeCall is available
      if (activeCall) {
        const timeNow = new Date().toLocaleTimeString();
        if (customerText) {
          activeCall.transcript.push({
            speaker: "customer",
            text: customerText,
            time: timeNow
          });

          // Broadcast customer speech to connected operators
          broadcastToOperators({
            type: "TRANSCRIPT_UPDATE",
            callId: activeCall.id,
            speaker: "customer",
            text: customerText,
            time: timeNow
          });
        }
        activeCall.transcript.push({
          speaker: "ai",
          text: aiText,
          time: timeNow
        });

        // Broadcast speech update to connected clients
        broadcastToOperators({
          type: "AI_SPEECH",
          callId: activeCall.id,
          text: aiText,
          shouldTransfer
        });

        const customerWs = callerClients.get(activeCall.id);
        if (customerWs && customerWs.readyState === WebSocket.OPEN) {
          customerWs.send(JSON.stringify({
            type: "AI_SPEECH",
            callId: activeCall.id,
            text: aiText,
            shouldTransfer
          }));
        }
      }

      // If requested by Twilio, return valid TwiML XML
      if (isTwilio) {
        const safeAiText = escapeXml(aiText);
        const resolvedCallId = activeCall?.id || callId;
        if (shouldTransfer) {
          const twiml = `<?xml version="1.0" encoding="UTF-8"?>
<Response>
  <Say voice="Polly.Matthew">${safeAiText}</Say>
  <Say voice="Polly.Matthew">Transferring your call to our executive operations queue now. Please hold.</Say>
</Response>`;
          return res.type("text/xml").send(twiml);
        } else {
          const actionUrl = `/api/telephony/ai-respond?callId=${encodeURIComponent(resolvedCallId)}`;
          const twiml = `<?xml version="1.0" encoding="UTF-8"?>
<Response>
  <Say voice="Polly.Matthew">${safeAiText}</Say>
  <Gather input="speech" timeout="5" speechTimeout="auto" action="${escapeXml(actionUrl)}" method="POST">
    <Say voice="Polly.Matthew">How else may I assist you today?</Say>
  </Gather>
  <Say voice="Polly.Matthew">Thank you for calling RCOS Enterprise Solutions. Goodbye.</Say>
  <Hangup/>
</Response>`;
          return res.type("text/xml").send(twiml);
        }
      }

      res.json({
        aiText,
        callId: activeCall?.id || callId,
        shouldTransfer,
        status: "ok"
      });
    } catch (err: any) {
      console.error("Telephony AI receptionist route error:", err);
      const isTwilio = Boolean(
        req.body?.CallSid || 
        req.headers["x-twilio-signature"] ||
        req.headers.accept?.includes("xml") || 
        req.headers["content-type"]?.includes("urlencoded")
      );
      if (isTwilio) {
        return res.type("text/xml").send(`<?xml version="1.0" encoding="UTF-8"?>
<Response>
  <Say voice="Polly.Matthew">Thank you for calling RCOS Enterprise Solutions. Please hold for an operator.</Say>
</Response>`);
      }
      res.json({
        aiText: "Thank you for calling RCOS Enterprise. Your message is noted.",
        callId: req.body?.callId,
        shouldTransfer: false,
        status: "ok"
      });
    }
  });

  // Twilio Call Status Callback Webhook
  app.post("/api/telephony/status", (req, res) => {
    try {
      const callSid = req.body.CallSid;
      const callStatus = req.body.CallStatus; // 'completed', 'busy', 'no-answer', 'failed', 'canceled'
      const duration = req.body.CallDuration;

      console.log(`Twilio Call Status Webhook: Sid=${callSid}, Status=${callStatus}, Duration=${duration}s`);

      if (callSid) {
        const call = activeTelephonyCalls.get(callSid);
        if (call) {
          if (callStatus === "completed" || callStatus === "canceled" || callStatus === "failed") {
            call.status = "ended";
            call.transcript.push({
              speaker: "system",
              text: `Call ended by carrier network (${callStatus}, duration: ${duration || 0}s).`,
              time: new Date().toLocaleTimeString()
            });
            broadcastToOperators({
              type: "CALL_ENDED",
              callId: call.id,
              callStatus,
              duration
            });
          }
        }
      }

      res.type("text/xml").send("<Response/>");
    } catch (err: any) {
      console.error("Twilio status webhook error:", err);
      res.status(200).send("<Response/>");
    }
  });

  // Twilio Inbound & Outbound SMS Webhook
  app.post("/api/telephony/sms", async (req, res) => {
    try {
      const fromNumber = req.body.From;
      const toNumber = req.body.To;
      const bodyText = req.body.Body || req.body.text || "";

      if (fromNumber && bodyText) {
        const ai = getGeminiClient();
        let reply = "Thank you for contacting RCOS Enterprise Solutions. An operational director has received your message and will respond promptly.";

        if (ai) {
          try {
            const { response } = await generateContentWithRetryAndFallback(
              ai,
              {
                model: "gemini-3.8-flash",
                contents: [{
                  role: "user",
                  parts: [{
                    text: `You are Aegis, executive communications assistant for RCOS Enterprise Solutions. 
A customer texted our enterprise number: "${bodyText}".
Write a professional, concise SMS reply under 160 characters. Do not use emojis.`
                  }]
                }]
              },
              ["gemini-flash-latest", "gemini-3.1-flash-lite"]
            );
            reply = response.text?.trim() || reply;
          } catch (e) {
            console.warn("SMS AI reply fallback applied:", e);
          }
        }

        broadcastToOperators({
          type: "INCOMING_SMS",
          from: fromNumber,
          to: toNumber,
          body: bodyText,
          reply,
          timestamp: new Date().toISOString()
        });

        const twiml = `<?xml version="1.0" encoding="UTF-8"?>
<Response>
  <Message>${escapeXml(reply)}</Message>
</Response>`;
        return res.type("text/xml").send(twiml);
      }

      res.json({ status: "ok" });
    } catch (err: any) {
      console.error("SMS webhook error:", err);
      res.status(500).type("text/xml").send("<Response/>");
    }
  });

  // Telephony Summarization & Action Extraction Endpoint (gemini-3.8-flash with fallback)
  app.post("/api/telephony/summarize", async (req, res) => {
    const { callId, transcript = [] } = req.body;
    const activeCall = callId ? activeTelephonyCalls.get(callId) : null;
    const transcriptText = Array.isArray(transcript)
      ? transcript.map((t: any) => `${t.speaker?.toUpperCase() || "CALLER"}: ${t.text || ""}`).join("\n")
      : (typeof transcript === "string" ? transcript : "");

    try {
      const ai = getGeminiClient();
      if (!ai) {
        const fallback = createLocalCallSummary(activeCall, transcriptText);
        if (activeCall) {
          activeCall.summary = fallback.summary;
          activeCall.sentiment = fallback.sentiment;
          activeCall.actionItems = fallback.actionItems;
          activeCall.status = "ended";
        }
        return res.json(fallback);
      }

      const prompt = `Analyze this completed telephone call transcript for RCOS Enterprise Solutions and provide a JSON summary.
Transcript:
${transcriptText || "No audible dialogue recorded."}

Return pure JSON matching this exact structure:
{
  "summary": "2-3 sentence executive recap of the call, caller intent, and resolution",
  "sentiment": "Positive" | "Neutral" | "Action Required",
  "callerName": "Caller name or 'Unknown Caller'",
  "company": "Company name or 'Direct Inbound'",
  "actionItems": ["Action item 1", "Action item 2"],
  "suggestedJob": {
    "title": "Title of deliverable job to create",
    "priority": "Urgent" | "High" | "Normal",
    "summary": "Detailed task description for the assigned agent"
  }
}`;

      const { response } = await generateContentWithRetryAndFallback(
        ai,
        {
          model: "gemini-3.8-flash",
          contents: [{ role: "user", parts: [{ text: prompt }] }],
          config: {
            responseMimeType: "application/json"
          }
        },
        ["gemini-flash-latest", "gemini-3.1-flash-lite"]
      );

      let parsed: any = {};
      try {
        parsed = JSON.parse(response.text?.trim() || "{}");
      } catch {
        parsed = createLocalCallSummary(activeCall, transcriptText);
      }

      if (!parsed.summary) {
        parsed = { ...createLocalCallSummary(activeCall, transcriptText), ...parsed };
      }

      if (activeCall) {
        activeCall.summary = parsed.summary;
        activeCall.sentiment = parsed.sentiment || "Neutral";
        activeCall.actionItems = parsed.actionItems || [];
        activeCall.status = "ended";
      }

      return res.json(parsed);
    } catch (err: any) {
      console.warn("Telephony summarization fallback applied:", err?.message || err);
      const fallback = createLocalCallSummary(activeCall, transcriptText);
      if (activeCall) {
        activeCall.summary = fallback.summary;
        activeCall.sentiment = fallback.sentiment;
        activeCall.actionItems = fallback.actionItems;
        activeCall.status = "ended";
      }
      return res.json(fallback);
    }
  });

  // Telephony Outbound Call Endpoint (utilizes Twilio Voice Service & Gemini Media Stream)
  app.post("/api/telephony/outbound", async (req, res) => {
    try {
      const { phoneNumber, callerName = "Outbound Call", purpose } = req.body;
      const result = await twilioVoiceService.initiateOutboundCall({
        destinationNumber: phoneNumber,
        callerName,
        purpose,
        req
      });
      res.json(result);
    } catch (err: any) {
      console.error("Outbound call dispatch error:", err);
      res.status(500).json({ error: err.message || "Failed to initiate outbound call" });
    }
  });

  // Telephony Hangup Endpoint
  app.post("/api/telephony/hangup", (req, res) => {
    try {
      const { callId } = req.body;
      if (callId && activeTelephonyCalls.has(callId)) {
        const call = activeTelephonyCalls.get(callId)!;
        call.status = "ended";
        broadcastToOperators({
          type: "CALL_ENDED",
          callId
        });
        const customerWs = callerClients.get(callId);
        if (customerWs && customerWs.readyState === WebSocket.OPEN) {
          customerWs.send(JSON.stringify({ type: "CALL_ENDED", callId }));
        }
      }
      res.json({ status: "ended", callId });
    } catch (err: any) {
      res.status(500).json({ error: err.message || "Failed to terminate call" });
    }
  });

  // Telephony WebSocket Stream Controller
  wssTelephony.on("connection", (ws: WebSocket) => {
    let clientType: "operator" | "caller" | null = null;
    let boundCallId: string | null = null;

    ws.on("message", async (raw: any) => {
      try {
        const data = JSON.parse(raw.toString());

        switch (data.type) {
          case "REGISTER_OPERATOR": {
            clientType = "operator";
            operatorClients.add(ws);
            // Send current active calls state
            ws.send(JSON.stringify({
              type: "ACTIVE_CALLS_SYNC",
              calls: Array.from(activeTelephonyCalls.values())
            }));
            break;
          }

          case "REGISTER_CALLER": {
            clientType = "caller";
            boundCallId = data.callId;
            if (boundCallId) {
              callerClients.set(boundCallId, ws);
            }
            break;
          }

          case "ANSWER_CALL": {
            // Operator answered call personally
            const callId = data.callId;
            const call = activeTelephonyCalls.get(callId);
            if (call) {
              call.status = "connected_user";
              call.answeredBy = "human";
              call.transcript.push({
                speaker: "system",
                text: "Human Operator connected to live call.",
                time: new Date().toLocaleTimeString()
              });

              // Notify both operator and caller
              const payload = {
                type: "CALL_ANSWERED",
                callId,
                answeredBy: "human"
              };
              broadcastToOperators(payload);
              const customerWs = callerClients.get(callId);
              if (customerWs && customerWs.readyState === WebSocket.OPEN) {
                customerWs.send(JSON.stringify(payload));
              }
            }
            break;
          }

          case "AI_ANSWER_CALL": {
            // Operator or auto-attendant handed call to AI Receptionist
            const callId = data.callId;
            const call = activeTelephonyCalls.get(callId);
            if (call) {
              call.status = "connected_ai";
              call.answeredBy = "ai";
              const greeting = "Thank you for calling RCOS Enterprise Solutions. My name is Aegis, your AI receptionist. How may I assist you today?";
              call.transcript.push({
                speaker: "system",
                text: "AI Receptionist (Aegis) answered call.",
                time: new Date().toLocaleTimeString()
              });
              call.transcript.push({
                speaker: "ai",
                text: greeting,
                time: new Date().toLocaleTimeString()
              });

              const payload = {
                type: "CALL_ANSWERED",
                callId,
                answeredBy: "ai",
                greeting
              };
              broadcastToOperators(payload);
              const customerWs = callerClients.get(callId);
              if (customerWs && customerWs.readyState === WebSocket.OPEN) {
                customerWs.send(JSON.stringify(payload));
              }
            }
            break;
          }

          case "TAKE_OVER_CALL": {
            // Operator takes over from AI
            const callId = data.callId;
            const call = activeTelephonyCalls.get(callId);
            if (call) {
              call.status = "connected_user";
              call.answeredBy = "human";
              call.transcript.push({
                speaker: "system",
                text: "Operator barged in and took over call from AI receptionist.",
                time: new Date().toLocaleTimeString()
              });

              const payload = {
                type: "CALL_TAKEN_OVER",
                callId,
                message: "Human operator has joined the line."
              };
              broadcastToOperators(payload);
              const customerWs = callerClients.get(callId);
              if (customerWs && customerWs.readyState === WebSocket.OPEN) {
                customerWs.send(JSON.stringify(payload));
              }
            }
            break;
          }

          case "WHISPER_DIRECTIVE": {
            // Operator whispered private directive to AI
            const { callId, directive } = data;
            const call = activeTelephonyCalls.get(callId);
            if (call && directive) {
              call.whisperDirectives.push(directive);
              broadcastToOperators({
                type: "WHISPER_ACKNOWLEDGED",
                callId,
                directive
              });
            }
            break;
          }

          case "HOLD_CALL": {
            const callId = data.callId;
            const call = activeTelephonyCalls.get(callId);
            if (call) {
              call.status = "on_hold";
              const payload = { type: "CALL_HELD", callId };
              broadcastToOperators(payload);
              const customerWs = callerClients.get(callId);
              if (customerWs && customerWs.readyState === WebSocket.OPEN) {
                customerWs.send(JSON.stringify(payload));
              }
            }
            break;
          }

          case "UNHOLD_CALL": {
            const callId = data.callId;
            const call = activeTelephonyCalls.get(callId);
            if (call) {
              call.status = call.answeredBy === "ai" ? "connected_ai" : "connected_user";
              const payload = { type: "CALL_UNHELD", callId, status: call.status };
              broadcastToOperators(payload);
              const customerWs = callerClients.get(callId);
              if (customerWs && customerWs.readyState === WebSocket.OPEN) {
                customerWs.send(JSON.stringify(payload));
              }
            }
            break;
          }

          case "CUSTOMER_SPEECH": {
            // Customer spoke into microphone or submitted message
            const { callId, text } = data;
            const call = activeTelephonyCalls.get(callId);
            if (call && text) {
              call.transcript.push({
                speaker: "customer",
                text,
                time: new Date().toLocaleTimeString()
              });

              // Relay to operator
              broadcastToOperators({
                type: "TRANSCRIPT_UPDATE",
                callId,
                speaker: "customer",
                text
              });

              // If call is handled by AI, trigger AI response
              if (call.status === "connected_ai") {
                const ai = getGeminiClient();
                if (ai) {
                  const whisper = call.whisperDirectives.length > 0 
                    ? `[PRIVATE OPERATOR DIRECTIVE: ${call.whisperDirectives[call.whisperDirectives.length - 1]}]` 
                    : "";
                  const prompt = `You are Aegis, the AI Receptionist for RCOS Enterprise Solutions on a live telephone call.
Caller: "${text}"
${whisper ? whisper + "\n" : ""}
Reply warmly, concisely (1-2 sentences), and professionally over the telephone.`;

                  let aiReply = "I understand. Let me note that for our operational team.";
                  try {
                    const { response: resp } = await generateContentWithRetryAndFallback(
                      ai,
                      {
                        model: "gemini-3.8-flash",
                        contents: [{ role: "user", parts: [{ text: prompt }] }]
                      },
                      ["gemini-flash-latest", "gemini-3.1-flash-lite"]
                    );
                    aiReply = resp.text?.trim() || aiReply;
                  } catch (genErr: any) {
                    console.warn("WebSocket AI speech fallback applied:", genErr?.message || genErr);
                  }

                  call.transcript.push({
                    speaker: "ai",
                    text: aiReply,
                    time: new Date().toLocaleTimeString()
                  });

                  const aiPayload = {
                    type: "AI_SPEECH",
                    callId,
                    text: aiReply
                  };
                  broadcastToOperators(aiPayload);
                  const customerWs = callerClients.get(callId);
                  if (customerWs && customerWs.readyState === WebSocket.OPEN) {
                    customerWs.send(JSON.stringify(aiPayload));
                  }
                }
              }
            }
            break;
          }

          case "OPERATOR_SPEECH": {
            // Human operator spoke into microphone or typed response
            const { callId, text } = data;
            const call = activeTelephonyCalls.get(callId);
            if (call && text) {
              call.transcript.push({
                speaker: "operator",
                text,
                time: new Date().toLocaleTimeString()
              });

              // Relay to customer and other operators
              const payload = {
                type: "OPERATOR_SPEECH",
                callId,
                text
              };
              broadcastToOperators(payload);
              const customerWs = callerClients.get(callId);
              if (customerWs && customerWs.readyState === WebSocket.OPEN) {
                customerWs.send(JSON.stringify(payload));
              }
            }
            break;
          }

          case "END_CALL": {
            const callId = data.callId;
            const call = activeTelephonyCalls.get(callId);
            if (call) {
              call.status = "ended";
              const payload = { type: "CALL_ENDED", callId };
              broadcastToOperators(payload);
              const customerWs = callerClients.get(callId);
              if (customerWs && customerWs.readyState === WebSocket.OPEN) {
                customerWs.send(JSON.stringify(payload));
              }
            }
            break;
          }
        }
      } catch (err) {
        console.error("Telephony WebSocket message error:", err);
      }
    });

    ws.on("close", () => {
      if (clientType === "operator") {
        operatorClients.delete(ws);
      } else if (boundCallId) {
        callerClients.delete(boundCallId);
      }
    });
  });

  // 3. Real-Time Voice Conversation Bridge with gemini-3.8-live (Live API)
  wssLive.on("connection", async (clientWs: WebSocket) => {
    const ai = getGeminiClient();
    if (!ai) {
      clientWs.send(JSON.stringify({
        error: "GEMINI_API_KEY is not configured on the server. Please add it to your environment secrets."
      }));
      clientWs.close();
      return;
    }

    try {
      // Connect to Gemini Live API with gemini-3.8-live
      const session = await ai.live.connect({
        model: "gemini-3.8-live",
        config: {
          responseModalities: [Modality.AUDIO],
          speechConfig: {
            voiceConfig: { prebuiltVoiceConfig: { voiceName: "Zephyr" } }
          },
          systemInstruction: "You are RCOS Sovereign AI, an autonomous enterprise operational voice assistant. Speak concisely, authoritatively, and professionally. Respond quickly and help the executive manage agent workflows, jobs, and fleet operations."
        },
        callbacks: {
          onmessage: (message: LiveServerMessage) => {
            const audio = message.serverContent?.modelTurn?.parts?.[0]?.inlineData?.data;
            if (audio) {
              clientWs.send(JSON.stringify({ audio }));
            }
            if (message.serverContent?.interrupted) {
              clientWs.send(JSON.stringify({ interrupted: true }));
            }
          },
          onclose: () => {
            if (clientWs.readyState === WebSocket.OPEN) {
              clientWs.close();
            }
          }
        }
      });

      clientWs.on("message", (raw: any) => {
        try {
          const payload = JSON.parse(raw.toString());
          if (payload.audio) {
            session.sendRealtimeInput({
              audio: { data: payload.audio, mimeType: "audio/pcm;rate=16000" }
            });
          }
        } catch (err) {
          console.error("Error processing live audio payload:", err);
        }
      });

      clientWs.on("close", () => {
        try {
          session.close();
        } catch {
          // ignore cleanup errors
        }
      });
    } catch (err: any) {
      console.error("Gemini Live session error:", err);
      clientWs.send(JSON.stringify({
        error: err.message || "Failed to establish Live API session"
      }));
      clientWs.close();
    }
  });

  // Vite middleware for development
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa"
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*all", (_req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  server.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
