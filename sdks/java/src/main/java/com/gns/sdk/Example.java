package com.gns.sdk;

import com.gns.sdk.models.SendRequest;

import java.util.HashMap;
import java.util.Map;

public class Example {
    public static void main(String[] args) {
        String apiUrl = System.getenv().getOrDefault("GNS_API_URL", "http://localhost:8080");
        String apiToken = System.getenv("GNS_API_TOKEN");
        String taskId = System.getenv("GNS_TASK_ID");

        if (apiToken == null || taskId == null) {
            System.err.println("Please set GNS_API_TOKEN and GNS_TASK_ID environment variables");
            return;
        }

        System.out.println("Initializing GNS Client with " + apiUrl);
        GnsClient client = new GnsClient(apiUrl, apiToken);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Java Developer");
        data.put("service", "Spring Boot Service");

        SendRequest request = SendRequest.builder()
                .taskId(taskId)
                .data(data)
                .priority("High")
                .build();

        try {
            System.out.println("Sending notification...");
            var response = client.sendNotification(request);
            System.out.println("✅ Notification Sent Successfully!");
            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.err.println("❌ Failed to send notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
