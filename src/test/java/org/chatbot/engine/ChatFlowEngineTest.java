package org.chatbot.engine;

import org.chatbot.ai.IntentDetector;
import org.chatbot.configuration.FlowConfigService;
import org.chatbot.conversationMessage.service.ConversationMessageService;
import org.chatbot.flow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChatFlowEngineTest {

    @Mock
    private FlowConfigService flowConfigService;

    @Mock
    private ConversationStateService stateService;

    @Mock
    private KeywordMatcher keywordMatcher;

    @Mock
    private IntentDetector intentDetector;

    @Mock
    private ConversationMessageService messageService;

    private ChatFlowEngine engine;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        engine = new ChatFlowEngine(flowConfigService, stateService, keywordMatcher, messageService, intentDetector);
    }

    /**
     * Creates a simple flow consisting of an initial greeting message followed
     * by a wait-for-response block. Invoking {@code start()} should
     * return the greeting and set the conversation state to the wait block.
     */
    @Test
    public void testStartReturnsInitialWriteMessagesAndSetsState() {
        // Arrange a flow with a greeting followed by a wait block
        Block greeting = new Block("greeting", BlockType.WRITE_MESSAGE, "Hello", "wait", null, null, null);
        Block wait = new Block("wait", BlockType.WAIT_FOR_RESPONSE, null, null, null, null, null);
        Flow flow = new Flow("greeting", List.of(greeting, wait));
        when(flowConfigService.getFlow()).thenReturn(flow);

        // Act
        List<String> messages = engine.start("session1");

        // Assert: one greeting message is returned
        assertEquals(1, messages.size());
        assertEquals("Hello", messages.get(0));
        // Verify the bot message was persisted and state was set to wait
        verify(messageService, times(1)).saveBotMessage("session1", "Hello", "greeting");
        verify(stateService, times(1)).setCurrentBlock("session1", "wait");
    }

    /**
     * Tests that onUserMessage with a WAIT_FOR_RESPONSE block delegates
     * to the KeywordMatcher and returns the next write message. The state
     * should remain at the wait block after sending the reply.
     */
    @Test
    public void testOnUserMessageWaitForResponseUsesKeywordMatcher() {
        // Build a flow: greeting -> wait -> price
        Block greeting = new Block("greeting", BlockType.WRITE_MESSAGE, "Hi", "wait", null, null, null);
        MatchSpec priceSpec = new MatchSpec(MatchType.KEYWORD, List.of("price"));
        Route priceRoute = new Route(priceSpec, "price");
        Block wait = new Block("wait", BlockType.WAIT_FOR_RESPONSE, null, null, List.of(priceRoute), null, null);
        Block price = new Block("price", BlockType.WRITE_MESSAGE, "Price is 49", null, null, null, null);
        Flow flow = new Flow("greeting", List.of(greeting, wait, price));
        when(flowConfigService.getFlow()).thenReturn(flow);
        // Simulate that the current block is the wait block
        when(stateService.getCurrentBlock("session2")).thenReturn("wait");
        // Configure keyword matcher to match on "price"
        when(keywordMatcher.resolveNext(wait, "What is the price?")).thenReturn("price");

        // Act
        List<String> messages = engine.onUserMessage("session2", "What is the price?");

        // Assert: the reply contains the price message
        assertEquals(1, messages.size());
        assertEquals("Price is 49", messages.get(0));
        // Verify the user message was stored
        verify(messageService, times(1)).saveUserMessage("session2", "What is the price?", "wait");
        // Verify the bot message was stored
        verify(messageService, times(1)).saveBotMessage("session2", "Price is 49", "price");
        // The state should not move to a new block (next is null)
        verify(stateService, never()).setCurrentBlock(eq("session2"), eq("price"));
    }

    /**
     * Tests that onUserMessage with DETECT_RESPONSE_INTENT calls the intent detector
     * and uses its result to select the next block. If LLM returns a recognised intent
     * name, the corresponding write message should be returned.
     */
    @Test
    public void testOnUserMessageDetectResponseIntentUsesLLM() {
        // New format using intent options
        IntentOption priceOpt = new IntentOption("price", List.of("price"), "priceBlock");
        Block detect = new Block("detect", BlockType.DETECT_RESPONSE_INTENT, null, null, null, List.of(priceOpt), "fallbackBlock");
        Block priceBlock = new Block("priceBlock", BlockType.WRITE_MESSAGE, "Price is 49", null, null, null, null);
        Block fallbackBlock = new Block("fallbackBlock", BlockType.WRITE_MESSAGE, "Fallback", null, null, null, null);
        Flow flow = new Flow("detect", List.of(detect, priceBlock, fallbackBlock));
        when(flowConfigService.getFlow()).thenReturn(flow);
        when(stateService.getCurrentBlock("sess3")).thenReturn("detect");
        // LLM detects "price" intent
        when(intentDetector.detectIntent("How much?", List.of("price"))).thenReturn(Optional.of("price"));

        List<String> replies = engine.onUserMessage("sess3", "How much?");

        assertEquals(List.of("Price is 49"), replies);
        // Verify we stored the user message with block id "detect"
        verify(messageService).saveUserMessage("sess3", "How much?", "detect");
        // And the bot reply for price
        verify(messageService).saveBotMessage("sess3", "Price is 49", "priceBlock");
        // State should not move because priceBlock has no next
        verify(stateService, never()).setCurrentBlock(eq("sess3"), eq("priceBlock"));
    }

    /**
     * Tests that if the intent detector fails (returns empty), the engine falls
     * back to keyword matching and then to the fallback block if no keyword matches.
     */
    @Test
    public void testDetectIntentFallbackToKeywordsAndFallbackBlock() {
        // New format: two intent options but user message doesn't match keywords
        IntentOption priceOpt = new IntentOption("price", List.of("price"), "priceBlock");
        IntentOption hoursOpt = new IntentOption("hours", List.of("hours"), "hoursBlock");
        Block detect = new Block("detect", BlockType.DETECT_RESPONSE_INTENT, null, null, null, List.of(priceOpt, hoursOpt), "fallbackBlock");
        Block priceBlock = new Block("priceBlock", BlockType.WRITE_MESSAGE, "Price", null, null, null, null);
        Block hoursBlock = new Block("hoursBlock", BlockType.WRITE_MESSAGE, "9 to 5", null, null, null, null);
        Block fallbackBlock = new Block("fallbackBlock", BlockType.WRITE_MESSAGE, "Sorry, I didn't understand.", null, null, null, null);
        Flow flow = new Flow("detect", List.of(detect, priceBlock, hoursBlock, fallbackBlock));
        when(flowConfigService.getFlow()).thenReturn(flow);
        when(stateService.getCurrentBlock("sess4")).thenReturn("detect");
        // LLM returns empty (no intent detected)
        when(intentDetector.detectIntent(anyString(), anyList())).thenReturn(Optional.empty());

        // User writes something unrelated
        List<String> replies = engine.onUserMessage("sess4", "Hello there");

        // Should fall back to fallback block
        assertEquals(List.of("Sorry, I didn't understand."), replies);
        verify(messageService).saveUserMessage("sess4", "Hello there", "detect");
        verify(messageService).saveBotMessage("sess4", "Sorry, I didn't understand.", "fallbackBlock");
    }
}