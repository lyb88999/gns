const GNSClient = require('./index');

// Configuration
// 1. Start your backend (localhost:8080)
// 2. Generate an API Token from the UI
// 3. Create a Task and get its ID
const API_URL = process.env.GNS_API_URL || 'http://localhost:8080';
const API_TOKEN = process.env.GNS_API_TOKEN || 'YOUR_API_TOKEN_HERE';
const TASK_ID = process.env.GNS_TASK_ID || 'YOUR_TASK_ID_HERE';

async function main() {
    if (API_TOKEN === 'YOUR_API_TOKEN_HERE' || TASK_ID === 'YOUR_TASK_ID_HERE') {
        console.error('Please set GNS_API_TOKEN and GNS_TASK_ID environment variables');
        return;
    }

    console.log(`Initializing GNS Client with URL: ${API_URL}`);
    const client = new GNSClient(API_URL, API_TOKEN);

    try {
        const data = {
            name: 'Node.js Developer',
            service: 'Express Service'
        };

        console.log(`Sending notification for Task: ${TASK_ID}...`);

        const response = await client.sendNotification({
            taskId: TASK_ID,
            data: data,
            priority: 'High'
        });

        console.log('✅ Notification Sent Successfully!');
        console.log('Response:', response);

    } catch (error) {
        console.error('❌ Failed to send notification:', error.message);
    }
}

main();
