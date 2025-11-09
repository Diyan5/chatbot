package org.chatbot.flow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// Describes a possible intent: name, keywords, and next block.
@JsonIgnoreProperties(ignoreUnknown = true)
public record IntentOption(
        String name,
        List<String> keywords,
        String next
) {}