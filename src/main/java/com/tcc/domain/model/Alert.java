package com.tcc.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tcc.domain.event.AlertConfirmationReason;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_reading_id")
    private HealthReading healthReading;

    @Column(nullable = false, length = 50)
    private String severity;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Momento em que o alerta passou de UNCONFIRMED para PENDING, ou seja, quando
     * uma segunda leitura seguida confirmou o problema. Nulo enquanto o alerta não
     * for confirmado. A janela de 4h que evita repetir o aviso ao médico é contada
     * a partir daqui.
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * Até quando a resposta da paciente é aceita em alerta AWAITING_PATIENT.
     * Preenchido na criação com "agora + 10 minutos". Nulo em alerta do fluxo
     * comum, que não pergunta nada à paciente.
     */
    @Column(name = "patient_response_deadline")
    private LocalDateTime patientResponseDeadline;

    /** Resposta da paciente, nula enquanto ela não responde. */
    @Enumerated(EnumType.STRING)
    @Column(name = "patient_response", length = 20)
    private PatientAlertAnswer patientResponse;

    /** Momento em que a resposta da paciente foi gravada. */
    @Column(name = "patient_responded_at")
    private LocalDateTime patientRespondedAt;

    /**
     * Por que este alerta foi confirmado, ou seja, qual evidência levou o status a
     * PENDING. Gravado no mesmo momento da troca de status, pelos três caminhos que
     * confirmam: duas leituras seguidas, a paciente respondendo que não está bem, e
     * o prazo vencendo sem resposta.
     *
     * <p>Nulo em alerta que não foi confirmado — UNCONFIRMED, AWAITING_PATIENT e
     * NOT_CONFIRMED — e também em alerta confirmado antes da V37, que não tem o dado
     * registrado em lugar nenhum. Nulo significa "não se sabe", e não "nenhum
     * motivo".
     *
     * <p>Mesmo enum que viaja em {@code AlertConfirmedEvent} e define o texto do
     * e-mail ao médico, agora persistido como texto: o motivo exibido ao médico e o
     * motivo gravado na linha são o mesmo valor, por construção.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_reason", length = 30)
    private AlertConfirmationReason confirmationReason;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public Alert() {
    }

    public Alert(Patient patient, String severity, String title, String status) {
        this.patient = patient;
        this.severity = severity;
        this.title = title;
        this.status = status;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public HealthReading getHealthReading() {
        return healthReading;
    }

    public void setHealthReading(HealthReading healthReading) {
        this.healthReading = healthReading;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getPatientResponseDeadline() {
        return patientResponseDeadline;
    }

    public void setPatientResponseDeadline(LocalDateTime patientResponseDeadline) {
        this.patientResponseDeadline = patientResponseDeadline;
    }

    public PatientAlertAnswer getPatientResponse() {
        return patientResponse;
    }

    public void setPatientResponse(PatientAlertAnswer patientResponse) {
        this.patientResponse = patientResponse;
    }

    public LocalDateTime getPatientRespondedAt() {
        return patientRespondedAt;
    }

    public void setPatientRespondedAt(LocalDateTime patientRespondedAt) {
        this.patientRespondedAt = patientRespondedAt;
    }

    public AlertConfirmationReason getConfirmationReason() {
        return confirmationReason;
    }

    public void setConfirmationReason(AlertConfirmationReason confirmationReason) {
        this.confirmationReason = confirmationReason;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }
}
