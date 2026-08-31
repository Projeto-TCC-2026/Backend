package com.tcc.exception;

import java.util.UUID;

public final class ErrorMessages {

    public static final String INVALID_ROLE =
            "Role inválida. Valores aceitos: ADMIN, DOCTOR, PATIENT, HOSPITAL";
    public static final String PATIENT_HAS_HEALTH_READINGS =
            "Não é possível excluir o paciente. Existem leituras de saúde associadas.";
    public static final String HOSPITAL_ALREADY_HAS_USER =
            "Este hospital já possui um usuário gestor vinculado.";

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

    public static String procedureNotFoundById(UUID id) {
        return "Procedimento não encontrado com ID: " + id;
    }

    public static String duplicateProcedureTitle(String title) {
        return "Já existe um procedimento cadastrado neste hospital com o título: " + title;
    }

    public static String procedureNotInHospital() {
        return "Este procedimento não pertence ao seu hospital";
    }

    public static String doctorNotInHospital() {
        return "Este médico não pertence ao seu hospital";
    }

    public static String doctorAlreadyAssignedToProcedure() {
        return "Este médico já está associado a este procedimento";
    }

    public static String doctorNotAssignedToProcedure() {
        return "Este médico não está associado a este procedimento";
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

    public static String procedureAlreadyInactive() {
        return "Este procedimento já está inativo";
    }

    public static String doctorProfileNotFound() {
        return "Usuário autenticado não possui perfil de médico";
    }

    public static String procedureNotAssignedToDoctor() {
        return "Este procedimento não está entre os que o hospital atribuiu a você";
    }

    public static String procedureInactiveForAssignment() {
        return "Este procedimento está inativo e não pode ser atribuído a pacientes";
    }

    public static String patientNotLinkedToDoctor() {
        return "Este paciente não está vinculado a você";
    }

    public static String patientNotInHospital() {
        return "Este paciente não está vinculado a um médico do seu hospital";
    }

    public static String hospitalProfileNotFound() {
        return "Usuário autenticado não possui hospital vinculado";
    }

    public static String patientProcedureNotFoundById(UUID id) {
        return "Atribuição de procedimento não encontrada com ID: " + id;
    }

    public static String duplicatePatientProcedure() {
        return "Este procedimento já está atribuído a este paciente por você";
    }

    public static String hospitalHasDoctors(long count) {
        return "Não é possível excluir o hospital. Existem " + count + " doutores associados.";
    }
}
