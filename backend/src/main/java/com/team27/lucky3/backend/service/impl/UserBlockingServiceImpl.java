package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.response.BlockUserResponse;
import com.team27.lucky3.backend.dto.response.UserProfile;
import com.team27.lucky3.backend.dto.response.UserResponse;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.service.UserBlockingService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserBlockingServiceImpl implements UserBlockingService {

    private final UserRepository userRepository;

    // Injection via constructor is best practice
    public UserBlockingServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional // Ensures data consistency
    public void blockUser(String email, String reason) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        // Optional: Prevent double blocking
        if (user.isBlocked()) {
            throw new IllegalStateException("User is already blocked.");
        }

        // Optional: Prevent blocking an Administrator
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Cannot block an administrator.");
        }

        user.setBlocked(true);
        user.setBlockReason(reason);

        // Explicitly saving is good practice, though @Transactional often handles it automatically
        userRepository.save(user);
    }

    @Override
    @Transactional
    public BlockUserResponse unblockUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        // Optional: Check if actually blocked
        if (!user.isBlocked()) {
            throw new IllegalStateException("User is not currently blocked.");
        }

        user.setBlocked(false);
        user.setBlockReason(null); // Clear the reason when unblocking

        User savedUser = userRepository.save(user);

        return new BlockUserResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getSurname(),
                savedUser.isBlocked(),
                savedUser.getBlockReason(),
                "User unblocked successfully"
        );
    }

    @Override
    public List<UserProfile> getBlockedUsers() {
        return userRepository.findByIsBlocked(true).stream()
                .filter(user -> user.getRole() != UserRole.ADMIN)
                .map(this::mapToUserProfile)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserProfile> getUnblockedUsers() {
        return userRepository.findByIsBlocked(false).stream()
                .filter(user -> user.getRole() != UserRole.ADMIN)
                .map(this::mapToUserProfile)
                .collect(Collectors.toList());
    }

    // Helper method to map Entity to DTO
    private UserProfile mapToUserProfile(User user) {
        return new UserProfile(user.getName(), user.getSurname(), user.getEmail(), user.getPhoneNumber(), user.getAddress(), "/api/users/" + user.getId() + "/profile-image");
    }
}

