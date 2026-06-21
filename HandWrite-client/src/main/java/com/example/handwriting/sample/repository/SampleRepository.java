package com.example.handwriting.sample.repository;

import com.example.handwriting.sample.entity.Sample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SampleRepository extends JpaRepository<Sample, Long> {

    Page<Sample> findByUserIdAndStatus(Long userId, Integer status, Pageable pageable);

    Page<Sample> findByUserId(Long userId, Pageable pageable);

    Page<Sample> findByStatus(Integer status, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Sample s WHERE s.createTime BETWEEN :start AND :end")
    long countInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByStatus(Integer status);

    /**
     * 状态分布聚合：返回 [status, count]
     */
    @Query("SELECT s.status, COUNT(s) FROM Sample s GROUP BY s.status")
    List<Object[]> countGroupByStatus();

    /**
     * 贡献者排行聚合：返回 [userId, totalCount, approvedCount]
     * 仅统计 status=APPROVED 的样本作为 approvedCount；total 包含全部状态
     */
    @Query("SELECT s.userId, COUNT(s), " +
            "SUM(CASE WHEN s.status = 1 THEN 1 ELSE 0 END) " +
            "FROM Sample s GROUP BY s.userId ORDER BY COUNT(s) DESC")
    List<Object[]> aggregateByUser(Pageable pageable);

    /**
     * 字符采集进度：返回 [charId, approvedCount]
     */
    @Query("SELECT s.charId, COUNT(s) FROM Sample s " +
            "WHERE s.status = 1 GROUP BY s.charId")
    List<Object[]> countApprovedGroupByCharId();

    /**
     * 按天聚合样本创建量：[date, count]
     * <p>原生 SQL 走 MySQL 的 DATE() 函数</p>
     */
    @Query(value = "SELECT DATE(create_time) AS d, COUNT(*) AS c " +
            "FROM t_sample " +
            "WHERE create_time >= :start AND create_time < :end " +
            "GROUP BY DATE(create_time)",
            nativeQuery = true)
    List<Object[]> countDailyCreated(@Param("start") LocalDate start,
                                     @Param("end") LocalDate end);
}
