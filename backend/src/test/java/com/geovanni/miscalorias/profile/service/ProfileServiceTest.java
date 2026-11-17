package com.geovanni.miscalorias.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.geovanni.miscalorias.auth.domain.AppUser;
import com.geovanni.miscalorias.profile.domain.Sex;
import com.geovanni.miscalorias.profile.domain.UserProfile;
import com.geovanni.miscalorias.profile.repository.UserProfileRepository;
import com.geovanni.miscalorias.profile.web.dto.ProfileResponse;
import com.geovanni.miscalorias.profile.web.dto.TargetResponse;
import com.geovanni.miscalorias.profile.web.dto.UpdateProfileRequest;
import com.geovanni.miscalorias.shared.web.ResourceNotFoundException;

/**
 * Locks the Mifflin-St Jeor engine against the agreed reference profile:
 * male, 70 kg, 170 cm, 25 years, factor 1.48, deficit 0.12, 2.0 g/kg protein, 0.8 g/kg fat
 * -> TMB 1643, TDEE 2431, target 2139 kcal, 140 g P, 56 g F, 269 g C.
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserProfileRepository profileRepository;

    private ProfileService profileService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(profileRepository);
        user = new AppUser("geovanni.vera23@gmail.com", "Geovanni Vera", null);
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    private UserProfile referenceProfile() {
        return new UserProfile(user, Sex.MALE, 25,
                new BigDecimal("170.00"), new BigDecimal("70.00"), 4,
                new BigDecimal("1.48"), new BigDecimal("0.12"),
                new BigDecimal("2.00"), new BigDecimal("0.80"));
    }

    @Test
    void computesReferenceTargetsForMaleProfile() {
        TargetResponse targets = profileService.computeTargets(referenceProfile());

        assertThat(targets.tmb()).isEqualTo(1643L);
        assertThat(targets.tdee()).isEqualTo(2431L);
        assertThat(targets.targetKcal()).isEqualTo(2139L);

        assertThat(targets.minGreenKcal()).isEqualTo(2032L);
        assertThat(targets.maxGreenKcal()).isEqualTo(2246L);

        assertThat(targets.proteinGrams()).isEqualTo(140L);
        assertThat(targets.minGreenProtein()).isEqualTo(126L);
        assertThat(targets.fatGrams()).isEqualTo(56L);
        assertThat(targets.carbsGrams()).isEqualTo(269L);

        assertThat(targets.waterRestMl()).isEqualTo(2450L);
        assertThat(targets.waterGymMl()).isEqualTo(2950L);
    }

    @Test
    void macrosAddUpToTargetCalories() {
        TargetResponse targets = profileService.computeTargets(referenceProfile());

        long kcalFromMacros = (targets.proteinGrams() * 4) + (targets.carbsGrams() * 4)
                + (targets.fatGrams() * 9);

        // Rounded grams cannot always hit the target exactly; allow one gram of carbs worth.
        assertThat(kcalFromMacros).isBetween(targets.targetKcal() - 4, targets.targetKcal() + 4);
    }

    @Test
    void usesMinus161ForFemaleProfile() {
        UserProfile female = new UserProfile(user, Sex.FEMALE, 30,
                new BigDecimal("160.00"), new BigDecimal("60.00"), 3,
                new BigDecimal("1.48"), new BigDecimal("0.12"),
                new BigDecimal("2.00"), new BigDecimal("0.80"));

        TargetResponse targets = profileService.computeTargets(female);

        // 10*60 + 6.25*160 - 5*30 - 161 = 1289
        assertThat(targets.tmb()).isEqualTo(1289L);
    }

    @Test
    void returnsProfileWithTargets() {
        UserProfile profile = referenceProfile();
        ReflectionTestUtils.setField(profile, "id", 10L);
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getProfile(USER_ID);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.sex()).isEqualTo(Sex.MALE);
        assertThat(response.currentWeightKg()).isEqualByComparingTo("70.00");
        assertThat(response.targets().targetKcal()).isEqualTo(2139L);
    }

    @Test
    void updateAppliesOnlyProvidedFieldsAndRecalculates() {
        UserProfile profile = referenceProfile();
        ReflectionTestUtils.setField(profile, "id", 10L);
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(profileRepository.saveAndFlush(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResponse response = profileService.updateProfile(USER_ID,
                new UpdateProfileRequest(30, null, new BigDecimal("72.50"), null, null));

        assertThat(response.age()).isEqualTo(30);
        assertThat(response.currentWeightKg()).isEqualByComparingTo("72.50");
        // Untouched fields keep their value.
        assertThat(response.heightCm()).isEqualByComparingTo("170.00");
        assertThat(response.gymDaysPerWeek()).isEqualTo(4);
        assertThat(response.deficitPct()).isEqualByComparingTo("0.12");

        // 10*72.5 + 6.25*170 - 5*30 + 5 = 1642.5 -> 1643 (same TMB by coincidence of +5kg/-5y)
        assertThat(response.targets().tmb()).isEqualTo(1643L);
        assertThat(response.targets().proteinGrams()).isEqualTo(145L); // 2.0 * 72.5
    }

    @Test
    void failsWhenProfileDoesNotExist() {
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No hay perfil para el usuario 1");
    }

    @Test
    void carbsNeverGoNegativeWhenMacrosExceedTarget() {
        UserProfile heavyProtein = new UserProfile(user, Sex.MALE, 40,
                new BigDecimal("170.00"), new BigDecimal("100.00"), 2,
                new BigDecimal("1.20"), new BigDecimal("0.30"),
                new BigDecimal("3.00"), new BigDecimal("1.50"));

        TargetResponse targets = profileService.computeTargets(heavyProtein);

        assertThat(targets.carbsGrams()).isZero();
        assertThat(targets.proteinGrams()).isEqualTo(300L);
        assertThat(targets.fatGrams()).isEqualTo(150L);
    }
}
