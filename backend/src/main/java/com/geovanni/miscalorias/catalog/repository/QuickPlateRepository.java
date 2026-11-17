package com.geovanni.miscalorias.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.geovanni.miscalorias.catalog.domain.QuickPlate;

public interface QuickPlateRepository extends JpaRepository<QuickPlate, Long> {

    @Query("""
            select p from QuickPlate p
            where p.userId is null or p.userId = :userId
            order by p.title asc
            """)
    List<QuickPlate> findVisibleTo(@Param("userId") Long userId);
}
