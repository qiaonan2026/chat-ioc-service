package com.chat.ioc.database;

import com.chat.ioc.entity.ChatMessage;
import com.chat.ioc.entity.ChatSession;
import com.chat.ioc.entity.User;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库操作类，用于用户数据的持久化存储
 */
public class DatabaseManager {
    
    private static final String DB_URL = "jdbc:h2:./chat_ioc_db;DB_CLOSE_ON_EXIT=FALSE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    
    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 Driver not found", e);
        }
    }
    
    /**
     * 初始化数据库表
     */
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            
            // 创建用户表
            String createUserTableSQL = """
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    nickname VARCHAR(255),
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createUserTableSQL);
                System.out.println("Database tables initialized successfully.");
            }

            // 兼容旧库：如果 users 表已存在但缺列，做最小迁移
            ensureUsersTableSchema(conn);

            // 创建聊天会话与消息表
            ensureChatTables(conn);
            
            // 检查是否已有管理员用户
            if (!hasAdminUser(conn)) {
                createDefaultAdminUser(conn);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private static void ensureChatTables(Connection conn) throws SQLException {
        String createSessions = """
            CREATE TABLE IF NOT EXISTS chat_sessions (
                session_id VARCHAR(64) PRIMARY KEY,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        String createMessages = """
            CREATE TABLE IF NOT EXISTS chat_messages (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                session_id VARCHAR(64) NOT NULL,
                role VARCHAR(32) NOT NULL,
                content CLOB NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id)
            )
            """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createSessions);
            stmt.execute(createMessages);
        }
    }

    private static void ensureUsersTableSchema(Connection conn) throws SQLException {
        // H2 对 "ALTER TABLE ... ADD COLUMN IF NOT EXISTS" 的支持在不同版本/模式下可能不一致，
        // 这里用 metadata 检测后再执行 ALTER，保证兼容性。
        if (!columnExists(conn, "USERS", "IS_ACTIVE")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE users ADD COLUMN is_active BOOLEAN DEFAULT TRUE");
            }
        }
        if (!columnExists(conn, "USERS", "CREATED_AT")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE users ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            }
        }
        if (!columnExists(conn, "USERS", "UPDATED_AT")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE users ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            }
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            if (rs.next()) return true;
        }
        // 部分数据库/配置下会把名称按原样存储，兜底再查一遍小写
        try (ResultSet rs = meta.getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase())) {
            return rs.next();
        }
    }
    
    /**
     * 检查是否存在管理员用户
     */
    private static boolean hasAdminUser(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "admin");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    
    /**
     * 创建默认管理员用户
     */
    private static void createDefaultAdminUser(Connection conn) throws SQLException {
        String sql = "INSERT INTO users (username, password, email, nickname) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "admin");
            stmt.setString(2, "password"); // 在实际应用中应该使用加密密码
            stmt.setString(3, "admin@example.com");
            stmt.setString(4, "Administrator");
            stmt.executeUpdate();
            System.out.println("Default admin user created: username=admin, password=password");
        }
    }
    
    /**
     * 保存用户
     */
    public User saveUser(User user) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO users (username, password, email, nickname, is_active) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getPassword()); // 在实际应用中应该使用加密密码
                stmt.setString(3, user.getEmail());
                stmt.setString(4, user.getNickname());
                stmt.setBoolean(5, user.getIsActive() != null ? user.getIsActive() : true);
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            user.setId(generatedKeys.getLong(1));
                        }
                    }
                }
                
                return user;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }
    
    /**
     * 根据用户名查找用户
     */
    public User findByUsername(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT id, username, password, email, nickname, is_active FROM users WHERE username = ? AND is_active = TRUE";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setEmail(rs.getString("email"));
                    user.setNickname(rs.getString("nickname"));
                    user.setIsActive(rs.getBoolean("is_active"));
                    return user;
                }
            }
            
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by username", e);
        }
    }
    
    /**
     * 更新用户
     */
    public User updateUser(User user) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "UPDATE users SET password = ?, email = ?, nickname = ?, is_active = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, user.getPassword());
                stmt.setString(2, user.getEmail());
                stmt.setString(3, user.getNickname());
                stmt.setBoolean(4, user.getIsActive() != null ? user.getIsActive() : true);
                stmt.setLong(5, user.getId());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    return user;
                }
            }
            
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user", e);
        }
    }
    
    /**
     * 获取所有用户
     */
    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT id, username, password, email, nickname, is_active FROM users WHERE is_active = TRUE";
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(sql);
                
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setEmail(rs.getString("email"));
                    user.setNickname(rs.getString("nickname"));
                    user.setIsActive(rs.getBoolean("is_active"));
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all users", e);
        }
        
        return users;
    }
    
    /**
     * 根据ID查找用户
     */
    public User findById(Long id) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT id, username, password, email, nickname, is_active FROM users WHERE id = ? AND is_active = TRUE";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setEmail(rs.getString("email"));
                    user.setNickname(rs.getString("nickname"));
                    user.setIsActive(rs.getBoolean("is_active"));
                    return user;
                }
            }
            
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by id", e);
        }
    }

    public boolean chatSessionExists(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) return false;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT COUNT(*) FROM chat_sessions WHERE session_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check chat session", e);
        }
    }

    public void createChatSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO chat_sessions (session_id) VALUES (?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create chat session", e);
        }
    }

    public void touchChatSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) return;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "UPDATE chat_sessions SET updated_at = CURRENT_TIMESTAMP WHERE session_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update chat session timestamp", e);
        }
    }

    public ChatMessage insertChatMessage(String sessionId, String role, String content) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("role is required");
        }
        if (content == null) content = "";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO chat_messages (session_id, role, content) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, sessionId);
                stmt.setString(2, role);
                stmt.setString(3, content);
                stmt.executeUpdate();
                long id = 0;
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) id = keys.getLong(1);
                }
                touchChatSession(sessionId);
                return new ChatMessage(id, sessionId, role, content, Instant.now());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert chat message", e);
        }
    }

    public List<ChatMessage> findChatMessages(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        List<ChatMessage> messages = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT id, session_id, role, content, created_at FROM chat_messages WHERE session_id = ? ORDER BY id ASC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sessionId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    ChatMessage m = new ChatMessage();
                    m.setId(rs.getLong("id"));
                    m.setSessionId(rs.getString("session_id"));
                    m.setRole(rs.getString("role"));
                    m.setContent(rs.getString("content"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) m.setCreatedAt(ts.toInstant());
                    messages.add(m);
                }
            }
            return messages;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query chat messages", e);
        }
    }

    public List<ChatSession> findChatSessions(int limit, int offset) {
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 200);
        int safeOffset = Math.max(offset, 0);
        List<ChatSession> sessions = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT session_id, created_at, updated_at FROM chat_sessions ORDER BY updated_at DESC LIMIT ? OFFSET ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, safeLimit);
                stmt.setInt(2, safeOffset);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String sessionId = rs.getString("session_id");
                    Timestamp createdTs = rs.getTimestamp("created_at");
                    Timestamp updatedTs = rs.getTimestamp("updated_at");
                    ChatSession s = new ChatSession();
                    s.setSessionId(sessionId);
                    if (createdTs != null) s.setCreatedAt(createdTs.toInstant());
                    if (updatedTs != null) s.setUpdatedAt(updatedTs.toInstant());
                    sessions.add(s);
                }
            }
            return sessions;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query chat sessions", e);
        }
    }

    public List<ChatSession> findChatSessions(int limit) {
        return findChatSessions(limit, 0);
    }

    public boolean deleteChatSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement delMsgs = conn.prepareStatement("DELETE FROM chat_messages WHERE session_id = ?")) {
                    delMsgs.setString(1, sessionId);
                    delMsgs.executeUpdate();
                }
                int affected;
                try (PreparedStatement delSession = conn.prepareStatement("DELETE FROM chat_sessions WHERE session_id = ?")) {
                    delSession.setString(1, sessionId);
                    affected = delSession.executeUpdate();
                }
                conn.commit();
                return affected > 0;
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                throw e;
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete chat session", e);
        }
    }
}