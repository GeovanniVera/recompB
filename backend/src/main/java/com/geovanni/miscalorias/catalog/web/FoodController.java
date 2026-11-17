package com.geovanni.miscalorias.catalog.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.geovanni.miscalorias.auth.security.JwtService.AuthenticatedUser;
import com.geovanni.miscalorias.catalog.service.CatalogService;
import com.geovanni.miscalorias.catalog.web.dto.CreateFoodItemRequest;
import com.geovanni.miscalorias.catalog.web.dto.FoodItemResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/foods")
@Tag(name = "Food catalog", description = "Global 3B foods plus the user's own items")
public class FoodController {

    private final CatalogService catalogService;

    public FoodController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "List visible foods, optionally filtered by name or state tag")
    public List<FoodItemResponse> list(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(name = "q", required = false) String query) {
        return catalogService.searchFoods(principal.id(), query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a custom 3B food item for the current user")
    public FoodItemResponse create(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateFoodItemRequest request) {
        return catalogService.createFood(principal.id(), request);
    }
}
