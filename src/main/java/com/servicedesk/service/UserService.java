package com.servicedesk.service;

import com.servicedesk.model.Role;
import com.servicedesk.model.User;
import com.servicedesk.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserService {
    private static final Pattern EMAIL_PATTERN = 
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String id, String name, String email, Role role, String department) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        if (userRepository.existsById(id)) {
            throw new IllegalStateException("User with ID " + id + " already exists");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("User with email " + email + " already exists");
        }

        User user = new User(id.trim(), name.trim(), email.trim().toLowerCase(), role, department.trim());
        return userRepository.save(user);
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getTechnicians() {
        return userRepository.findByRole(Role.TECHNICIAN);
    }

    public void deactivateUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setActive(false);
        userRepository.save(user);
    }
}