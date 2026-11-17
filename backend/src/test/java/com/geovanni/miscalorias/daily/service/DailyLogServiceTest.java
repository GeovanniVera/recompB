package com.geovanni.miscalorias.daily.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.geovanni.miscalorias.catalog.domain.FoodItem;
import com.geovanni.miscalorias.catalog.domain.QuickPlate;
import com.geovanni.miscalorias.catalog.service.CatalogService;
import com.geovanni.miscalorias.daily.domain.DailyLog;
import com.geovanni.miscalorias.daily.domain.MealEntry;
import com.geovanni.miscalorias.daily.repository.DailyLogRepository;
import com.geovanni.miscalorias.daily.repository.MealEntryRepository;
import com.geovanni.miscalorias.daily.web.dto.DailySummaryResponse;
import com.geovanni.miscalorias.daily.web.dto.OffPlanEntryRequest;
import com.geovanni.miscalorias.daily.web.dto.QuickPlateEntryRequest;
import com.geovanni.miscalorias.daily.web.dto.ScaleEntryRequest;
import com.geovanni.miscalorias.daily.web.dto.UpdateDailyLogRequest;
import com.geovanni.miscalorias.profile.service.ProfileService;
import com.geovanni.miscalorias.profile.web.dto.TargetResponse;
import com.geovanni.miscalorias.shared.web.ResourceNotFoundException;

/**
 * Reference profile: male 70 kg / 170 cm / 25 y, factor 1.48, deficit 0.12.
 * TMB 1643 · TDEE 2431 · target 2139 · green band 2032-2246 · 140 P / 269 C / 56 F.
 */
