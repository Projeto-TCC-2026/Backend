package com.tcc.application.port.out;

/** Porta de saída para publicar o evento de "conta criada" (fluxo de boas-vindas). */
public interface AccountActivationPublisher {

    void publishAccountCreated(
            String email,
            String fullName,
            String token,
            String frontendBaseUrl);
}
