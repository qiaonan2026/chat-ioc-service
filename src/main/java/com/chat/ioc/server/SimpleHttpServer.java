package com.chat.ioc.server;

import com.chat.ioc.config.AppConfig;
import com.chat.ioc.controller.AuthController;
import com.chat.ioc.controller.HomeController;
import com.chat.ioc.database.DatabaseManager;
import com.chat.ioc.entity.ApiResponse;
import com.chat.ioc.entity.ChatMessage;
import com.chat.ioc.entity.ChatSession;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final DatabaseManager databaseManager;
    
    public SimpleHttpServer() {
        // 初始化IoC容器和控制器
        var container = AppConfig.createContainer();
        HomePageService homePageService = (HomePageService) container.getBean("homePageService");
        this.homeController = new HomeController(homePageService);
        
        AuthService authService = (AuthService) container.getBean("authService");
        this.authController = new AuthController(authService);

        this.databaseManager = new DatabaseManager();
        // Ensure DB is ready (AuthServiceImpl also initializes, but keep server usable even if auth isn't hit)
        DatabaseManager.initializeDatabase();
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
        try (InputStream rawIn = clientSocket.getInputStream();
             OutputStream rawOut = clientSocket.getOutputStream();
             PrintWriter out = new PrintWriter(new OutputStreamWriter(rawOut, StandardCharsets.UTF_8), true);
             BufferedWriter dataOut = new BufferedWriter(new OutputStreamWriter(rawOut, StandardCharsets.UTF_8))) {

            HttpRequest req = readHttpRequest(rawIn);
            if (req == null) return;

            String method = req.method;
            String rawPath = req.rawPath;
            String path = rawPath;
            String rawQuery = null;
            int qIdx = rawPath.indexOf('?');
            if (qIdx >= 0) {
                path = rawPath.substring(0, qIdx);
                rawQuery = rawPath.substring(qIdx + 1);
            }
            Map<String, String> queryParams = parseQueryParams(rawQuery);

            String authorizationHeader = req.headers.get("authorization");
            String requestBody = req.body == null ? "" : new String(req.body, StandardCharsets.UTF_8);

            // 提取Bearer token
            String extractedToken = null;
            if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer ")) {
                extractedToken = authorizationHeader.substring(7).trim();
            }

            // 预检请求
            if ("OPTIONS".equalsIgnoreCase(method)) {
                sendResponse(out, dataOut, "{\"code\":200,\"message\":\"OK\",\"data\":null}");
                return;
            }

            // /api/chat：支持 SSE（流式）和普通 JSON（兼容 xhr/axios）
            if ("POST".equalsIgnoreCase(method) && "/api/chat".equals(path)) {
                boolean wantsSse = clientWantsSse(req.headers, queryParams);
                boolean wantsStream = clientWantsStream(req.headers, queryParams, requestBody);
                if (wantsSse) {
                    handleChatStream(rawOut, requestBody);
                } else if (wantsStream) {
                    handleChatNdjsonStream(rawOut, requestBody);
                } else {
                    String response = handleChatJson(requestBody);
                    sendResponse(out, dataOut, response);
                }
                return;
            }

            // 其他路由：普通 JSON
            String response = routeRequest(method, path, queryParams, requestBody, extractedToken);
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
    
    private String routeRequest(String method, String path, Map<String, String> queryParams, String requestBody, String token) {
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
                } else if ("/api/chat/session".equals(path)) {
                    String sessionId = queryParams.get("sessionId");
                    if (sessionId == null || sessionId.trim().isEmpty()) {
                        return createErrorResponse(400, "Bad Request: sessionId required");
                    }
                    if (!databaseManager.chatSessionExists(sessionId)) {
                        return createErrorResponse(404, "Not Found");
                    }
                    List<ChatMessage> messages = databaseManager.findChatMessages(sessionId);
                    return createSuccessResponse(buildChatHistoryDataJson(sessionId, messages));
                } else if ("/api/chat/sessions".equals(path)) {
                    int limit = parseIntOrDefault(queryParams.get("limit"), 20);
                    int offset = parseIntOrDefault(queryParams.get("offset"), 0);
                    List<ChatSession> sessions = databaseManager.findChatSessions(limit, offset);
                    return createSuccessResponse(buildChatSessionsDataJson(sessions, limit, offset));
                }
            } else if ("DELETE".equalsIgnoreCase(method)) {
                if ("/api/chat/session".equals(path)) {
                    String sessionId = queryParams.get("sessionId");
                    if (sessionId == null || sessionId.trim().isEmpty()) {
                        return createErrorResponse(400, "Bad Request: sessionId required");
                    }
                    if (!databaseManager.chatSessionExists(sessionId)) {
                        return createErrorResponse(404, "Not Found");
                    }
                    boolean ok = databaseManager.deleteChatSession(sessionId);
                    if (!ok) {
                        return createErrorResponse(500, "Internal Server Error");
                    }
                    return createSuccessResponse("{\"deleted\":true,\"sessionId\":\"" + escapeJson(sessionId) + "\"}");
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
                } else if ("/api/chat/session".equals(path)) {
                    // 创建会话
                    String sessionId = UUID.randomUUID().toString().replace("-", "");
                    databaseManager.createChatSession(sessionId);
                    String dataJson = "{\"sessionId\":\"" + escapeJson(sessionId) + "\",\"createdAt\":\"" + Instant.now().toString() + "\"}";
                    return createSuccessResponse(dataJson);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(500, "Internal Server Error: " + e.getMessage());
        }
        
        return createErrorResponse(404, "Not Found");
    }

    private void handleChatStream(OutputStream outputStream, String requestBody) throws IOException {
        Map<String, String> req = parseChatRequest(requestBody);
        if (req == null) {
            sendSseError(outputStream, 400, "Bad Request: Invalid JSON");
            return;
        }

        String message = req.get("message");
        String sessionId = req.get("sessionId");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            sendSseError(outputStream, 400, "Bad Request: sessionId required");
            return;
        }
        if (!databaseManager.chatSessionExists(sessionId)) {
            sendSseError(outputStream, 404, "Not Found");
            return;
        }

        String assistant = generateAssistantReply(message);

        // SSE headers
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        w.write("HTTP/1.1 200 OK\r\n");
        w.write("Content-Type: text/event-stream; charset=utf-8\r\n");
        w.write("Cache-Control: no-cache\r\n");
        w.write("X-Accel-Buffering: no\r\n");
        w.write("Connection: keep-alive\r\n");
        w.write("Access-Control-Allow-Origin: *\r\n");
        w.write("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n");
        w.write("Access-Control-Allow-Headers: Content-Type, Authorization\r\n");
        w.write("\r\n");
        w.flush();

        // 先发一个 start 事件（含 sessionId）
        String startData = "{\"code\":200,\"message\":\"OK\",\"data\":{\"event\":\"start\",\"sessionId\":\""
            + escapeJson(sessionId)
            + "\"}}";
        writeSseEvent(w, "message", startData);

        // 流式发送 delta
        StringBuilder full = new StringBuilder();
        if (assistant != null) {
            for (int i = 0; i < assistant.length(); i++) {
                String delta = String.valueOf(assistant.charAt(i));
                full.append(delta);
                String chunk = "{\"code\":200,\"message\":\"OK\",\"data\":{\"event\":\"delta\",\"delta\":\"" + escapeJson(delta) + "\"}}";
                writeSseEvent(w, "message", chunk);
                // 给前端留出可感知的流式节奏，避免被中间层聚合缓存
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }
        }

        // 持久化：sessionId 已校验存在
        databaseManager.insertChatMessage(sessionId, "user", message == null ? "" : message);
        databaseManager.insertChatMessage(sessionId, "assistant", full.toString());

        // done 事件
        String done = "{\"code\":200,\"message\":\"OK\",\"data\":{\"event\":\"done\"}}";
        writeSseEvent(w, "message", done);
        w.flush();
    }

    private String handleChatJson(String requestBody) {
        Map<String, String> req = parseChatRequest(requestBody);
        if (req == null) {
            return createErrorResponse(400, "Bad Request: Invalid JSON");
        }

        String message = req.get("message");
        String sessionId = req.get("sessionId");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            return createErrorResponse(400, "Bad Request: sessionId required");
        }
        if (!databaseManager.chatSessionExists(sessionId)) {
            return createErrorResponse(404, "Not Found");
        }

        String assistant = generateAssistantReply(message);
        databaseManager.insertChatMessage(sessionId, "user", message == null ? "" : message);
        databaseManager.insertChatMessage(sessionId, "assistant", assistant == null ? "" : assistant);

        String dataJson =
            "{"
                + "\"sessionId\":\"" + escapeJson(sessionId) + "\","
                + "\"reply\":\"" + escapeJson(assistant == null ? "" : assistant) + "\""
            + "}";
        return createSuccessResponse(dataJson);
    }

    private boolean clientWantsSse(Map<String, String> headers, Map<String, String> queryParams) {
        if (queryParams != null) {
            String sse = queryParams.get("sse");
            if ("1".equals(sse) || "true".equalsIgnoreCase(sse)) return true;
            String transport = queryParams.get("transport");
            if (transport != null && transport.equalsIgnoreCase("sse")) return true;
        }
        if (headers == null) return false;
        String accept = headers.get("accept");
        if (accept == null) return false;
        return accept.toLowerCase().contains("text/event-stream");
    }

    private boolean clientWantsStream(Map<String, String> headers, Map<String, String> queryParams, String requestBody) {
        if (queryParams != null) {
            String stream = queryParams.get("stream");
            if ("1".equals(stream) || "true".equalsIgnoreCase(stream)) return true;
        }
        if (headers != null) {
            String xStream = headers.get("x-stream");
            if ("1".equals(xStream) || "true".equalsIgnoreCase(xStream)) return true;
        }
        if (requestBody == null || requestBody.trim().isEmpty()) return false;
        // 轻量解析：支持 {"stream":true} / {"stream":1}
        Pattern p = Pattern.compile("\"stream\"\\s*:\\s*(true|1)", Pattern.CASE_INSENSITIVE);
        return p.matcher(requestBody).find();
    }

    private void handleChatNdjsonStream(OutputStream outputStream, String requestBody) throws IOException {
        Map<String, String> req = parseChatRequest(requestBody);
        if (req == null) {
            sendNdjsonError(outputStream, 400, "Bad Request: Invalid JSON");
            return;
        }

        String message = req.get("message");
        String sessionId = req.get("sessionId");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            sendNdjsonError(outputStream, 400, "Bad Request: sessionId required");
            return;
        }
        if (!databaseManager.chatSessionExists(sessionId)) {
            sendNdjsonError(outputStream, 404, "Not Found");
            return;
        }

        String assistant = generateAssistantReply(message);

        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        w.write("HTTP/1.1 200 OK\r\n");
        w.write("Content-Type: application/x-ndjson; charset=utf-8\r\n");
        w.write("Cache-Control: no-cache\r\n");
        w.write("X-Accel-Buffering: no\r\n");
        w.write("Connection: close\r\n");
        w.write("Access-Control-Allow-Origin: *\r\n");
        w.write("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n");
        w.write("Access-Control-Allow-Headers: Content-Type, Authorization\r\n");
        w.write("\r\n");
        w.flush();

        writeNdjsonLine(w, "{\"code\":200,\"message\":\"OK\",\"data\":{\"event\":\"start\",\"sessionId\":\""
            + escapeJson(sessionId)
            + "\"}}");

        StringBuilder full = new StringBuilder();
        if (assistant != null) {
            for (int i = 0; i < assistant.length(); i++) {
                String delta = String.valueOf(assistant.charAt(i));
                full.append(delta);
                writeNdjsonLine(w, "{\"code\":200,\"message\":\"OK\",\"data\":{\"event\":\"delta\",\"delta\":\"" + escapeJson(delta) + "\"}}");
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }
        }

        databaseManager.insertChatMessage(sessionId, "user", message == null ? "" : message);
        databaseManager.insertChatMessage(sessionId, "assistant", full.toString());

        writeNdjsonLine(w, "{\"code\":200,\"message\":\"OK\",\"data\":{\"event\":\"done\"}}");
        w.flush();
    }

    private void sendNdjsonError(OutputStream outputStream, int code, String message) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        w.write("HTTP/1.1 200 OK\r\n");
        w.write("Content-Type: application/x-ndjson; charset=utf-8\r\n");
        w.write("Cache-Control: no-cache\r\n");
        w.write("X-Accel-Buffering: no\r\n");
        w.write("Connection: close\r\n");
        w.write("Access-Control-Allow-Origin: *\r\n");
        w.write("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n");
        w.write("Access-Control-Allow-Headers: Content-Type, Authorization\r\n");
        w.write("\r\n");
        writeNdjsonLine(w, "{\"code\":" + code + ",\"message\":\"" + escapeJson(message) + "\",\"data\":null}");
        w.flush();
    }

    private void writeNdjsonLine(BufferedWriter w, String jsonLine) throws IOException {
        w.write(jsonLine);
        w.write("\n");
        w.flush();
    }

    private void sendSseError(OutputStream outputStream, int code, String message) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        w.write("HTTP/1.1 200 OK\r\n");
        w.write("Content-Type: text/event-stream; charset=utf-8\r\n");
        w.write("Cache-Control: no-cache\r\n");
        w.write("X-Accel-Buffering: no\r\n");
        w.write("Connection: keep-alive\r\n");
        w.write("Access-Control-Allow-Origin: *\r\n");
        w.write("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n");
        w.write("Access-Control-Allow-Headers: Content-Type, Authorization\r\n");
        w.write("\r\n");
        String data = "{\"code\":" + code + ",\"message\":\"" + escapeJson(message) + "\",\"data\":null}";
        writeSseEvent(w, "error", data);
        w.flush();
    }

    private void writeSseEvent(BufferedWriter w, String event, String dataJson) throws IOException {
        w.write("event: " + event + "\n");
        // SSE 的 data 不能有裸换行，简单起见把 JSON 压成单行
        w.write("data: " + dataJson + "\n\n");
        w.flush();
    }

    private Map<String, String> parseChatRequest(String json) {
        try {
            if (json == null || json.trim().isEmpty()) return null;
            Pattern msgPattern = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
            Pattern contentPattern = Pattern.compile("\"content\"\\s*:\\s*\"([^\"]*)\"");
            Pattern sessionPattern = Pattern.compile("\"sessionId\"\\s*:\\s*\"([^\"]*)\"");
            Matcher mm = msgPattern.matcher(json);
            Matcher cm = contentPattern.matcher(json);
            Matcher sm = sessionPattern.matcher(json);
            String message = null;
            String sessionId = null;
            if (mm.find()) {
                message = mm.group(1);
            } else if (cm.find()) {
                message = cm.group(1);
            }
            if (sm.find()) sessionId = sm.group(1);
            if (message == null) return null;
            Map<String, String> m = new HashMap<>();
            m.put("message", message);
            m.put("sessionId", sessionId);
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    private static class HttpRequest {
        final String method;
        final String rawPath;
        final Map<String, String> headers;
        final byte[] body;

        private HttpRequest(String method, String rawPath, Map<String, String> headers, byte[] body) {
            this.method = method;
            this.rawPath = rawPath;
            this.headers = headers;
            this.body = body;
        }
    }

    private HttpRequest readHttpRequest(InputStream in) throws IOException {
        byte[] headerBytes = readUntilDoubleCrlf(in, 64 * 1024);
        if (headerBytes == null) return null;

        String headerText = new String(headerBytes, StandardCharsets.ISO_8859_1);
        String[] lines = headerText.split("\r\n");
        if (lines.length == 0) return null;

        String[] requestLineParts = lines[0].split(" ");
        if (requestLineParts.length < 2) return null;
        String method = requestLineParts[0];
        String rawPath = requestLineParts[1];

        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isEmpty()) continue;
            int idx = line.indexOf(':');
            if (idx <= 0) continue;
            String name = line.substring(0, idx).trim().toLowerCase();
            String value = line.substring(idx + 1).trim();
            headers.put(name, value);
        }

        int contentLength = 0;
        String cl = headers.get("content-length");
        if (cl != null && !cl.isEmpty()) {
            try {
                contentLength = Integer.parseInt(cl);
            } catch (NumberFormatException ignored) {
                contentLength = 0;
            }
        }

        byte[] body = null;
        if (contentLength > 0) {
            body = readFixedBytes(in, contentLength);
        }

        return new HttpRequest(method, rawPath, headers, body);
    }

    private byte[] readUntilDoubleCrlf(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int state = 0; // tracks \r\n\r\n
        while (buf.size() < maxBytes) {
            int b = in.read();
            if (b == -1) return null;
            buf.write(b);
            if (state == 0 && b == '\r') state = 1;
            else if (state == 1 && b == '\n') state = 2;
            else if (state == 2 && b == '\r') state = 3;
            else if (state == 3 && b == '\n') return buf.toByteArray();
            else state = (b == '\r') ? 1 : 0;
        }
        throw new IOException("Request header too large");
    }

    private byte[] readFixedBytes(InputStream in, int len) throws IOException {
        byte[] data = new byte[len];
        int off = 0;
        while (off < len) {
            int r = in.read(data, off, len - off);
            if (r == -1) throw new EOFException("Unexpected EOF while reading request body");
            off += r;
        }
        return data;
    }

    private String generateAssistantReply(String message) {
        if (message == null) return "我没有收到消息内容。";
        String trimmed = message.trim();
        if (trimmed.isEmpty()) return "消息为空。";
        return "你说： " + trimmed;
    }

    private Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.trim().isEmpty()) return map;
        String[] pairs = rawQuery.split("&");
        for (String p : pairs) {
            if (p.isEmpty()) continue;
            int idx = p.indexOf('=');
            if (idx < 0) {
                map.put(urlDecode(p), "");
            } else {
                String k = urlDecode(p.substring(0, idx));
                String v = urlDecode(p.substring(idx + 1));
                map.put(k, v);
            }
        }
        return map;
    }

    private String urlDecode(String s) {
        if (s == null) return null;
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
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

    private String createSuccessResponse(String dataJson) {
        return "{\"code\":200,\"message\":\"OK\",\"data\":" + (dataJson == null ? "null" : dataJson) + "}";
    }

    private String buildChatHistoryDataJson(String sessionId, List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"sessionId\":\"").append(escapeJson(sessionId)).append("\",");
        sb.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"id\":").append(m.getId()).append(",");
            sb.append("\"role\":\"").append(escapeJson(m.getRole())).append("\",");
            sb.append("\"content\":\"").append(escapeJson(m.getContent())).append("\"");
            if (m.getCreatedAt() != null) {
                sb.append(",\"createdAt\":\"").append(escapeJson(m.getCreatedAt().toString())).append("\"");
            }
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String buildChatSessionsDataJson(List<ChatSession> sessions, int limit, int offset) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"limit\":").append(limit).append(",");
        sb.append("\"offset\":").append(offset).append(",");
        sb.append("\"sessions\":[");
        for (int i = 0; i < sessions.size(); i++) {
            ChatSession s = sessions.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"sessionId\":\"").append(escapeJson(s.getSessionId())).append("\"");
            if (s.getCreatedAt() != null) {
                sb.append(",\"createdAt\":\"").append(escapeJson(s.getCreatedAt().toString())).append("\"");
            }
            if (s.getUpdatedAt() != null) {
                sb.append(",\"updatedAt\":\"").append(escapeJson(s.getUpdatedAt().toString())).append("\"");
            }
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
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