# AutoSwipe AI – Manual Real-Device Testing Checklist

> **Important Distinction**: The automated JVM unit tests in `app/src/test/` verify state-machine logic, confidence scoring, duplicate-swipe prevention, cooldown enforcement, package matching, and gesture geometry using simulated accessibility snapshots. **Actual UI inspection, gesture injection, overlay rendering, and OEM background survival can only be verified on physical Android hardware.**

---

## 1. Pre-Test Environment & Installation
- [ ] Build debug APK via `./gradlew assembleDebug` (artifact at `app/build/outputs/apk/debug/app-debug.apk`).
- [ ] Install on device via `adb install -r app/build/outputs/apk/debug/app-debug.apk` (verify separate package ID `com.autoswipe.ai.studio` installs cleanly without conflicting with older builds).
- [ ] On Android 13–16 (sideloaded APK): verify `Settings -> Apps -> AutoSwipe AI -> Allow restricted settings` works if Accessibility toggle is greyed out.

## 2. First-Time Setup & Permissions (Android 10 & Android 14–16)
- [ ] Launch AutoSwipe AI: Welcome onboarding screen appears first.
- [ ] Verify Accessibility Service is **OFF** by default and never silently enabled.
- [ ] Tap **Open Accessibility Settings** -> enable *AutoSwipe AI Engine* -> return to app -> status updates to **Verified / Connected**.
- [ ] Tap **Grant Overlay Permission** -> toggle **Allow display over other apps** -> return -> status updates to **Verified / Granted**.
- [ ] On Android 13+: verify `POST_NOTIFICATIONS` prompt allows persistent notification controls.
- [ ] Complete setup -> verify main AutoSwipe switch starts **OFF** until explicitly enabled by user.

## 3. Android 10 (API 29) Specific Verification
- [ ] Verify floating control overlay (`TYPE_APPLICATION_OVERLAY`) renders, drags smoothly, expands/collapses, and hides without stopping engine.
- [ ] Verify gesture navigation (if enabled on Android 10): upward swipe starts high enough (~78% height) so it does **not** trigger Android Home or Recents gesture.
- [ ] Verify Foreground Service starts cleanly and notification shows Pause / Hide Panel / Stop actions.

## 4. Android 14–16 (API 34–36) Specific Verification
- [ ] Verify `FOREGROUND_SERVICE_SPECIAL_USE` starts without `MissingForegroundServiceTypeException`.
- [ ] Verify edge-to-edge UI layout does not clip under status bar or gesture pill.
- [ ] Verify screen rotation updates floating overlay bounds cleanly without pushing panel off-screen.
- [ ] Verify locking the screen (`POWER` button) pauses monitoring and cancels any pending delayed swipe.

## 5. Platform-by-Platform Live Detection Verification
### TikTok (`com.zhiliaoapp.musically` / `com.ss.android.ugc.trill`)
- [ ] Open TikTok feed with AutoSwipe AI active.
- [ ] Floating panel shows `App: TikTok` and `Status: Monitoring Playback`.
- [ ] Open comments sheet -> verify AutoSwipe AI reports `Comments or share sheet open` and does **not** swipe.
- [ ] On a video that shows a *Watch again / Replay* overlay at completion -> verify single swipe after configured delay (default 800ms), then cooldown, never double-swiping.
- [ ] On a continuously looping video with no Replay prompt -> verify AutoSwipe AI does **not** falsely swipe mid-play and logs timeout after configured threshold.

### YouTube Shorts (`com.google.android.youtube`)
- [ ] Open YouTube Home feed -> verify AutoSwipe AI reports `waiting for Shorts player` and does **not** swipe the Home feed.
- [ ] Open Shorts -> verify `Monitoring YouTube Shorts`.
- [ ] Confirm swipe triggers once when progress completes / loops (`>= 97%` or wrap) or Replay button becomes visible.

### Instagram Reels (`com.instagram.android`)
- [ ] Open Instagram Home feed / DMs -> verify AutoSwipe AI does **not** swipe outside Reels viewer.
- [ ] Open Reels tab -> verify `Monitoring Instagram Reels`.
- [ ] Confirm swipe triggers on Replay / Watch again controls; verify manual finger scroll triggers override cooldown.

## 6. Reliability, Errors & Statistics
- [ ] Switch to Chrome or Android Settings while AutoSwipe AI is ON -> verify status shows `Unrelated App Paused` and zero gestures occur.
- [ ] Revoke Overlay permission in system settings while running -> verify helpful error banner appears without crashing.
- [ ] Tap **Emergency Stop** (on Dashboard, Floating Panel, or Notification) -> verify engine stops immediately.
- [ ] Open **Usage Statistics** -> verify real swipe counts, session count, active duration, and per-app breakdown match actual test run and survive app restart.
