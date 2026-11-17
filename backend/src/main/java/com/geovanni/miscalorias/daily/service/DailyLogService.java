package com.geovanni.miscalorias.daily.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.geovanni.miscalorias.catalog.domain.FoodItem;
import com.geovanni.miscalorias.catalog.domain.QuickPlate;
import com.geovanni.miscalorias.catalog.service.CatalogService;
import com.geovanni.miscalorias.daily.domain.DailyLog;
import com.geovanni.miscalorias.daily.domain.MealEntry;
import com.geovanni.miscalorias.daily.repository.DailyLogRepository;
import com.geovanni.miscalorias.daily.repository.MealEntryRepository;
import com.geovanni.miscalorias.daily.web.dto.DailySummaryResponse;
import com.geovanni.miscalorias.daily.web.dto.MacroTotals;
import com.geovanni.miscalorias.daily.web.dto.MealEntryResponse;
import com.geovanni.miscalorias.daily.web.dto.OffPlanEntryRequest;
import com.geovanni.miscalorias.daily.web.dto.QuickPlateEntryRequest;
import com.geovanni.miscalorias.daily.web.dto.ScaleEntryRequest;
import com.geovanni.miscalorias.daily.web.dto.UpdateDailyLogRequest;
import com.geovanni.miscalorias.profile.service.ProfileService;
import com.geovanni.miscalorias.profile.web.dto.TargetResponse;
import com.geovanni.miscalorias.shared.web.ResourceNotFoundException;

/**
 * Daily food log. Every public method returns the full {@link DailySummaryResponse}
 * so the client always renders one consistent snapshot.
 */
