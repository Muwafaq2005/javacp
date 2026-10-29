package ai.typesafe.voicebrowser.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.typesafe.voicebrowser.model.User;

class DatabaseServiceTest {

    private DatabaseService dbService;

    @BeforeEach
    void setUp() {
        dbService = new DatabaseService("jdbc:sqlite:target/test-voicebrowser.db");
        dbService.initDatabase();
    }

    @Test
    void testDefaultAdminSeed() {
        assertTrue(dbService.userExists("admin"));
        User admin = dbService.authenticate("admin", "admin123");
        assertNotNull(admin);
        assertEquals("admin@voicebrowser.local", admin.getEmail());
    }

    @Test
    void testRegisterAndAuthenticate() {
        String testUser = "testuser_" + System.currentTimeMillis();
        boolean registered = dbService.registerUser(testUser, testUser + "@example.com", "secretpass");
        assertTrue(registered);

        assertTrue(dbService.userExists(testUser));

        User user = dbService.authenticate(testUser, "secretpass");
        assertNotNull(user);
        assertEquals(testUser, user.getUsername());
        assertEquals(testUser + "@example.com", user.getEmail());

        // Incorrect password
        User failed = dbService.authenticate(testUser, "wrongpassword");
        assertNull(failed);
    }

    @Test
    void testDuplicateUsernameFails() {
        String duplicateUser = "dup_" + System.currentTimeMillis();
        assertTrue(dbService.registerUser(duplicateUser, "dup@example.com", "pass1"));
        // Duplicate insertion should fail unique constraint
        assertFalse(dbService.registerUser(duplicateUser, "dup2@example.com", "pass2"));
    }

    @Test
    void testGetAllUsers() {
        List<User> users = dbService.getAllUsers();
        assertNotNull(users);
        assertFalse(users.isEmpty());
    }
}
