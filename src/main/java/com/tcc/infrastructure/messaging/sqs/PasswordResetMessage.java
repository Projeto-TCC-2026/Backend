package com.tcc.infrastructure.messaging.sqs;

public record PasswordResetMessage(
        String event,
        String email,
        String token,
        String frontendBaseUrl
) {
}