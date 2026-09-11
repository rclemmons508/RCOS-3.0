# RCOS cloud APK build

This project includes `.github/workflows/android-apk.yml`. It builds a debug APK in GitHub Actions without Android Studio on the user's device.

## Build
1. Create a GitHub repository and upload this project.
2. Open **Actions** → **Build RCOS Android APK** → **Run workflow**.
3. Wait for the job to finish.
4. Open the completed run and download the `rcos-mobile-debug-apk` artifact.

The workflow uses JDK 17 and Gradle 9.3.1, matching the current Android Gradle Plugin requirement for AGP 9.1.1.

## Release APK
A production release APK must be signed with a private release keystore. Do not put the keystore or passwords in the repository. Configure them as GitHub Actions secrets before adding a release build job.
