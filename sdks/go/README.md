# GNS Go SDK

Official Go client for the General Notification System (GNS).

## Installation

```bash
go get github.com/lyb88999/gns/sdks/go
```

## Usage

```go
package main

import (
	"fmt"
	"github.com/lyb88999/gns/sdks/go/gns"
)

func main() {
	// Initialize client
	client := gns.NewClient("http://your-gns-server:8080", "YOUR_API_TOKEN")

	// Send a notification
	response, err := client.SendNotification(gns.NotificationRequest{
		TaskId: "your-task-uuid",
		Data: map[string]interface{}{
			"name":     "User",
			"order_id": "123456",
		},
		Priority: "High",
	})

	if err != nil {
		panic(err)
	}

	fmt.Printf("Success: %+v\n", response)
}
```
