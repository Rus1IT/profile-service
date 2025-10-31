package com.cashpilot.userservice.entity;

import com.cashpilot.userservice.enums.AppTheme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private String userId; // UUID из Keycloak (из 'sub' токена)


    @Column(name = "default_currency", length = 3)
    private String defaultCurrency;

    @Column(name = "balance_visibility")
    private boolean balanceVisibility;

    @Column(name = "timezone", length = 50)
    private String timezone; // Например, "Europe/Berlin" или "Asia/Almaty"

    @Column(name = "language", length = 5)
    private String language;

    @Column(name = "theme", length = 10)
    @Enumerated(EnumType.STRING)
    private AppTheme theme;

    @Column(name = "notify_on_budget_limit")
    private boolean sendNotificationToGmail;

    @Column(name = "onboarding_completed")
    private boolean onboardingCompleted; // Прошел ли пользователь обучение

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}