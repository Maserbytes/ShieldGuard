# 🛡️ ShieldGuard (Android Communication Firewall)

> **Zero Cloud Leaks. Zero Server Bills. 100% On-Device Privacy.**  
> A unified Android communication firewall and spam defense system built to filter Calls, SMS, WhatsApp alerts, and Gmail notifications simultaneously.

---

## 🌟 $0 Investment Architecture

ShieldGuard was engineered from day one to operate with **zero financial overhead ($0.00)**:

| Layer | Traditional Model (Expensive) | ShieldGuard $0 Model |
| :--- | :--- | :--- |
| **Backend / Servers** | Cloud VMs / Serverless ($50 - $500/mo) | **100% Offline Local SQLite / Room DB** ($0) |
| **Spam Intelligence** | Cloud Lookup API per call/SMS ($0.005/req) | **Local Regex + Heuristic + Quantized TFLite Engine** ($0) |
| **Community Updates**| AWS CloudFront / Cloudflare Paid CDN | **GitHub Raw / Pages as a Free Global CDN** ($0) |
| **Distribution** | Google Play Store ($25 fee + policy ban risk)| **F-Droid + GitHub Releases + IzzyOnDroid** ($0) |
| **User Privacy** | User contacts harvested and sold (Truecaller) | **Zero Contacts Leave The Phone (100% Private)** |

---

## 🏗️ Core Android Components

```
                                  ShieldGuard Architecture
                                 ┌────────────────────────┐
                                 │  Incoming Interaction  │
                                 └───────────┬────────────┘
                                             │
                      ┌──────────────────────┼──────────────────────┐
                      │                      │                      │
                      ▼                      ▼                      ▼
           [ Telephony Subsystem ]     [ SMS Subsystem ]     [ App Notifications ]
                      │                      │                      │
                      ▼                      ▼                      ▼
            CallScreeningService      SmsDeliverReceiver  NotificationListenerService
             (ROLE_DIALER /            (ROLE_SMS default   (com.whatsapp,
              ROLE_CALL_SCREENING)      app receiver)       com.google.android.gm)
                      │                      │                      │
                      └──────────────────────┼──────────────────────┘
                                             │
                                             ▼
                             ┌──────────────────────────────┐
                             │       Local Spam Engine      │
                             │  1. Emergency Guard (Pass)   │
                             │  2. Contact Allowlist (Pass) │
                             │  3. Bank OTP Guard (Pass)    │
                             │  4. Regex / Keyword Rules    │
                             │  5. Heuristic URL Scanner    │
                             └───────────────┬──────────────┘
                                             │
                             ┌───────────────┴──────────────┐
                             ▼                              ▼
                        [ ALLOWED ]                    [ BLOCKED ]
                    • Normal ring/vibrate         • Disallow / Reject call
                    • Saved to system SMS inbox   • Drop SMS / Never in inbox
                                                  • cancelNotification(key)
                                                  • Logged to Room DB
```

1. **Call Screening (`SpamCallScreeningService` & `InCallServiceImpl`)**:
   - Rejects telemarketing prefixes (`+1800*`, `+91140*`) before the ringer triggers.
   - Emergency call bypass (`TelephonyManager.isEmergencyNumber`).
   - Non-blocking async evaluation via Kotlin Coroutines.
2. **SMS Engine (`SmsDeliverReceiver` & `HeadlessSmsSendService`)**:
   - Intercepts `SMS_DELIVER_ACTION` as the Default SMS app.
   - Isolates fraudulent lottery / phishing links.
   - Preserves bank 2FA / OTP messages (`OtpGuard`).
3. **Notification Interceptor (`SpamNotificationListenerService`)**:
   - Catches promotional notifications from WhatsApp and Gmail.
   - Instantly dismisses them via `cancelNotification(sbn.key)`.
   - Includes a watchdog (`NotificationWatchdog`) to recover from aggressive OEM task killers (MIUI, ColorOS, OneUI).
4. **Persistent Shield (`ShieldForegroundService`)**:
   - Runs with `foregroundServiceType="phoneCall|specialUse"` to prevent OS process reaping.
5. **Community Rule Sync (`RuleSyncWorker`)**:
   - Uses `WorkManager` to pull updated JSON rules from GitHub on Wi-Fi once a week for $0.

---

## 🚀 Onboarding Wizard (Critical Sequence)

