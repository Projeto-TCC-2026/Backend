package com.tcc.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tcc.application.dto.request.DoctorRegistrationRequest;
import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.request.HospitalRegistrationRequest;
import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.request.PatientUpdateRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Garante que {@link Cpf} e {@link Cnpj} estão de fato aplicados nos DTOs de entrada, ou
 * seja, que um documento com dígito verificador errado vira erro de validação (400) no
 * endpoint, com a mensagem no campo certo.
 */
@DisplayName("CPF/CNPJ nos DTOs de requisição")
class DocumentConstraintsOnRequestsTest {

    private static final String VALID_CPF = "12345678909";
    private static final String INVALID_CPF = "12345678901";
    private static final String VALID_CNPJ = "11222333000181";
    private static final String VALID_ALPHANUMERIC_CNPJ = "12ABC34501DE35";
    private static final String INVALID_CNPJ = "12345678000100";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static <T> Set<String> messagesFor(Set<ConstraintViolation<T>> violations, String field) {
        return violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals(field))
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static HospitalRequest hospital(String cnpj) {
        return new HospitalRequest("Hospital Central", cnpj, "1133334444",
                "contato@hospital.com", "Rua B", "Sao Paulo", "SP");
    }

    @Test
    @DisplayName("HospitalRequest aceita CNPJ numérico e alfanumérico válidos")
    void hospitalAcceptsValidCnpj() {
        assertThat(validator.validate(hospital(VALID_CNPJ))).isEmpty();
        assertThat(validator.validate(hospital(VALID_ALPHANUMERIC_CNPJ))).isEmpty();
    }

    @Test
    @DisplayName("HospitalRequest recusa CNPJ com dígito errado")
    void hospitalRejectsInvalidCnpj() {
        assertThat(messagesFor(validator.validate(hospital(INVALID_CNPJ)), "cnpj"))
                .containsExactly("CNPJ inválido");
    }

    @Test
    @DisplayName("CNPJ vazio gera só a mensagem de obrigatoriedade")
    void blankCnpjReportsOnlyRequired() {
        assertThat(messagesFor(validator.validate(hospital("")), "cnpj"))
                .containsExactly("CNPJ é obrigatório");
    }

    @Test
    @DisplayName("HospitalRegistrationRequest recusa CNPJ com dígito errado")
    void hospitalRegistrationRejectsInvalidCnpj() {
        HospitalRegistrationRequest request = new HospitalRegistrationRequest("Hospital Novo", INVALID_CNPJ,
                "1133334444", "contato@hospitalnovo.com", "Rua C", "Sao Paulo", "SP", "senha123");
        assertThat(messagesFor(validator.validate(request), "cnpj")).containsExactly("CNPJ inválido");
    }

    @Test
    @DisplayName("DoctorRequest e DoctorRegistrationRequest validam o CPF")
    void doctorRequestsValidateCpf() {
        DoctorRequest valid = new DoctorRequest(UUID.randomUUID(), UUID.randomUUID(), "Dr. Carlos",
                VALID_CPF, "CRM12345", "Cardiologia", "11988887777");
        DoctorRequest invalid = new DoctorRequest(UUID.randomUUID(), UUID.randomUUID(), "Dr. Carlos",
                INVALID_CPF, "CRM12345", "Cardiologia", "11988887777");
        DoctorRegistrationRequest invalidRegistration = new DoctorRegistrationRequest("dr@hospital.com",
                UUID.randomUUID(), "Dr. Carlos", "123.456.789-09", "CRM12345", "Cardiologia", "11988887777");

        assertThat(validator.validate(valid)).isEmpty();
        assertThat(messagesFor(validator.validate(invalid), "cpf")).containsExactly("CPF inválido");
        assertThat(messagesFor(validator.validate(invalidRegistration), "cpf")).containsExactly("CPF inválido");
    }

    @Test
    @DisplayName("PatientUpdateRequest valida o CPF")
    void patientUpdateValidatesCpf() {
        PatientUpdateRequest invalid = new PatientUpdateRequest("Joao Silva", INVALID_CPF,
                LocalDate.of(1990, 1, 1), "M", "11999999999", "patient@test.com", "Rua A",
                "Sao Paulo", "SP", "01000000", "O+", 70.0, 1.75);

        assertThat(messagesFor(validator.validate(invalid), "cpf")).containsExactly("CPF inválido");
    }
}
