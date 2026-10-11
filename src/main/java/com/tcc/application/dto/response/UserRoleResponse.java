package com.tcc.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Perfil do usuário cuja senha acabou de ser definida (ativação de conta ou
 * redefinição de senha).
 *
 * <p>Carrega apenas a role, para o front decidir a tela seguinte. Nenhum dado de
 * identificação, nenhum identificador e nenhum token passam por aqui, porque os
 * endpoints que devolvem este DTO são públicos.
 */
@Schema(description = "Perfil do usuário")
public record UserRoleResponse(

        @Schema(description = "Perfil do usuário", example = "PATIENT")
        String role
) {}
