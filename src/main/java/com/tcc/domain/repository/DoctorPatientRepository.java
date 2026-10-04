package com.tcc.domain.repository;

import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorPatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorPatientRepository extends JpaRepository<DoctorPatient, UUID> {

    /**
     * Médicos com vínculo ativo com o paciente, já com o usuário carregado para
     * leitura do e-mail. "Vínculo ativo" aqui exige médico ativo, hospital ativo e
     * usuário ativo: a regra do projeto é que médico de hospital inativo não opera,
     * então ele também não deve receber aviso de alerta.
     *
     * <p>doctor_patients não tem coluna própria de atividade — o vínculo existe ou
     * não existe. A atividade vem das três entidades acima.
     */
    @Query("""
            SELECT DISTINCT d FROM DoctorPatient dp
            JOIN dp.doctor d
            JOIN FETCH d.user u
            JOIN d.hospital h
            WHERE dp.patient.id = :patientId
              AND d.active = true
              AND h.active = true
              AND u.active = true
            """)
    List<Doctor> findActiveDoctorsByPatientId(@Param("patientId") UUID patientId);

    List<DoctorPatient> findByDoctorId(UUID doctorId);
    
    List<DoctorPatient> findByPatientId(UUID patientId);

    Optional<DoctorPatient> findFirstByPatientIdOrderByCreatedAtDesc(UUID patientId);
    
    Optional<DoctorPatient> findByDoctorIdAndPatientId(UUID doctorId, UUID patientId);
    
    boolean existsByDoctorIdAndPatientId(UUID doctorId, UUID patientId);

    boolean existsByDoctor_Hospital_IdAndPatient_Id(UUID hospitalId, UUID patientId);
}
