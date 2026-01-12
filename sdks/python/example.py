from gns_sdk import GNSClient
import os

# Configuration
# 1. Start your backend (localhost:8080)
# 2. Generate an API Token from the UI (Access -> Tokens)
# 3. Create a Task and get its ID
API_URL = os.getenv("GNS_API_URL", "http://localhost:8080")
API_TOKEN = os.getenv("GNS_API_TOKEN", "YOUR_API_TOKEN_HERE")
TASK_ID = os.getenv("GNS_TASK_ID", "YOUR_TASK_ID_HERE")

def main():
    print(f"Initializing GNS Client with URL: {API_URL}")
    client = GNSClient(base_url=API_URL, token=API_TOKEN)

    try:
        # Prepare template data
        # Assuming your template is "Hello ${name}, welcome to ${service}!"
        data = {
            "name": "Developer",
            "service": "Platform V2"
        }

        print(f"Sending notification for Task: {TASK_ID}...")
        response = client.send_notification(
            task_id=TASK_ID, 
            data=data,
            priority="High"
        )
        
        print("✅ Notification Sent Successfully!")
        print(f"Response: {response}")
        
    except Exception as e:
        print(f"❌ Failed to send notification: {e}")

if __name__ == "__main__":
    main()
