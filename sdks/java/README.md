# GNS Java SDK

Official Java client for the General Notification System (GNS).

## Installation

### Maven

```xml
<dependency>
    <groupId>com.gns.sdk</groupId>
    <artifactId>gns-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

```java
import com.gns.sdk.client.GNSClient;
import com.gns.sdk.model.NotificationRequest;
import com.gns.sdk.model.NotificationResponse;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Initialize client
        GNSClient client = new GNSClient(
            "http://your-gns-server:8080", 
            "YOUR_API_TOKEN"
        );

        // Send a notification
        try {
            NotificationResponse response = client.sendNotification(
                NotificationRequest.builder()
                    .taskId("your-task-uuid")
                    .data(Map.of(
                        "name", "User",
                        "order_id", "123456"
                    ))
                    .priority("High")
                    .build()
            );
            
            System.out.println("Success: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
