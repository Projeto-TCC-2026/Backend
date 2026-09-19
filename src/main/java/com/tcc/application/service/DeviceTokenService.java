package com.tcc.application.service;

import com.tcc.application.dto.request.DeviceTokenRequest;

public interface DeviceTokenService {

    /**
     * Registra o token de push do usuário autenticado. Se o token já existir,
     * atualiza o vínculo em vez de criar duplicado.
     *
     * @param email e-mail do usuário autenticado, nunca vindo do corpo da requisição
     */
    void register(String email, DeviceTokenRequest request);

    /**
     * Remove o token de push do usuário autenticado.
     *
     * @param email e-mail do usuário autenticado, nunca vindo do corpo da requisição
     */
    void unregister(String email, String token);
}
