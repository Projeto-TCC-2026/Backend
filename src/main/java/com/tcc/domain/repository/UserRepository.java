package com.tcc.domain.repository;

import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndActiveTrue(UUID id);
    
    Optional<User> findByEmailAndActiveTrue(String email);

    boolean existsByEmail(String email);

    Page<User> findAllByActiveTrue(Pageable pageable);
    
    Page<User> findAllByRoleAndActiveTrue(Role role, Pageable pageable);
    
    long countByActiveTrue();
    
    long countByRoleAndActiveTrue(Role role);
    
    long countByActiveFalse();
    
    long countByRoleAndActiveFalse(Role role);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.hospital WHERE u.email = :email")
    Optional<User> findByEmailWithHospital(@Param("email") String email);

    boolean existsByHospitalId(UUID hospitalId);
}
