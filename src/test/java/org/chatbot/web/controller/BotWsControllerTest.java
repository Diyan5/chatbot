package org.chatbot.web.controller;

import org.chatbot.engine.ChatFlowEngine;
import org.chatbot.web.DTO.ChatIn;
import org.chatbot.web.DTO.ChatOut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BotWsControllerTest {

    private ChatFlowEngine engine;
    private SimpMessageSendingOperations messaging;
    private BotWsController controller;

    @BeforeEach
    void setUp() {
        engine = mock(ChatFlowEngine.class);
        messaging = mock(SimpMessageSendingOperations.class);
        controller = new BotWsController(engine, messaging);
    }

    @Test
    void testInitChatSendsInitialMessages() {
        when(engine.start("sid")).thenReturn(List.of("Hello", "How can I help?"));

        controller.initChat("sid");

        // Capture the ChatOut messages sent
        ArgumentCaptor<ChatOut> captor = ArgumentCaptor.forClass(ChatOut.class);
        verify(messaging, times(2))
                .convertAndSendToUser(eq("sid"), eq("/queue/replies"), captor.capture(), any(MessageHeaders.class));

        List<ChatOut> messages = captor.getAllValues();
        assertEquals("Hello", messages.get(0).content());
        assertEquals("How can I help?", messages.get(1).content());
    }

    @Test
    void testHandleUserMessageSendsBotReplies() {
        when(engine.onUserMessage("sid", "price")).thenReturn(List.of("The price is 49"));

        controller.handleUserMessage(new ChatIn("price"), "sid");

        ArgumentCaptor<ChatOut> captor = ArgumentCaptor.forClass(ChatOut.class);
        verify(messaging)
                .convertAndSendToUser(eq("sid"), eq("/queue/replies"), captor.capture(), any(MessageHeaders.class));

        ChatOut message = captor.getValue();
        assertEquals("The price is 49", message.content());
    }

    @Test
    void testHandleUserMessageWithNullSessionDoesNothing() {
        controller.handleUserMessage(new ChatIn("price"), null);
        verifyNoInteractions(engine);
        verifyNoInteractions(messaging);
    }
}
