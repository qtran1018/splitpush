package com.splitpush.service;

import com.splitpush.model.User;
import com.splitpush.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@SuppressWarnings("null")
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Cacheable(value = "userByUsername", key = "#username")
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Cacheable(value = "userByEmail", key = "#email")
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Cacheable(value = "users", key = "#id")
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Cacheable(value = "users", key = "'search:' + #username")
    public java.util.List<User> searchUsersByUsername(String username) {
        return userRepository.findByUsernameContainingIgnoreCase(username);
    }

    @Cacheable(value = "users", key = "'all'")
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
