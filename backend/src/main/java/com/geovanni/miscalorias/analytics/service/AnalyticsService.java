package com.geovanni.miscalorias.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geovanni.miscalorias.analytics.domain.BiweeklyCheckIn;
import com.geovanni.miscalorias.analytics.repository.BiweeklyCheckInRepository;
import com.geovanni.miscalorias.analytics.web.dto.CheckInResponse;
import com.geovanni.miscalorias.analytics.web.dto.CheckInResult;
import com.geovanni.miscalorias.analytics.web.dto.CreateCheckInRequest;
import com.geovanni.miscalorias.analytics.web.dto.DailyMetricPoint;
import com.geovanni.miscalorias.analytics.web.dto.FourteenDayResponse;
import com.geovanni.miscalorias.analytics.web.dto.PeriodStats;
import com.geovanni.miscalorias.analytics.web.dto.WeekBlock;
import com.geovanni.miscalorias.analytics.web.dto.WeekComparisonResponse;
import com.geovanni.miscalorias.analytics.web.dto.WeekDeltas;
import com.geovanni.miscalorias.config.AppTimeProperties;
import com.geovanni.miscalorias.daily.domain.DailyLog;
import com.geovanni.miscalorias.daily.domain.MealEntry;
import com.geovanni.miscalorias.daily.repository.DailyLogRepository;
import com.geovanni.miscalorias.daily.repository.MealEntryRepository;
import com.geovanni.miscalorias.daily.service.DayMetricsCalculator;
import com.geovanni.miscalorias.profile.service.ProfileService;
import com.geovanni.miscalorias.profile.web.dto.TargetResponse;

@Service
public class AnalyticsService {

    private static final int WINDOW_DAYS = 14;
    private static final int WEEK_DAYS = 7;
    private static final int GREEN_ADHERENCE_THRESHOLD = 80;
    private static final BigDecimal SUGGESTED_DEFICIT = new BigDecimal("0.14");
    private static final BigDecimal ZERO_PROTEIN = new BigDecimal("0.0");

    private final DailyLogRepository logRepository;
    private final MealEntryRepository entryRepository;
    private final BiweeklyCheckInRepository checkInRepository;
    private final ProfileService profileService;
    private final AppTimeProperties timeProperties;

    public AnalyticsService(DailyLogRepository logRepository, MealEntryRepository entryRepository,
            BiweeklyCheckInRepository checkInRepository, ProfileService profileService,
            AppTimeProperties timeProperties) {
        this.logRepository = logRepository;
        this.entryRepository = entryRepository;
        this.checkInRepository = checkInRepository;
        this.profileService = profileService;
        this.timeProperties = timeProperties;
    }

    /** One day of the series; {@code logged} is false when nothing was recorded. */
    public record DaySnapshot(LocalDate date, long kcal, BigDecimal protein, boolean inGreenMargin,
            int cleanEatingPct, boolean logged) {
    }

    // ---------- analytics ----------

    @Transactional(readOnly = true)
    public FourteenDayResponse getFourteenDays(Long userId, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now(timeProperties.zoneId());
        LocalDate start = end.minusDays(WINDOW_DAYS - 1);

        TargetResponse targets = profileService.getTargets(userId);
        List<DaySnapshot> series = series(userId, start, end, targets);

        PeriodStats summary = stats(series, start, end);
        List<DailyMetricPoint> points = points(series);
        PeriodStats previousWeek = stats(series.subList(0, WEEK_DAYS), start, start.plusDays(WEEK_DAYS - 1));
        PeriodStats currentWeek = stats(series.subList(WEEK_DAYS, WINDOW_DAYS), start.plusDays(WEEK_DAYS), end);

        return new FourteenDayResponse(start, end, summary, points, previousWeek, currentWeek,
                deltas(previousWeek, currentWeek));
    }

    @Transactional(readOnly = true)
    public WeekComparisonResponse compareWeeks(Long userId, LocalDate baseStart, LocalDate currentStart) {
        TargetResponse targets = profileService.getTargets(userId);
        WeekBlock base = weekBlock(userId, baseStart, targets);
        WeekBlock current = weekBlock(userId, currentStart, targets);
        return new WeekComparisonResponse(base, current, deltas(base.stats(), current.stats()));
    }

