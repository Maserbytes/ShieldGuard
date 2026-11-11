"""
Zero-Cost Local Rule Tester & Verification Suite for ShieldGuard
Tests the exact regex and keyword patterns implemented in the Android Kotlin Engine.
"""
import re
import json
import os

def load_rules():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    rules_path = os.path.join(base_dir, "..", "app", "src", "main", "assets", "default_rules.json")
    with open(rules_path, "r", encoding="utf-8") as f:
        return json.load(f)["rules"]

def is_otp_protected(text):
    otp_regex = r"(?i)\b(otp|verification code|one time password|secret code|do not share|valid for \d+ mins?)\b"
    digits_regex = r"\b\d{4,8}\b"
    return bool(re.search(otp_regex, text)) and bool(re.search(digits_regex, text))

def test_engine():
    rules = load_rules()
    print("==================================================")
    print("      ShieldGuard $0 Rules Verification Suite     ")
    print("==================================================")

    test_cases = [
        # Inbound Calls
        {"type": "CALL", "input": "+18001234567", "expected": "BLOCK", "label": "US Toll-Free Robocall"},
        {"type": "CALL", "input": "+911409876543", "expected": "BLOCK", "label": "India 140 Telemarketer Series"},
        {"type": "CALL", "input": "+14155552671", "expected": "ALLOW", "label": "Legitimate Personal Caller"},
        {"type": "CALL", "input": "911", "expected": "ALLOW", "label": "Emergency Number"},
        
        # Inbound SMS
        {"type": "SMS", "input": "Congratulations! You won a $1,000 lottery prize. Claim prize now at http://scam.cc", "expected": "BLOCK", "label": "Scam Lottery SMS"},
        {"type": "SMS", "input": "Your pre-approved loan of $50,000 is ready. Apply today!", "expected": "BLOCK", "label": "Loan Promotion SMS"},
        {"type": "SMS", "input": "Your HDFC Bank OTP is 849201. Valid for 10 mins. Do not share with anyone.", "expected": "ALLOW", "label": "Critical Bank 2FA OTP"},
        {"type": "SMS", "input": "Hey, are we still meeting for lunch today at 1pm?", "expected": "ALLOW", "label": "Personal Friend SMS"},

        # Notifications (WhatsApp Groups / Facebook / Browser / Gmail / Discord)
        {"type": "NOTIFICATION", "input": "Deals Group: Lowest price wholesale! Order now, free delivery and COD available.", "expected": "BLOCK", "label": "WhatsApp Group Product Spam"},
        {"type": "NOTIFICATION", "input": "Facebook: People you may know: Alex and 5 others. Add as friend.", "expected": "BLOCK", "label": "Facebook Friend Suggestions"},
        {"type": "NOTIFICATION", "input": "Facebook: Say congrats to Michael on celebrating 3 years at TechCorp.", "expected": "BLOCK", "label": "Facebook Congrats Spam"},
        {"type": "NOTIFICATION", "input": "Vivo Browser: Breaking news! Shocking viral video, click to read immediately.", "expected": "BLOCK", "label": "iQOO Native Browser Clickbait"},
        {"type": "NOTIFICATION", "input": "V-Appstore: Must-have apps! Install trending apps now and get coins reward.", "expected": "BLOCK", "label": "iQOO App Store Promotion"},
        {"type": "NOTIFICATION", "input": "Gmail: [Discord] @everyone Bob mentioned you in #crypto-chat", "expected": "BLOCK", "label": "Discord Mentions via Gmail"},
        {"type": "NOTIFICATION", "input": "Mom: Can you pick up milk on your way home?", "expected": "ALLOW", "label": "Legitimate Family Chat"},
        {"type": "NOTIFICATION", "input": "WhatsApp: Your Google verification code is 382910.", "expected": "ALLOW", "label": "WhatsApp 2FA Alert"}
    ]

    passed = 0
    total = len(test_cases)

    for case in test_cases:
        actual = "ALLOW"
        
        # Emergency Guard check
        if case["type"] == "CALL" and case["input"] in ["911", "112", "999"]:
            actual = "ALLOW"
        # OTP Guard check
        elif case["type"] in ["SMS", "NOTIFICATION"] and is_otp_protected(case["input"]):
            actual = "ALLOW"
        else:
            for rule in rules:
                if not rule.get("enabled", True):
                    continue
                pattern = rule["pattern"]
                if case["type"] == "CALL" and rule["type"] == "CALL_REGEX":
                    if re.search(pattern, case["input"]):
                        actual = rule["action"]
                        break
                elif case["type"] == "SMS" and rule["type"] in ["SMS_KEYWORD", "SMS_REGEX"]:
                    if re.search(pattern, case["input"]):
                        actual = rule["action"]
                        break
                elif case["type"] == "NOTIFICATION" and rule["type"] in ["NOTIFICATION_KEYWORD", "NOTIFICATION_REGEX"]:
                    if re.search(pattern, case["input"]):
                        actual = rule["action"]
                        break

        status = "PASSED" if actual == case["expected"] else "FAILED"
        if status == "PASSED":
            passed += 1

        print(f"[{status}] {case['label']}")
        print(f"   Input: \"{case['input']}\"")
        print(f"   Result: {actual} (Expected: {case['expected']})\n")

    print(f"Verification Result: {passed}/{total} tests passed ({round(passed/total*100)}% Accuracy).")
    return passed == total

if __name__ == "__main__":
    import sys
    success = test_engine()
    sys.exit(0 if success else 1)
