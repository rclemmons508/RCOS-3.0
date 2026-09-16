export type GeminiModelType = 'gemini-3.5-flash' | 'gemini-3.1-pro-preview' | 'gemini-3.1-flash-lite';

export interface ChatHistoryMessage {
  role: 'user' | 'model';
  content: string;
  timestamp?: string;
  sources?: { title: string; uri: string }[];
  modelUsed?: string;
}

export interface ChatCompletionResponse {
  text: string;
  sources?: { title: string; uri: string }[];
  modelUsed: string;
}

export interface SearchGroundingResponse {
  text: string;
  sources: { title: string; uri: string }[];
  query: string;
  modelUsed: string;
}

export class GeminiClientService {
  private static instance: GeminiClientService;

  public static getInstance(): GeminiClientService {
    if (!GeminiClientService.instance) {
      GeminiClientService.instance = new GeminiClientService();
    }
    return GeminiClientService.instance;
  }

  // Multi-turn Chat using Server Proxy (API keys stay strictly server-side)
  public async sendChatMessage(
    messages: ChatHistoryMessage[],
    model: GeminiModelType = 'gemini-3.5-flash',
    systemInstruction?: string,
    useSearchGrounding: boolean = false
  ): Promise<ChatCompletionResponse> {
    const res = await fetch('/api/gemini/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        messages: messages.map(m => ({
          role: m.role,
          content: m.content
        })),
        model,
        systemInstruction,
        useSearchGrounding
      })
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({ error: 'Request failed' }));
      throw new Error(errorData.error || `Gemini API returned status ${res.status}`);
    }

    return await res.json();
  }

  // Google Search Grounding with gemini-3.5-flash
  public async executeSearchGrounding(query: string): Promise<SearchGroundingResponse> {
    const res = await fetch('/api/gemini/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query })
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({ error: 'Search failed' }));
      throw new Error(errorData.error || `Search Grounding returned status ${res.status}`);
    }

    return await res.json();
  }

  // Check health and server configuration
  public async checkHealth(): Promise<{ status: string; hasGeminiKey: boolean }> {
    try {
      const res = await fetch('/api/health');
      if (!res.ok) return { status: 'error', hasGeminiKey: false };
      return await res.json();
    } catch {
      return { status: 'offline', hasGeminiKey: false };
    }
  }
}

export const geminiClientService = GeminiClientService.getInstance();
