package com.tcc.domain.repository;

import com.tcc.domain.model.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcedureRepository extends JpaRepository<Procedure, Long> {
    
    List<Procedure> findByDoctorId(Long doctorId);
    
    List<Procedure> findByDoctorIdAndActive(Long doctorId, Boolean active);
    
    List<Procedure> findByActive(Boolean active);
}
