package com.chat.ioc;

import com.chat.ioc.server.SimpleHttpServer;

/**
 * HTTP服务器启动类
 */
public class HttpServerApplication {
    
    public static void main(String[] args) {
        try {
            int port = 8080;
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
            
            SimpleHttpServer server = new SimpleHttpServer();
            System.out.println("Starting HTTP Server for Chat IOC Service...");
            server.start(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}