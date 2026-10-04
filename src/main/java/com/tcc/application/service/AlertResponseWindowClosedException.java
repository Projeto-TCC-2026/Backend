package com.tcc.application.service;

/**
 * Sinaliza que o alerta não está mais aguardando a resposta da paciente: o prazo
 * de 10 minutos venceu e o médico já foi avisado, ou a resposta já havia sido
 * registrada antes.
 *
 * <p>Fica no pacote de service, e não em {@code com.tcc.exception}, pelo mesmo
 * motivo de {@link AlertDuplicateReadingException}: o {@code GlobalExceptionHandler}
 * não traduz este caso, que não é 400 nem 404 nem 401. Quem traduz é o
 * {@code MobileAlertController}, que a converte em 409.
 *
 * <p>A mensagem é a que a paciente vê, então diz o que aconteceu em português e
 * sem valor numérico de medição.
 */
public class AlertResponseWindowClosedException extends RuntimeException {

    public AlertResponseWindowClosedException(String message) {
        super(message);
    }

    /** Prazo vencido sem resposta: o agendador já levou o alerta ao médico. */
    public static AlertResponseWindowClosedException deadlineExpired() {
        return new AlertResponseWindowClosedException(
                "O prazo para responder já venceu e seu médico foi avisado. "
                        + "Não é mais necessário responder.");
    }

    /** Resposta já registrada: segunda tentativa para o mesmo alerta. */
    public static AlertResponseWindowClosedException alreadyAnswered() {
        return new AlertResponseWindowClosedException(
                "Sua resposta para este alerta já foi registrada.");
    }
}
