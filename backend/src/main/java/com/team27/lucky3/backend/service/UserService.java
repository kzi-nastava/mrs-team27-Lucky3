package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.response.UserProfile;
import com.team27.lucky3.backend.entity.Image;
import com.team27.lucky3.backend.entity.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface UserService {
    User findByEmail(String email);
    User save(User user);
    Optional<User> findById(Long id);
    User updateUser(Long id, UserProfile request, MultipartFile file) throws IOException;
    @Transactional(readOnly = true)
    Image getProfileImage(Long userId);
}