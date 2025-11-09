package org.chatbot.flow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Determines a path between blocks: if it matches, it proceeds to the next.
@JsonIgnoreProperties(ignoreUnknown = true)
public record Route(
        MatchSpec match,
        String next
) {}