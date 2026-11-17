package com.geovanni.miscalorias.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.geovanni.miscalorias.auth.domain.OtpCode;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByUserIdAndConsumedFalseOrderByIdDesc(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update OtpCode o set o.consumed = true where o.user.id = :userId and o.consumed = false")
    int invalidateActiveCodes(@Param("userId") Long userId);
}
