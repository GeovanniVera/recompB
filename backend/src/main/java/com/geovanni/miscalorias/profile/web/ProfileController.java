package com.geovanni.miscalorias.profile.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geovanni.miscalorias.auth.security.JwtService.AuthenticatedUser;
import com.geovanni.miscalorias.profile.service.ProfileService;
import com.geovanni.miscalorias.profile.web.dto.ProfileResponse;
import com.geovanni.miscalorias.profile.web.dto.UpdateProfileRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Body composition profile and live calorie/macro goals")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Current profile with calculated goals")
    public ProfileResponse getProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return profileService.getProfile(principal.id());
    }

    @PutMapping
    @Operation(summary = "Update the profile and return recalculated goals")
    public ProfileResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(principal.id(), request);
    }
}
