package com.tcc.application.service;

/**
 * Sinaliza que a leitura recebida já estava gravada.
 *
 * <p>Só é lançada no caso de corrida: duas requisições idênticas que passam juntas
 * pela checagem de idempotência e são separadas pela restrição UNIQUE da V33. O
 * caminho comum de leitura repetida nem chega aqui — ele é detectado por consulta
 * e devolve resposta de sucesso normalmente.
 *
 * <p>Fica no pacote de service, e não em {@code com.tcc.exception}, porque não é um
 * erro traduzido pelo handler global: leitura repetida é sucesso. Quem trata é o
 * {@code AlertController}, que a converte em 2xx.
 */
public class AlertDuplicateReadingException extends RuntimeException {

    public AlertDuplicateReadingException(Throwable cause) {
        super("Leitura já recebida anteriormente", cause);
    }
}
