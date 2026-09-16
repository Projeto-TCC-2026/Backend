package com.tcc.infrastructure.messaging.sqs;

public record PasswordResetMessage(
        String event,
        String email,
        String code,
        String token,
        String frontendBaseUrl
) {
    public PasswordResetMessage(String event, String email, String token, String frontendBaseUrl) {
        this(event, email, token, token, frontendBaseUrl);
    }
}