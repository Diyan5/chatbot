package org.chatbot.web.DTO;

//Message sent from the chatbot to the client
public record ChatOut(String sender, String content) {}