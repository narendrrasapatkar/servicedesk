package com.servicedesk.repository;

import com.servicedesk.model.Role;
import com.servicedesk.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserRepository {
    private final Map<String, User> userDatabase = new ConcurrentHashMap<>();

    public User save(User user) {
        userDatabase.put(user.getId(), user);
        return user;
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(userDatabase.get(id));
    }

    public Optional<User> findByEmail(String email) {
        return userDatabase.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public List<User> findAll() {
        return new ArrayList<>(userDatabase.values());
    }

    public List<User> findByRole(Role role) {
        return userDatabase.values().stream()
                .filter(u -> u.getRole() == role)
                .collect(Collectors.toList());
    }

    public boolean existsById(String id) {
        return userDatabase.containsKey(id);
    }

    public void clear() {
        userDatabase.clear();
    }
}