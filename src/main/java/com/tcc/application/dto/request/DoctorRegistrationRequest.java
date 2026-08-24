package com.tcc.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Cadastro de um doutor sem informar userId/senha: o backend cria a conta de
 * usuário automaticamente (com senha temporária desconhecida) e dispara o
 * e-mail de boas-vindas para o doutor definir sua própria senha.
 */
public record DoctorRegistrationRequest(

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
        String email,

        @NotNull(message = "ID do hospital é obrigatório")
        UUID hospitalId,

        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String fullName,

        @NotBlank(message = "CPF é obrigatório")
        @Size(min = 11, max = 11, message = "CPF deve ter 11 caracteres")
        String cpf,

        @NotBlank(message = "CRM é obrigatório")
        @Size(max = 20, message = "CRM deve ter no máximo 20 caracteres")
        String crm,

        @Size(max = 100, message = "Especialidade deve ter no máximo 100 caracteres")
        String specialty,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String phone
) {}
