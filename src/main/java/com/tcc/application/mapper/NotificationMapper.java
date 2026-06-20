package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.NotificationRequest;
import com.tcc.application.dto.response.NotificationResponse;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Notification;

@Component
public class NotificationMapper {

    private final AlertMapper alertMapper;
    private final DoctorMapper doctorMapper;

    public NotificationMapper(AlertMapper alertMapper, DoctorMapper doctorMapper) {
        this.alertMapper = alertMapper;
        this.doctorMapper = doctorMapper;
    }

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) return null;
        return new NotificationResponse(
                notification.getId(),
                alertMapper.toSummary(notification.getAlert()),
                doctorMapper.toSummary(notification.getDoctor()),
                notification.getMessage(),
                notification.getSentAt(),
                notification.getReadAt(),
                notification.getStatus()
        );
    }

    public Notification toEntity(NotificationRequest request, Alert alert, Doctor doctor) {
        if (request == null) return null;
        Notification notification = new Notification();
        notification.setAlert(alert);
        notification.setDoctor(doctor);
        notification.setMessage(request.message());
        notification.setStatus(request.status());
        return notification;
    }

    public void updateEntity(Notification notification, NotificationRequest request) {
        notification.setMessage(request.message());
        notification.setStatus(request.status());
    }
}
