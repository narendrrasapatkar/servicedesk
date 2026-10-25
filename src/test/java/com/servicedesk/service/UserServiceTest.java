package com.servicedesk.service;

import com.servicedesk.model.Role;
import com.servicedesk.model.User;
import com.servicedesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
        userService = new UserService(userRepository);
    }

    @Test
    @DisplayName("Should successfully register a valid employee user")
    void shouldRegisterValidUser() {
        User user = userService.registerUser("U001", "Alice Smith", "alice@company.com", Role.EMPLOYEE, "Finance");

        assertNotNull(user);
        assertEquals("U001", user.getId());
        assertEquals("Alice Smith", user.getName());
        assertEquals("alice@company.com", user.getEmail());
        assertEquals(Role.EMPLOYEE, user.getRole());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("Should reject user registration with invalid email")
    void shouldRejectInvalidEmail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            userService.registerUser("U002", "Bob Jones", "invalid-email-address", Role.EMPLOYEE, "Sales")
        );

        assertTrue(exception.getMessage().contains("Invalid email format"));
    }

    @Test
    @DisplayName("Should reject duplicate user IDs")
    void shouldRejectDuplicateUserId() {
        userService.registerUser("U003", "Charlie", "charlie@company.com", Role.HELPDESK_AGENT, "IT");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            userService.registerUser("U003", "Charlie Duplicate", "charlie2@company.com", Role.HELPDESK_AGENT, "IT")
        );

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Should filter and return only users with TECHNICIAN role")
    void shouldFilterTechniciansCorrectly() {
        userService.registerUser("U10", "Tech One", "tech1@company.com", Role.TECHNICIAN, "Infrastructure");
        userService.registerUser("U20", "Admin One", "admin@company.com", Role.ADMIN, "Management");
        userService.registerUser("U30", "Tech Two", "tech2@company.com", Role.TECHNICIAN, "Hardware");

        List<User> technicians = userService.getTechnicians();

        assertEquals(2, technicians.size());
        assertTrue(technicians.stream().allMatch(u -> u.getRole() == Role.TECHNICIAN));
    }

    @Test
    @DisplayName("Should deactivate user correctly")
    void shouldDeactivateUser() {
        userService.registerUser("U40", "Dev Dave", "dave@company.com", Role.EMPLOYEE, "Engineering");
        userService.deactivateUser("U40");

        Optional<User> userOpt = userService.getUserById("U40");
        assertTrue(userOpt.isPresent());
        assertFalse(userOpt.get().isActive());
    }
}