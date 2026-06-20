package com.tcc.domain.repository;

import com.tcc.domain.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByDoctorId(Long doctorId);
    
    List<Notification> findByAlertId(Long alertId);
    
    List<Notification> findByStatus(String status);
    
    List<Notification> findByDoctorIdAndStatus(Long doctorId, String status);
    
    List<Notification> findByDoctorIdOrderBySentAtDesc(Long doctorId);
    
    List<Notification> findByDoctorIdAndReadAtIsNull(Long doctorId);
}
