package com.example.handwriting.recovery.repository;

import com.example.handwriting.recovery.entity.UserSecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSecurityQuestionRepository extends JpaRepository<UserSecurityQuestion, Long> {

    List<UserSecurityQuestion> findByUserIdOrderByQuestionIndexAsc(Long userId);

    Optional<UserSecurityQuestion> findByUserIdAndQuestionIndex(Long userId, Integer questionIndex);

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);
}
