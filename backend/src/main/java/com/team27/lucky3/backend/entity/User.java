package com.team27.lucky3.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team27.lucky3.backend.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String surname;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    private String password;

    private String phoneNumber;
    private String address;
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private boolean isBlocked;

    // 2.2.1 Driver Availability
    private boolean isActive;
    private boolean isInactiveRequested;

    @Column(name = "last_password_reset_date")
    private Timestamp lastPasswordResetDate;

    // --- UserDetails Implementation ---

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Map Enum to Spring Security Authority
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return this.email; // Spec 2.2.1: Login with Email
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() { return true; }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() { return !this.isBlocked; }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return !this.isBlocked; } // Or add a specific enabled field if needed
}