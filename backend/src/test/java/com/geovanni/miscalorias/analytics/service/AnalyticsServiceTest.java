package com.geovanni.miscalorias.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

import com.geovanni.miscalorias.analytics.domain.BiweeklyCheckIn;
import com.geovanni.miscalorias.analytics.repository.BiweeklyCheckInRepository;
import com.geovanni.miscalorias.analytics.web.dto.CheckInResult;
import com.geovanni.miscalorias.analytics.web.dto.CreateCheckInRequest;
import com.geovanni.miscalorias.analytics.web.dto.FourteenDayResponse;
import com.geovanni.miscalorias.analytics.web.dto.WeekComparisonResponse;
import com.geovanni.miscalorias.config.AppTimeProperties;
import com.geovanni.miscalorias.daily.domain.DailyLog;
import com.geovanni.miscalorias.daily.domain.MealEntry;
import com.geovanni.miscalorias.daily.repository.DailyLogRepository;
import com.geovanni.miscalorias.daily.repository.MealEntryRepository;
import com.geovanni.miscalorias.profile.service.ProfileService;
import com.geovanni.miscalorias.profile.web.dto.TargetResponse;

/**
 * Reference profile: TMB 1643 · TDEE 2431 · target 2139 · band 2032-2246 · 140 P.
 * A day qualifies as green at 2139 kcal with >= 126 g of protein.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate END = LocalDate.of(2026, 11, 17);
    private static final LocalDate START = END.minusDays(13); // 2026-11-04

    private static final TargetResponse TARGETS = new TargetResponse(
            1643, 2431, 2139, 2032, 2246, 140, 126, 269, 56, 2450, 2950);

    @Mock
    private DailyLogRepository logRepository;
    @Mock
    private MealEntryRepository entryRepository;
    @Mock
    private BiweeklyCheckInRepository checkInRepository;
    @Mock
    private ProfileService profileService;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(logRepository, entryRepository, checkInRepository,
                profileService, new AppTimeProperties("America/Mexico_City"));
    }

    // ---------- helpers ----------

    private record Fixture(List<DailyLog> logs, List<MealEntry> entries) {
    }

    /** Builds logs and one entry per listed day, in green or off-target mode. */
    private Fixture days(List<LocalDate> dates, long kcal, String protein) {
        List<DailyLog> logs = new ArrayList<>();
        List<MealEntry> entries = new ArrayList<>();
        long id = 1;
        for (LocalDate date : dates) {
            DailyLog log = new DailyLog(USER_ID, date);
            ReflectionTestUtils.setField(log, "id", id);
            logs.add(log);
            MealEntry entry = new MealEntry(log, null, "Test", null,
                    BigDecimal.valueOf(kcal), new BigDecimal(protein),
                    new BigDecimal("0.0"), new BigDecimal("0.0"), false, "12:00");
            ReflectionTestUtils.setField(entry, "id", id);
            entries.add(entry);
            id++;
        }
        return new Fixture(logs, entries);
    }

    private List<LocalDate> consecutive(LocalDate start, int count) {
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            dates.add(start.plusDays(i));
        }
        return dates;
    }

    /** Concatenates fixtures, reassigning unique ids (each fixture starts at 1). */
    private Fixture merge(Fixture... fixtures) {
        List<DailyLog> logs = new ArrayList<>();
        List<MealEntry> entries = new ArrayList<>();
        long id = 1;
        for (Fixture fixture : fixtures) {
            for (int index = 0; index < fixture.logs().size(); index++) {
                DailyLog log = fixture.logs().get(index);
                MealEntry entry = fixture.entries().get(index);
                ReflectionTestUtils.setField(log, "id", id);
                ReflectionTestUtils.setField(entry, "id", id);
                logs.add(log);
                entries.add(entry);
                id++;
            }
        }
        return new Fixture(logs, entries);
    }

    private void stubSeries(LocalDate start, LocalDate end, Fixture fixture) {        when(logRepository.findByUserIdAndLogDateBetweenOrderByLogDateAsc(USER_ID, start, end))
                .thenReturn(fixture.logs());
        when(entryRepository.findByDailyLogIdInOrderByIdAsc(any()))
                .thenReturn(fixture.entries());
        when(profileService.getTargets(USER_ID)).thenReturn(TARGETS);
    }

    // ---------- 14-day adherence ----------

    @Test
    void computesGreenAdherenceFromLoggedDays() {
        Fixture fixture = days(List.of(START, START.plusDays(4), START.plusDays(9), END), 2139, "140.0");
        stubSeries(START, END, fixture);

        FourteenDayResponse response = service.getFourteenDays(USER_ID, END);

        assertThat(response.startDate()).isEqualTo(START);
        assertThat(response.endDate()).isEqualTo(END);
        assertThat(response.points()).hasSize(14);
        assertThat(response.points().getFirst().dayLabel()).isEqualTo("D1");
        assertThat(response.points().get(13).dayLabel()).isEqualTo("D14");

        var summary = response.summary();
        assertThat(summary.totalDays()).isEqualTo(14);
        assertThat(summary.loggedDays()).isEqualTo(4);
        assertThat(summary.greenDaysCount()).isEqualTo(4);
        assertThat(summary.greenDaysPct()).isEqualTo(29);   // 4/14 = 28.57 -> 29
        assertThat(summary.avgKcal()).isEqualTo(2139);
        assertThat(summary.avgProtein()).isEqualByComparingTo("140.0");
        assertThat(summary.avgCleanEatingPct()).isEqualTo(100);

        // An unlogged day is not green and reports no intake.
        var emptyDay = response.points().get(1);
        assertThat(emptyDay.kcal()).isZero();
        assertThat(emptyDay.inGreenMargin()).isFalse();
        assertThat(emptyDay.cleanEatingPct()).isEqualTo(100);
    }

    @Test
    void offPlanCaloriesBreakTheGreenDay() {
        // 2139 kcal but 60% of them off-plan: still in the band, yet cleanEating drops.
        DailyLog log = new DailyLog(USER_ID, END);
        ReflectionTestUtils.setField(log, "id", 1L);
        MealEntry clean = new MealEntry(log, null, "Clean", null, new BigDecimal("1000"),
                new BigDecimal("140.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), false, "12:00");
        MealEntry offPlan = new MealEntry(log, null, "Craving", null, new BigDecimal("1139"),
                new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), true, "20:00");
        when(logRepository.findByUserIdAndLogDateBetweenOrderByLogDateAsc(USER_ID, START, END))
                .thenReturn(List.of(log));
        when(entryRepository.findByDailyLogIdInOrderByIdAsc(any())).thenReturn(List.of(clean, offPlan));
        when(profileService.getTargets(USER_ID)).thenReturn(TARGETS);

        var summary = service.getFourteenDays(USER_ID, END).summary();

        assertThat(summary.avgKcal()).isEqualTo(2139);
        assertThat(summary.avgCleanEatingPct()).isEqualTo(47);   // 1000/2139 = 46.8 -> 47
        assertThat(summary.greenDaysCount()).isEqualTo(1);
    }

    // ---------- week comparison ----------

    @Test
    void comparesPreviousAndCurrentWeek() {
        Fixture previous = days(consecutive(START, 7), 2000, "120.0");
        Fixture current = days(consecutive(START.plusDays(7), 7), 2139, "140.0");
        stubSeries(START, END, merge(previous, current));

        FourteenDayResponse response = service.getFourteenDays(USER_ID, END);

        assertThat(response.previousWeek().avgKcal()).isEqualTo(2000);
        assertThat(response.previousWeek().greenDaysPct()).isZero();
        assertThat(response.currentWeek().avgKcal()).isEqualTo(2139);
        assertThat(response.currentWeek().greenDaysPct()).isEqualTo(100);

        assertThat(response.deltas().kcalDelta()).isEqualTo(139);
        assertThat(response.deltas().proteinDelta()).isEqualByComparingTo("20.0");
        assertThat(response.deltas().greenPctDelta()).isEqualTo(100);
        assertThat(response.deltas().cleanPctDelta()).isZero();
    }

    @Test
    void compareWeeksUsesArbitraryBlocksAndAttachesClosestCheckIn() {
        LocalDate baseStart = LocalDate.of(2026, 10, 21);
        LocalDate currentStart = LocalDate.of(2026, 11, 11);
        Fixture base = days(consecutive(baseStart, 7), 2000, "120.0");
        Fixture current = days(consecutive(currentStart, 7), 2200, "150.0");

        when(logRepository.findByUserIdAndLogDateBetweenOrderByLogDateAsc(USER_ID, baseStart, baseStart.plusDays(6)))
                .thenReturn(base.logs());
        when(logRepository.findByUserIdAndLogDateBetweenOrderByLogDateAsc(USER_ID, currentStart, currentStart.plusDays(6)))
                .thenReturn(current.logs());
        when(entryRepository.findByDailyLogIdInOrderByIdAsc(any()))
                .thenReturn(base.entries())
                .thenReturn(current.entries());
        when(profileService.getTargets(USER_ID)).thenReturn(TARGETS);

        BiweeklyCheckIn marker = new BiweeklyCheckIn(USER_ID, LocalDate.of(2026, 11, 10),
                new BigDecimal("70.00"), new BigDecimal("80.00"), 90, 95, null);
        ReflectionTestUtils.setField(marker, "id", 5L);
        when(checkInRepository.findFirstByUserIdAndCheckinDateLessThanEqualOrderByCheckinDateDesc(
                USER_ID, baseStart.plusDays(6))).thenReturn(Optional.empty());
        when(checkInRepository.findFirstByUserIdAndCheckinDateLessThanEqualOrderByCheckinDateDesc(
                USER_ID, currentStart.plusDays(6))).thenReturn(Optional.of(marker));

        WeekComparisonResponse response = service.compareWeeks(USER_ID, baseStart, currentStart);

        assertThat(response.base().stats().avgKcal()).isEqualTo(2000);
        assertThat(response.base().checkIn()).isNull();
        assertThat(response.current().stats().avgKcal()).isEqualTo(2200);
        assertThat(response.current().checkIn().weightKg()).isEqualByComparingTo("70.00");
        assertThat(response.current().points()).hasSize(7);
        assertThat(response.deltas().kcalDelta()).isEqualTo(200);
    }

    // ---------- adjustment rule ----------

    private void stubTwelveGreenDays() {
        Fixture fixture = days(consecutive(START, 12), 2139, "140.0");
        stubSeries(START, END, fixture);
    }

    private BiweeklyCheckIn previousCheckIn(String weight, String waist) {
        BiweeklyCheckIn checkIn = new BiweeklyCheckIn(USER_ID, LocalDate.of(2026, 11, 03),
                new BigDecimal(weight), new BigDecimal(waist), 85, 95, "baseline");
        ReflectionTestUtils.setField(checkIn, "id", 10L);
        when(checkInRepository.findFirstByUserIdAndCheckinDateLessThanOrderByCheckinDateDesc(USER_ID, END))
                .thenReturn(Optional.of(checkIn));
        return checkIn;
    }

    private void stubSave() {
        when(checkInRepository.saveAndFlush(any(BiweeklyCheckIn.class))).thenAnswer(invocation -> {
            BiweeklyCheckIn saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 11L);
            return saved;
        });
    }

    @Test
    void suggestsFourteenPercentDeficitWhenStalledWithHighAdherence() {
        stubTwelveGreenDays();
        previousCheckIn("70.00", "80.00");
        stubSave();

        CheckInResult result = service.createCheckIn(USER_ID, new CreateCheckInRequest(
                END, new BigDecimal("70.00"), new BigDecimal("80.00"), "same weight, same waist"));

        assertThat(result.checkIn().greenDaysPct()).isEqualTo(86);   // 12/14
        assertThat(result.adjustmentSuggested()).isTrue();
        assertThat(result.suggestedDeficitPct()).isEqualByComparingTo("0.14");
        assertThat(result.message()).contains("14%");
        verify(profileService).updateCurrentWeight(USER_ID, new BigDecimal("70.00"));
    }

    @Test
    void doesNotSuggestWhenAdherenceIsBelowEightyPercent() {
        // Only five green days out of fourteen.
        Fixture fixture = days(consecutive(START, 5), 2139, "140.0");
        stubSeries(START, END, fixture);
        previousCheckIn("70.00", "80.00");
        stubSave();

        CheckInResult result = service.createCheckIn(USER_ID, new CreateCheckInRequest(
                END, new BigDecimal("70.00"), new BigDecimal("80.00"), null));

        assertThat(result.checkIn().greenDaysPct()).isEqualTo(36);   // 5/14
        assertThat(result.adjustmentSuggested()).isFalse();
        assertThat(result.suggestedDeficitPct()).isNull();
        assertThat(result.message()).isNull();
    }

    @Test
    void doesNotSuggestWhenWeightDropped() {
        stubTwelveGreenDays();
        previousCheckIn("70.00", "80.00");
        stubSave();

        CheckInResult result = service.createCheckIn(USER_ID, new CreateCheckInRequest(
                END, new BigDecimal("69.20"), new BigDecimal("80.00"), null));

        assertThat(result.adjustmentSuggested()).isFalse();
    }

    @Test
    void doesNotSuggestWithoutWaistMeasurement() {
        stubTwelveGreenDays();
        previousCheckIn("70.00", "80.00");
        stubSave();

        CheckInResult result = service.createCheckIn(USER_ID, new CreateCheckInRequest(
                END, new BigDecimal("70.00"), null, null));

        assertThat(result.adjustmentSuggested()).isFalse();
    }

    @Test
    void doesNotSuggestWithoutPreviousCheckIn() {
        stubTwelveGreenDays();
        when(checkInRepository.findFirstByUserIdAndCheckinDateLessThanOrderByCheckinDateDesc(USER_ID, END))
                .thenReturn(Optional.empty());
        stubSave();

        CheckInResult result = service.createCheckIn(USER_ID, new CreateCheckInRequest(
                END, new BigDecimal("70.00"), new BigDecimal("80.00"), null));

        assertThat(result.adjustmentSuggested()).isFalse();
    }

    // ---------- reminder rule ----------

    @Test
    void checkInIsDueAfterFourteenDays() {
        BiweeklyCheckIn last = new BiweeklyCheckIn(USER_ID, END.minusDays(14), new BigDecimal("70.00"),
                new BigDecimal("80.00"), 80, 90, null);
        when(checkInRepository.findFirstByUserIdOrderByCheckinDateDesc(USER_ID))
                .thenReturn(Optional.of(last));

        assertThat(service.isCheckInDue(USER_ID, END)).isTrue();
    }

    @Test
    void checkInIsNotDueBeforeFourteenDays() {
        BiweeklyCheckIn last = new BiweeklyCheckIn(USER_ID, END.minusDays(13), new BigDecimal("70.00"),
                new BigDecimal("80.00"), 80, 90, null);
        when(checkInRepository.findFirstByUserIdOrderByCheckinDateDesc(USER_ID))
                .thenReturn(Optional.of(last));

        assertThat(service.isCheckInDue(USER_ID, END)).isFalse();
    }

    @Test
    void fallsBackToProfileCreationWhenNoCheckInExists() {
        when(checkInRepository.findFirstByUserIdOrderByCheckinDateDesc(USER_ID)).thenReturn(Optional.empty());
        when(profileService.profileCreatedAt(USER_ID))
                .thenReturn(Optional.of(END.minusDays(20).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));

        assertThat(service.isCheckInDue(USER_ID, END)).isTrue();
    }
}
