package com.tcc.application.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo do cadastro de paciente. Não recebe {@code userId}: a conta de acesso é criada
 * pelo backend a partir do {@code email}, com senha temporária desconhecida, e o paciente
 * define a própria senha pelo link de ativação.
 *
 * <p>A atualização usa {@link PatientUpdateRequest}, que não tem {@code procedures}:
 * procedimento é atribuído e removido pelos endpoints de
 * {@code /api/patients/{patientId}/assigned-procedures}.
 */
public record PatientRequest(

        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String fullName,

        @NotBlank(message = "CPF é obrigatório")
        @Size(min = 11, max = 11, message = "CPF deve ter 11 caracteres")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter apenas números")
        String cpf,

        @NotNull(message = "Data de nascimento é obrigatória")
        @Past(message = "Data de nascimento deve ser no passado")
        LocalDate birthDate,

        @Size(max = 20, message = "Gênero deve ter no máximo 20 caracteres")
        String gender,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String phone,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail deve ser válido")
        @Size(max = 255, message = "E-mail deve ter no máximo 255 caracteres")
        String email,

        @Size(max = 500, message = "Endereço deve ter no máximo 500 caracteres")
        String address,

        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
        String city,

        @Size(min = 2, max = 2, message = "Estado deve ter 2 caracteres (sigla)")
        @Pattern(regexp = "[A-Z]{2}", message = "Estado deve ser uma sigla válida (ex: SP, RJ)")
        String state,

        @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres")
        String zipCode,

        @Size(max = 10, message = "Tipo sanguíneo deve ter no máximo 10 caracteres")
        String bloodType,

        Double weight,

        Double height,

        /**
         * Nenhum paciente existe sem procedimento: o médico define ao menos um no
         * cadastro. {@code @Valid} é necessário para que a validação desça nos itens
         * da lista.
         */
        @NotEmpty(message = "Informe pelo menos um procedimento para o paciente")
        @Valid
        List<PatientProcedureRequest> procedures
) {}
