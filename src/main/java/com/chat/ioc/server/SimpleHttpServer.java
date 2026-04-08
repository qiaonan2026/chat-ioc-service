package com.chat.ioc.server;

import com.chat.ioc.config.AppConfig;
import com.chat.ioc.controller.AuthController;
import com.chat.ioc.controller.HomeController;
import com.chat.ioc.entity.ApiResponse;
import com.chat.ioc.entity.LoginRequest;
import com.chat.ioc.entity.LoginResponse;
import com.chat.ioc.entity.RegisterRequest;
import com.chat.ioc.entity.UpdateUserRequest;
import com.chat.ioc.service.AuthService;
import com.chat.ioc.service.HomePageService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单的HTTP服务器，用于处理API请求
 */
public class SimpleHttpServer {
    
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    
    private final HomeController homeController;
    private final AuthController authController;
    
    public SimpleHttpServer() {
        // 初始化IoC容器和控制器
        var container = AppConfig.createContainer();
        HomePageService homePageService = (HomePageService) container.getBean("homePageService");
        this.homeController = new HomeController(homePageService);
        
        AuthService authService = (AuthService) container.getBean("authService");
        this.authController = new AuthController(authService);
    }
    
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        isRunning = true;
        
        System.out.println("HTTP Server started on port " + port);
        