    @Transactional(readOnly = true)
    public Optional<DaySnapshot> findDay(Long userId, LocalDate date) {
        return logRepository.findByUserIdAndLogDate(userId, date)
                .map(log -> snapshot(date, entryRepository.findByDailyLogIdOrderByIdAsc(log.getId()),
                        profileService.getTargets(userId)));
    }

    // ---------- check-ins ----------

    @Transactional(readOnly = true)
    public List<CheckInResponse> listCheckIns(Long userId) {
        return checkInRepository.findByUserIdOrderByCheckinDateAsc(userId).stream()
                .map(CheckInResponse::of)
                .toList();
    }

    @Transactional
    public CheckInResult createCheckIn(Long userId, CreateCheckInRequest request) {
        LocalDate date = request.checkinDate() != null
                ? request.checkinDate()
                : LocalDate.now(timeProperties.zoneId());

        LocalDate windowStart = date.minusDays(WINDOW_DAYS - 1);
        TargetResponse targets = profileService.getTargets(userId);
        PeriodStats window = stats(series(userId, windowStart, date, targets), windowStart, date);

        Optional<BiweeklyCheckIn> previous = checkInRepository
                .findFirstByUserIdAndCheckinDateLessThanOrderByCheckinDateDesc(userId, date);

        BiweeklyCheckIn saved = checkInRepository.saveAndFlush(new BiweeklyCheckIn(
                userId,
                date,
                request.weightKg(),
                request.waistCm(),
                window.greenDaysPct(),
                window.avgCleanEatingPct(),
                request.note()));

        profileService.updateCurrentWeight(userId, request.weightKg());

        boolean suggested = shouldRaiseDeficit(previous, request, window.greenDaysPct());
        String message = suggested
                ? ("El peso y la cintura no se movieron y traes %d%% de días verdes. Vale la pena subir el déficit a %d%%.")
                        .formatted(window.greenDaysPct(), SUGGESTED_DEFICIT.movePointRight(2).intValue())
                : null;

        return new CheckInResult(CheckInResponse.of(saved), suggested,
                suggested ? SUGGESTED_DEFICIT : null, message);
    }

    /**
     * Guided adjustment rule: the body stalled (weight and waist did not drop) while
     * adherence was at least 80%, so a slightly larger deficit is justified.
     */
    private boolean shouldRaiseDeficit(Optional<BiweeklyCheckIn> previous, CreateCheckInRequest request,
            int greenDaysPct) {
        if (previous.isEmpty() || request.waistCm() == null) {
            return false;
        }
        BiweeklyCheckIn baseline = previous.get();
        if (baseline.getWaistCm() == null) {
            return false;
        }
        boolean weightStalled = request.weightKg().compareTo(baseline.getWeightKg()) >= 0;
        boolean waistStalled = request.waistCm().compareTo(baseline.getWaistCm()) >= 0;
        return weightStalled && waistStalled && greenDaysPct >= GREEN_ADHERENCE_THRESHOLD;
    }

    /** True when a new fortnightly check-in is due (>= 14 days since the last reference). */
    @Transactional(readOnly = true)
    public boolean isCheckInDue(Long userId, LocalDate today) {
        Optional<BiweeklyCheckIn> last = checkInRepository.findFirstByUserIdOrderByCheckinDateDesc(userId);
        if (last.isPresent()) {
            return ChronoUnit.DAYS.between(last.get().getCheckinDate(), today) >= WINDOW_DAYS;
        }
        return profileService.profileCreatedAt(userId)
                .map(createdAt -> ChronoUnit.DAYS.between(
                        createdAt.atZone(ZoneOffset.UTC).toLocalDate(), today) >= WINDOW_DAYS)
                .orElse(false);
    }

    // ---------- internals ----------

    private WeekBlock weekBlock(Long userId, LocalDate start, TargetResponse targets) {
        LocalDate end = start.plusDays(WEEK_DAYS - 1);
        List<DaySnapshot> series = series(userId, start, end, targets);
        CheckInResponse checkIn = checkInRepository
                .findFirstByUserIdAndCheckinDateLessThanEqualOrderByCheckinDateDesc(userId, end)
                .map(CheckInResponse::of)
                .orElse(null);
        return new WeekBlock(stats(series, start, end), points(series), checkIn);
    }

