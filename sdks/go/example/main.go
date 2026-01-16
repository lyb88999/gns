package main

import (
	"fmt"
	"log"
	"os"

	"github.com/lyb88999/gns/sdks/go/gns"
)

func main() {
	apiURL := os.Getenv("GNS_API_URL")
	if apiURL == "" {
		apiURL = "http://localhost:8080"
	}

	token := os.Getenv("GNS_API_TOKEN")
	if token == "" {
		fmt.Println("Please set GNS_API_TOKEN environment variable")
		return
	}

	taskID := os.Getenv("GNS_TASK_ID")
	if taskID == "" {
		fmt.Println("Please set GNS_TASK_ID environment variable")
		return
	}

	client := gns.NewClient(apiURL, token)

	fmt.Printf("Sending notification for Task: %s...\n", taskID)

	data := map[string]interface{}{
		"name":    "Go Developer",
		"service": "Go Microservice",
	}

	req := gns.SendRequest{
		TaskId:   taskID,
		Data:     data,
		Priority: "High",
	}

	resp, err := client.SendNotification(req)
	if err != nil {
		log.Fatalf("❌ Failed to send notification: %v", err)
	}

	fmt.Println("✅ Notification Sent Successfully!")
	fmt.Printf("Response: %+v\n", resp)
}
