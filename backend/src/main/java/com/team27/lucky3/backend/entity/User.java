package com.team27.lucky3.backend.entity;

import com.team27.lucky3.backend.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String surname;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;
    private String phoneNumber;
    private String address;
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private boolean isBlocked;
    private boolean isActive;
    private boolean isInactiveRequested;
}

