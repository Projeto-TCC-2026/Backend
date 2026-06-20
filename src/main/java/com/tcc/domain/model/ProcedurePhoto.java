package com.tcc.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "procedure_photos")
public class ProcedurePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_execution_id", nullable = false)
    private ProcedureExecution procedureExecution;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    // Constructors
    public ProcedurePhoto() {
    }

    public ProcedurePhoto(ProcedureExecution procedureExecution, String imageUrl, String fileName) {
        this.procedureExecution = procedureExecution;
        this.imageUrl = imageUrl;
        this.fileName = fileName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProcedureExecution getProcedureExecution() {
        return procedureExecution;
    }

    public void setProcedureExecution(ProcedureExecution procedureExecution) {
        this.procedureExecution = procedureExecution;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
