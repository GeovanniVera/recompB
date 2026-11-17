package com.geovanni.miscalorias.analytics.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.geovanni.miscalorias.analytics.domain.BiweeklyCheckIn;

public interface BiweeklyCheckInRepository extends JpaRepository<BiweeklyCheckIn, Long> {

    List<BiweeklyCheckIn> findByUserIdOrderByCheckinDateAsc(Long userId);

    Optional<BiweeklyCheckIn> findFirstByUserIdOrderByCheckinDateDesc(Long userId);

    /** Check-in immediately before a date, used for the adjustment rule baseline. */
    Optional<BiweeklyCheckIn> findFirstByUserIdAndCheckinDateLessThanOrderByCheckinDateDesc(
            Long userId, LocalDate date);

    /** Latest check-in on or before a date, used as the closest marker of a week. */
    Optional<BiweeklyCheckIn> findFirstByUserIdAndCheckinDateLessThanEqualOrderByCheckinDateDesc(
            Long userId, LocalDate date);
}
