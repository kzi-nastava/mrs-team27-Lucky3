package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.response.BlockUserResponse;

public interface UserBlockingService{
    void blockUser(Long userId, String reason);
    BlockUserResponse unblockUser(Long userId);
}