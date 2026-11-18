package com.tcc.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CpfValidator")
class CpfValidatorTest {

    @ParameterizedTest(name = "{0} é válido")
    @ValueSource(strings = {"12345678909", "52998224725", "11144477735", "00000000191"})
    @DisplayName("aceita CPF com dígitos verificadores corretos")
    void shouldAcceptValidCpf(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isTrue();
    }

    @ParameterizedTest(name = "{0} é inválido")
    @ValueSource(strings = {"12345678901", "12345678919", "52998224724", "11144477734"})
    @DisplayName("recusa CPF com dígito verificador errado")
    void shouldRejectWrongCheckDigit(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isFalse();
    }

    @ParameterizedTest(name = "{0} é inválido")
    @ValueSource(strings = {"00000000000", "11111111111", "99999999999"})
    @DisplayName("recusa sequência de dígitos repetidos, mesmo com cálculo batendo")
    void shouldRejectRepeatedDigits(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isFalse();
    }

    @ParameterizedTest(name = "\"{0}\" é inválido")
    @ValueSource(strings = {"123.456.789-09", "1234567890", "123456789090", "1234567890A", " 12345678909"})
    @DisplayName("recusa valor com máscara, tamanho errado ou caractere não numérico")
    void shouldRejectBadFormat(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isFalse();
    }

    @Test
    @DisplayName("isValid estático recusa null")
    void shouldRejectNullInStaticCheck() {
        assertThat(CpfValidator.isValid((String) null)).isFalse();
    }

    @Test
    @DisplayName("como constraint, deixa null e vazio para o @NotBlank")
    void shouldIgnoreNullAndBlankAsConstraint() {
        CpfValidator validator = new CpfValidator();
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }
}
