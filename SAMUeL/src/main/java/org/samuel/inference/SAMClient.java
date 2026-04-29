package org.samuel.inference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * HTTP client for SAM FastAPI backend.
 */
public class SAMClient {

    private static final Logger logger = LoggerFactory.getLogger(SAMClient.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String endpoint;

    public SAMClient(String endpoint) {
        this.endpoint = endpoint;
        // Use HTTP/1.1 to avoid protocol upgrade issues with Uvicorn
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public SAMResponse segment(SAMRequest request) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(request);
        logger.debug("SAM /segment request JSON (length={}): {}", json.length(), json);

        // Build request with minimal headers to avoid protocol issues
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/segment"))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        logger.debug("SAM /segment sending POST to: {} with {} bytes", endpoint + "/segment", json.length());

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            logger.debug("SAM /segment response status: {}", response.statusCode());
            logger.debug("SAM /segment response headers: {}", response.headers());
            logger.debug("SAM /segment response body (length={}): {}", response.body().length(), response.body());

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
            String json = objectMapper.writeValueAsString(request);
            System.out.println("---- SAM REQUEST ----");
            System.out.println("Length: " + json.length());
            System.out.println(json.substring(0, Math.min(1000, json.length())));
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
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
