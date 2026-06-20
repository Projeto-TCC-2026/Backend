package com.tcc.domain.repository;

import com.tcc.domain.model.ProcedurePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcedurePhotoRepository extends JpaRepository<ProcedurePhoto, Long> {
    
    List<ProcedurePhoto> findByProcedureExecutionId(Long procedureExecutionId);
}