@Service
public class DailyLogService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO_MACRO = new BigDecimal("0.0");
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final DailyLogRepository logRepository;
    private final MealEntryRepository entryRepository;
    private final CatalogService catalogService;
    private final ProfileService profileService;

    public DailyLogService(DailyLogRepository logRepository, MealEntryRepository entryRepository,
            CatalogService catalogService, ProfileService profileService) {
        this.logRepository = logRepository;
        this.entryRepository = entryRepository;
        this.catalogService = catalogService;
        this.profileService = profileService;
    }

    @Transactional
    public DailySummaryResponse getDay(Long userId, LocalDate date) {
        return summarize(getOrCreate(userId, date));
    }

    @Transactional
    public DailySummaryResponse addScaleEntry(Long userId, LocalDate date, ScaleEntryRequest request) {
        DailyLog log = getOrCreate(userId, date);
        FoodItem food = catalogService.requireVisibleFood(request.foodItemId(), userId);

        BigDecimal factor = request.grams().divide(HUNDRED, 6, RoundingMode.HALF_UP);
        long kcal = Math.round(food.getKcalPer100g().multiply(factor).doubleValue());
        BigDecimal protein = DayMetricsCalculator.scale1(food.getProteinPer100g().multiply(factor));
        BigDecimal carbs = DayMetricsCalculator.scale1(food.getCarbsPer100g().multiply(factor));
        BigDecimal fat = DayMetricsCalculator.scale1(food.getFatPer100g().multiply(factor));

        entryRepository.save(new MealEntry(log, food.getId(), entryName(food), request.grams(),
                BigDecimal.valueOf(kcal), protein, carbs, fat, false, resolveTime(request.loggedTime())));
        entryRepository.flush();

        return summarize(log);
    }

    @Transactional
    public DailySummaryResponse addQuickPlateEntry(Long userId, LocalDate date, QuickPlateEntryRequest request) {
        DailyLog log = getOrCreate(userId, date);
        QuickPlate plate = catalogService.requireVisiblePlate(request.quickPlateId(), userId);

        entryRepository.save(new MealEntry(log, null, plate.getTitle(), null,
                plate.getKcal(), plate.getProtein(), plate.getCarbs(), plate.getFat(), false,
                resolveTime(request.loggedTime())));
        entryRepository.flush();

        return summarize(log);
    }

    @Transactional
    public DailySummaryResponse addOffPlanEntry(Long userId, LocalDate date, OffPlanEntryRequest request) {
        DailyLog log = getOrCreate(userId, date);

        entryRepository.save(new MealEntry(log, null, request.label().trim(), null,
                BigDecimal.valueOf(request.kcal()), ZERO_MACRO, ZERO_MACRO, ZERO_MACRO, true,
                resolveTime(request.loggedTime())));
        entryRepository.flush();

        return summarize(log);
    }

    @Transactional
    public DailySummaryResponse deleteEntry(Long userId, LocalDate date, Long entryId) {
        DailyLog log = getOrCreate(userId, date);
        MealEntry entry = entryRepository.findByIdAndDailyLogId(entryId, log.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos esa entrada".formatted(entryId)));

        entryRepository.delete(entry);
        entryRepository.flush();

        return summarize(log);
    }

    @Transactional
    public DailySummaryResponse updateDay(Long userId, LocalDate date, UpdateDailyLogRequest request) {
        DailyLog log = getOrCreate(userId, date);

        if (request.waterMl() != null) {
            log.setWaterMl(request.waterMl());
        }
        if (request.addWaterMl() != null) {
            log.setWaterMl(log.getWaterMl() + request.addWaterMl());
        }
        if (request.isGymDay() != null) {
            log.setGymDay(request.isGymDay());
        }

        DailyLog saved = logRepository.saveAndFlush(log);
        return summarize(saved);
    }

    private DailyLog getOrCreate(Long userId, LocalDate date) {
        return logRepository.findByUserIdAndLogDate(userId, date)
                .orElseGet(() -> logRepository.saveAndFlush(new DailyLog(userId, date)));
    }

    private DailySummaryResponse summarize(DailyLog log) {
        List<MealEntry> entries = entryRepository.findByDailyLogIdOrderByIdAsc(log.getId());
        TargetResponse targets = profileService.getTargets(log.getUserId());

        long consumedKcal = DayMetricsCalculator.consumedKcal(entries);
        BigDecimal protein = DayMetricsCalculator.consumedMacro(entries, MealEntry::getProtein);
        BigDecimal carbs = DayMetricsCalculator.consumedMacro(entries, MealEntry::getCarbs);
        BigDecimal fat = DayMetricsCalculator.consumedMacro(entries, MealEntry::getFat);

        MacroTotals consumed = new MacroTotals(consumedKcal, protein, carbs, fat);
        MacroTotals remaining = new MacroTotals(
                targets.targetKcal() - consumedKcal,
                BigDecimal.valueOf(targets.proteinGrams()).subtract(protein),
                BigDecimal.valueOf(targets.carbsGrams()).subtract(carbs),
                BigDecimal.valueOf(targets.fatGrams()).subtract(fat));

        long missingToTmb = DayMetricsCalculator.missingToTmb(targets.tmb(), consumedKcal);

        boolean inGreenMargin = DayMetricsCalculator.inGreenMargin(consumedKcal, protein, targets);

        int cleanEatingPct = DayMetricsCalculator.cleanEatingPct(entries);

        long waterTargetMl = log.isGymDay() ? targets.waterGymMl() : targets.waterRestMl();

        return new DailySummaryResponse(
                log.getLogDate(),
                log.isGymDay(),
                log.getWaterMl(),
                waterTargetMl,
                targets,
                consumed,
                remaining,
                missingToTmb,
                inGreenMargin,
                cleanEatingPct,
                entries.stream().map(MealEntryResponse::of).toList());
    }

    private static String entryName(FoodItem food) {
        String composed = "%s (%s)".formatted(food.getName(), food.getStateTag());
        return composed.length() <= 200 ? composed : composed.substring(0, 200);
    }

    private static String resolveTime(String requested) {
        return StringUtils.hasText(requested) ? requested : LocalTime.now(ZoneOffset.UTC).format(HH_MM);
    }
}
