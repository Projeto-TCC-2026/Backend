package com.tcc.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica chamadas de serviço a serviço na área de integração da API.
 *
 * <p>A Lambda de análise de risco não é um usuário do sistema: não tem linha em
 * {@code users}, não faz login e não recebe JWT. Por isso ela se identifica por uma
 * chave estática em header dedicado, e recebe a authority {@code ROLE_INTEGRATION},
 * que não existe no enum {@code Role} justamente porque não é um papel de usuário.
 *
 * <p>Quando a chave está ausente ou não confere, o filtro não autentica e apenas segue
 * a cadeia: o {@code authenticationEntryPoint} configurado no SecurityConfig produz o
 * 401 no formato padrão do projeto.
 */
@Component
public class ServiceKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServiceKeyAuthFilter.class);

    /** Header dedicado, para não colidir com o Authorization usado pelo filtro JWT. */
    public static final String SERVICE_KEY_HEADER = "X-Integration-Key";

    /** Authority concedida à chamada de integração. Não é um valor do enum Role. */
    public static final String INTEGRATION_AUTHORITY = "ROLE_INTEGRATION";

    /** Prefixo das rotas protegidas por chave de serviço. */
    private static final String PROTECTED_PATH_PREFIX = "/api/integration/";

    private final String expectedKey;

    public ServiceKeyAuthFilter(@Value("${app.integration.service-key:}") String expectedKey) {
        this.expectedKey = expectedKey;
    }

    /**
     * O filtro só participa das rotas de integração. Em qualquer outra rota ele é
     * ignorado pelo container, sem tocar no SecurityContext.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PROTECTED_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String providedKey = request.getHeader(SERVICE_KEY_HEADER);

        if (providedKey != null
                && SecurityContextHolder.getContext().getAuthentication() == null
                && matchesExpectedKey(providedKey)) {

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            "integration-service",
                            null,
                            List.of(new SimpleGrantedAuthority(INTEGRATION_AUTHORITY))
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Compara a chave recebida com a esperada em tempo constante, para não vazar
     * informação por diferença de tempo de resposta. Nunca usa String.equals.
     *
     * <p>Chave não configurada significa integração desligada: nega sempre, em vez de
     * liberar a rota por omissão.
     */
    private boolean matchesExpectedKey(String providedKey) {
        if (expectedKey == null || expectedKey.isBlank()) {
            // Sem chave configurada não há como autenticar. Falha fechada.
            log.warn("Chave de integração não configurada. Requisição de integração recusada.");
            return false;
        }

        byte[] provided = providedKey.getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedKey.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(provided, expected);
    }
}
