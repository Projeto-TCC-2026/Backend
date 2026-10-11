package com.tcc.application.service;

import com.tcc.application.dto.request.AccountActivationRequest;
import com.tcc.application.dto.response.UserRoleResponse;
import com.tcc.domain.model.User;

public interface AccountActivationService {

    /** Gera um token de ativação, dispara o e-mail de boas-vindas e retorna o link gerado (para exibir uma cópia na UI). */
    String issueActivationToken(User user, String fullName);

    /** Valida o token recebido por e-mail, define a primeira senha da conta e devolve o perfil do usuário. */
    UserRoleResponse activateAccount(AccountActivationRequest request);
}
