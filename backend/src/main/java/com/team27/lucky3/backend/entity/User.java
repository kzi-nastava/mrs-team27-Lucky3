package com.team27.lucky3.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team27.lucky3.backend.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.sql.Timestamp;
import java.util.Collection;
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

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_image_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore // also prevents accidental serialization
    private Image profileImage;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private boolean isBlocked; // Admin blocked user
    @Column(name = "block_reason", length = 500)
    private String blockReason;

    private boolean isEnabled; // Email activated (Spec 2.2.2)

    // 2.2.1 Driver Availability
    private boolean isActive;
    private boolean isInactiveRequested;

    @OneToOne(mappedBy = "driver", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Vehicle vehicle;

    @Column(name = "last_password_reset_date")
    private Timestamp lastPasswordResetDate;

    /**
     * FCM device registration token for push notifications.
     * Updated on login and whenever the token refreshes (via PUT /api/users/{id}/fcm-token).
     * Nullable — null means push notifications are not available for this user.
     */
    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    // UserDetails Implementation
    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    // This method MUST be named "getUsername()" because of the UserDetails interface
    @JsonIgnore
    @Override
    public String getUsername() {
        return this.email;
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
    public boolean isEnabled() {
        return this.isEnabled && !this.isBlocked;
    }
}