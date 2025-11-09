package org.chatbot.conversationMessage.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "conversation_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column
    private String blockId;

    @Column(nullable = false)
    private Instant createdAt;
}