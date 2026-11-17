package com.geovanni.miscalorias.analytics.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Fortnightly fasted measurement. Percentages are computed from the daily logs at
 * creation time, never typed by hand.
 */
@Entity
@Table(name = "biweekly_checkins")
public class BiweeklyCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "waist_cm", precision = 5, scale = 2)
    private BigDecimal waistCm;

    @Column(name = "green_days_pct")
    private Integer greenDaysPct;

    @Column(name = "clean_eating_pct")
    private Integer cleanEatingPct;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BiweeklyCheckIn() {
    }

    public BiweeklyCheckIn(Long userId, LocalDate checkinDate, BigDecimal weightKg, BigDecimal waistCm,
            Integer greenDaysPct, Integer cleanEatingPct, String note) {
        this.userId = userId;
        this.checkinDate = checkinDate;
        this.weightKg = weightKg;
        this.waistCm = waistCm;
        this.greenDaysPct = greenDaysPct;
        this.cleanEatingPct = cleanEatingPct;
        this.note = note;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getCheckinDate() {
        return checkinDate;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public BigDecimal getWaistCm() {
        return waistCm;
    }

    public Integer getGreenDaysPct() {
        return greenDaysPct;
    }

    public Integer getCleanEatingPct() {
        return cleanEatingPct;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
