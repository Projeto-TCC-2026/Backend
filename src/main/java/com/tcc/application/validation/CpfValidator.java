package com.tcc.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Regra de validação do {@link Cpf}. O cálculo fica em {@link #isValid(String)}, estático,
 * para poder ser usado fora do Bean Validation e testado sem montar um validator.
 */
public class CpfValidator implements ConstraintValidator<Cpf, String> {

    private static final int LENGTH = 11;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return isValid(value);
    }

    /**
     * Indica se {@code cpf} é um CPF válido, sem máscara.
     *
     * <p>Cada dígito verificador é a soma dos dígitos anteriores multiplicados por pesos
     * decrescentes (10..2 para o primeiro, 11..2 para o segundo), módulo 11: resto menor
     * que 2 vira 0; caso contrário, o dígito é {@code 11 - resto}.
     */
    public static boolean isValid(String cpf) {
        if (cpf == null || !cpf.matches("\\d{" + LENGTH + "}")) {
            return false;
        }
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        int first = checkDigit(cpf, 9);
        int second = checkDigit(cpf, 10);
        return first == cpf.charAt(9) - '0' && second == cpf.charAt(10) - '0';
    }

    /** Calcula o dígito verificador a partir dos {@code length} primeiros dígitos. */
    private static int checkDigit(String cpf, int length) {
        int sum = 0;
        int weight = length + 1;
        for (int i = 0; i < length; i++) {
            sum += (cpf.charAt(i) - '0') * weight--;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
