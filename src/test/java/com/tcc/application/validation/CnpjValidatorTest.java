package com.tcc.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CnpjValidator")
class CnpjValidatorTest {

    @ParameterizedTest(name = "{0} é válido")
    @ValueSource(strings = {"11222333000181", "11444777000161", "45997418000153"})
    @DisplayName("aceita CNPJ numérico com dígitos verificadores corretos")
    void shouldAcceptValidNumericCnpj(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isTrue();
    }

    @ParameterizedTest(name = "{0} é válido")
    @ValueSource(strings = {"12ABC34501DE35", "AB12CD34EF5602"})
    @DisplayName("aceita CNPJ alfanumérico com dígitos verificadores corretos")
    void shouldAcceptValidAlphanumericCnpj(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isTrue();
    }

    @ParameterizedTest(name = "{0} é inválido")
    @ValueSource(strings = {"11222333000180", "11222333000191", "12345678000100", "12ABC34501DE36", "12ABC34501DF35"})
    @DisplayName("recusa CNPJ com dígito verificador errado")
    void shouldRejectWrongCheckDigit(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isFalse();
    }

    @ParameterizedTest(name = "{0} é inválido")
    @ValueSource(strings = {"00000000000000", "11111111111111"})
    @DisplayName("recusa sequência de caracteres repetidos, mesmo com cálculo batendo")
    void shouldRejectRepeatedCharacters(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isFalse();
    }

    @ParameterizedTest(name = "\"{0}\" é inválido")
    @ValueSource(strings = {
            "11.222.333/0001-81",   // com máscara
            "1122233300018",        // 13 caracteres
            "112223330001811",      // 15 caracteres
            "12abc34501de35",       // letras minúsculas
            "12ABC34501DEA5",       // letra na posição do dígito verificador
            "12ABC34501D-35"        // caractere especial
    })
    @DisplayName("recusa valor com máscara, tamanho errado, minúsculas ou letra no dígito verificador")
    void shouldRejectBadFormat(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isFalse();
    }

    @Test
    @DisplayName("isValid estático recusa null")
    void shouldRejectNullInStaticCheck() {
        assertThat(CnpjValidator.isValid((String) null)).isFalse();
    }

    @Test
    @DisplayName("como constraint, deixa null e vazio para o @NotBlank")
    void shouldIgnoreNullAndBlankAsConstraint() {
        CnpjValidator validator = new CnpjValidator();
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
    }
}
