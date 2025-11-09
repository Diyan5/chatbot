package org.chatbot.conversationMessage.repository;

import org.chatbot.conversationMessage.model.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
}