package com.example.handwriting.user.repository;

import com.example.handwriting.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createTime BETWEEN :start AND :end")
    long countInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 管理员视图：按关键字 + 状态过滤分页
     * 关键字匹配 username / nickname / email（任一 LIKE）
     */
    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR :keyword = '' " +
            "  OR u.username LIKE CONCAT('%', :keyword, '%') " +
            "  OR u.nickname LIKE CONCAT('%', :keyword, '%') " +
            "  OR u.email LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:status IS NULL OR u.status = :status) " +
            "ORDER BY u.createTime DESC")
    Page<User> searchAdmin(@Param("keyword") String keyword,
                           @Param("status") Integer status,
                           Pageable pageable);

    /**
     * 批量查询用户（用于补全角色/样本数等扩展信息）
     */
    @Query("SELECT u.id, u.username, u.nickname, u.avatar FROM User u WHERE u.id IN :ids")
    List<Object[]> findBriefByIds(@Param("ids") List<Long> ids);

    /**
     * 按天聚合用户注册量：[date, count]
     * <p>原生 SQL 走 MySQL 的 DATE() 函数</p>
     */
    @Query(value = "SELECT DATE(create_time) AS d, COUNT(*) AS c " +
            "FROM t_user " +
            "WHERE create_time >= :start AND create_time < :end " +
            "GROUP BY DATE(create_time)",
            nativeQuery = true)
    List<Object[]> countDailyCreated(@Param("start") LocalDate start,
                                     @Param("end") LocalDate end);
}
