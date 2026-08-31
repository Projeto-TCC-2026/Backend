package com.tcc.domain.repository;

import com.tcc.domain.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    
    Optional<Patient> findByUserId(UUID userId);
    
    Optional<Patient> findByCpf(String cpf);
    
    boolean existsByCpf(String cpf);
    
    List<Patient> findByActiveTrue();
    
    @Query("SELECT p FROM Patient p WHERE p.active = true")
    Page<Patient> findPagedByActiveTrue(Pageable pageable);

    @Query(
            value = "SELECT DISTINCT p FROM Patient p " +
                    "JOIN p.doctorPatients dp " +
                    "WHERE p.active = true AND dp.doctor.id = :doctorId",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Patient p " +
                    "JOIN p.doctorPatients dp " +
                    "WHERE p.active = true AND dp.doctor.id = :doctorId")
    Page<Patient> findPagedActiveByDoctorId(@Param("doctorId") UUID doctorId, Pageable pageable);

    @Query(
            value = "SELECT DISTINCT p FROM Patient p " +
                    "JOIN p.doctorPatients dp " +
                    "WHERE p.active = true AND dp.doctor.hospital.id = :hospitalId",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Patient p " +
                    "JOIN p.doctorPatients dp " +
                    "WHERE p.active = true AND dp.doctor.hospital.id = :hospitalId")
    Page<Patient> findPagedActiveByHospitalId(@Param("hospitalId") UUID hospitalId, Pageable pageable);

    @Query(
            value = "SELECT DISTINCT p FROM Patient p " +
                    "LEFT JOIN p.doctorPatients dp " +
                    "WHERE p.active = true " +
                    "AND (:doctorId IS NULL OR dp.doctor.id = :doctorId) " +
                    "AND (:hospitalId IS NULL OR dp.doctor.hospital.id = :hospitalId) " +
                    "AND (:name IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
                    "AND (:cpf IS NULL OR p.cpf LIKE CONCAT('%', :cpf, '%')) " +
                    "AND (:email IS NULL OR LOWER(p.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
                    "AND (:phone IS NULL OR p.phone LIKE CONCAT('%', :phone, '%')) " +
                    "AND (:gender IS NULL OR p.gender = :gender) " +
                    "AND (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
                    "AND (:state IS NULL OR p.state = :state)",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Patient p " +
                    "LEFT JOIN p.doctorPatients dp " +
                    "WHERE p.active = true " +
                    "AND (:doctorId IS NULL OR dp.doctor.id = :doctorId) " +
                    "AND (:hospitalId IS NULL OR dp.doctor.hospital.id = :hospitalId) " +
                    "AND (:name IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
                    "AND (:cpf IS NULL OR p.cpf LIKE CONCAT('%', :cpf, '%')) " +
                    "AND (:email IS NULL OR LOWER(p.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
                    "AND (:phone IS NULL OR p.phone LIKE CONCAT('%', :phone, '%')) " +
                    "AND (:gender IS NULL OR p.gender = :gender) " +
                    "AND (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
                    "AND (:state IS NULL OR p.state = :state)")
    Page<Patient> findVisible(@Param("doctorId") UUID doctorId,
                              @Param("hospitalId") UUID hospitalId,
                              @Param("name") String name,
                              @Param("cpf") String cpf,
                              @Param("email") String email,
                              @Param("phone") String phone,
                              @Param("gender") String gender,
                              @Param("city") String city,
                              @Param("state") String state,
                              Pageable pageable);
    
    Optional<Patient> findByIdAndActiveTrue(UUID id);
    
    boolean existsByCpfAndActiveTrue(String cpf);
    
    Page<Patient> findByFullNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);
    
    Page<Patient> findByCpfContainingAndActiveTrue(String cpf, Pageable pageable);
    
    Page<Patient> findByEmailContainingIgnoreCaseAndActiveTrue(String email, Pageable pageable);
    
    Optional<Patient> findByEmailAndActiveTrue(String email);
    
    boolean existsByEmailAndActiveTrue(String email);
    
    Page<Patient> findByPhoneContainingAndActiveTrue(String phone, Pageable pageable);
    
    @Query("SELECT p FROM Patient p WHERE p.active = true " +
           "AND (:name IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:gender IS NULL OR p.gender = :gender) " +
           "AND (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND (:state IS NULL OR p.state = :state)")
    Page<Patient> findByFilters(@Param("name") String name,
                                 @Param("gender") String gender,
                                 @Param("city") String city,
                                 @Param("state") String state,
                                 Pageable pageable);
                                 
    long countByActiveTrue();

    long countByActiveFalse();

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.active = true")
    Long countActivePatientsTotal();
    
    @Query("SELECT COUNT(DISTINCT dp.patient) FROM DoctorPatient dp " +
           "WHERE dp.doctor.hospital.id = :hospitalId " +
           "AND dp.patient.active = true")
    Long countActivePatientsByHospitalId(@Param("hospitalId") UUID hospitalId);

    @Query("SELECT COUNT(DISTINCT dp.patient) FROM DoctorPatient dp " +
           "WHERE dp.doctor.hospital.id = :hospitalId " +
           "AND dp.patient.active = false")
    Long countInactivePatientsByHospitalId(@Param("hospitalId") UUID hospitalId);

    @Query("SELECT COUNT(DISTINCT dp.patient) FROM DoctorPatient dp " +
           "WHERE dp.doctor.id = :doctorId")
    Long countPatientsByDoctorId(@Param("doctorId") UUID doctorId);

    @Query("SELECT COUNT(DISTINCT dp.patient) FROM DoctorPatient dp " +
           "WHERE dp.doctor.id = :doctorId AND dp.patient.active = true")
    Long countActivePatientsByDoctorId(@Param("doctorId") UUID doctorId);

    @Query("SELECT COUNT(DISTINCT dp.patient) FROM DoctorPatient dp " +
           "WHERE dp.doctor.id = :doctorId AND dp.patient.createdAt >= :since")
    Long countNewPatientsByDoctorId(@Param("doctorId") UUID doctorId,
                                    @Param("since") java.time.LocalDateTime since);
    
    @Query("SELECT DISTINCT p FROM Patient p " +
           "JOIN p.doctorPatients dp " +
           "WHERE dp.doctor.hospital.id = :hospitalId " +
           "AND p.active = true " +
           "ORDER BY p.createdAt DESC")
    List<Patient> findLatestPatientsByHospitalId(@Param("hospitalId") UUID hospitalId, Pageable pageable);
    
    @Query("SELECT dp.doctor.hospital.id as hospitalId, dp.doctor.hospital.name as hospitalName, " +
           "COUNT(DISTINCT dp.patient) as totalPatients " +
           "FROM DoctorPatient dp " +
           "WHERE dp.patient.active = true " +
           "GROUP BY dp.doctor.hospital.id, dp.doctor.hospital.name " +
           "ORDER BY totalPatients DESC")
    List<Object[]> countPatientsByHospital();
}
