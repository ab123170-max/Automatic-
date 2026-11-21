# AutoSwipe AI (Native Android – Kotlin & Jetpack Compose)

**Package / Application ID:** `com.autoswipe.ai.studio`  
**Min SDK:** Android 8.0 (API 26)  
**Target & Compile SDK:** Android 16 (API 36) — verified for Android 10 through Android 16  

AutoSwipe AI is a 100% on-device native Android application built in Kotlin with Jetpack Compose and Material 3. It monitors supported short-video apps via Android `AccessibilityService`, evaluates multi-signal end-of-video UI cues, waits a configurable delay (default `800 ms`), and dispatches a single upward swipe gesture (`350 ms` default).

---

## Project Architecture

- `model/` — `SupportedAppRegistry`, `SwipeSettings`, `EngineRuntimeState`, `ScreenRoute`
- `detection/` — Pure Kotlin `VideoEndDetectionEngine`, per-platform `PlatformDetectionStrategy` implementations (TikTok, TikTok Lite, Instagram Reels, YouTube Shorts, Facebook Reels, generic compatible apps), `SwipeGestureCalculator`
- `data/` — `SettingsRepository` (Jetpack DataStore Preferences) & `StatsDatabase` / `StatsRepository` (Room)
- `service/` — `AutoSwipeAccessibilityService`, `AutoSwipeForegroundService` (`specialUse`), `FloatingControlManager` (`TYPE_APPLICATION_OVERLAY`), `EngineCoordinator`, `PermissionHelper`
- `ui/` — Material 3 Dark Navy & Electric Blue theme, Onboarding flow, and all 9 screens (navigation drawer + top bar, no bottom navigation bar)

---

## Building the APK

### Prerequisites
- JDK 17
- Android SDK Platform 36 (`platforms;android-36`) & Build-Tools `36.0.0`
- Set `sdk.dir` in `local.properties` or export `ANDROID_HOME`

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
Output APK:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK
1. Create a release keystore (once, and keep it safely for future consistent updates):
   ```bash
   keytool -genkeypair -v -keystore release.keystore -alias autoswipe -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Create `keystore.properties` in the project root:
   ```properties
   storeFile=../release.keystore
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=autoswipe
   keyPassword=YOUR_KEY_PASSWORD
   ```
3. Build release:
   ```bash
   ./gradlew assembleRelease
   ```
