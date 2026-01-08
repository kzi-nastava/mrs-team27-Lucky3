package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.entity.User;
import java.util.Optional;

public interface UserService {
    User findByEmail(String email);
    User save(User user);
    Optional<User> findById(Long id);
}