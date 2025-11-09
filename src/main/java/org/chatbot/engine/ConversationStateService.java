package org.chatbot.engine;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Keeps in memory up to which block (stage) of the conversation each user has reached by sessionId.
@Service
public class ConversationStateService {

    private final Map<String, String> sessionToBlock = new ConcurrentHashMap<>();

    public String getCurrentBlock(String sessionId) {
        return sessionToBlock.get(sessionId);
    }

    public void setCurrentBlock(String sessionId, String blockId) {
        if (sessionId != null) {
            sessionToBlock.put(sessionId, blockId);
        }
    }

}