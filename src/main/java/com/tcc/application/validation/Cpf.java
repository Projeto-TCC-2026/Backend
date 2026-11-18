package com.tcc.application.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida um CPF sem máscara: 11 dígitos com os dois dígitos verificadores corretos.
 * Sequências repetidas como {@code 11111111111} são recusadas.
 *
 * <p>Valor nulo ou em branco é considerado válido aqui; a obrigatoriedade fica a cargo
 * de {@code @NotBlank}, para não gerar duas mensagens para o mesmo erro.
 */
@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cpf {

    String message() default "CPF inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
