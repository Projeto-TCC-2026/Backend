package com.tcc.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcc.domain.model.FieldTypePreset;

public interface FieldTypePresetRepository extends JpaRepository<FieldTypePreset, UUID> {
  List<FieldTypePreset> findByActiveTrueOrderByNameAsc();
}
