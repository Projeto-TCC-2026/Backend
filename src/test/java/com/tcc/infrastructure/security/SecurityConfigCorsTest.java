package com.tcc.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Cobre a leitura de {@code app.cors.allowed-origins}. O cenário do valor padrão
 * resolve o placeholder a partir do próprio {@code application.properties}, com o
 * ambiente real do processo de teste — onde {@code APP_CORS_ALLOWED_ORIGINS} não
 * está definida —, para que o padrão versionado seja de fato verificado.
 */
class SecurityConfigCorsTest {

    private static final String PROPERTY_NAME = "app.cors.allowed-origins";

    private String resolveConfiguredValue() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application.properties"));

        String rawValue = properties.getProperty(PROPERTY_NAME);
        assertThat(rawValue)
                .as("application.properties deve declarar " + PROPERTY_NAME)
                .isNotNull();

        return new StandardEnvironment().resolvePlaceholders(rawValue);
    }

    @Nested
    @DisplayName("resolveAllowedOriginPatterns")
    class ResolveAllowedOriginPatterns {

        @Test
        @DisplayName("sem a variável de ambiente deve usar as origens locais do padrão")
        void shouldFallBackToLocalhostOriginsWhenEnvironmentVariableIsAbsent() throws IOException {
            List<String> patterns =
                    SecurityConfig.resolveAllowedOriginPatterns(resolveConfiguredValue());

            assertThat(patterns)
                    .containsExactly("http://localhost:*", "http://127.0.0.1:*");
        }

        @Test
        @DisplayName("deve aceitar múltiplas origens separadas por vírgula, ignorando espaços")
        void shouldSplitCommaSeparatedOriginsAndTrimSpaces() {
            List<String> patterns = SecurityConfig.resolveAllowedOriginPatterns(
                    "https://exemplo.cloudfront.net, http://localhost:*");

            assertThat(patterns)
                    .containsExactly("https://exemplo.cloudfront.net", "http://localhost:*");
        }

        @Test
        @DisplayName("deve descartar itens vazios entre vírgulas")
        void shouldDiscardEmptyItems() {
            List<String> patterns = SecurityConfig.resolveAllowedOriginPatterns(
                    "https://exemplo.cloudfront.net, ,, http://localhost:*,");

            assertThat(patterns)
                    .containsExactly("https://exemplo.cloudfront.net", "http://localhost:*");
        }

        @Test
        @DisplayName("deve falhar quando o curinga * é informado")
        void shouldRejectWildcardOrigin() {
            assertThatThrownBy(() -> SecurityConfig.resolveAllowedOriginPatterns("*"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não aceita");
        }

        @Test
        @DisplayName("deve falhar quando o curinga * aparece junto de outra origem")
        void shouldRejectWildcardMixedWithOtherOrigins() {
            assertThatThrownBy(() -> SecurityConfig.resolveAllowedOriginPatterns(
                    "https://exemplo.cloudfront.net, *"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não aceita");
        }

        @Test
        @DisplayName("deve falhar quando nenhuma origem sobra depois da limpeza")
        void shouldRejectBlankConfiguration() {
            assertThatThrownBy(() -> SecurityConfig.resolveAllowedOriginPatterns(" , , "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não pode estar vazio");
        }
    }
}
