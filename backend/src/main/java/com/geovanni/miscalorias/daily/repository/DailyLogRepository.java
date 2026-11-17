package com.geovanni.miscalorias.daily.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.geovanni.miscalorias.daily.domain.DailyLog;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {

    Optional<DailyLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);

    List<DailyLog> findByUserIdAndLogDateBetweenOrderByLogDateAsc(Long userId, LocalDate start, LocalDate end);
}
