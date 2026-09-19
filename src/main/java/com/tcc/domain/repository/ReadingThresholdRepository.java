package com.tcc.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcc.domain.model.ReadingThreshold;

public interface ReadingThresholdRepository extends JpaRepository<ReadingThreshold, UUID> {

    /**
     * A constraint UNIQUE em reading_type garante no máximo uma faixa por tipo.
     */
    Optional<ReadingThreshold> findByReadingType(String readingType);
}