        while (isRunning) {
            Socket clientSocket = serverSocket.accept();
            handleRequest(clientSocket);
        }
    }
    
    private void handleRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedWriter dataOut = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8))) {
            
            // 读取请求行
            String requestLine = in.readLine();
            if (requestLine == null) return;
            
            String[] parts = requestLine.split(" ");
            if (parts.length != 3) return;
            
            String method = parts[0];
            String path = parts[1];
            
            // 读取请求头并存储Content-Length和Authorization
            String line;
            String contentLengthHeader = null;
            String authorizationHeader = null;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLengthHeader = line.substring("content-length:".length()).trim();
                } else if (line.toLowerCase().startsWith("authorization:")) {
                    authorizationHeader = line.substring("authorization:".length()).trim();
                }
            }
            
            // 处理POST请求体
            String requestBody = "";
            if ("POST".equalsIgnoreCase(method) && contentLengthHeader != null) {
                int contentLength = Integer.parseInt(contentLengthHeader);
                char[] bodyChars = new char[contentLength];
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int read = in.read(bodyChars, totalRead, contentLength - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }
                requestBody = new String(bodyChars);
            }
            
            // 提取Bearer token
            String extractedToken = null;
            if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer ")) {
                extractedToken = authorizationHeader.substring(7).trim();
            }
            
            // 路由处理
            String response = routeRequest(method, path, requestBody, extractedToken);
            
            // 发送响应
            sendResponse(out, dataOut, response);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private String routeRequest(String method, String path, String requestBody, String token) {
        try {
            // 定义路由模式
            if ("GET".equalsIgnoreCase(method)) {
                if ("/api/home".equals(path)) {
                    var response = homeController.getHome();
                    return toJson(response);
                } else if ("/api/home/detail".equals(path)) {
                    var response = homeController.getDetailedHome();
                    return toJson(response);
                } else if ("/api/ping".equals(path)) {
                    var response = homeController.ping();
                    return toJson(response);
                } else if ("/api/health".equals(path)) {
                    var response = homeController.health();
                    return toJson(response);
                } else if ("/api/me".equals(path) || "/api/auth/me".equals(path)) {
                    // 获取当前用户信息
                    var response = authController.getCurrentUser(token);
                    return toJson(response);
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                if ("/api/login".equals(path) || "/api/auth/login".equals(path)) {
                    // 解析JSON请求体
                    LoginRequest loginRequest = parseLoginRequest(requestBody);
                    if (loginRequest != null) {
                        var response = authController.login(loginRequest);
                        return toJson(response);
                    } else {
                        // 返回错误响应
                        return createErrorResponse(400, "Bad Request: Invalid JSON");
                    }
                } else if ("/api/logout".equals(path)) {
                    // 解析token
                    String tokenFromBody = parseTokenFromRequest(requestBody);
                    var response = authController.logout(tokenFromBody);
                    return toJson(response);
                } else if ("/api/register".equals(path) || "/api/auth/register".equals(path)) {
                    // 解析注册请求体
                    RegisterRequest registerRequest = parseRegisterRequest(requestBody);
                    if (registerRequest != null) {
                        var response = authController.register(registerRequest);
                        return toJson(response);
                    } else {
                        // 返回错误响应
                        return createErrorResponse(400, "Bad Request: Invalid JSON");
                    }
                } else if ("/api/me/sync".equals(path) || "/api/user/sync".equals(path)) {
                    UpdateUserRequest updateRequest = parseUpdateUserRequest(requestBody);
                    if (updateRequest != null) {
                        var response = authController.syncCurrentUser(token, updateRequest);
                        return toJson(response);
                    } else {
                        return createErrorResponse(400, "Bad Request: Invalid JSON");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(500, "Internal Server Error: " + e.getMessage());
        }
        
        return createErrorResponse(404, "Not Found");
    }
    
    private LoginRequest parseLoginRequest(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                System.err.println("Empty or null JSON request body");
                return null;
            }
            
            System.out.println("Parsing login request JSON: " + json);
            
            // 简单解析JSON字符串
            Pattern usernamePattern = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"");
            Pattern passwordPattern = Pattern.compile("\"password\"\\s*:\\s*\"([^\"]+)\"");
            
            Matcher usernameMatcher = usernamePattern.matcher(json);
            Matcher passwordMatcher = passwordPattern.matcher(json);
            
            String username = null;
            String password = null;
            
            if (usernameMatcher.find()) {
                username = usernameMatcher.group(1);
            } else {
                System.err.println("Username not found in JSON: " + json);
            }
            
            if (passwordMatcher.find()) {
                password = passwordMatcher.group(1);
            } else {
                System.err.println("Password not found in JSON: " + json);
            }
            
            if (username != null && password != null) {
                System.out.println("Successfully parsed login request for username: " + username);
                return new LoginRequest(username, password);
            } else {
                System.err.println("Failed to parse login request - missing username or password");
            }
        } catch (Exception e) {
            System.err.println("Exception occurred while parsing login request: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    private String parseTokenFromRequest(String json) {
        try {
            Pattern tokenPattern = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
            Matcher tokenMatcher = tokenPattern.matcher(json);
            
            if (tokenMatcher.find()) {
                return tokenMatcher.group(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private RegisterRequest parseRegisterRequest(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                System.err.println("Empty or null JSON request body for registration");
                return null;
            }
            
            System.out.println("Parsing register request JSON: " + json);
            
            // 简单解析JSON字符串
            Pattern usernamePattern = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"");
            Pattern passwordPattern = Pattern.compile("\"password\"\\s*:\\s*\"([^\"]+)\"");
            Pattern emailPattern = Pattern.compile("\"email\"\\s*:\\s*\"([^\"]+)\"");
            Pattern nicknamePattern = Pattern.compile("\"nickname\"\\s*:\\s*\"([^\"]+)\"");
            
            Matcher usernameMatcher = usernamePattern.matcher(json);
            Matcher passwordMatcher = passwordPattern.matcher(json);
            Matcher emailMatcher = emailPattern.matcher(json);
            Matcher nicknameMatcher = nicknamePattern.matcher(json);
            
            String username = null;
            String password = null;
            String email = null;
            String nickname = null;
            
            if (usernameMatcher.find()) {
                username = usernameMatcher.group(1);
            } else {
                System.err.println("Username not found in JSON: " + json);
            }
            
            if (passwordMatcher.find()) {
                password = passwordMatcher.group(1);
            } else {
                System.err.println("Password not found in JSON: " + json);
            }
            
            if (emailMatcher.find()) {
                email = emailMatcher.group(1);
            } else {
                System.err.println("Email not found in JSON: " + json);
            }
            
            if (nicknameMatcher.find()) {
                nickname = nicknameMatcher.group(1);
            } else {
                System.err.println("Nickname not found in JSON: " + json);
            }
            
            if (username != null && password != null) {
                System.out.println("Successfully parsed register request for username: " + username);
                RegisterRequest request = new RegisterRequest();
                request.setUsername(username);
                request.setPassword(password);
                request.setEmail(email);
                request.setNickname(nickname);
                return request;
            } else {
                System.err.println("Failed to parse register request - missing required fields (username or password)");
            }
        } catch (Exception e) {
            System.err.println("Exception occurred while parsing register request: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private UpdateUserRequest parseUpdateUserRequest(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                System.err.println("Empty or null JSON request body for user update");
                return null;
            }

            Pattern emailPattern = Pattern.compile("\"email\"\\s*:\\s*\"([^\"]*)\"");
            Pattern nicknamePattern = Pattern.compile("\"nickname\"\\s*:\\s*\"([^\"]*)\"");

            Matcher emailMatcher = emailPattern.matcher(json);
            Matcher nicknameMatcher = nicknamePattern.matcher(json);

            String email = null;
            String nickname = null;

            if (emailMatcher.find()) {
                email = emailMatcher.group(1);
            }
            if (nicknameMatcher.find()) {
                nickname = nicknameMatcher.group(1);
            }

            // allow partial update, but require at least one field
            if (email == null && nickname == null) {
                return null;
            }

            UpdateUserRequest req = new UpdateUserRequest();
            req.setEmail(email);
            req.setNickname(nickname);
            return req;
        } catch (Exception e) {
            System.err.println("Exception occurred while parsing update user request: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String toJson(Object obj) {
        // 简单的对象到JSON转换
        if (obj instanceof ApiResponse) {
            ApiResponse<?> apiResponse = (ApiResponse<?>) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"code\":").append(apiResponse.getCode()).append(",");
            sb.append("\"message\":\"").append(escapeJson(apiResponse.getMessage())).append("\",");
            
            if (apiResponse.getData() != null) {
                sb.append("\"data\":").append(objectToJson(apiResponse.getData()));
            } else {
                sb.append("\"data\":null");
            }
            
            sb.append("}");
            return sb.toString();
        }
        return objectToJson(obj);
    }
    
    private String objectToJson(Object obj) {
        if (obj instanceof LoginResponse) {
            LoginResponse lr = (LoginResponse) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"success\":").append(lr.isSuccess()).append(",");
            if (lr.getToken() != null) {
                sb.append("\"token\":\"").append(escapeJson(lr.getToken())).append("\",");
            } else {
                sb.append("\"token\":null,");
            }
            if (lr.getUser() != null) {
                sb.append("\"user\":").append(objectToJson(lr.getUser())).append(",");
            } else {
                sb.append("\"user\":null,");
            }
            sb.append("\"message\":\"").append(escapeJson(lr.getMessage())).append("\"");
            sb.append("}");
            return sb.toString();
        } else if (obj instanceof com.chat.ioc.entity.User) {
            com.chat.ioc.entity.User user = (com.chat.ioc.entity.User) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"id\":").append(user.getId()).append(",");
            sb.append("\"username\":\"").append(escapeJson(user.getUsername())).append("\",");
            sb.append("\"email\":\"").append(escapeJson(user.getEmail())).append("\",");
            sb.append("\"nickname\":\"").append(escapeJson(user.getNickname())).append("\"");
            sb.append("}");
            return sb.toString();
        } else if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        } else if (obj instanceof com.chat.ioc.entity.HomePageInfo) {
            com.chat.ioc.entity.HomePageInfo info = (com.chat.ioc.entity.HomePageInfo) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"title\":\"").append(escapeJson(info.getTitle())).append("\",");
            sb.append("\"description\":\"").append(escapeJson(info.getDescription())).append("\",");
            sb.append("\"version\":\"").append(escapeJson(info.getVersion())).append("\",");
            sb.append("\"environment\":\"").append(escapeJson(info.getEnvironment())).append("\",");
            sb.append("\"activeUsers\":").append(info.getActiveUsers()).append(",");
            sb.append("\"status\":\"").append(escapeJson(info.getStatus())).append("\"");
            sb.append("}");
            return sb.toString();
        } else {
            return "\"" + obj.toString() + "\"";
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) return null;
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    private String createErrorResponse(int statusCode, String message) {
        return "{\"code\":" + statusCode + ",\"message\":\"" + escapeJson(message) + "\",\"data\":null}";
    }
    
    private void sendResponse(PrintWriter out, BufferedWriter dataOut, String responseBody) throws IOException {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/json; charset=utf-8");
        out.println("Access-Control-Allow-Origin: *");
        out.println("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
        out.println("Access-Control-Allow-Headers: Content-Type, Authorization");
        out.println("Content-Length: " + responseBody.getBytes(StandardCharsets.UTF_8).length);
        out.println(); // 空行表示头部结束
        dataOut.write(responseBody);
        dataOut.flush();
    }
    
    public void stop() throws IOException {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}