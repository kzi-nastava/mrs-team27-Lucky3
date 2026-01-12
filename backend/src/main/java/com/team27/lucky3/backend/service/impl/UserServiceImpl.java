package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.response.UserProfile;
import com.team27.lucky3.backend.entity.Image;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.service.ImageService;
import com.team27.lucky3.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ImageService imageService;

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User updateUser(Long id, UserProfile request, MultipartFile file) throws IOException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // map fields from UserProfile -> User
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());

        //TODO: If user is driver, call service to update vehicle info, and update vehicle for driver, + send notification

        if (file != null && !file.isEmpty()) {
            Image newImage = imageService.store(file);
            user.setProfileImage(newImage);
        }

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Image getProfileImage(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (user.getProfileImage() == null) {
            return imageService.getDefaultAvatar();
        }
        return user.getProfileImage();
    }
}