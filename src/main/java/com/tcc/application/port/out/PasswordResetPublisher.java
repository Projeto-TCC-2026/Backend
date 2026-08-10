package com.tcc.application.port.out;

public interface PasswordResetPublisher {

    void publishResetRequested(
            String email,
            String token,
            String frontendBaseUrl);
}