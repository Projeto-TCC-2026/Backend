package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.ProcedurePhotoRequest;
import com.tcc.application.dto.response.ProcedurePhotoResponse;
import com.tcc.domain.model.ProcedureExecution;
import com.tcc.domain.model.ProcedurePhoto;

@Component
public class ProcedurePhotoMapper {

    public ProcedurePhotoResponse toResponse(ProcedurePhoto procedurePhoto) {
        if (procedurePhoto == null) return null;
        return new ProcedurePhotoResponse(
                procedurePhoto.getId(),
                procedurePhoto.getProcedureExecution() != null ? procedurePhoto.getProcedureExecution().getId() : null,
                procedurePhoto.getImageUrl(),
                procedurePhoto.getFileName(),
                procedurePhoto.getUploadedAt()
        );
    }

    public ProcedurePhoto toEntity(ProcedurePhotoRequest request, ProcedureExecution procedureExecution) {
        if (request == null) return null;
        ProcedurePhoto procedurePhoto = new ProcedurePhoto();
        procedurePhoto.setProcedureExecution(procedureExecution);
        procedurePhoto.setImageUrl(request.imageUrl());
        procedurePhoto.setFileName(request.fileName());
        return procedurePhoto;
    }

    public void updateEntity(ProcedurePhoto procedurePhoto, ProcedurePhotoRequest request) {
        procedurePhoto.setImageUrl(request.imageUrl());
        procedurePhoto.setFileName(request.fileName());
    }
}
