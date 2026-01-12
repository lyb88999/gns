const axios = require('axios');

class GNSClient {
    /**
     * @param {string} baseURL - The base URL of the GNS API (e.g., http://localhost:8080)
     * @param {string} token - The API Token
     */
    constructor(baseURL, token) {
        this.client = axios.create({
            baseURL: baseURL.replace(/\/$/, ''),
            timeout: 10000,
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
    }

    /**
     * Send a notification
     * @param {Object} params
     * @param {string} params.taskId - The UUID of the Notification Task
     * @param {Object} params.data - Dictionary of variables to inject
     * @param {Array<Object>} [params.attachments] - List of {filename, content}
     * @param {string} [params.priority] - Priority override
     * @returns {Promise<Object>} API Response
     */
    async sendNotification({ taskId, data, attachments, priority }) {
        try {
            const payload = {
                taskId,
                data,
                attachments,
                priority
            };

            const response = await this.client.post('/api/v1/notify', payload);
            return response.data;
        } catch (error) {
            if (error.response) {
                // The request was made and the server responded with a status code
                // that falls out of the range of 2xx
                throw new Error(`API Error (${error.response.status}): ${JSON.stringify(error.response.data)}`);
            } else if (error.request) {
                // The request was made but no response was received
                throw new Error('No response received from GNS server');
            } else {
                // Something happened in setting up the request that triggered an Error
                throw new Error(`Request Error: ${error.message}`);
            }
        }
    }
}

module.exports = GNSClient;
