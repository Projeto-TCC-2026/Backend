package com.tcc.domain.repository;

import com.tcc.domain.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    
    Optional<Hospital> findByCnpj(String cnpj);
    
    boolean existsByCnpj(String cnpj);
}
