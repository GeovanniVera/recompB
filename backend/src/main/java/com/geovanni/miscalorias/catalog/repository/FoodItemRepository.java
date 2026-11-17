package com.geovanni.miscalorias.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.geovanni.miscalorias.catalog.domain.FoodItem;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    /**
     * Global items plus the ones owned by the given user. The text filter is applied
     * in the service: the catalog is small and this keeps the query null-safe.
     */
    @Query("""
            select f from FoodItem f
            where f.userId is null or f.userId = :userId
            order by f.name asc, f.stateTag asc
            """)
    List<FoodItem> findVisibleTo(@Param("userId") Long userId);
}
