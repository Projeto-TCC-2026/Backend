package com.tcc.application.dto.request;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Dado inválido tem de virar 4xx no endpoint de integração. O controller usa
 * {@code @Valid}, e o {@code GlobalExceptionHandler} já traduz
 * {@code MethodArgumentNotValidException} em 400 — então provar o 4xx aqui é
 * provar que cada campo obrigatório realmente tem restrição que falha.
 */
@DisplayName("validação de AlertEvaluationRequest")
class AlertEvaluationRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private Set<ConstraintViolation<AlertEvaluationRequest>> validate(AlertEvaluationRequest request) {
        return validator.validate(request);
    }

    @Test
    @DisplayName("requisição completa deve passar")
    void shouldAcceptCompleteRequest() {
        AlertEvaluationRequest request = new AlertEvaluationRequest(
                UUID.randomUUID(), "HEART_RATE", 155.0, OffsetDateTime.now(), "bpm");

        assertThat(validate(request)).isEmpty();
    }

    @Test
    @DisplayName("unidade é opcional")
    void shouldAcceptNullUnit() {
        AlertEvaluationRequest request = new AlertEvaluationRequest(
                UUID.randomUUID(), "HEART_RATE", 155.0, OffsetDateTime.now(), null);

        assertThat(validate(request)).isEmpty();
    }

    @Test
    @DisplayName("paciente ausente deve violar a validação")
    void shouldRejectMissingPatient() {
        AlertEvaluationRequest request = new AlertEvaluationRequest(
                null, "HEART_RATE", 155.0, OffsetDateTime.now(), "bpm");

        assertThat(validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("patientId");
    }

    @Test
    @DisplayName("tipo de leitura em branco deve violar a validação")
    void shouldRejectBlankReadingType() {
        AlertEvaluationRequest request = new AlertEvaluationRequest(
                UUID.randomUUID(), "   ", 155.0, OffsetDateTime.now(), "bpm");

        assertThat(validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("readingType");
    }

    @Test
    @DisplayName("valor ausente deve violar a validação")
    void shouldRejectMissingValue() {
        AlertEvaluationRequest request = new AlertEvaluationRequest(
                UUID.randomUUID(), "HEART_RATE", null, OffsetDateTime.now(), "bpm");

        assertThat(validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("value");
    }

    @Test
    @DisplayName("horário da medição ausente deve violar a validação")
    void shouldRejectMissingMeasuredAt() {
        AlertEvaluationRequest request = new AlertEvaluationRequest(
                UUID.randomUUID(), "HEART_RATE", 155.0, null, "bpm");

        assertThat(validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("measuredAt");
    }
}

/**
 * Desserialização do {@code measuredAt}.
 *
 * <p>Usa o {@code JacksonJsonHttpMessageConverter}, que é o conversor que o Spring
 * MVC realmente emprega neste projeto (Spring Boot 4 / Jackson 3). Assim o teste
 * exercita a borda HTTP de verdade, inclusive a exceção que o Spring lança — e não
 * uma simulação dela.
 *
 * <p>O que estes testes travam:
 * <ul>
 *   <li>{@code Z} e {@code -03:00} do mesmo instante produzem o mesmo instante;</li>
 *   <li>horário sem fuso é recusado com {@code HttpMessageNotReadableException},
 *       que o {@code GlobalExceptionHandler} traduz em 400;</li>
 *   <li>JSON malformado cai na mesma exceção, também 400.</li>
 * </ul>
 *
 * <p>Nenhum caso depende do fuso da máquina: todos os valores carregam o
 * deslocamento, e as comparações são por instante.
 */
@DisplayName("desserialização do measuredAt de AlertEvaluationRequest")
class AlertEvaluationRequestDeserializationTest {

    private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();

    private String bodyWithMeasuredAt(String measuredAt) {
        return """
                {
                  "patientId": "550e8400-e29b-41d4-a716-446655440000",
                  "readingType": "HEART_RATE",
                  "value": 155,
                  "measuredAt": "%s",
                  "unit": "bpm"
                }
                """.formatted(measuredAt);
    }

    /**
     * Lê o corpo pelo mesmo caminho do endpoint. A exceção de corpo ilegível é
     * lançada pelo próprio conversor do Spring, não construída pelo teste.
     */
    private AlertEvaluationRequest read(String body) {
        try {
            return (AlertEvaluationRequest) converter.read(
                    AlertEvaluationRequest.class,
                    new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("Falha de I/O inesperada no teste", e);
        }
    }

    @Test
    @DisplayName("aceita horário em UTC com Z")
    void shouldAcceptUtcWithZ() {
        AlertEvaluationRequest request = read(bodyWithMeasuredAt("2026-07-01T14:32:00Z"));

        assertThat(request.measuredAt().toInstant())
                .isEqualTo(Instant.parse("2026-07-01T14:32:00Z"));
    }

    @Test
    @DisplayName("aceita horário com deslocamento -03:00")
    void shouldAcceptExplicitNegativeOffset() {
        AlertEvaluationRequest request = read(bodyWithMeasuredAt("2026-07-01T11:32:00-03:00"));

        assertThat(request.measuredAt().toInstant())
                .isEqualTo(Instant.parse("2026-07-01T14:32:00Z"));
    }

    /**
     * Os dois textos descrevem o mesmo instante, então precisam convergir para o
     * mesmo {@code LocalDateTime} em UTC — que é exatamente o valor usado como chave
     * de idempotência e gravado em {@code measured_at}.
     *
     * <p>A comparação é feita depois da conversão para UTC, não sobre o
     * {@code OffsetDateTime} cru: o Jackson ajusta o deslocamento para o fuso de
     * contexto da JVM, então o offset que sobra no objeto varia por ambiente. O
     * instante, não.
     */
    @Test
    @DisplayName("Z e -03:00 do mesmo instante devem convergir para o mesmo horário em UTC")
    void shouldTreatBothOffsetsAsTheSameInstant() {
        AlertEvaluationRequest withZ = read(bodyWithMeasuredAt("2026-07-01T14:32:00Z"));
        AlertEvaluationRequest withOffset = read(bodyWithMeasuredAt("2026-07-01T11:32:00-03:00"));

        assertThat(withZ.measuredAt().toInstant())
                .isEqualTo(withOffset.measuredAt().toInstant());

        LocalDateTime fromZ = withZ.measuredAt()
                .withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime fromOffset = withOffset.measuredAt()
                .withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();

        assertThat(fromZ)
                .isEqualTo(fromOffset)
                .isEqualTo(LocalDateTime.of(2026, 7, 1, 14, 32));
    }

    @Test
    @DisplayName("horário sem fuso deve ser recusado")
    void shouldRejectMeasuredAtWithoutOffset() {
        assertThatThrownBy(() -> read(bodyWithMeasuredAt("2026-07-01T14:32:00")))
                .isInstanceOf(HttpMessageNotReadableException.class);
    }

    @Test
    @DisplayName("JSON malformado deve ser recusado")
    void shouldRejectMalformedJson() {
        String malformed = """
                {
                  "patientId": "550e8400-e29b-41d4-a716-446655440000",
                  "readingType": "HEART_RATE",
                  "value": 155,
                """;

        assertThatThrownBy(() -> read(malformed))
                .isInstanceOf(HttpMessageNotReadableException.class);
    }
}
