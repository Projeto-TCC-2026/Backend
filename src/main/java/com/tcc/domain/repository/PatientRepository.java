package com.tcc.domain.repository;

import com.tcc.domain.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    Optional<Patient> findByUserId(Long userId);
    
    Optional<Patient> findByCpf(String cpf);
    
    boolean existsByCpf(String cpf);
}
