package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.response.BlockUserResponse;
import com.team27.lucky3.backend.dto.response.UserResponse;

import java.util.List;

public interface UserBlockingService{
    void blockUser(Long userId, String reason);
    BlockUserResponse unblockUser(Long userId);
    List<UserResponse> getBlockedUsers();
    List<UserResponse> getUnblockedUsers();
}