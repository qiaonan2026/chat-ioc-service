package com.chat.ioc.example;

import com.chat.ioc.IOCContainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChatApplicationTest {

    @Test
    public void testChatApplicationExample() {
        // Create and configure the IOC container
        IOCContainer container = new IOCContainer();
        
        // Register services
        container.register("messageService", EmailMessageService.class);
        container.register("notificationService", PushNotificationService.class);
        container.register("userService", DatabaseUserService.class);
        container.register("chatService", ChatService.class);  // Will have dependencies injected
        
        // Retrieve and use the chat service
        ChatService chatService = (ChatService) container.getBean("chatService");
        
        // Verify that all dependencies were injected
        assertNotNull(chatService);
        assertNotNull(chatService.getMessageService());
        assertNotNull(chatService.getNotificationService());
        assertNotNull(chatService.getUserService());
        
        // Verify the correct types were injected
        assertEquals(EmailMessageService.class, chatService.getMessageService().getClass());
        assertEquals(PushNotificationService.class, chatService.getNotificationService().getClass());
        assertEquals(DatabaseUserService.class, chatService.getUserService().getClass());
        
        // Test the functionality
        assertDoesNotThrow(() -> {
            chatService.sendChatMessage("testUser", "Test message");
        });
    }
    
    @Test
    public void testSingletonBehavior() {
        // Create and configure the IOC container
        IOCContainer container = new IOCContainer();
        container.register("chatService", ChatService.class);
        
        // Get the same bean twice
        ChatService service1 = (ChatService) container.getBean("chatService");
        ChatService service2 = (ChatService) container.getBean("chatService");
        
        // They should be the same instance (singleton)
        assertSame(service1, service2);
    }
    
    @Test
    public void testPrototypeScope() {
        // Create and configure the IOC container
        IOCContainer container = new IOCContainer();
        container.register("chatService", ChatService.class, false); // Not a singleton
        
        // Get the same bean twice
        ChatService service1 = (ChatService) container.getBean("chatService");
        ChatService service2 = (ChatService) container.getBean("chatService");
        
        // They should be different instances (prototype)
        assertNotSame(service1, service2);
    }
}