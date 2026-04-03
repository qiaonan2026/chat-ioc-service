package com.chat.ioc.example;

import com.chat.ioc.Autowired;

public class ChatService {
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserService userService;
    
    // Default constructor
    public ChatService() {
    }
    
    public void sendChatMessage(String userId, String message) {
        User user = userService.getUser(userId);
        messageService.sendMessage(message, user.toString());
        notificationService.notifyUser(userId, "New message received");
    }
    
    // Getters for testing
    public MessageService getMessageService() { return messageService; }
    public NotificationService getNotificationService() { return notificationService; }
    public UserService getUserService() { return userService; }
}