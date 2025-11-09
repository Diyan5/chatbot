package org.chatbot.web.controller;

import org.chatbot.web.DTO.ChatIn;
import org.chatbot.web.DTO.ChatOut;
import org.chatbot.engine.ChatFlowEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class BotWsController {

    private final ChatFlowEngine engine;
    private final SimpMessageSendingOperations messagingTemplate;

    @Autowired
    public BotWsController(ChatFlowEngine engine, SimpMessageSendingOperations messagingTemplate) {
        this.engine = engine;
        this.messagingTemplate = messagingTemplate;
    }

    // Creates headers for a specific WebSocket session.
    private org.springframework.messaging.MessageHeaders createHeaders(String sessionId) {
        org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor =
                org.springframework.messaging.simp.SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }

    // Start chat upon initial client connection.
    @MessageMapping("/chat.init")
    public void initChat(@Header("simpSessionId") String sessionId) {
        if (sessionId != null) {
            List<String> initial = engine.start(sessionId);
            for (String m : initial) {
                // Send the message to the user identified by the session ID. The
                // headers ensure the sessionId is used as the user identifier.
                messagingTemplate.convertAndSendToUser(sessionId, "/queue/replies",
                        new ChatOut("BOT", m), createHeaders(sessionId));
            }
        }
    }

    // Processes the messages sent by the user.
    @MessageMapping("/chat.user")
    public void handleUserMessage(@Payload ChatIn message,
                                  @Header("simpSessionId") String sessionId) {
        if (sessionId == null) {
            return;
        }
        List<String> replies = engine.onUserMessage(sessionId, message.text());
        for (String m : replies) {
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/replies",
                    new ChatOut("BOT", m), createHeaders(sessionId));
        }
    }
}
