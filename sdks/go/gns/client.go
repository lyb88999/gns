package gns

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"
)

type Client struct {
	BaseURL    string
	Token      string
	HTTPClient *http.Client
}

func NewClient(baseURL, token string) *Client {
	return &Client{
		BaseURL: strings.TrimRight(baseURL, "/"),
		Token:   token,
		HTTPClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

type Attachment struct {
	Filename string `json:"filename"`
	Content  string `json:"content"` // base64 encoded string
}

type SendRequest struct {
	TaskId      string                 `json:"taskId"`
	Data        map[string]interface{} `json:"data"`
	Attachments []Attachment           `json:"attachments,omitempty"`
	Priority    string                 `json:"priority,omitempty"`
}

func (c *Client) SendNotification(req SendRequest) (map[string]interface{}, error) {
	url := fmt.Sprintf("%s/api/v1/notify", c.BaseURL)

	body, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	httpReq, err := http.NewRequest("POST", url, bytes.NewBuffer(body))
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	httpReq.Header.Set("Authorization", "Bearer "+c.Token)
	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := c.HTTPClient.Do(httpReq)
	if err != nil {
		return nil, fmt.Errorf("failed to send request: %w", err)
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response body: %w", err)
	}

	var result map[string]interface{}
	// Try to unmarshal response
	if err := json.Unmarshal(respBody, &result); err != nil {
		// If fails, generic error
		if resp.StatusCode >= 400 {
			return nil, fmt.Errorf("API error (status %d): %s", resp.StatusCode, string(respBody))
		}
	}

	if resp.StatusCode >= 400 {
		return nil, fmt.Errorf("API error (status %d): %v", resp.StatusCode, result)
	}

	return result, nil
}
