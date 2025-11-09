package org.chatbot.engine;

import org.chatbot.flow.Block;
import org.chatbot.flow.MatchSpec;
import org.chatbot.flow.MatchType;
import org.chatbot.flow.Route;
import org.springframework.stereotype.Component;


// Checks if the user's message contains keywords from the block and returns the next block according to the match.
@Component
public class KeywordMatcher {

    public String resolveNext(Block block, String userText) {
        if (block == null || block.on() == null) {
            return null;
        }
        String text = userText == null ? "" : userText.toLowerCase();
        // First evaluate KEYWORD and INTENT routes in order of definition.
        for (Route r : block.on()) {
            MatchSpec spec = r.match();
            if (spec == null) {
                continue;
            }
            MatchType type = spec.type();
            if (type == MatchType.KEYWORD && spec.anyOf() != null) {
                for (String keyword : spec.anyOf()) {
                    if (keyword != null && text.contains(keyword.toLowerCase())) {
                        return r.next();
                    }
                }
            }
            // INTENT matching is intentionally left unimplemented. A real implementation
            // would call an external service to detect the intent and compare it with
            // spec.getAnyOf(). For now we skip these routes.
        }
        // If no keyword match was found, fall back to any FALLBACK route.
        for (Route r : block.on()) {
            MatchSpec spec = r.match();
            if (spec != null && spec.type() == MatchType.FALLBACK) {
                return r.next();
            }
        }
        return null;
    }
}