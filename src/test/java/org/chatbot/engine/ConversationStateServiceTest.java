package org.chatbot.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConversationStateServiceTest {

    @Test
    void testSetAndGetCurrentBlock() {
        ConversationStateService service = new ConversationStateService();
        service.setCurrentBlock("session1", "block1");
        assertEquals("block1", service.getCurrentBlock("session1"));
    }

    @Test
    void testUnknownSessionReturnsNull() {
        ConversationStateService service = new ConversationStateService();
        assertNull(service.getCurrentBlock("unknown"));
    }
}
