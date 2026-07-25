package com.tcc.exception;

import java.util.UUID;

public final class ErrorMessages {

    public static final String INVALID_ROLE =
            "Role inválida. Valores aceitos: ADMIN, DOCTOR, PATIENT";
    public static final String PATIENT_HAS_HEALTH_READINGS =
            "Não é possível excluir o paciente. Existem leituras de saúde associadas.";

    private ErrorMessages() {
    }

    public static String userNotFoundById(UUID id) {
        return "Usuário não encontrado com ID: " + id;
    }

    public static String userNotFoundByEmail(String email) {
        return "Usuário não encontrado com e-mail: " + email;
    }

    public static String patientNotFoundById(UUID id) {
        return "Paciente não encontrado com ID: " + id;
    }

    public static String doctorNotFoundById(UUID id) {
        return "Doutor não encontrado com ID: " + id;
    }

    public static String doctorNotFoundByCrm(String crm) {
        return "Doutor não encontrado com CRM: " + crm;
    }

    public static String hospitalNotFoundById(UUID id) {
        return "Hospital não encontrado com ID: " + id;
    }

    public static String duplicateUserEmail(String email) {
        return "Já existe um usuário cadastrado com o e-mail: " + email;
    }

    public static String duplicateActivePatientCpf(String cpf) {
        return "Já existe um paciente ativo cadastrado com o CPF: " + cpf;
    }

    public static String duplicateActivePatientEmail(String email) {
        return "Já existe um paciente ativo cadastrado com o e-mail: " + email;
    }

    public static String duplicateDoctorCpf(String cpf) {
        return "Já existe um doutor cadastrado com o CPF: " + cpf;
    }

    public static String duplicateDoctorCrm(String crm) {
        return "Já existe um doutor cadastrado com o CRM: " + crm;
    }

    public static String duplicateHospitalCnpj(String cnpj) {
        return "Já existe um hospital cadastrado com o CNPJ: " + cnpj;
    }

    public static String userAlreadyAssociatedWithPatient() {
        return "Usuário já está associado a um paciente";
    }

    public static String userAlreadyAssociatedWithDoctor() {
        return "Usuário já está associado a um doutor";
    }

    public static String patientHasProcedureExecutions(long count) {
        return "Não é possível excluir o paciente. Existem " + count
                + " procedimento(s) realizado(s) associado(s). "
                + "Use a inativação ao invés da exclusão para manter o histórico.";
    }

    public static String doctorHasPatients(long count) {
        return "Não é possível excluir o doutor. Existem " + count + " pacientes associados.";
    }

    public static String doctorHasProcedures(long count) {
        return "Não é possível excluir o doutor. Existem " + count + " procedimentos associados.";
    }

    public static String hospitalHasDoctors(long count) {
        return "Não é possível excluir o hospital. Existem " + count + " doutores associados.";
    }
}
