# RCOS 2.0 - Complete Setup Guide for Demo

This guide walks you through setting up RCOS 2.0 as a fully functional demo app.

## Table of Contents
1. [Firebase Setup](#firebase-setup)
2. [Gemini API Configuration](#gemini-api-configuration)
3. [Building the App](#building-the-app)
4. [Cloud Functions Backend](#cloud-functions-backend)
5. [Demo Data Setup](#demo-data-setup)
6. [Testing & Deployment](#testing--deployment)

---

## Firebase Setup

### Step 1: Create Firebase Project
1. Go to https://console.firebase.google.com
2. Click "Add project"
3. Name it "RCOS-3.0"
4. Accept terms and create

### Step 2: Add Android App
1. In Firebase Console, click "Add app" → Android
2. Package name: `com.rcsolutions.rcos.app`
3. App nickname: RCOS Demo
4. SHA-1 fingerprint: Get yours with:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Download `google-services.json`
6. Place it in `app/google-services.json`

### Step 3: Enable Authentication
1. Go to Firebase Console → Authentication
2. Click "Get started"
3. Enable "Email/Password"
4. Click "Add new provider" → Google (optional)

### Step 4: Create Firestore Database
1. Go to Firebase Console → Firestore Database
2. Click "Create database"
3. Start in test mode (we'll secure it later)
4. Choose region: us-central1

### Step 5: Enable App Check
1. Go to Firebase Console → App Check
2. Click "Manage apps"
3. Register Android app
4. Choose "reCAPTCHA v3"

---

## Gemini API Configuration

### Step 1: Get API Key
1. Visit https://aistudio.google.com/app/apikeys
2. Click "Create API key in new project"
3. Copy the key

### Step 2: Add to .env File
1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
2. Edit `.env` and replace:
   ```
   GEMINI_API_KEY=your_actual_key_here

**Security:** Do not commit or distribute a real Gemini API key. Rotate any key previously included in a ZIP/APK or source repository. For production, route Gemini requests through a secured backend rather than embedding a long-lived key in the Android app.
   ```

### Step 3: Verify Configuration
The Secrets Gradle Plugin automatically reads from `.env` during build.

---

## Building the App

### Step 1: Install Dependencies

**macOS/Linux:**
```bash
# Install Java 11+
brew install openjdk@11
export JAVA_HOME=$(brew --prefix openjdk@11)

# Install Android SDK (via Android Studio or command line tools)
# Download from: https://developer.android.com/studio
```

**Windows:**
- Download Android Studio: https://developer.android.com/studio
- Install Java 11+: https://www.oracle.com/java/technologies/downloads/

### Step 2: Clone Repository
```bash
git clone https://github.com/rclemmons508/RCOS-3.0.git
cd RCOS-3.0/android/rcos-mobile-fixed
```

### Step 3: Configure Files
```bash
# Copy and configure .env file
cp .env.example .env
# Edit .env and add your Gemini API key

# Add google-services.json (from Firebase Setup Step 2)
# File should be at: app/google-services.json
```

### Step 4: Build Debug APK
```bash
# Clean build (first time)
./gradlew clean build

# Just build APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

**If build fails:**
```bash
# Check for errors
./gradlew clean build --stacktrace

# Update gradle wrapper
./gradlew wrapper --gradle-version=8.5
```

### Step 5: Install on Device
```bash
# Enable USB Debugging on device:
# Settings → Developer Options → USB Debugging → On

# Connect device via USB
adb devices

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Or let Android Studio handle it:
# Click Run → Run 'app'
```

---

## Cloud Functions Backend

See `BACKEND_SETUP.md` for detailed configuration.

---

## Demo Data Setup

See `DEMO_DATA_SETUP.md` for loading sample data.

---

## Testing & Deployment

### Test Checklist
- [ ] App launches without crashes
- [ ] Email/Password login works
- [ ] Can create new account
- [ ] Onboarding dialog appears and saves data
- [ ] Dashboard displays welcome message
- [ ] Chat screen loads Gemini responses
- [ ] Navigation between screens works
- [ ] Logout works

### View App Logs
```bash
# See all logs
adb logcat

# Filter for your app
adb logcat | grep com.rcsolutions.rcos.app

# See errors only
adb logcat | grep -i error
```

### Build Release APK (For Distribution)

**Step 1: Create Signing Key**
```bash
keytool -genkey -v -keystore ~/rcos-release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias rcos-key

# Enter password and key details when prompted
# Example:
# Keystore password: YourSecurePassword123
# Key alias: rcos-key
# Key password: YourSecurePassword123
```

**Step 2: Update build.gradle.kts**
Add this to `app/build.gradle.kts` in the `android` block:

```kotlin
signingConfigs {
    getByName("debug") {
        // existing debug config
    }
    create("release") {
        storeFile = file(System.getenv("HOME") + "/rcos-release.keystore")
        storePassword = "YourSecurePassword123"
        keyAlias = "rcos-key"
        keyPassword = "YourSecurePassword123"
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

**Step 3: Build Release APK**
```bash
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
# Size: ~50-100 MB
```

---

## Troubleshooting

### APK Build Fails
```bash
# Clear cache
./gradlew clean

# Rebuild with verbose output
./gradlew assembleDebug --stacktrace

# Check SDK versions
./gradlew --version
```

### App Crashes on Launch
```bash
# View crash logs
adb logcat | grep -A 20 -E "(AndroidRuntime|FATAL|Exception)"

# Common fixes:
# 1. Verify google-services.json exists in app/
# 2. Check .env file has GEMINI_API_KEY
# 3. Ensure Android API 24+ on test device
```

### Firebase Connection Issues
- Verify internet permission in `app/src/main/AndroidManifest.xml`
- Check Firebase project ID matches `google-services.json`
- Test internet: `adb shell ping google.com`

### Gemini API Errors
- Verify API key at https://aistudio.google.com/app/apikeys
- Check quota: https://aistudio.google.com/app/usage
- Test API: `curl -X POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=YOUR_KEY`

### Permission Denied Errors
```bash
# Grant permissions if needed
adb shell pm grant com.rcsolutions.rcos.app android.permission.RECORD_AUDIO
adb shell pm grant com.rcsolutions.rcos.app android.permission.INTERNET
```

---

## Summary Checklist

Before showing to customers:
- [ ] Firebase project created
- [ ] google-services.json added
- [ ] Gemini API key configured
- [ ] Debug APK builds successfully
- [ ] App installs and launches
- [ ] Authentication works
- [ ] Gemini chat responds
- [ ] Cloud Functions deployed (optional)
- [ ] Demo data loaded
- [ ] Release APK created
