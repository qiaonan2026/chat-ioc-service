package com.chat.ioc.example;

import com.chat.ioc.IOCContainer;

public class ChatApplication {
    public static void main(String[] args) {
        System.out.println("Starting Chat Application with IOC Container...");
        
        // Create and configure the IOC container
        IOCContainer container = new IOCContainer();
        
        // Register services
        container.register("messageService", EmailMessageService.class);
        container.register("notificationService", PushNotificationService.class);
        container.register("userService", DatabaseUserService.class);
        container.register("chatService", ChatService.class);  // Will have dependencies injected
        
        // Retrieve and use the chat service
        ChatService chatService = (ChatService) container.getBean("chatService");
        
        // Test the functionality
        System.out.println("\nTesting chat service:");
        chatService.sendChatMessage("123", "Hello from IOC container!");
        
        System.out.println("\nServices successfully injected:");
        System.out.println("- Message service: " + chatService.getMessageService().getClass().getSimpleName());
        System.out.println("- Notification service: " + chatService.getNotificationService().getClass().getSimpleName());
        System.out.println("- User service: " + chatService.getUserService().getClass().getSimpleName());
        
        System.out.println("\nDemonstrating singleton behavior:");
        ChatService chatService2 = (ChatService) container.getBean("chatService");
        System.out.println("Are both instances the same? " + (chatService == chatService2));
    }
}