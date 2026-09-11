# RCOS AI Backend — Free Setup

RCOS no longer uses Firebase Cloud Functions for Gemini. The Firebase Blaze plan is **not required** for the AI gateway.

## Architecture

Android app → Cloudflare Worker → Gemini API

Firebase Authentication remains in the Android app. The Worker verifies the Firebase ID token before sending a request to Gemini.

## Free setup

Use the `worker/` folder. Follow `worker/README.md` for the phone-only Cloudflare Dashboard setup.

The Worker stores `GEMINI_API_KEY` and `FIREBASE_WEB_API_KEY` as Cloudflare secrets. Never put the Gemini key into the Android APK or GitHub repository.

## GitHub deployment (optional)

If you prefer automatic deployment, add these GitHub Actions secrets:

- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`

Then run the **Deploy RCOS AI Gateway** workflow. Cloudflare's official GitHub Actions flow uses an API token and account ID.
