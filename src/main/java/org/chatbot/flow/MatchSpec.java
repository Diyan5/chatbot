package org.chatbot.flow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// Matching conditions — how the bot recognizes the user's response.
@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchSpec(
        MatchType type,
        List<String> anyOf
) {}