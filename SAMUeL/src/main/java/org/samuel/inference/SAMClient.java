package org.samuel.inference;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client for SAM FastAPI backend.
 */
public class SAMClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String endpoint;

    public SAMClient(String endpoint) {
        this.endpoint = endpoint;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
    }

    public SAMResponse segment(SAMRequest request) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/segment"))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("SAM server error " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readValue(response.body(), SAMResponse.class);
        } catch (IOException e) {
            if (e.getMessage().contains("Connection reset by peer") || e.getMessage().contains("bytes received: 0")) {
                throw new IOException("Connection to SAM backend failed. The Python backend may not be running or may have crashed. Please check that the backend is started and accessible at " + endpoint, e);
            }
            throw e;
        }
    }
    
    public void printRequest(SAMRequest request) {
        try {
            String json = objectMapper.writeValueAsString( request.getTileId() );
            System.out.println("---- SAM REQUEST ----");
            System.out.println("Length: " + json.length());
            System.out.println(json.substring(0, Math.min(500, json.length())));
            System.out.println("---------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isHealthy() {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
