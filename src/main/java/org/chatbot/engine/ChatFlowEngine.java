package org.chatbot.engine;

import lombok.extern.slf4j.Slf4j;
import org.chatbot.ai.IntentDetector;
import org.chatbot.configuration.FlowConfigService;
import org.chatbot.conversationMessage.service.ConversationMessageService;
import org.chatbot.flow.Block;
import org.chatbot.flow.BlockType;
import org.chatbot.flow.Flow;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// The chatbot's main logic – processes the conversation logic according to the JSON configuration.
@Service
@Slf4j
public class ChatFlowEngine {

    private final FlowConfigService flowConfigService;
    private final ConversationStateService stateService;
    private final KeywordMatcher keywordMatcher;
    private final IntentDetector intentDetector;
    private final ConversationMessageService messageService;

    public ChatFlowEngine(FlowConfigService flowConfigService,
                          ConversationStateService stateService,
                          KeywordMatcher keywordMatcher,
                          ConversationMessageService messageService,
                          IntentDetector intentDetector) {
        this.flowConfigService = flowConfigService;
        this.stateService = stateService;
        this.keywordMatcher = keywordMatcher;
        this.messageService = messageService;
        this.intentDetector = intentDetector;
    }

    // Start a new conversation by sending the first messages to the chatbot according to the initial block.
    public List<String> start(String sessionId) {
        Flow flow = flowConfigService.getFlow();
        if (flow == null) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        String nextId = flow.getStartBlockId();
        while (nextId != null) {
            Block b = flow.byId(nextId);
            if (b == null) {
                break;
            }
            if (b.type() == BlockType.WRITE_MESSAGE) {
                if (b.message() != null) {
                    out.add(b.message());
                    messageService.saveBotMessage(sessionId, b.message(), b.id());
                }
                nextId = b.next();
            } else if (b.type() == BlockType.WAIT_FOR_RESPONSE || b.type() == BlockType.DETECT_RESPONSE_INTENT) {
                stateService.setCurrentBlock(sessionId, b.id());
                break;
            } else {
                break;
            }
        }
        return out;
    }

    // Processes an incoming message from the user and finds the next block in the conversation.
    public List<String> onUserMessage(String sessionId, String userText) {
        Flow flow = flowConfigService.getFlow();
        if (flow == null) {
            return Collections.emptyList();
        }
        // If there is no current block, start a new conversation.
        String currentBlockId = stateService.getCurrentBlock(sessionId);
        if (currentBlockId == null) {
            return start(sessionId);
        }
        Block current = flow.byId(currentBlockId);
        if (current == null) {
            return start(sessionId);
        }
        // Save the user's message in the history with the current block context.
        messageService.saveUserMessage(sessionId, userText, current.id());
        String nextId;

        // Determine next block based on the current block type. For WAIT_FOR_RESPONSE we use keyword matching,
        // for DETECT_RESPONSE_INTENT we attempt to detect the user's intent via the configured detector.
        if (current.type() == BlockType.WAIT_FOR_RESPONSE) {
            nextId = keywordMatcher.resolveNext(current, userText);
        } else if (current.type() == BlockType.DETECT_RESPONSE_INTENT) {
            nextId = resolveNextForIntent(current, userText);
        } else {
            nextId = null;
        }
        List<String> out = new ArrayList<>();
        while (nextId != null) {
            Block b = flow.byId(nextId);
            if (b == null) {
                break;
            }
            if (b.type() == BlockType.WRITE_MESSAGE) {
                if (b.message() != null) {
                    out.add(b.message());
                    messageService.saveBotMessage(sessionId, b.message(), b.id());
                }
                nextId = b.next();
            } else if (b.type() == BlockType.WAIT_FOR_RESPONSE || b.type() == BlockType.DETECT_RESPONSE_INTENT) {
                stateService.setCurrentBlock(sessionId, b.id());
                nextId = null;
            } else {
                nextId = null;
            }
        }
        return out;
    }

