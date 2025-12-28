package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private String profilePictureUrl;
    private UserRole role;
    private String phoneNumber;
}
