package com.tcc.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Regra de validação do {@link Cnpj}. O cálculo fica em {@link #isValid(String)}, estático,
 * para poder ser usado fora do Bean Validation e testado sem montar um validator.
 */
public class CnpjValidator implements ConstraintValidator<Cnpj, String> {

    /** 12 posições alfanuméricas (raiz + ordem) seguidas de 2 dígitos verificadores. */
    private static final String FORMAT = "[0-9A-Z]{12}\\d{2}";

    private static final int[] FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return isValid(value);
    }

    /**
     * Indica se {@code cnpj} é um CNPJ válido, sem máscara.
     *
     * <p>Cada caractere vale o seu código ASCII menos 48: os dígitos continuam valendo
     * 0 a 9 e as letras passam a valer de 17 ({@code A}) a 42 ({@code Z}). Com esses
     * valores o cálculo é o mesmo do CNPJ numérico: soma ponderada módulo 11, e resto
     * menor que 2 vira 0; caso contrário, o dígito é {@code 11 - resto}.
     */
    public static boolean isValid(String cnpj) {
        if (cnpj == null || !cnpj.matches(FORMAT)) {
            return false;
        }
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }
        int first = checkDigit(cnpj, FIRST_WEIGHTS);
        int second = checkDigit(cnpj, SECOND_WEIGHTS);
        return first == cnpj.charAt(12) - '0' && second == cnpj.charAt(13) - '0';
    }

    /** Calcula o dígito verificador sobre as primeiras {@code weights.length} posições. */
    private static int checkDigit(String cnpj, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (cnpj.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
