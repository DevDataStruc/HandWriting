package com.example.handwriting.dict.repository;

import com.example.handwriting.dict.entity.CharDict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CharDictRepository extends JpaRepository<CharDict, Long> {

    Page<CharDict> findByCategoryAndEnabled(String category, Integer enabled, Pageable pageable);

    Page<CharDict> findByEnabled(Integer enabled, Pageable pageable);

    boolean existsByCharValue(String charValue);

    java.util.Optional<CharDict> findByCharValue(String charValue);

    long countByEnabled(Integer enabled);
}
