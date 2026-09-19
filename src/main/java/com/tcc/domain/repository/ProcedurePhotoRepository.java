package com.tcc.domain.repository;

import com.tcc.domain.model.ProcedurePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProcedurePhotoRepository extends JpaRepository<ProcedurePhoto, UUID> {
    
    List<ProcedurePhoto> findByProcedureExecutionId(UUID procedureExecutionId);
}
