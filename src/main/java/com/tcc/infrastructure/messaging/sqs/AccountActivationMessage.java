package com.tcc.infrastructure.messaging.sqs;

public record AccountActivationMessage(
        String event,
        String email,
        String fullName,
        String token,
        String frontendBaseUrl
) {
}
