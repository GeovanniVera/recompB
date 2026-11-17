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
 * Food item. {@code userId == null} means the shared 3B catalog; a value means the
 * item was created by that user.
 */
@Entity
@Table(name = "food_items")
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "state_tag", nullable = false, length = 30)
    private String stateTag;

    @Column(name = "kcal_per_100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal kcalPer100g;

    @Column(name = "protein_per_100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal proteinPer100g;

    @Column(name = "carbs_per_100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal carbsPer100g;

    @Column(name = "fat_per_100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal fatPer100g;

    @Column(name = "quick_grams_json", length = 100)
    private String quickGramsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FoodItem() {
    }

    public FoodItem(Long userId, String name, String stateTag, BigDecimal kcalPer100g,
            BigDecimal proteinPer100g, BigDecimal carbsPer100g, BigDecimal fatPer100g,
            String quickGramsJson) {
        this.userId = userId;
        this.name = name;
        this.stateTag = stateTag;
        this.kcalPer100g = kcalPer100g;
        this.proteinPer100g = proteinPer100g;
        this.carbsPer100g = carbsPer100g;
        this.fatPer100g = fatPer100g;
        this.quickGramsJson = quickGramsJson;
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

    public String getName() {
        return name;
    }

    public String getStateTag() {
        return stateTag;
    }

    public BigDecimal getKcalPer100g() {
        return kcalPer100g;
    }

    public BigDecimal getProteinPer100g() {
        return proteinPer100g;
    }

    public BigDecimal getCarbsPer100g() {
        return carbsPer100g;
    }

    public BigDecimal getFatPer100g() {
        return fatPer100g;
    }

    public String getQuickGramsJson() {
        return quickGramsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
