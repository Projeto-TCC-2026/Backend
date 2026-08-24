package com.tcc.application.dto.response;

/** Novo link de ativação/primeiro acesso gerado sob demanda (ex.: botão "copiar link" na listagem de doutores). */
public record AccessLinkResponse(
        String activationLink
) {}