    // Attempts to determine the user's intent through OpenAI; if unsuccessful, falls back to keywords.
    private String resolveNextForIntent(Block block, String userText) {
        if (block == null) {
            return null;
        }

        // There are two possible representations for DETECT_RESPONSE_INTENT blocks:
        //  1) The older "routes" representation using Block.on with Route/MatchSpec entries.
        //  2) The newer "intents" representation using Block.intents and Block.fallback.

        // First handle the newer "intents" format if present. If intents are defined
        // explicitly, they take precedence over the older format. Otherwise, fall
        // back to processing the routes list.
        if (block.intents() != null && !block.intents().isEmpty()) {
            return resolveNextFromIntentOptions(block, userText);
        }

        if (block.on() == null) {
            return null;
        }

        List<String> candidateIntents = new ArrayList<>();
        for (org.chatbot.flow.Route r : block.on()) {
            var spec = r.match();
            if (spec != null && spec.type() == org.chatbot.flow.MatchType.INTENT && spec.anyOf() != null) {
                candidateIntents.addAll(spec.anyOf());
            }
        }

        String intent = null;
        if (!candidateIntents.isEmpty()) {
            try {
                var detected = intentDetector.detectIntent(userText, candidateIntents);
                if (detected.isPresent()) {
                    intent = detected.get();
                } else {
                    log.info("LLM returned no intent for input '{}'", userText);
                }
            } catch (Exception e) {
                log.warn("LLM intent detection failed: {}", e.toString());
                intent = null;
            }
        }

        if (intent == null || intent.isBlank()) {
            log.info("Falling back to keyword matcher for user input: '{}'", userText);
            String keywordNext = keywordMatcher.resolveNext(block, userText);
            if (keywordNext != null) {
                return keywordNext;
            }
        }

        if (intent != null && !intent.isBlank()) {
            for (org.chatbot.flow.Route r : block.on()) {
                var spec = r.match();
                if (spec != null && spec.type() == org.chatbot.flow.MatchType.INTENT && spec.anyOf() != null
                        && spec.anyOf().contains(intent)) {
                    return r.next();
                }
            }
        }

        log.info("No matching route found, using fallback for block '{}'", block.id());
        return keywordMatcher.resolveNext(block, userText);
    }

    // Finds the next block through the list of possible intents or fallback, if there is no match.
    private String resolveNextFromIntentOptions(Block block, String userText) {

        List<String> intentNames = new ArrayList<>();
        for (org.chatbot.flow.IntentOption opt : block.intents()) {
            if (opt != null && opt.name() != null) {
                intentNames.add(opt.name());
            }
        }
        String detectedIntent = null;
        if (!intentNames.isEmpty()) {
            try {
                var detected = intentDetector.detectIntent(userText, intentNames);
                if (detected.isPresent()) {
                    detectedIntent = detected.get();
                } else {
                    log.info("LLM returned no intent for input '{}'", userText);
                }
            } catch (Exception e) {
                log.warn("LLM intent detection failed: {}", e.toString());
                detectedIntent = null;
            }
        }

        if (detectedIntent != null && !detectedIntent.isBlank()) {
            for (org.chatbot.flow.IntentOption opt : block.intents()) {
                if (opt != null && opt.name() != null && opt.name().equalsIgnoreCase(detectedIntent)) {
                    return opt.next();
                }
            }
        }

        String lower = userText == null ? "" : userText.toLowerCase();
        for (org.chatbot.flow.IntentOption opt : block.intents()) {
            if (opt != null && opt.keywords() != null) {
                for (String kw : opt.keywords()) {
                    if (kw != null && !kw.isBlank() && lower.contains(kw.toLowerCase())) {
                        return opt.next();
                    }
                }
            }
        }

        if (block.fallback() != null && !block.fallback().isBlank()) {
            return block.fallback();
        }

        return null;
    }

}