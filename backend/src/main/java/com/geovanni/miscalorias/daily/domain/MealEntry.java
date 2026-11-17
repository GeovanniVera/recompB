package com.geovanni.miscalorias.daily.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * A logged food. Values are denormalized on purpose: editing a catalog item later
 * must not rewrite historical logs.
 */
@Entity
@Table(name = "meal_entries")
public class MealEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_log_id", nullable = false)
    private DailyLog dailyLog;

    @Column(name = "food_item_id")
    private Long foodItemId;

    @Column(name = "entry_name", nullable = false, length = 200)
    private String entryName;

    @Column(precision = 7, scale = 2)
    private BigDecimal grams;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal kcal;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal protein;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal carbs;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal fat;

    @Column(name = "is_off_plan", nullable = false)
    private boolean offPlan = false;

    @Column(name = "logged_time", length = 10)
    private String loggedTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MealEntry() {
    }

    public MealEntry(DailyLog dailyLog, Long foodItemId, String entryName, BigDecimal grams,
            BigDecimal kcal, BigDecimal protein, BigDecimal carbs, BigDecimal fat,
            boolean offPlan, String loggedTime) {
        this.dailyLog = dailyLog;
        this.foodItemId = foodItemId;
        this.entryName = entryName;
        this.grams = grams;
        this.kcal = kcal;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.offPlan = offPlan;
        this.loggedTime = loggedTime;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public DailyLog getDailyLog() {
        return dailyLog;
    }

    public Long getFoodItemId() {
        return foodItemId;
    }

    public String getEntryName() {
        return entryName;
    }

    public BigDecimal getGrams() {
        return grams;
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

    public boolean isOffPlan() {
        return offPlan;
    }

    public String getLoggedTime() {
        return loggedTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
