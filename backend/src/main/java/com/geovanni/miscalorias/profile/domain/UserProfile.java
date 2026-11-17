package com.geovanni.miscalorias.profile.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.geovanni.miscalorias.auth.domain.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sex sex;

    @Column(nullable = false)
    private int age;

    @Column(name = "height_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "current_weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal currentWeightKg;

    @Column(name = "gym_days_per_week", nullable = false)
    private int gymDaysPerWeek;

    @Column(name = "activity_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal activityFactor;

    @Column(name = "deficit_pct", nullable = false, precision = 4, scale = 2)
    private BigDecimal deficitPct;

    @Column(name = "protein_per_kg", nullable = false, precision = 4, scale = 2)
    private BigDecimal proteinPerKg;

    @Column(name = "fat_per_kg", nullable = false, precision = 4, scale = 2)
    private BigDecimal fatPerKg;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
    }

    public UserProfile(AppUser user, Sex sex, int age, BigDecimal heightCm, BigDecimal currentWeightKg,
            int gymDaysPerWeek, BigDecimal activityFactor, BigDecimal deficitPct,
            BigDecimal proteinPerKg, BigDecimal fatPerKg) {
        this.user = user;
        this.sex = sex;
        this.age = age;
        this.heightCm = heightCm;
        this.currentWeightKg = currentWeightKg;
        this.gymDaysPerWeek = gymDaysPerWeek;
        this.activityFactor = activityFactor;
        this.deficitPct = deficitPct;
        this.proteinPerKg = proteinPerKg;
        this.fatPerKg = fatPerKg;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public BigDecimal getCurrentWeightKg() {
        return currentWeightKg;
    }

    public void setCurrentWeightKg(BigDecimal currentWeightKg) {
        this.currentWeightKg = currentWeightKg;
    }

    public int getGymDaysPerWeek() {
        return gymDaysPerWeek;
    }

    public void setGymDaysPerWeek(int gymDaysPerWeek) {
        this.gymDaysPerWeek = gymDaysPerWeek;
    }

    public BigDecimal getActivityFactor() {
        return activityFactor;
    }

    public void setActivityFactor(BigDecimal activityFactor) {
        this.activityFactor = activityFactor;
    }

    public BigDecimal getDeficitPct() {
        return deficitPct;
    }

    public void setDeficitPct(BigDecimal deficitPct) {
        this.deficitPct = deficitPct;
    }

    public BigDecimal getProteinPerKg() {
        return proteinPerKg;
    }

    public void setProteinPerKg(BigDecimal proteinPerKg) {
        this.proteinPerKg = proteinPerKg;
    }

    public BigDecimal getFatPerKg() {
        return fatPerKg;
    }

    public void setFatPerKg(BigDecimal fatPerKg) {
        this.fatPerKg = fatPerKg;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
