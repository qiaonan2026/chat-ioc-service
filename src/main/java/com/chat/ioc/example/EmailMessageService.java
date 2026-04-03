package com.chat.ioc.example;

public class EmailMessageService implements MessageService {
    @Override
    public void sendMessage(String message, String recipient) {
        System.out.println("Sending email: '" + message + "' to " + recipient);
    }
}