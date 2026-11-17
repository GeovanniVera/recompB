package com.geovanni.miscalorias.daily.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.geovanni.miscalorias.daily.domain.MealEntry;

public interface MealEntryRepository extends JpaRepository<MealEntry, Long> {

    List<MealEntry> findByDailyLogIdOrderByIdAsc(Long dailyLogId);

    List<MealEntry> findByDailyLogIdInOrderByIdAsc(Collection<Long> dailyLogIds);

    Optional<MealEntry> findByIdAndDailyLogId(Long id, Long dailyLogId);
}
