# GNS Python SDK

Official Python client for the General Notification System (GNS).

## Installation

```bash
pip install gns-sdk
```

## Usage

```python
from gns_sdk import GNSClient

# Initialize client
client = GNSClient(
    base_url="http://your-gns-server:8080", 
    token="YOUR_API_TOKEN"
)

# Send a notification
response = client.send_notification(
    task_id="your-task-uuid",
    data={
        "name": "User",
        "order_id": "123456"
    },
    priority="High"
)

print(response)
```
