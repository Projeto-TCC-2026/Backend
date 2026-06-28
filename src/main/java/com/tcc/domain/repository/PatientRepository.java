package com.tcc.domain.repository;

import com.tcc.domain.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    Optional<Patient> findByUserId(Long userId);
    
    Optional<Patient> findByCpf(String cpf);
    
    boolean existsByCpf(String cpf);
    
    // Buscar apenas pacientes ativos
    List<Patient> findByActiveTrue();
    
    // Buscar paciente ativo por ID
    Optional<Patient> findByIdAndActiveTrue(Long id);
    
    // Verificar se CPF já existe em paciente ativo
    boolean existsByCpfAndActiveTrue(String cpf);
}
