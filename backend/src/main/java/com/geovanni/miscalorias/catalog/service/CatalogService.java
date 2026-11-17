package com.geovanni.miscalorias.catalog.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geovanni.miscalorias.catalog.domain.FoodItem;
import com.geovanni.miscalorias.catalog.domain.QuickPlate;
import com.geovanni.miscalorias.catalog.repository.FoodItemRepository;
import com.geovanni.miscalorias.catalog.repository.QuickPlateRepository;
import com.geovanni.miscalorias.catalog.web.dto.CreateFoodItemRequest;
import com.geovanni.miscalorias.catalog.web.dto.CreateQuickPlateRequest;
import com.geovanni.miscalorias.catalog.web.dto.FoodItemResponse;
import com.geovanni.miscalorias.catalog.web.dto.QuickPlateResponse;
import com.geovanni.miscalorias.shared.web.ResourceNotFoundException;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);
    private static final TypeReference<List<Integer>> GRAMS_TYPE = new TypeReference<>() {
    };

    private final FoodItemRepository foodRepository;
    private final QuickPlateRepository plateRepository;
    private final ObjectMapper objectMapper;

    public CatalogService(FoodItemRepository foodRepository, QuickPlateRepository plateRepository,
            ObjectMapper objectMapper) {
        this.foodRepository = foodRepository;
        this.plateRepository = plateRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<FoodItemResponse> searchFoods(Long userId, String query) {
        String needle = StringUtils.hasText(query) ? query.trim().toLowerCase(Locale.ROOT) : null;
        return foodRepository.findVisibleTo(userId).stream()
                .filter(item -> matches(item, needle))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FoodItemResponse createFood(Long userId, CreateFoodItemRequest request) {
        FoodItem saved = foodRepository.saveAndFlush(new FoodItem(
                userId,
                request.name().trim(),
                request.stateTag().label(),
                request.kcalPer100g(),
                request.proteinPer100g(),
                request.carbsPer100g(),
                request.fatPer100g(),
                writeQuickGrams(request.quickGrams())));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<QuickPlateResponse> listQuickPlates(Long userId) {
        return plateRepository.findVisibleTo(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public QuickPlateResponse createQuickPlate(Long userId, CreateQuickPlateRequest request) {
        QuickPlate saved = plateRepository.saveAndFlush(new QuickPlate(
                userId,
                request.title().trim(),
                request.subtitle() == null ? null : request.subtitle().trim(),
                request.kcal(),
                request.protein(),
                request.carbs(),
                request.fat()));
        return toResponse(saved);
    }

    /** Loads a food the caller is allowed to log (global or their own). */
    @Transactional(readOnly = true)
    public FoodItem requireVisibleFood(Long foodItemId, Long userId) {
        FoodItem item = foodRepository.findById(foodItemId)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos ese alimento".formatted(foodItemId)));
        if (!item.isVisibleTo(userId)) {
            // Same response as "not found": never reveal other users' items.
            throw new ResourceNotFoundException("No encontramos ese alimento".formatted(foodItemId));
        }
        return item;
    }

    /** Loads a quick plate the caller is allowed to use. */
    @Transactional(readOnly = true)
    public QuickPlate requireVisiblePlate(Long quickPlateId, Long userId) {
        QuickPlate plate = plateRepository.findById(quickPlateId)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos ese plato rápido".formatted(quickPlateId)));
        if (!plate.isVisibleTo(userId)) {
            throw new ResourceNotFoundException("No encontramos ese plato rápido".formatted(quickPlateId));
        }
        return plate;
    }

    private boolean matches(FoodItem item, String needle) {
        if (needle == null) {
            return true;
        }
        return item.getName().toLowerCase(Locale.ROOT).contains(needle)
                || item.getStateTag().toLowerCase(Locale.ROOT).contains(needle);
    }

    private FoodItemResponse toResponse(FoodItem item) {
        return new FoodItemResponse(
                item.getId(),
                item.getUserId(),
                item.getName(),
                item.getStateTag(),
                item.getKcalPer100g(),
                item.getProteinPer100g(),
                item.getCarbsPer100g(),
                item.getFatPer100g(),
                readQuickGrams(item.getQuickGramsJson()),
                item.getCreatedAt());
    }

    private QuickPlateResponse toResponse(QuickPlate plate) {
        return new QuickPlateResponse(
                plate.getId(),
                plate.getUserId(),
                plate.getTitle(),
                plate.getSubtitle(),
                plate.getKcal(),
                plate.getProtein(),
                plate.getCarbs(),
                plate.getFat(),
                plate.getCreatedAt());
    }

    private List<Integer> readQuickGrams(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<Integer> parsed = objectMapper.readValue(json, GRAMS_TYPE);
            return parsed.stream()
                    .filter(value -> value != null && value > 0)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (Exception ex) {
            log.warn("Ignoring unreadable quick_grams_json value: {}", json);
            return List.of();
        }
    }

    private String writeQuickGrams(List<Integer> grams) {
        if (grams == null || grams.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(grams);
        } catch (Exception ex) {
            log.warn("Could not serialize quick grams, storing null", ex);
            return null;
        }
    }
}
