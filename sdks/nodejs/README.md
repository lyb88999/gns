# GNS Node.js SDK

Official Node.js client for the General Notification System (GNS).

## Installation

```bash
npm install gns-sdk
```

## Usage

```javascript
const GNSClient = require('gns-sdk');

// Initialize client
const client = new GNSClient({
    baseUrl: 'http://your-gns-server:8080',
    token: 'YOUR_API_TOKEN'
});

// Send a notification
async function send() {
    try {
        const response = await client.sendNotification({
            taskId: 'your-task-uuid',
            data: {
                name: 'User',
                order_id: '123456'
            },
            priority: 'High'
        });
        console.log('Success:', response);
    } catch (error) {
        console.error('Error:', error.message);
    }
}

send();
```
