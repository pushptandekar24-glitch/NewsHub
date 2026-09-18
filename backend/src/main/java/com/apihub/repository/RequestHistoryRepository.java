package com.apihub.repository;

import com.apihub.entity.RequestHistory;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {

    Page<RequestHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    @Query("select count(r) from RequestHistory r where r.user.id = :userId and r.statusCode between 200 and 299")
    long countSuccessfulByUser(@Param("userId") Long userId);

    @Query("select coalesce(avg(r.responseTimeMs), 0) from RequestHistory r where r.user.id = :userId")
    double averageResponseTimeByUser(@Param("userId") Long userId);

    /** [date, count] rows for the "requests over time" chart. */
    @Query("""
           select function('date', r.createdAt), count(r)
           from RequestHistory r
           where r.user.id = :userId and r.createdAt >= :since
           group by function('date', r.createdAt)
           order by function('date', r.createdAt)
           """)
    List<Object[]> countPerDayByUser(@Param("userId") Long userId, @Param("since") Instant since);

    long countByUserIdAndCreatedAtAfter(Long userId, Instant after);
}
