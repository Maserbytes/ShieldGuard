"""
Offline Spam Dataset & Quantized TFLite Exporter ($0 Cost)
Prepares training pairs for offline text classification.
"""
import json
import os

SAMPLE_DATASET = [
    # Spam (Label: 1)
    ("Claim your $1000 gift card immediately! Click link", 1),
    ("URGENT: Your account has been locked. Verify here", 1),
    ("Get instant personal loan approved in 5 minutes with zero interest", 1),
    ("Free entry into weekly contest! Text WIN to 88202", 1),
    ("Congratulations! You have been selected for a free holiday package", 1),
    ("Flat 70% discount on fashion! Shop now coupon SAVE70", 1),
    
    # Ham / Safe (Label: 0)
    ("Your bank account xxx123 was debited by $45. OTP: 492019", 0),
    ("Your verification code is 884129. Valid for 10 minutes.", 0),
    ("Hey dad, I will be late for dinner today.", 0),
    ("Package delivered by courier at your front door.", 0),
    ("Can we reschedule our project sync to tomorrow 10am?", 0),
    ("Doctor appointment confirmed for Friday at 3:30 PM", 0)
]

def export_offline_dataset():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    out_path = os.path.join(base_dir, "spam_train_samples.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(SAMPLE_DATASET, f, indent=2)
    print(f"Exported {len(SAMPLE_DATASET)} sample vectors to {out_path}")

if __name__ == "__main__":
    export_offline_dataset()