@ExtendWith(MockitoExtension.class)
class DailyLogServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long LOG_ID = 50L;
    private static final LocalDate DATE = LocalDate.of(2026, 11, 17);

    private static final TargetResponse TARGETS = new TargetResponse(
            1643, 2431, 2139, 2032, 2246, 140, 126, 269, 56, 2450, 2950);

    @Mock
    private DailyLogRepository logRepository;
    @Mock
    private MealEntryRepository entryRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private ProfileService profileService;

    private DailyLogService service;
    private DailyLog log;
    private List<MealEntry> store;

    @BeforeEach
    void setUp() {
        service = new DailyLogService(logRepository, entryRepository, catalogService, profileService);

        log = new DailyLog(USER_ID, DATE);
        ReflectionTestUtils.setField(log, "id", LOG_ID);

        store = new ArrayList<>();

        // Used by every test: resolving the day and summarizing it.
        when(logRepository.findByUserIdAndLogDate(USER_ID, DATE)).thenReturn(Optional.of(log));
        // Exceptions thrown before the summary (e.g. entry not found) skip these two.
        lenient().when(profileService.getTargets(USER_ID)).thenReturn(TARGETS);
        lenient().when(entryRepository.findByDailyLogIdOrderByIdAsc(LOG_ID))
                .thenAnswer(inv -> List.copyOf(store));
    }

    // ---------- fixtures ----------

    private FoodItem chickenCooked() {
        FoodItem food = new FoodItem(null, "Pechuga/Fajitas Pollo 3B", "Cocido",
                new BigDecimal("165.00"), new BigDecimal("31.00"),
                new BigDecimal("0.00"), new BigDecimal("3.60"), "[100,150,200]");
        ReflectionTestUtils.setField(food, "id", 1L);
        return food;
    }

    private void stubEntrySave() {
        when(entryRepository.save(any(MealEntry.class))).thenAnswer(inv -> {
            MealEntry entry = inv.getArgument(0);
            ReflectionTestUtils.setField(entry, "id", (long) store.size() + 1);
            store.add(entry);
            return entry;
        });
    }

    private void stubDeleteLookup() {
        when(entryRepository.findByIdAndDailyLogId(anyLong(), eq(LOG_ID))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return store.stream().filter(entry -> id.equals(entry.getId())).findFirst();
        });
    }

    private void stubDelete() {
        doAnswer(inv -> {
            MealEntry entry = inv.getArgument(0);
            store.removeIf(candidate -> candidate.getId().equals(entry.getId()));
            return null;
        }).when(entryRepository).delete(any(MealEntry.class));
    }

    private void stubLogSave() {
        when(logRepository.saveAndFlush(any(DailyLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MealEntry entryOf(long kcal, String protein, boolean offPlan) {
        MealEntry entry = new MealEntry(log, null, "Test entry", null,
                BigDecimal.valueOf(kcal), new BigDecimal(protein),
                new BigDecimal("0.0"), new BigDecimal("0.0"), offPlan, "12:00");
        ReflectionTestUtils.setField(entry, "id", (long) store.size() + 100);
        return entry;
    }

    // ---------- tests ----------

    @Test
    void scaleEntryComputesProportionallyToGrams() {
        stubEntrySave();
        when(catalogService.requireVisibleFood(1L, USER_ID)).thenReturn(chickenCooked());

        DailySummaryResponse summary = service.addScaleEntry(USER_ID, DATE,
                new ScaleEntryRequest(1L, new BigDecimal("150"), "13:05"));

        assertThat(summary.entries()).hasSize(1);
        var entry = summary.entries().getFirst();
        assertThat(entry.entryName()).isEqualTo("Pechuga/Fajitas Pollo 3B (Cocido)");
        assertThat(entry.kcal()).isEqualTo(248L);        // 165 * 1.5 = 247.5 -> 248
        assertThat(entry.protein()).isEqualByComparingTo("46.5");
        assertThat(entry.carbs()).isEqualByComparingTo("0.0");
        assertThat(entry.fat()).isEqualByComparingTo("5.4");
        assertThat(entry.isOffPlan()).isFalse();
        assertThat(entry.loggedTime()).isEqualTo("13:05");

        assertThat(summary.consumed().kcal()).isEqualTo(248L);
        assertThat(summary.consumed().protein()).isEqualByComparingTo("46.5");
        assertThat(summary.remaining().kcal()).isEqualTo(1891L);
        assertThat(summary.cleanEatingPct()).isEqualTo(100);
    }

    @Test
    void quickPlateEntryUsesPlateTotals() {
        stubEntrySave();
        QuickPlate plate = new QuickPlate(null, "Desayuno Base", "60 g avena + 2 huevos + 1 tortilla",
                new BigDecimal("728.00"), new BigDecimal("37.82"),
                new BigDecimal("87.22"), new BigDecimal("25.10"));
        ReflectionTestUtils.setField(plate, "id", 9L);
        when(catalogService.requireVisiblePlate(9L, USER_ID)).thenReturn(plate);

        DailySummaryResponse summary = service.addQuickPlateEntry(USER_ID, DATE,
                new QuickPlateEntryRequest(9L, "08:30"));

        assertThat(summary.entries()).hasSize(1);
        assertThat(summary.consumed().kcal()).isEqualTo(728L);
        assertThat(summary.consumed().protein()).isEqualByComparingTo("37.8");
        assertThat(summary.entries().getFirst().grams()).isNull();
        assertThat(summary.entries().getFirst().loggedTime()).isEqualTo("08:30");
    }

    @Test
    void offPlanEntryReducesCleanEatingPct() {
        stubEntrySave();
        when(catalogService.requireVisibleFood(1L, USER_ID)).thenReturn(chickenCooked());
        service.addScaleEntry(USER_ID, DATE, new ScaleEntryRequest(1L, new BigDecimal("150"), "13:05"));

        DailySummaryResponse summary = service.addOffPlanEntry(USER_ID, DATE,
                new OffPlanEntryRequest("Tacos al pastor", 200, "21:00"));

        // 248 clean / 448 total = 55.35% -> 55
        assertThat(summary.consumed().kcal()).isEqualTo(448L);
        assertThat(summary.cleanEatingPct()).isEqualTo(55);
        assertThat(summary.entries()).hasSize(2);
        var offPlan = summary.entries().get(1);
        assertThat(offPlan.isOffPlan()).isTrue();
        assertThat(offPlan.protein()).isEqualByComparingTo("0.0");
    }

    @Test
    void cleanEatingPctIsHundredWhenNothingLogged() {
        DailySummaryResponse summary = service.getDay(USER_ID, DATE);

        assertThat(summary.consumed().kcal()).isZero();
        assertThat(summary.cleanEatingPct()).isEqualTo(100);
        assertThat(summary.inGreenMargin()).isFalse();
    }

    @Test
    void missingToTmbNeverGoesBelowZero() {
        stubEntrySave();

        DailySummaryResponse light = service.addOffPlanEntry(USER_ID, DATE,
                new OffPlanEntryRequest("Snack", 500, "10:00"));
        assertThat(light.missingToTmb()).isEqualTo(1143L);   // 1643 - 500

        DailySummaryResponse heavy = service.addOffPlanEntry(USER_ID, DATE,
                new OffPlanEntryRequest("Banquete", 2000, "20:00"));
        assertThat(heavy.consumed().kcal()).isEqualTo(2500L);
        assertThat(heavy.missingToTmb()).isZero();           // max(0, 1643 - 2500)
    }

    @Test
    void inGreenMarginRequiresCaloriesAndProtein() {
        store.add(entryOf(2139, "140.0", false));
        assertThat(service.getDay(USER_ID, DATE).inGreenMargin()).isTrue();

        store.clear();
        store.add(entryOf(2139, "125.0", false));            // protein below the 90% floor
        assertThat(service.getDay(USER_ID, DATE).inGreenMargin()).isFalse();

        store.clear();
        store.add(entryOf(1900, "140.0", false));            // below the -5% band
        assertThat(service.getDay(USER_ID, DATE).inGreenMargin()).isFalse();

        store.clear();
        store.add(entryOf(2400, "140.0", false));            // above the +5% band
        assertThat(service.getDay(USER_ID, DATE).inGreenMargin()).isFalse();
    }

    @Test
    void waterTargetDependsOnGymFlagAndAddsUp() {
        stubLogSave();

        DailySummaryResponse rest = service.updateDay(USER_ID, DATE,
                new UpdateDailyLogRequest(null, null, false));
        assertThat(rest.isGymDay()).isFalse();
        assertThat(rest.waterTargetMl()).isEqualTo(2450L);

        DailySummaryResponse gym = service.updateDay(USER_ID, DATE,
                new UpdateDailyLogRequest(null, null, true));
        assertThat(gym.isGymDay()).isTrue();
        assertThat(gym.waterTargetMl()).isEqualTo(2950L);

        service.updateDay(USER_ID, DATE, new UpdateDailyLogRequest(null, 250, null));
        DailySummaryResponse twoGlasses = service.updateDay(USER_ID, DATE,
                new UpdateDailyLogRequest(null, 250, null));
        assertThat(twoGlasses.waterMl()).isEqualTo(500);

        DailySummaryResponse absolute = service.updateDay(USER_ID, DATE,
                new UpdateDailyLogRequest(1200, null, null));
        assertThat(absolute.waterMl()).isEqualTo(1200);
    }

    @Test
    void waterNeverGoesNegative() {
        stubLogSave();
        service.updateDay(USER_ID, DATE, new UpdateDailyLogRequest(null, 250, null));

        DailySummaryResponse summary = service.updateDay(USER_ID, DATE,
                new UpdateDailyLogRequest(null, -1000, null));

        assertThat(summary.waterMl()).isZero();
    }

    @Test
    void deletingAnEntryRecomputesTheDay() {
        stubEntrySave();
        stubDeleteLookup();
        stubDelete();
        when(catalogService.requireVisibleFood(1L, USER_ID)).thenReturn(chickenCooked());
        service.addScaleEntry(USER_ID, DATE, new ScaleEntryRequest(1L, new BigDecimal("150"), "13:05"));

        DailySummaryResponse summary = service.deleteEntry(USER_ID, DATE, 1L);

        assertThat(summary.entries()).isEmpty();
        assertThat(summary.consumed().kcal()).isZero();
    }

    @Test
    void deletingSomeoneElsesEntryFailsAsNotFound() {
        stubDeleteLookup();

        assertThatThrownBy(() -> service.deleteEntry(USER_ID, DATE, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No encontramos esa entrada");
    }
}
