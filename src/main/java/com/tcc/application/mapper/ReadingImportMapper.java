package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.ReadingImportRequest;
import com.tcc.application.dto.response.ReadingImportResponse;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientDevice;
import com.tcc.domain.model.ReadingImport;

@Component
public class ReadingImportMapper {

    private final PatientMapper patientMapper;

    public ReadingImportMapper(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    public ReadingImportResponse toResponse(ReadingImport readingImport) {
        if (readingImport == null) return null;
        return new ReadingImportResponse(
                readingImport.getId(),
                patientMapper.toSummary(readingImport.getPatient()),
                readingImport.getPatientDevice() != null ? readingImport.getPatientDevice().getId() : null,
                readingImport.getSourceFile(),
                readingImport.getImportedAt()
        );
    }

    public ReadingImport toEntity(ReadingImportRequest request, Patient patient, PatientDevice patientDevice) {
        if (request == null) return null;
        ReadingImport readingImport = new ReadingImport();
        readingImport.setPatient(patient);
        readingImport.setPatientDevice(patientDevice);
        readingImport.setSourceFile(request.sourceFile());
        return readingImport;
    }
}
