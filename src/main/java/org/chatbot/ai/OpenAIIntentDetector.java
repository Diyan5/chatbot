package org.chatbot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Component
@Slf4j
public class OpenAIIntentDetector implements IntentDetector {

    private final String apiKey;
    private final String model;
    private final String apiUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIIntentDetector(
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.api.model:gpt-3.5-turbo}") String model,
            @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}") String apiUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    //Tries to classify userText into one of the provided intents.
    //Returns Optional.empty() if the key/inputs/error/unconvincing response is missing.
    @Override
    public Optional<String> detectIntent(String userText, List<String> intents) {
        // Validate input and configuration.
        if (apiKey == null || apiKey.isBlank() || userText == null || intents == null || intents.isEmpty()) {
            return Optional.empty();
        }
        try {
            // Build a prompt instructing the model to select one intent or NONE. The system message
            // provides the classification rules, and the user message lists the possible intents and
            // user text. The model is expected to respond with the exact intent name or "NONE".
            String possibleIntents = String.join(", ", intents);
            String systemMessage = "You are an intent classifier. You will be provided with a set of possible intents and a user message. " +
                    "Return the name of the intent that best matches the user message. If none of the intents apply, return NONE.";
            String userMessage = "Possible intents: " + possibleIntents + "\n" +
                    "User message: " + userText + "\n" +
                    "Intent:";

            // Construct the request body according to OpenAI Chat API format.
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            // Build messages array: system and user messages.
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", systemMessage),
                    Map.of("role", "user", "content", userMessage)
            );
            body.put("messages", messages);
            body.put("max_tokens", 10);
            body.put("temperature", 0);

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    // Chat completions return "message" with "content"; Fallback to "text" for older API.
                    JsonNode first = choices.get(0);
                    JsonNode messageNode = first.path("message");
                    String content;
                    if (messageNode.isObject()) {
                        content = messageNode.path("content").asText();
                    } else {
                        content = first.path("text").asText();
                    }
                    if (content != null) {
                        String intent = content.trim();
                        // Normalize and check if the response matches one of the candidate intents.
                        for (String candidate : intents) {
                            if (candidate.equalsIgnoreCase(intent)) {
                                return Optional.of(candidate);
                            }
                        }
                        // If the model responded with NONE or something else, return empty.
                    }
                }
            } else {
                // Non-successful response; log status and body for diagnostics
                System.err.println("OpenAI API call returned status " + status + " with body: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.warn("OpenAI intent detection failed: {}", e.getMessage());
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }
}