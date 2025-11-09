package org.chatbot.flow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// A block from the JSON file
@JsonIgnoreProperties(ignoreUnknown = true)
public record Block(
        String id,
        BlockType type,
        String message,
        String next,
        List<Route> on,
        List<IntentOption> intents,
        String fallback
) {}
