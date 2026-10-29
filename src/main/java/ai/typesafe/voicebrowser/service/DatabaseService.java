package ai.typesafe.voicebrowser.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ai.typesafe.voicebrowser.model.User;
import jakarta.annotation.PostConstruct;

/**
 * Service managing SQLite database persistence using raw JDBC (java.sql.*).
 */
@Service
public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);
    private static final String DB_URL = "jdbc:sqlite:voicebrowser.db";

    @PostConstruct
    public void initDatabase() {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    email TEXT NOT NULL,
                    password TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            log.info("JDBC SQLite database initialized: voicebrowser.db");

            // Seed a default demo admin user if not exists
            if (!userExists("admin")) {
                registerUser("admin", "admin@voicebrowser.local", "admin123");
                log.info("Created default demo user: admin / admin123");
            }
        } catch (SQLException e) {
            log.error("Failed to initialize database via JDBC", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Register a new user using JDBC PreparedStatement.
     */
    public boolean registerUser(String username, String email, String password) {
        String sql = "INSERT INTO users(username, email, password) VALUES(?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim());
            pstmt.setString(2, email.trim());
            pstmt.setString(3, password);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            log.error("JDBC Error inserting user '{}': {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Authenticate user credentials via JDBC.
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT id, username, email, password, created_at FROM users WHERE username = ? AND password = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim());
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String uname = rs.getString("username");
                    String email = rs.getString("email");
                    String pass = rs.getString("password");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    return new User(id, uname, email, pass, createdAt);
                }
            }
        } catch (SQLException e) {
            log.error("JDBC Error authenticating user '{}': {}", username, e.getMessage());
        }
        return null;
    }

    /**
     * Check if a username already exists.
     */
    public boolean userExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("JDBC Error checking user '{}': {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Retrieve all users using JDBC Statement and ResultSet.
     */
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, email, password, created_at FROM users ORDER BY id ASC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            log.error("JDBC Error fetching users", e);
        }
        return list;
    }
}
