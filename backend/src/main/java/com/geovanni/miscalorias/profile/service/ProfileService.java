package com.geovanni.miscalorias.profile.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geovanni.miscalorias.profile.domain.Sex;
import com.geovanni.miscalorias.profile.domain.UserProfile;
import com.geovanni.miscalorias.profile.repository.UserProfileRepository;
import com.geovanni.miscalorias.profile.web.dto.ProfileResponse;
import com.geovanni.miscalorias.profile.web.dto.TargetResponse;
import com.geovanni.miscalorias.profile.web.dto.UpdateProfileRequest;
import com.geovanni.miscalorias.shared.web.ResourceNotFoundException;

/**
 * Profile reads/writes plus the Mifflin-St Jeor calorie engine.
 *
 * <p>Formulas (male shown; female uses -161 instead of +5):
 * <pre>
 *   TMB        = 10 * kg + 6.25 * cm - 5 * age + 5
 *   TDEE       = TMB * activityFactor
 *   targetKcal = TDEE * (1 - deficitPct)
 *   green band = targetKcal +/- 5%
 *   protein    = proteinPerKg * kg     (green floor = 90% of it)
 *   fat        = fatPerKg * kg
 *   carbs      = (targetKcal - protein*4 - fat*9) / 4
 *   water      = kg * 35 ml   (gym day: + 500 ml)
 * </pre>
 */
@Service
public class ProfileService {

    private static final double KCAL_PER_G_PROTEIN = 4.0;
    private static final double KCAL_PER_G_FAT = 9.0;
    private static final double KCAL_PER_G_CARBS = 4.0;
    private static final double GREEN_BAND_PCT = 0.05;
    private static final double PROTEIN_FLOOR_PCT = 0.90;
    private static final double WATER_ML_PER_KG = 35.0;
    private static final double GYM_WATER_BONUS_ML = 500.0;

    private final UserProfileRepository profileRepository;

    public ProfileService(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        UserProfile profile = requireProfile(userId);
        return ProfileResponse.of(profile, computeTargets(profile));
    }

    /** Current derived goals for a user, used by other slices (e.g. the daily log). */
    @Transactional(readOnly = true)
    public TargetResponse getTargets(Long userId) {
        return computeTargets(requireProfile(userId));
    }

    /** Keeps the profile weight in sync with a new check-in. */
    @Transactional
    public void updateCurrentWeight(Long userId, BigDecimal weightKg) {
        UserProfile profile = requireProfile(userId);
        profile.setCurrentWeightKg(weightKg);
        profileRepository.saveAndFlush(profile);
    }

    /** When the profile was created; used as the fallback reference for check-in reminders. */
    @Transactional(readOnly = true)
    public Optional<Instant> profileCreatedAt(Long userId) {
        return profileRepository.findByUserId(userId).map(UserProfile::getCreatedAt);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserProfile profile = requireProfile(userId);

        if (request.age() != null) {
            profile.setAge(request.age());
        }
        if (request.heightCm() != null) {
            profile.setHeightCm(request.heightCm());
        }
        if (request.currentWeightKg() != null) {
            profile.setCurrentWeightKg(request.currentWeightKg());
        }
        if (request.gymDaysPerWeek() != null) {
            profile.setGymDaysPerWeek(request.gymDaysPerWeek());
        }
        if (request.deficitPct() != null) {
            profile.setDeficitPct(request.deficitPct());
        }

        // Flush inside the transaction so @PreUpdate stamps updatedAt before it is mapped
        // into the response; otherwise the client receives the previous timestamp.
        UserProfile saved = profileRepository.saveAndFlush(profile);
        return ProfileResponse.of(saved, computeTargets(saved));
    }

    /**
     * Computes every derived goal from the stored profile. Pure function of the
     * profile fields, so it is cheap and safe to call on every read.
     */
    public TargetResponse computeTargets(UserProfile profile) {
        double weightKg = profile.getCurrentWeightKg().doubleValue();
        double heightCm = profile.getHeightCm().doubleValue();
        double ageYears = profile.getAge();
        boolean male = profile.getSex() == Sex.MALE;

        double tmbRaw = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * ageYears) + (male ? 5.0 : -161.0);
        double tdeeRaw = tmbRaw * profile.getActivityFactor().doubleValue();
        double targetRaw = tdeeRaw * (1.0 - profile.getDeficitPct().doubleValue());

        long tmb = Math.round(tmbRaw);
        long tdee = Math.round(tdeeRaw);
        long targetKcal = Math.round(targetRaw);
        long minGreenKcal = Math.round(targetRaw * (1.0 - GREEN_BAND_PCT));
        long maxGreenKcal = Math.round(targetRaw * (1.0 + GREEN_BAND_PCT));

        double proteinRaw = profile.getProteinPerKg().doubleValue() * weightKg;
        double fatRaw = profile.getFatPerKg().doubleValue() * weightKg;
        long proteinGrams = Math.round(proteinRaw);
        long fatGrams = Math.round(fatRaw);
        long minGreenProtein = Math.round(proteinRaw * PROTEIN_FLOOR_PCT);

        long remainingKcal = targetKcal - Math.round(proteinGrams * KCAL_PER_G_PROTEIN)
                - Math.round(fatGrams * KCAL_PER_G_FAT);
        long carbsGrams = Math.max(0, Math.round(remainingKcal / KCAL_PER_G_CARBS));

        long waterRestMl = Math.round(weightKg * WATER_ML_PER_KG);
        long waterGymMl = Math.round((weightKg * WATER_ML_PER_KG) + GYM_WATER_BONUS_ML);

        return new TargetResponse(tmb, tdee, targetKcal, minGreenKcal, maxGreenKcal,
                proteinGrams, minGreenProtein, carbsGrams, fatGrams, waterRestMl, waterGymMl);
    }

    private UserProfile requireProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay perfil para el usuario %d".formatted(userId)));
    }
}
