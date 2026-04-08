package com.chat.ioc.database;

import com.chat.ioc.entity.User;

import java.sql.*;
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
            
            // 检查是否已有管理员用户
            if (!hasAdminUser(conn)) {
                createDefaultAdminUser(conn);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
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
}