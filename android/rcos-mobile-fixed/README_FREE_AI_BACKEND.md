# RCOS Free AI Backend

This build is configured to use a Cloudflare Worker instead of Firebase Cloud Functions.

No Firebase Blaze upgrade is required.

1. Create a free Cloudflare Worker named `rcos-ai-gateway`.
2. Paste `worker/src/index.js` into the Worker editor.
3. Add the Google AI Studio Gemini API key as a Worker **Secret** named `GEMINI_API_KEY`.
4. Deploy.
5. Build the Android APK from the existing GitHub Actions workflow.

The Android app is already pointed at `https://rcos-ai-gateway.rclemmons1021.workers.dev`.
