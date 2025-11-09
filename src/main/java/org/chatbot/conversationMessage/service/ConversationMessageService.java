package org.chatbot.conversationMessage.service;

import org.chatbot.conversationMessage.model.ConversationMessage;
import org.chatbot.conversationMessage.repository.ConversationMessageRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ConversationMessageService {

    private final ConversationMessageRepository repository;

    public ConversationMessageService(ConversationMessageRepository repository) {
        this.repository = repository;
    }

    public void saveUserMessage(String sessionId, String content, String blockId) {
        ConversationMessage msg = ConversationMessage.builder()
                .sessionId(sessionId)
                .sender("USER")
                .content(content)
                .blockId(blockId)
                .createdAt(Instant.now())
                .build();
        repository.save(msg);
    }

    public void saveBotMessage(String sessionId, String content, String blockId) {
        ConversationMessage msg = ConversationMessage.builder()
                .sessionId(sessionId)
                .sender("BOT")
                .content(content)
                .blockId(blockId)
                .createdAt(Instant.now())
                .build();
        repository.save(msg);
    }
}