    private List<DaySnapshot> series(Long userId, LocalDate start, LocalDate end, TargetResponse targets) {
        List<DailyLog> logs = logRepository.findByUserIdAndLogDateBetweenOrderByLogDateAsc(userId, start, end);

        Map<LocalDate, List<MealEntry>> entriesByDate = new HashMap<>();
        if (!logs.isEmpty()) {
            Map<Long, LocalDate> dateByLogId = new HashMap<>();
            for (DailyLog log : logs) {
                dateByLogId.put(log.getId(), log.getLogDate());
            }
            for (MealEntry entry : entryRepository.findByDailyLogIdInOrderByIdAsc(dateByLogId.keySet())) {
                LocalDate date = dateByLogId.get(entry.getDailyLog().getId());
                if (date != null) {
                    entriesByDate.computeIfAbsent(date, key -> new ArrayList<>()).add(entry);
                }
            }
        }

        List<DaySnapshot> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            result.add(snapshot(date, entriesByDate.getOrDefault(date, List.of()), targets));
        }
        return result;
    }

    private DaySnapshot snapshot(LocalDate date, List<MealEntry> entries, TargetResponse targets) {
        long kcal = DayMetricsCalculator.consumedKcal(entries);
        BigDecimal protein = DayMetricsCalculator.consumedMacro(entries, MealEntry::getProtein);
        return new DaySnapshot(
                date,
                kcal,
                protein,
                DayMetricsCalculator.inGreenMargin(kcal, protein, targets),
                DayMetricsCalculator.cleanEatingPct(entries),
                !entries.isEmpty());
    }

    private PeriodStats stats(List<DaySnapshot> days, LocalDate start, LocalDate end) {
        List<DaySnapshot> logged = days.stream().filter(DaySnapshot::logged).toList();
        int loggedDays = logged.size();

        long avgKcal = 0;
        BigDecimal avgProtein = ZERO_PROTEIN;
        int avgCleanEatingPct = 0;

        if (loggedDays > 0) {
            long sumKcal = logged.stream().mapToLong(DaySnapshot::kcal).sum();
            BigDecimal sumProtein = logged.stream()
                    .map(DaySnapshot::protein)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int sumClean = logged.stream().mapToInt(DaySnapshot::cleanEatingPct).sum();

            avgKcal = Math.round((double) sumKcal / loggedDays);
            avgProtein = sumProtein.divide(BigDecimal.valueOf(loggedDays), 1, RoundingMode.HALF_UP);
            avgCleanEatingPct = (int) Math.round((double) sumClean / loggedDays);
        }

        int greenDaysCount = (int) days.stream().filter(DaySnapshot::inGreenMargin).count();
        int totalDays = (int) ChronoUnit.DAYS.between(start, end) + 1;
        int greenDaysPct = totalDays <= 0 ? 0 : (int) Math.round(greenDaysCount * 100.0 / totalDays);

        return new PeriodStats(start, end, totalDays, loggedDays, avgKcal, avgProtein,
                greenDaysCount, greenDaysPct, avgCleanEatingPct);
    }

    private List<DailyMetricPoint> points(List<DaySnapshot> days) {
        List<DailyMetricPoint> points = new ArrayList<>(days.size());
        for (int index = 0; index < days.size(); index++) {
            DaySnapshot day = days.get(index);
            points.add(new DailyMetricPoint(
                    day.date(),
                    "D" + (index + 1),
                    day.kcal(),
                    day.protein(),
                    day.inGreenMargin(),
                    day.cleanEatingPct()));
        }
        return points;
    }

    private WeekDeltas deltas(PeriodStats base, PeriodStats current) {
        return new WeekDeltas(
                current.avgKcal() - base.avgKcal(),
                current.avgProtein().subtract(base.avgProtein()),
                current.greenDaysPct() - base.greenDaysPct(),
                current.avgCleanEatingPct() - base.avgCleanEatingPct());
    }
}
