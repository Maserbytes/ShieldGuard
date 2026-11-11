# 🛡️ ShieldGuard (Android Communication Firewall)

> **Zero Cloud Leaks. Zero Server Bills. 100% On-Device Privacy.**  
> A unified Android communication firewall and spam defense system built to filter Calls, SMS, WhatsApp alerts, and Gmail notifications simultaneously.

---

## 🌟 $0 Investment Architecture

ShieldGuard was engineered from day one to operate with **zero financial overhead ($0.00)**:

| Layer | Traditional Model (Expensive) | ShieldGuard $0 Model |
| :--- | :--- | :--- |
| **Backend / Servers** | Cloud VMs / Serverless ($50 - $500/mo) | **100% Offline Local SQLite / Room DB** ($0) |
| **Spam Intelligence** | Cloud Lookup API per call/SMS ($0.005/req) | **Local Regex + Heuristic Engine** ($0) |
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
   - Preserves bank 2FA / OTP messages (`OtpGuard`), including alphanumeric codes.
   - **Fails open**: if the engine throws, the message is written to the inbox rather than lost.
3. **Notification Interceptor (`SpamNotificationListenerService`)**:
   - Catches promotional notifications from WhatsApp, Facebook, Gmail, browsers and app stores.
   - Dismisses them via `cancelNotification(sbn.key)`.
   - Never touches ongoing notifications, group summaries, or a 1:1 chat with a saved contact.
   - Includes a watchdog (`NotificationWatchdog`) to recover from aggressive OEM task killers (MIUI, ColorOS, OneUI).
4. **Persistent Shield (`ShieldForegroundService`)**:
   - Runs with `foregroundServiceType="phoneCall|specialUse"` to prevent OS process reaping.
5. **Community Rule Sync (`RuleSyncWorker`)**:
   - Uses `WorkManager` to pull updated JSON rules from GitHub on Wi-Fi once a week for $0.
   - Remote rules must declare a `packageScope` and may not create `CALL_REGEX` rules, so a bad
     feed can never stop the phone from ringing.

### The one rule that matters most

**A message from someone you know is never filtered.** Contacts, allowlisted senders and OTPs
short-circuit the engine before any heuristic runs. A false positive here costs a real message,
so the engine is deliberately biased towards letting borderline traffic through.

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
│   ├── src/main/
│       ├── AndroidManifest.xml              # Roles, Permissions, Services & Receivers
│       ├── assets/
│       │   └── default_rules.json           # Seed rules (Robocalls, OTP protections)
│       └── java/com/shieldguard/app/
│           ├── ShieldGuardApp.kt            # App class & Notification Channel setup
│           ├── data/
│           │   ├── local/
│           │   │   ├── AppDatabase.kt       # Room SQLite + v2->v3 migration + seeding
│           │   │   ├── dao/                 # RuleDao, AllowlistDao, BlockLogDao
│           │   │   └── entity/              # RuleEntity, AllowlistEntity, BlockLogEntity
│           │   └── repository/
│           │       └── ShieldRepository.kt  # Repository & Contact Resolver
│           ├── engine/
│           │   ├── FilterDecision.kt        # Decision sealed classes (Allow, Block, Silence)
│           │   ├── EmergencyGuard.kt        # 911 / 112 / 999 non-negotiable pass
│           │   ├── OtpGuard.kt              # Bank 2FA & OTP protector (pure)
│           │   ├── SpamPolicy.kt            # Pure decision core + rule compiler (no Android)
│           │   └── SpamEngine.kt            # Android adapter: rules, allowlist, contacts
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
│   └── src/test/java/com/shieldguard/app/engine/
│       ├── SpamPolicyTest.kt                # JVM tests for the filtering decisions
│       └── OtpGuardTest.kt                  # JVM tests for OTP / transaction protection
└── tools/
    ├── validate_rules.py                    # Rule-file schema & regex validator
    └── generate_offline_dataset.py          # Offline training dataset generator
```

---

## 🛠️ Verification & Testing

The filtering logic is covered by real Kotlin unit tests that run on the JVM — no emulator, no
device, milliseconds per run:

```bash
./gradlew testDebugUnitTest
```

They assert the behaviour that actually matters, including the cases that previously failed:

| Case | Expected |
| :--- | :--- |
| WhatsApp 1:1 from a saved contact saying "order now / free delivery" | **Allowed** |
| WhatsApp group deal spam | Blocked |
| Bundled WhatsApp rule firing on a Gmail notification | Not applied |
| Bank OTP in a notification that also matches a spam rule | **Allowed** |
| Unknown third-party app pushed "trending apps" | **Allowed** |
| A user-written rule against a saved contact | Blocked (user rules outrank the guard) |
| Invalid regex in a rule | Ignored, never crashes |
| Emergency number matching a block rule | **Allowed** |

Rules-file sanity check (structure, scopes, regex validity):

```bash
python tools/validate_rules.py
```

> **Why not a Python test suite?** This repo previously shipped
> `tools/test_rules_engine.py` and `tools/test_android_harness.py`, which re-implemented the
> Kotlin engine in Python and simulated the Android subsystem. They were removed: they drifted
> from the real code (the OTP copy had already lost half the patterns) and could not see contact
> lookups, package scoping or rule precedence. A green mirror proves nothing about the APK.

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

### Release signing (required for updates)

The build reads signing material from `keystore.properties` at the repo root (git-ignored):

```properties
storeFile=release.keystore
storePassword=…
keyAlias=shieldguard
keyPassword=…
```

Create the key once and **keep it safe**:

```bash
keytool -genkeypair -v -keystore release.keystore -alias shieldguard \
        -keyalg RSA -keysize 4096 -validity 10000
```

If the file is missing, `assembleRelease` still works but is signed with the throwaway debug
key and logs a warning. That APK can never be updated in place, because the debug key is
regenerated per machine — CI refuses to publish it for exactly this reason.

For CI, add four repository secrets: `SIGNING_KEY_BASE64` (`base64 -w0 release.keystore`),
`KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

### Version codes

`versionCode` is the git commit count (or `$VERSION_CODE` when set, which CI does), and
`versionName` is `1.0.<count>`. A constant version code would make Android reject every update
after the first install.

5. **Distribution**:
   - Host the `.apk` on **GitHub Releases** (100% free hosting and unlimited downloads).
   - Submit your repo to **F-Droid** or **IzzyOnDroid** for automated open-source builds.

### F-Droid notes

- F-Droid builds from source, so the tag it builds must be immutable. Releases are tagged
  `build-<run>-<sha>`, never a moving `latest`.
- The app declares `INTERNET` for one purpose: a weekly `GET` of the public community rule
  JSON. No telemetry, no accounts, nothing uploaded. Expect the "NonFreeNet" anti-feature
  marker; the justification above is the one to submit.
- `android:allowBackup="false"`: blocked-message metadata must not end up in a cloud backup.

### Roadmap

- Tier 3, an on-device scoring/quantized model, is **not shipped**. The current engine is
  Tier 1 (contacts, allowlist, OTP) + Tier 2 (regex, keywords, scoped heuristics). A scorer
  only becomes worth building once the false-positive rate above is measurably zero.
