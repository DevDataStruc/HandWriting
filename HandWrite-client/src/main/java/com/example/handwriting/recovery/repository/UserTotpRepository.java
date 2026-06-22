package com.example.handwriting.recovery.repository;

import com.example.handwriting.recovery.entity.UserTotp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTotpRepository extends JpaRepository<UserTotp, Long> {

    Optional<UserTotp> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}
