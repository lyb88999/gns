package com.gns.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gns.sdk.models.SendRequest;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;

public class GnsClient {
    private final String baseUrl;
    private final String token;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public GnsClient(String baseUrl, String token) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> sendNotification(SendRequest request) throws IOException {
        String url = this.baseUrl + "/api/v1/notify";
        String jsonBody = objectMapper.writeValueAsString(request);

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + this.token)
                .post(body)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Unexpected code " + response + ". Body: " + errorBody);
            }

            if (response.body() != null) {
                return objectMapper.readValue(response.body().string(), Map.class);
            } else {
                return Map.of("status", "success");
            }
        }
    }
}