Inside the Jetpack Compose app, users are guided step-by-step:
1. **Welcome & Privacy Guarantee**: Value proposition (100% offline, zero server leaks).
2. **Default Phone Role**: Requests `RoleManager.ROLE_CALL_SCREENING` / `ROLE_DIALER`.
3. **Default SMS Role**: Requests `RoleManager.ROLE_SMS`.
4. **Notification Access**: Deep link into `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
5. **Battery Exemption**: Deep link into `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

---

## 📁 Repository Structure

```
d:\ShieldGuard\
├── app/
│   ├── build.gradle.kts                     # App dependencies (Room, Compose, WorkManager)
│   ├── proguard-rules.pro                   # Proguard optimization rules
│   └── src/main/
│       ├── AndroidManifest.xml              # Roles, Permissions, Services & Receivers
│       ├── assets/
│       │   └── default_rules.json           # Seed rules (Robocalls, OTP protections)
│       └── java/com/shieldguard/app/
│           ├── ShieldGuardApp.kt            # App class & Notification Channel setup
│           ├── data/
│           │   ├── local/
│           │   │   ├── AppDatabase.kt       # Room SQLite database with prepopulation
│           │   │   ├── dao/                 # RuleDao, AllowlistDao, BlockLogDao
│           │   │   └── entity/              # RuleEntity, AllowlistEntity, BlockLogEntity
│           │   └── repository/
│           │       └── ShieldRepository.kt  # Repository & Contact Resolver
│           ├── engine/
│           │   ├── FilterDecision.kt        # Decision sealed classes (Allow, Block, Silence)
│           │   ├── EmergencyGuard.kt        # 911 / 112 / 999 non-negotiable pass
│           │   ├── OtpGuard.kt              # Bank 2FA & OTP protector
│           │   └── SpamEngine.kt            # Central evaluation engine
│           ├── service/
│           │   ├── SpamCallScreeningService.kt
│           │   ├── InCallServiceImpl.kt
│           │   ├── SpamNotificationListenerService.kt
│           │   ├── ShieldForegroundService.kt
│           │   ├── HeadlessSmsSendService.kt
│           │   └── NotificationWatchdog.kt  # OEM task-killer recovery
│           ├── receiver/
│           │   ├── SmsDeliverReceiver.kt    # Default SMS handler
│           │   ├── SmsReceiver.kt           # Secondary SMS listener
│           │   ├── MmsReceiver.kt           # MMS compatibility
│           │   └── BootCompletedReceiver.kt # Auto-start on reboot
│           ├── worker/
│           │   └── RuleSyncWorker.kt        # $0 GitHub community rules sync
│           └── ui/
│               ├── MainActivity.kt          # Compose Navigation Host
│               ├── onboarding/              # Step-by-step role request wizard
│               ├── home/                    # Shield status & metrics dashboard
│               ├── rules/                   # Spam rules & allowlist manager
│               └── logs/                    # Audit logs of blocked items
└── tools/
    ├── test_rules_engine.py                 # Local offline Python verification suite
    └── generate_offline_dataset.py          # Offline training dataset generator
```

---

## 🛠️ Verification & Testing

Run the included rule verification test suite using Python:

```bash
python tools/test_rules_engine.py
```

Output:
```
==================================================
      ShieldGuard $0 Rules Verification Suite     
==================================================
[PASSED] US Toll-Free Robocall
[PASSED] India 140 Telemarketer Series
[PASSED] Legitimate Personal Caller
[PASSED] Emergency Number
[PASSED] Scam Lottery SMS
[PASSED] Loan Promotion SMS
[PASSED] Critical Bank 2FA OTP
[PASSED] Personal Friend SMS
[PASSED] Promotional Notification
[PASSED] Legitimate Family Chat
[PASSED] WhatsApp 2FA Alert

Verification Result: 11/11 tests passed (100% Accuracy).
```

---

## 📦 How to Build the APK ($0 Distribution)

1. Open `d:\ShieldGuard` in **Android Studio** (Koala / Ladybug or newer).
2. Sync Project with Gradle Files.
3. Build Release APK:
   ```bash
   ./gradlew assembleRelease
   ```
4. Output APK location:
   `app/build/outputs/apk/release/app-release.apk`
5. **Distribution**:
   - Host the `.apk` on **GitHub Releases** (100% free hosting and unlimited downloads).
   - Submit your repo to **F-Droid** or **IzzyOnDroid** for automated open-source builds.
