package com.geovanni.miscalorias.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Quick plate combo. {@code userId == null} means a shared base plate.
 */
@Entity
@Table(name = "quick_plates")
public class QuickPlate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 255)
    private String subtitle;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal kcal;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal protein;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal carbs;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal fat;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected QuickPlate() {
    }

    public QuickPlate(Long userId, String title, String subtitle, BigDecimal kcal,
            BigDecimal protein, BigDecimal carbs, BigDecimal fat) {
        this.userId = userId;
        this.title = title;
        this.subtitle = subtitle;
        this.kcal = kcal;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isVisibleTo(Long otherUserId) {
        return userId == null || userId.equals(otherUserId);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public BigDecimal getKcal() {
        return kcal;
    }

    public BigDecimal getProtein() {
        return protein;
    }

    public BigDecimal getCarbs() {
        return carbs;
    }

    public BigDecimal getFat() {
        return fat;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
