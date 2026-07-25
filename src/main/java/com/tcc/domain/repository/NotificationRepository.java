package com.tcc.domain.repository;

import com.tcc.domain.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    List<Notification> findByDoctorId(UUID doctorId);
    
    List<Notification> findByAlertId(UUID alertId);
    
    List<Notification> findByStatus(String status);
    
    List<Notification> findByDoctorIdAndStatus(UUID doctorId, String status);
    
    List<Notification> findByDoctorIdOrderBySentAtDesc(UUID doctorId);
    
    List<Notification> findByDoctorIdAndReadAtIsNull(UUID doctorId);
}
