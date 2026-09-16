import express from "express";
import http from "http";
import path from "path";
import { createServer as createViteServer } from "vite";
import { WebSocketServer, WebSocket } from "ws";
import { GoogleGenAI, Modality, LiveServerMessage } from "@google/genai";

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

async function startServer() {
  const app = express();
  const server = http.createServer(app);

  // Dedicated WebSocket servers for Live API and Telephony stream
  const wssLive = new WebSocketServer({ noServer: true });
  const wssTelephony = new WebSocketServer({ noServer: true });

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

  // In-memory active telephony calls registry
  interface TelephonyCallSession {
    id: string;
    callerName: string;
    phoneNumber: string;
    company: string;
    status: 'ringing' | 'connected_user' | 'connected_ai' | 'on_hold' | 'ended';
    answeredBy: 'human' | 'ai' | null;
    startTime: number;
    transcript: { speaker: 'customer' | 'operator' | 'ai' | 'system'; text: string; time: string }[];
    whisperDirectives: string[];
    summary?: string;
    sentiment?: string;
    actionItems?: string[];
  }

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

  // 1. Gemini Multi-turn Chat Endpoint (gemini-3.1-pro-preview, gemini-3.5-flash, gemini-3.1-flash-lite)
  app.post("/api/gemini/chat", async (req, res) => {
    try {
      const {
        messages,
        model = "gemini-3.5-flash",
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
      if (!["gemini-3.1-pro-preview", "gemini-3.5-flash", "gemini-3.1-flash-lite"].includes(selectedModel)) {
        selectedModel = "gemini-3.5-flash";
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

      const response = await ai.models.generateContent({
        model: selectedModel,
        contents,
        config
      });

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
        modelUsed: selectedModel
      });
    } catch (err: any) {
      console.error("Gemini Chat API error:", err);
      return res.status(500).json({
        error: err.message || "Failed to generate conversation response via Gemini"
      });
    }
  });

  // 2. Google Search Grounding with gemini-3.5-flash
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

      const response = await ai.models.generateContent({
        model: "gemini-3.5-flash",
        contents: `Provide an accurate, grounded, real-time factual briefing with sources for: ${query}`,
        config: {
          tools: [{ googleSearch: {} }],
          systemInstruction: "You are the RCOS Market Intelligence & Search Grounding Agent. Use real-time Google Search data to deliver concise, factual briefings with accurate citations."
        }
      });

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
        modelUsed: "gemini-3.5-flash"
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
    res.json({
      phoneNumber: "+1 (888) 550-RCOS",
      phoneDidFormatted: "+1 (888) 550-7267",
      companyName: "RCOS Enterprise Solutions",
      receptionistName: "Aegis AI Receptionist",
      webhookUrl: `${protocol}://${host}/api/telephony/incoming`,
      statusUrl: `${protocol}://${host}/api/telephony/status`,
      supportedModes: ["user_answer", "ai_receptionist", "auto_fallback"],
      activeCalls: Array.from(activeTelephonyCalls.values())
    });
  });

  // Telephony Webhook for Incoming Calls (Twilio & Web Calling Portal compatible)
  app.post("/api/telephony/incoming", (req, res) => {
    try {
      const callerNumber = req.body.From || req.body.phoneNumber || "+1 (555) 234-8901";
      const callerName = req.body.CallerName || req.body.callerName || "Customer Inquiry";
      const company = req.body.company || (req.body.FromCity ? `${req.body.FromCity} Enterprise` : "Direct Inbound Caller");
      const issueSummary = req.body.issueSummary || "Incoming customer telephone call";

      const callId = `call-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
      const newCall: TelephonyCallSession = {
        id: callId,
        callerName,
        phoneNumber: callerNumber,
        company,
        status: "ringing",
        answeredBy: null,
        startTime: Date.now(),
        transcript: [
          { speaker: "system", text: `Incoming call received from ${callerNumber} (${callerName}). Telephone line ringing...`, time: new Date().toLocaleTimeString() }
        ],
        whisperDirectives: []
      };

      activeTelephonyCalls.set(callId, newCall);

      // Broadcast ringing event to all operator dashboards
      broadcastToOperators({
        type: "INCOMING_CALL",
        call: newCall
      });

      // If requested by Twilio (accepts XML or has CallSid), return TwiML
      if (req.body.CallSid || req.headers.accept?.includes("xml") || req.headers["content-type"]?.includes("urlencoded")) {
        const twiml = `<?xml version="1.0" encoding="UTF-8"?>
<Response>
  <Say voice="Polly.Matthew">Thank you for calling RCOS Enterprise Solutions. Connecting your call to our operational team or AI receptionist.</Say>
  <Gather input="speech" timeout="5" action="/api/telephony/ai-respond" method="POST">
    <Say>Please state how we may assist you today.</Say>
  </Gather>
</Response>`;
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

  // Telephony AI Receptionist Response Generator (Gemini 3.5 Flash)
  app.post("/api/telephony/ai-respond", async (req, res) => {
    try {
      const {
        callId,
        customerText,
        conversationHistory = [],
        whisperDirectives = []
      } = req.body;

      const ai = getGeminiClient();
      if (!ai) {
        return res.status(503).json({
          error: "GEMINI_API_KEY is not configured on the server."
        });
      }

      const activeCall = callId ? activeTelephonyCalls.get(callId) : null;
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

      const response = await ai.models.generateContent({
        model: "gemini-3.5-flash",
        contents,
        config: {
          systemInstruction,
          temperature: 0.7,
        }
      });

      const aiText = response.text?.trim() || "Thank you for calling RCOS. How may I assist you today?";
      const shouldTransfer = aiText.toLowerCase().includes("connect you directly to our human") || 
                             aiText.toLowerCase().includes("please hold") || 
                             aiText.toLowerCase().includes("human operational lead");

      // Update call session transcript if callId is provided
      if (activeCall) {
        if (customerText) {
          activeCall.transcript.push({
            speaker: "customer",
            text: customerText,
            time: new Date().toLocaleTimeString()
          });
        }
        activeCall.transcript.push({
          speaker: "ai",
          text: aiText,
          time: new Date().toLocaleTimeString()
        });

        // Broadcast speech update to connected clients
        broadcastToOperators({
          type: "AI_SPEECH",
          callId,
          text: aiText,
          shouldTransfer
        });

        const customerWs = callerClients.get(callId);
        if (customerWs && customerWs.readyState === WebSocket.OPEN) {
          customerWs.send(JSON.stringify({
            type: "AI_SPEECH",
            callId,
            text: aiText,
            shouldTransfer
          }));
        }
      }

      res.json({
        aiText,
        callId,
        shouldTransfer,
        status: "ok"
      });
    } catch (err: any) {
      console.error("Telephony AI receptionist error:", err);
      res.status(500).json({ error: err.message || "Failed to generate AI response" });
    }
  });

  // Telephony Summarization & Action Extraction Endpoint (Gemini 3.5 Flash)
  app.post("/api/telephony/summarize", async (req, res) => {
    try {
      const { callId, transcript = [] } = req.body;
      const ai = getGeminiClient();
      if (!ai) {
        return res.status(503).json({ error: "GEMINI_API_KEY is not configured" });
      }

      const transcriptText = transcript
        .map((t: any) => `${t.speaker.toUpperCase()}: ${t.text}`)
        .join("\n");

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

      const response = await ai.models.generateContent({
        model: "gemini-3.5-flash",
        contents: [{ role: "user", parts: [{ text: prompt }] }],
        config: {
          responseMimeType: "application/json"
        }
      });

      const parsed = JSON.parse(response.text?.trim() || "{}");

      if (callId && activeTelephonyCalls.has(callId)) {
        const call = activeTelephonyCalls.get(callId)!;
        call.summary = parsed.summary;
        call.sentiment = parsed.sentiment;
        call.actionItems = parsed.actionItems;
        call.status = "ended";
      }

      res.json(parsed);
    } catch (err: any) {
      console.error("Telephony summarization error:", err);
      res.status(500).json({
        summary: "Telephone conversation completed and recorded in telemetry logs.",
        sentiment: "Neutral",
        actionItems: ["Review conversation logs", "Follow up if requested"],
        suggestedJob: null
      });
    }
  });

  // Telephony Outbound Call Endpoint
  app.post("/api/telephony/outbound", (req, res) => {
    try {
      const { phoneNumber, callerName = "Outbound Call", dispatchAi = false, purpose } = req.body;
      const callId = `outbound-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`;

      const newCall: TelephonyCallSession = {
        id: callId,
        callerName,
        phoneNumber,
        company: "Outbound Telephony Dispatch",
        status: dispatchAi ? "connected_ai" : "connected_user",
        answeredBy: dispatchAi ? "ai" : "human",
        startTime: Date.now(),
        transcript: [
          { speaker: "system", text: `Outbound connection established to ${phoneNumber}. Purpose: ${purpose || 'General Direct Telephony'}.`, time: new Date().toLocaleTimeString() }
        ],
        whisperDirectives: []
      };

      activeTelephonyCalls.set(callId, newCall);

      broadcastToOperators({
        type: "OUTBOUND_CALL_STARTED",
        call: newCall
      });

      res.json({
        status: "connected",
        call: newCall
      });
    } catch (err: any) {
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

                  const resp = await ai.models.generateContent({
                    model: "gemini-3.5-flash",
                    contents: [{ role: "user", parts: [{ text: prompt }] }]
                  });
                  const aiReply = resp.text?.trim() || "I understand. Let me note that for our operational team.";
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
