package com.tcc.application.service;

import com.tcc.application.dto.request.ResetPasswordRequest;
import com.tcc.application.dto.response.UserRoleResponse;

public interface ForgotPasswordService {

    void requestPasswordReset(String email);

    /** Redefine a senha a partir do token recebido por e-mail e devolve o perfil do usuário. */
    UserRoleResponse resetPassword(ResetPasswordRequest request);
}
