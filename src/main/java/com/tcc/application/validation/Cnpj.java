package com.tcc.application.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida um CNPJ sem máscara, numérico ou alfanumérico, com os dois dígitos verificadores
 * corretos.
 *
 * <p>Segue o formato alfanumérico da Receita Federal (IN RFB nº 2.229/2024): as 12
 * primeiras posições aceitam dígitos e letras maiúsculas, e as 2 últimas (os dígitos
 * verificadores) são sempre numéricas. Um CNPJ só com números continua válido. Letras
 * minúsculas são recusadas: o cliente deve enviar o valor em maiúsculas.
 *
 * <p>Valor nulo ou em branco é considerado válido aqui; a obrigatoriedade fica a cargo
 * de {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = CnpjValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cnpj {

    String message() default "CNPJ inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
