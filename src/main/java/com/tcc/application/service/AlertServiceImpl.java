package com.tcc.application.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.request.AlertRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.mapper.AlertMapper;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.ReadingThreshold;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ReadingThresholdRepository;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;

@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    /**
     * Status inicial do alerta. O JPQL de contagem do dashboard filtra por esta
     * string literal, então qualquer outra grafia torna o alerta invisível nos
     * indicadores de "alertas pendentes".
     */
    private static final String STATUS_PENDING = "PENDING";

    private final PatientRepository patientRepository;
    private final ReadingThresholdRepository readingThresholdRepository;
    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    public AlertServiceImpl(PatientRepository patientRepository,
                            ReadingThresholdRepository readingThresholdRepository,
                            AlertRepository alertRepository,
                            AlertMapper alertMapper) {
        this.patientRepository = patientRepository;
        this.readingThresholdRepository = readingThresholdRepository;
        this.alertRepository = alertRepository;
        this.alertMapper = alertMapper;
    }

    @Override
    @Transactional
    public AlertEvaluationResponse evaluateReading(AlertEvaluationRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.patientNotFoundById(request.patientId())));

        Optional<ReadingThreshold> threshold =
                readingThresholdRepository.findByReadingType(request.readingType());

        if (threshold.isEmpty()) {
            log.warn("Nenhuma faixa cadastrada para o tipo de leitura {}. Avaliacao ignorada para o paciente {}.",
                    request.readingType(), patient.getId());
            return new AlertEvaluationResponse(false, null, null, null);
        }

        ReadingThreshold range = threshold.get();
        String reason = buildReason(request.value(), range);

        if (reason == null) {
            return new AlertEvaluationResponse(false, null, null, null);
        }

        Alert alert = buildAlert(patient, range, reason);
        Alert savedAlert = alertRepository.save(alert);

        log.info("Alerta {} gerado para o paciente {} a partir do tipo de leitura {}.",
                savedAlert.getId(), patient.getId(), range.getReadingType());

        return new AlertEvaluationResponse(true, range.getSeverity(), savedAlert.getId(), reason);
    }

    /**
     * Compara o valor com a faixa normal e descreve o desvio encontrado.
     * Retorna null quando o valor está dentro da faixa.
     *
     * <p>Os limites são inclusivos no normal: a comparação usa estritamente menor e
     * estritamente maior, então valor igual a um dos limites não gera alerta.
     */
    private String buildReason(Double value, ReadingThreshold range) {
        Double min = range.getNormalMin();
        Double max = range.getNormalMax();

        if (min != null && value < min) {
            return "Valor abaixo do mínimo normal de " + min + " para " + range.getReadingType();
        }
        if (max != null && value > max) {
            return "Valor acima do máximo normal de " + max + " para " + range.getReadingType();
        }
        return null;
    }

    /**
     * Monta o alerta reaproveitando o AlertMapper já existente, para não duplicar a
     * atribuição de campos da entidade. O healthReadingId fica nulo: esta versão não
     * persiste a leitura, apenas o alerta derivado dela.
     */
    private Alert buildAlert(Patient patient, ReadingThreshold range, String reason) {
        AlertRequest alertRequest = new AlertRequest(
                patient.getId(),
                null,
                range.getSeverity(),
                buildTitle(range),
                reason,
                STATUS_PENDING
        );
        return alertMapper.toEntity(alertRequest, patient, null);
    }

    private String buildTitle(ReadingThreshold range) {
        String label = range.getLabel();
        String title = "Leitura fora da faixa normal: " + range.getReadingType()
                + (label != null ? " (" + label + ")" : "");
        return title.length() > 255 ? title.substring(0, 255) : title;
    }
}
