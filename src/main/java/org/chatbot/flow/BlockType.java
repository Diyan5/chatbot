package org.chatbot.flow;

// Type of block in the conversation
public enum BlockType {
    WRITE_MESSAGE,      // Sent a message
    WAIT_FOR_RESPONSE,  // Wait answer from user
    DETECT_RESPONSE_INTENT // Try fo find intent through OpenAI
}