package org.chatbot.ai;

import java.util.List;
import java.util.Optional;

//Detects the intent in the user's text.
public interface IntentDetector {

    //Finds the most suitable intent from the list.
    Optional<String> detectIntent(String userText, List<String> intents);
}