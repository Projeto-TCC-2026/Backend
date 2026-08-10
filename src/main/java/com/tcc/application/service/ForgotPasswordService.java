package com.tcc.application.service;

import com.tcc.application.dto.request.ResetPasswordRequest;

public interface ForgotPasswordService {

    void requestPasswordReset(String email);

    void resetPassword(ResetPasswordRequest request);
}
