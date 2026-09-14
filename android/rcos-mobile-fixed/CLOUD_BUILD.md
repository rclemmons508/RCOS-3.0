# RCOS 3.0 Cloud APK Build

This repository includes `.github/workflows/build-apk.yml` which builds the new RCOS 3.0 debug APK directly from the current Android source code in `android/rcos-mobile-fixed` via GitHub Actions.

## Build
1. Push changes to GitHub (`main` or `master` branch) or go to **Actions** in GitHub.
2. Select **Build RCOS 3.0 Mobile APK** → **Run workflow**.
3. The workflow sets up Java 17, makes the Gradle wrapper executable, and executes `./gradlew assembleDebug` directly on the `android/rcos-mobile-fixed` source.
4. Download the generated `RCOS-3.0-Mobile-Debug-APK` artifact from the completed run.

## Direct Source Build
The build executes directly against the current repository source:
`cd android/rcos-mobile-fixed && ./gradlew assembleDebug`

No ZIP archives, precompiled binaries, or legacy packages are used.
