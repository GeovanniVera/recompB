package com.geovanni.miscalorias.catalog.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.geovanni.miscalorias.auth.security.JwtService.AuthenticatedUser;
import com.geovanni.miscalorias.catalog.service.CatalogService;
import com.geovanni.miscalorias.catalog.web.dto.CreateQuickPlateRequest;
import com.geovanni.miscalorias.catalog.web.dto.QuickPlateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/quick-plates")
@Tag(name = "Quick plates", description = "Base 3B combos plus the user's own plates")
public class QuickPlateController {

    private final CatalogService catalogService;

    public QuickPlateController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "List visible quick plates")
    public List<QuickPlateResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return catalogService.listQuickPlates(principal.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Save a new quick plate combo")
    public QuickPlateResponse create(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateQuickPlateRequest request) {
        return catalogService.createQuickPlate(principal.id(), request);
    }
}
