package org.chatbot.engine;

import org.chatbot.flow.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KeywordMatcherTest {

    @Test
    void testKeywordMatch() {
        MatchSpec spec = new MatchSpec(MatchType.KEYWORD, List.of("price", "cost"));
        Route route = new Route(spec, "nextBlock");
        Block block = new Block("1", BlockType.WAIT_FOR_RESPONSE, null, null, List.of(route), null, null);

        KeywordMatcher matcher = new KeywordMatcher();
        String result = matcher.resolveNext(block, "What is the price?");
        assertEquals("nextBlock", result);
    }

    @Test
    void testFallbackMatch() {
        MatchSpec fallbackSpec = new MatchSpec(MatchType.FALLBACK, null);
        Route route = new Route(fallbackSpec, "fallbackBlock");
        Block block = new Block("2", BlockType.WAIT_FOR_RESPONSE, null, null, List.of(route), null, null);

        KeywordMatcher matcher = new KeywordMatcher();
        String result = matcher.resolveNext(block, "random text");
        assertEquals("fallbackBlock", result);
    }
}
