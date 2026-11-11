#!/usr/bin/env python3
"""
Validates the bundled rule feed: app/src/main/assets/default_rules.json

This does NOT test filtering behaviour. Behaviour is covered by real Kotlin unit tests in
app/src/test/ (run with `./gradlew testDebugUnitTest`), which exercise the exact code that
ships in the APK.

Earlier versions of this folder contained a Python re-implementation of the Kotlin engine plus
a simulated "Android harness". They were removed on purpose: they duplicated the logic, drifted
from it, and reported green while the real engine had no package scoping or contact protection.
A passing copy proves nothing about the shipped app.

What this script is still good for is catching a malformed rule file before it reaches a device,
because a bad file is what silently disables built-in protection.
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RULES_FILE = ROOT / "app" / "src" / "main" / "assets" / "default_rules.json"

VALID_TYPES = {
    "CALL_REGEX",
    "SMS_KEYWORD",
    "SMS_REGEX",
    "NOTIFICATION_KEYWORD",
    "NOTIFICATION_REGEX",
    "WHITELIST_REGEX",
}
VALID_ACTIONS = {"BLOCK", "ALLOW", "SILENCE"}

KNOWN_PACKAGES = {
    "com.whatsapp",
    "com.whatsapp.w4b",
    "com.google.android.gm",
    "com.facebook.katana",
    "com.facebook.orca",
    "com.facebook.lite",
    "com.facebook.mlite",
    "com.vivo.browser",
    "com.vivo.hybrid",
    "com.android.chrome",
    "com.heytap.browser",
    "com.coloros.browser",
    "com.opera.browser",
    "com.vivo.appstore",
    "com.bbk.appstore",
    "com.vivo.game",
    "com.vivo.gamewatch",
    "com.heytap.market",
}


failures: list[str] = []


def fail(message: str) -> None:
    print(f"  FAIL  {message}")
    failures.append(message)


def main() -> int:
    if not RULES_FILE.exists():
        print(f"Rules file not found: {RULES_FILE}")
        return 1

    payload = json.loads(RULES_FILE.read_text(encoding="utf-8"))
    rules = payload.get("rules")
    if not isinstance(rules, list) or not rules:
        print("Rules file has no 'rules' array.")
        return 1

    print(f"Validating {len(rules)} rules in {RULES_FILE.name}")

    seen: set[tuple[str, str]] = set()

    for index, rule in enumerate(rules):
        label = f"rule[{index}]"

        for required in ("pattern", "type", "action"):
            if required not in rule:
                fail(f"{label}: missing '{required}'")

        rule_type = rule.get("type")
        if rule_type not in VALID_TYPES:
            fail(f"{label}: unknown type {rule_type!r}")
        elif rule_type == "CALL_REGEX" and rule.get("action") == "ALLOW":
            fail(f"{label}: CALL_REGEX ALLOW rules are not supported by the call path")

        if rule.get("action") not in VALID_ACTIONS:
            fail(f"{label}: unknown action {rule.get('action')!r}")

        pattern = rule.get("pattern", "")
        try:
            re.compile(pattern)
        except re.error as exc:
            fail(f"{label}: invalid regex ({exc})")

        # Every notification rule must declare which apps it may act on, otherwise it can
        # silence an unrelated app's notification.
        if rule_type in ("NOTIFICATION_KEYWORD", "NOTIFICATION_REGEX"):
            scope = rule.get("packageScope", "")
            if not scope:
                fail(f"{label}: notification rule has no packageScope")
            else:
                for entry in scope.split(","):
                    entry = entry.strip()
                    if entry and entry != "*" and entry not in KNOWN_PACKAGES:
                        fail(f"{label}: packageScope contains unknown package {entry!r}")

        key = (pattern, rule_type or "")
        if key in seen:
            fail(f"{label}: duplicate (pattern, type) pair - the unique index will reject it")
        seen.add(key)

    print()
    if failures:
        print(f"{len(failures)} problem(s) found:")
        for item in failures:
            print(f"  - {item}")
        return 1

    print("All rules are structurally valid.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
