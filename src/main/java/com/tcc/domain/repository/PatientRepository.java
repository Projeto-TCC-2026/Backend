package com.tcc.domain.repository;

import com.tcc.domain.model.Patient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    // Dashboard queries - Consultas otimizadas com COUNT
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.active = true")
    Long countActivePatientsTotal();
    
    @Query("SELECT COUNT(DISTINCT dp.patient) FROM DoctorPatient dp " +
           "WHERE dp.doctor.hospital.id = :hospitalId " +
           "AND dp.patient.active = true")
    Long countActivePatientsByHospitalId(@Param("hospitalId") Long hospitalId);
    
    // Buscar últimos pacientes de um hospital
    @Query("SELECT DISTINCT p FROM Patient p " +
           "JOIN p.doctorPatients dp " +
           "WHERE dp.doctor.hospital.id = :hospitalId " +
           "AND p.active = true " +
           "ORDER BY p.createdAt DESC")
    List<Patient> findLatestPatientsByHospitalId(@Param("hospitalId") Long hospitalId, Pageable pageable);
    
    // Report queries
    @Query("SELECT dp.doctor.hospital.id as hospitalId, dp.doctor.hospital.name as hospitalName, " +
           "COUNT(DISTINCT dp.patient) as totalPatients " +
           "FROM DoctorPatient dp " +
           "WHERE dp.patient.active = true " +
           "GROUP BY dp.doctor.hospital.id, dp.doctor.hospital.name " +
           "ORDER BY totalPatients DESC")
    List<Object[]> countPatientsByHospital();
}
