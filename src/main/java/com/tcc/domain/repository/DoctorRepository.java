package com.tcc.domain.repository;

import com.tcc.domain.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    
    Optional<Doctor> findByUserId(Long userId);
    
    Optional<Doctor> findByCpf(String cpf);
    
    Optional<Doctor> findByCrm(String crm);
    
    List<Doctor> findByHospitalId(Long hospitalId);
    
    boolean existsByCpf(String cpf);
    
    boolean existsByCrm(String crm);
}
