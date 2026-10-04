package com.tcc.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.request.AlertRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.dto.response.AlertResponse;
import com.tcc.application.dto.response.PatientAlertAnswerResponse;
import com.tcc.application.mapper.AlertMapper;
import com.tcc.domain.event.AlertConfirmationReason;
import com.tcc.domain.event.AlertConfirmedEvent;
import com.tcc.domain.event.AlertCreatedEvent;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.AlertStatus;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientAlertAnswer;
import com.tcc.domain.model.ReadingThreshold;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.DoctorPatientRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HealthReadingRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ReadingThresholdRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;

@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    /**
     * Distância máxima entre a leitura que criou o alerta e a que o confirma. Acima
     * disso as duas medições não descrevem o mesmo episódio, e a segunda abre um
     * alerta novo em vez de confirmar o antigo.
     */
    private static final Duration CONFIRMATION_WINDOW = Duration.ofHours(2);

    /**
     * Tempo durante o qual um alerta já confirmado silencia novos avisos do mesmo
     * paciente e tipo. Contado a partir de {@code confirmed_at}.
     */
    private static final Duration RENOTIFY_WINDOW = Duration.ofHours(4);

    /**
     * Prazo que a paciente tem para responder à pergunta disparada por uma leitura
     * grave. Vencido o prazo sem resposta, o agendador avisa o médico — o silêncio
     * dela é tratado como motivo de preocupação, não como "está tudo bem".
     */
    private static final Duration PATIENT_RESPONSE_WINDOW = Duration.ofMinutes(10);

    /** Só a leitura anterior e o alerta mais recente interessam. */
    private static final Pageable LATEST_ONE = PageRequest.of(0, 1);

    /**
     * Teto de alertas vencidos tratados por varredura. O agendador roda a cada
     * minuto, então um acúmulo maior que isso drena em poucos ciclos, e o teto
     * evita que uma varredura isolada carregue a transação inteira do banco.
     */
    private static final Pageable EXPIRED_BATCH = PageRequest.of(0, 200);

    private final PatientRepository patientRepository;
    private final ReadingThresholdRepository readingThresholdRepository;
    private final AlertRepository alertRepository;
    private final HealthReadingRepository healthReadingRepository;
    private final AlertMapper alertMapper;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorPatientRepository doctorPatientRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public AlertServiceImpl(PatientRepository patientRepository,
                            ReadingThresholdRepository readingThresholdRepository,
                            AlertRepository alertRepository,
                            HealthReadingRepository healthReadingRepository,
                            AlertMapper alertMapper,
                            UserRepository userRepository,
                            DoctorRepository doctorRepository,
                            DoctorPatientRepository doctorPatientRepository,
                            ApplicationEventPublisher eventPublisher,
                            Clock clock) {
        this.patientRepository = patientRepository;
        this.readingThresholdRepository = readingThresholdRepository;
        this.alertRepository = alertRepository;
        this.healthReadingRepository = healthReadingRepository;
        this.alertMapper = alertMapper;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.doctorPatientRepository = doctorPatientRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Grava a leitura e decide o que fazer com ela.
     *
     * <p>Ordem fixa:
     * <ol>
     *   <li>idempotência — leitura já recebida não é regravada;</li>
     *   <li>paciente;</li>
     *   <li>faixa plausível — valor impossível é gravado como suspeito e para aqui;</li>
     *   <li>gravação da leitura;</li>
     *   <li>faixa normal;</li>
     *   <li>dentro da faixa: descarta um UNCONFIRMED aberto do tipo;</li>
     *   <li>fora da faixa: janela de 4h, confirmação de um UNCONFIRMED, pergunta à
     *       paciente quando o valor é grave, ou alerta novo — nessa ordem.</li>
     * </ol>
     *
     * <p>A leitura é gravada independentemente do resultado da avaliação, porque o
     * histórico de medições não depende de haver faixa cadastrada nem de o valor ser
     * anormal. Mesmo a leitura implausível é gravada — só fica marcada como suspeita.
     */
    @Override
    @Transactional
    public AlertEvaluationResponse evaluateReading(AlertEvaluationRequest request) {
        LocalDateTime measuredAtUtc = toUtc(request.measuredAt());

        Optional<HealthReading> alreadyReceived = healthReadingRepository
                .findByPatientIdAndReadingTypeAndMeasuredAt(
                        request.patientId(), request.readingType(), measuredAtUtc);

        if (alreadyReceived.isPresent()) {
            log.info("Leitura repetida do paciente {} para o tipo {}. Nada gravado, nada avisado.",
                    request.patientId(), request.readingType());
            return AlertEvaluationResponse.duplicate(alreadyReceived.get().getId());
        }

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.patientNotFoundById(request.patientId())));

        Optional<ReadingThreshold> threshold =
                readingThresholdRepository.findByReadingType(request.readingType());

        // Valor impossível de medir: defeito de sensor, não quadro clínico. Grava
        // marcada como suspeita e encerra — sem avaliação, sem alerta, sem aviso.
        // A resposta é de sucesso de propósito: 4xx faria a fila mandar a mensagem
        // para a DLQ e a leitura se perderia.
        if (threshold.isPresent() && threshold.get().isImplausible(request.value())) {
            ReadingThreshold range = threshold.get();
            HealthReading suspectReading =
                    saveReading(patient, request, measuredAtUtc, true);

            log.warn("Leitura implausivel do paciente {} no tipo {}. Gravada como suspeita.",
                    patient.getId(), range.getReadingType());

            return AlertEvaluationResponse.suspect(
                    suspectReading.getId(), buildImplausibleReason(range));
        }

        HealthReading reading = saveReading(patient, request, measuredAtUtc, false);

        if (threshold.isEmpty()) {
            log.warn("Nenhuma faixa cadastrada para o tipo de leitura {}. Avaliacao ignorada para o paciente {}.",
                    request.readingType(), patient.getId());
            return AlertEvaluationResponse.withoutAlert(reading.getId());
        }

        ReadingThreshold range = threshold.get();
        String reason = buildReason(request.value(), range);

        return reason == null
                ? handleNormalReading(patient, reading, range)
                : handleAbnormalReading(patient, reading, range, request.value(), reason);
    }

    /**
     * Leitura dentro da faixa normal. Se havia um alerta aguardando confirmação para
     * este tipo, o desvio não se repetiu: o alerta vira NOT_CONFIRMED e ninguém é
     * avisado.
     */
    private AlertEvaluationResponse handleNormalReading(Patient patient, HealthReading reading,
                                                        ReadingThreshold range) {
        Optional<Alert> unconfirmed = findLatestAlert(
                patient.getId(), AlertStatus.UNCONFIRMED, range.getReadingType());

        if (unconfirmed.isEmpty()) {
            return AlertEvaluationResponse.withoutAlert(reading.getId());
        }

        Alert alert = unconfirmed.get();
        alert.setStatus(AlertStatus.NOT_CONFIRMED);
        alertRepository.save(alert);

        log.info("Alerta {} do paciente {} passou a {}: leitura seguinte voltou ao normal.",
                alert.getId(), patient.getId(), AlertStatus.NOT_CONFIRMED);

        return AlertEvaluationResponse.withUpdatedAlert(
                alert.getSeverity(), alert.getId(), null, reading.getId(),
                AlertStatus.NOT_CONFIRMED);
    }

    /**
     * Leitura fora da faixa normal. Quatro decisões, nesta ordem:
     *
     * <ol>
     *   <li>janela de 4h de um PENDING recente — o médico acabou de ser avisado
     *       deste paciente e tipo, então a leitura é apenas gravada. Vale para os
     *       dois fluxos;</li>
     *   <li>confirmação de um UNCONFIRMED do mesmo tipo, pelas regras de sempre
     *       (a leitura do alerta é a anterior não suspeita, e as duas medições
     *       cabem em 2h). <strong>Independe de a leitura atual ser grave;</strong></li>
     *   <li>não confirmou e a leitura é grave — pergunta à paciente;</li>
     *   <li>não confirmou e não é grave — alerta novo UNCONFIRMED.</li>
     * </ol>
     *
     * <p>A tentativa de confirmação precede a checagem de gravidade de propósito, e
     * é isso que corrige o caminho em que a paciente responde "estou bem". Essa
     * resposta devolve o alerta a UNCONFIRMED; se a leitura grave seguinte abrisse
     * outra pergunta em vez de confirmar, uma sequência de leituras graves com
     * "estou bem" a cada vez nunca chegaria ao médico — cada pergunta descartaria o
     * UNCONFIRMED que a anterior deixou. Confirmando primeiro, a segunda leitura
     * seguida fora da faixa avisa o médico, grave ou não.
     *
     * <p>O UNCONFIRMED que não confirmou não é descartado aqui: ele segue para o
     * passo 3 ou 4, que só o fecham quando um alerta novo efetivamente nasce. Fechar
     * antes deixaria o alerta antigo como NOT_CONFIRMED mesmo nos caminhos que não
     * criam nada — e aí nenhuma leitura seguinte poderia mais confirmá-lo.
     */
    private AlertEvaluationResponse handleAbnormalReading(Patient patient, HealthReading reading,
                                                          ReadingThreshold range, Double value,
                                                          String reason) {
        if (isSilencedByRecentConfirmation(patient, range, reading)) {
            return AlertEvaluationResponse.withoutAlert(reading.getId());
        }

        Optional<Alert> unconfirmed = findLatestAlert(
                patient.getId(), AlertStatus.UNCONFIRMED, range.getReadingType());

        if (unconfirmed.isPresent()) {
            Optional<Alert> confirmed =
                    confirmPendingCandidate(patient, unconfirmed.get(), range, reading);

            if (confirmed.isPresent()) {
                Alert alert = confirmed.get();
                return AlertEvaluationResponse.withUpdatedAlert(
                        alert.getSeverity(), alert.getId(), reason, reading.getId(),
                        AlertStatus.PENDING);
            }
        }

        return range.isSevere(value)
                ? handleSevereReading(patient, reading, range, reason, unconfirmed)
                : createUnconfirmedAlert(patient, reading, range, reason, unconfirmed);
    }

    /**
     * Cria o alerta UNCONFIRMED do fluxo comum, fechando antes o UNCONFIRMED que a
     * leitura atual não confirmou.
     *
     * @param staleUnconfirmed o UNCONFIRMED que não foi confirmado, se havia algum
     */
    private AlertEvaluationResponse createUnconfirmedAlert(Patient patient, HealthReading reading,
                                                           ReadingThreshold range, String reason,
                                                           Optional<Alert> staleUnconfirmed) {
        staleUnconfirmed.ifPresent(alert -> discardUnconfirmed(patient, alert));

        Alert savedAlert = alertRepository.save(
                buildAlert(patient, reading, range, reason, AlertStatus.UNCONFIRMED));

        // Consumido em AFTER_COMMIT: se esta transação sofrer rollback, nenhuma
        // notificação é enviada para um alerta que não existe. Este evento avisa
        // apenas o paciente; o médico só é avisado na confirmação.
        eventPublisher.publishEvent(new AlertCreatedEvent(savedAlert));

        log.info("Alerta {} criado como {} para o paciente {} no tipo {}.",
                savedAlert.getId(), AlertStatus.UNCONFIRMED, patient.getId(), range.getReadingType());

        return AlertEvaluationResponse.withAlert(
                range.getSeverity(), savedAlert.getId(), reason, reading.getId(),
                AlertStatus.UNCONFIRMED);
    }

    /**
     * Leitura plausível e GRAVE. Em vez de esperar a segunda leitura do fluxo comum,
     * o alerta nasce AWAITING_PATIENT e pergunta à paciente se ela está bem — é ela
     * quem tem a informação que o número não dá.
     *
     * <p>Só chega aqui leitura grave que <strong>não</strong> confirmou um
     * UNCONFIRMED: a tentativa de confirmação acontece antes, no chamador, e vale
     * para leitura grave igual à comum. Duas medições seguidas fora da faixa avisam
     * o médico mesmo quando a segunda é grave.
     *
     * <p>Duas saídas antes de criar qualquer coisa:
     * <ul>
     *   <li>já existe AWAITING_PATIENT do mesmo paciente e tipo — a pergunta está
     *       de pé e um segundo push só confundiria;</li>
     *   <li>a janela de 4h de um PENDING recente, verificada pelo chamador, que já
     *       impediu a chegada até aqui.</li>
     * </ul>
     *
     * <p>O prazo de resposta é "agora + 10 minutos", contado pelo {@link Clock}
     * injetado e não pelo horário da medição: o prazo é para a paciente responder,
     * e ela só pode começar a contar quando o push sai. Leitura atrasada pela fila
     * daria um prazo já vencido se usasse {@code measuredAt}.
     *
     * @param staleUnconfirmed o UNCONFIRMED que a leitura atual não confirmou, se
     *        havia algum. Ele é fechado só quando a pergunta nasce de fato: a saída
     *        por AWAITING_PATIENT já aberto não cria nada, e fechá-lo ali tiraria de
     *        uma leitura futura a chance de confirmá-lo
     */
    private AlertEvaluationResponse handleSevereReading(Patient patient, HealthReading reading,
                                                         ReadingThreshold range, String reason,
                                                         Optional<Alert> staleUnconfirmed) {
        Optional<Alert> awaiting = findLatestAlert(
                patient.getId(), AlertStatus.AWAITING_PATIENT, range.getReadingType());

        if (awaiting.isPresent()) {
            log.info("Alerta {} do paciente {} ja aguarda resposta no tipo {}. Leitura gravada sem nova pergunta.",
                    awaiting.get().getId(), patient.getId(), range.getReadingType());
            return AlertEvaluationResponse.withoutAlert(reading.getId());
        }

        // O UNCONFIRMED que não foi confirmado perde a vez: a pergunta à paciente
        // substitui a espera pela segunda leitura, então ninguém mais vai confirmá-lo.
        // Sem isso ele ficaria UNCONFIRMED para sempre, como no fluxo comum.
        staleUnconfirmed.ifPresent(unconfirmed -> discardUnconfirmed(patient, unconfirmed));

        LocalDateTime deadline = LocalDateTime.now(clock).plus(PATIENT_RESPONSE_WINDOW);

        Alert alert = buildAlert(patient, reading, range, reason, AlertStatus.AWAITING_PATIENT);
        alert.setPatientResponseDeadline(deadline);

        Alert savedAlert = alertRepository.save(alert);

        // Mesmo evento de criação do fluxo comum: ele avisa só o paciente. O listener
        // de push distingue os dois pelo status do alerta.
        eventPublisher.publishEvent(new AlertCreatedEvent(savedAlert));

        log.info("Alerta {} criado como {} para o paciente {} no tipo {}. Prazo de resposta de {} min.",
                savedAlert.getId(), AlertStatus.AWAITING_PATIENT, patient.getId(),
                range.getReadingType(), PATIENT_RESPONSE_WINDOW.toMinutes());

        return AlertEvaluationResponse.withAlert(
                range.getSeverity(), savedAlert.getId(), reason, reading.getId(),
                AlertStatus.AWAITING_PATIENT);
    }

    /**
     * Verdadeiro quando existe alerta PENDING do mesmo paciente e tipo confirmado há
     * menos de 4h. Nesse caso a leitura é gravada e nada mais acontece: o médico já
     * foi avisado e não precisa de um segundo e-mail pelo mesmo episódio.
     *
     * <p>PENDING sem {@code confirmedAt} não silencia. Isso cobre os alertas criados
     * antes da V35, que nasciam PENDING direto e nunca tiveram confirmação
     * registrada — tratá-los como silenciadores bloquearia o fluxo indefinidamente,
     * já que a janela nunca venceria.
     */
    private boolean isSilencedByRecentConfirmation(Patient patient, ReadingThreshold range,
                                                   HealthReading reading) {
        Optional<Alert> pending = findLatestAlert(
                patient.getId(), AlertStatus.PENDING, range.getReadingType());

        if (pending.isEmpty()) {
            return false;
        }

        LocalDateTime confirmedAt = pending.get().getConfirmedAt();

        if (confirmedAt == null) {
            log.debug("Alerta {} esta {} sem confirmed_at. Nao silencia a leitura atual.",
                    pending.get().getId(), AlertStatus.PENDING);
            return false;
        }

        boolean withinWindow = Duration.between(confirmedAt, reading.getMeasuredAt())
                .compareTo(RENOTIFY_WINDOW) < 0;

        if (withinWindow) {
            log.info("Alerta {} do paciente {} confirmado ha menos de {}h. Leitura gravada sem novo aviso.",
                    pending.get().getId(), patient.getId(), RENOTIFY_WINDOW.toHours());
        }

        return withinWindow;
    }

    /**
     * Tenta confirmar o alerta UNCONFIRMED do tipo com a leitura atual.
     *
     * <p>Confirma somente se a leitura do alerta for exatamente a leitura anterior
     * (a mais recente não suspeita antes da atual) e a distância entre as duas
     * medições couber na janela de confirmação. Exigir que seja a leitura
     * imediatamente anterior é o que torna "duas leituras seguidas" literal: uma
     * leitura normal no meio quebra a sequência, e uma suspeita não, porque a
     * consulta a ignora.
     *
     * @param alert o alerta UNCONFIRMED candidato, já carregado pelo chamador
     * @return o alerta confirmado, ou vazio quando não houve confirmação
     */
    private Optional<Alert> confirmPendingCandidate(Patient patient, Alert alert,
                                                    ReadingThreshold range,
                                                    HealthReading reading) {
        HealthReading alertReading = alert.getHealthReading();
        Optional<HealthReading> previous = findPreviousTrustedReading(
                patient.getId(), range.getReadingType(), reading.getMeasuredAt());

        if (previous.isEmpty() || alertReading == null
                || !previous.get().getId().equals(alertReading.getId())) {
            log.info("Alerta {} nao foi confirmado: a leitura anterior do tipo {} nao e a do alerta.",
                    alert.getId(), range.getReadingType());
            return Optional.empty();
        }

        Duration gap = Duration.between(alertReading.getMeasuredAt(), reading.getMeasuredAt());

        if (gap.compareTo(CONFIRMATION_WINDOW) > 0) {
            log.info("Alerta {} nao foi confirmado: {} min entre as leituras excedem a janela de {}h.",
                    alert.getId(), gap.toMinutes(), CONFIRMATION_WINDOW.toHours());
            return Optional.empty();
        }

        alert.setStatus(AlertStatus.PENDING);
        alert.setConfirmedAt(reading.getMeasuredAt());
        Alert saved = alertRepository.save(alert);

        // Este é o evento que dispara o e-mail ao médico. Carrega as duas leituras
        // porque o e-mail mostra a progressão, não só o último valor.
        eventPublisher.publishEvent(
                AlertConfirmedEvent.byTwoReadings(saved, alertReading, reading));

        log.info("Alerta {} do paciente {} confirmado por segunda leitura e passou a {}.",
                saved.getId(), patient.getId(), AlertStatus.PENDING);

        return Optional.of(saved);
    }

    /**
     * Fecha como NOT_CONFIRMED o UNCONFIRMED que a leitura atual não confirmou —
     * seja porque as duas medições não couberam na janela de 2h, seja porque a
     * sequência de leituras foi quebrada no meio.
     *
     * <p>Sem isso o alerta antigo ficaria UNCONFIRMED para sempre: a leitura atual
     * abre um alerta novo, que passa a ser o mais recente do tipo, e nenhuma leitura
     * seguinte volta a alcançar o antigo. NOT_CONFIRMED é o mesmo desfecho usado
     * quando a leitura seguinte volta ao normal: o desvio não se repetiu em
     * sequência e ninguém é avisado por ele.
     */
    private void discardUnconfirmed(Patient patient, Alert alert) {
        alert.setStatus(AlertStatus.NOT_CONFIRMED);
        alertRepository.save(alert);

        log.info("Alerta {} do paciente {} passou a {}: nao foi confirmado e um alerta novo o substitui.",
                alert.getId(), patient.getId(), AlertStatus.NOT_CONFIRMED);
    }

    private Optional<Alert> findLatestAlert(UUID patientId, String status, String readingType) {
        return alertRepository
                .findLatestByPatientAndStatusAndReadingType(patientId, status, readingType, LATEST_ONE)
                .stream()
                .findFirst();
    }

    private Optional<HealthReading> findPreviousTrustedReading(UUID patientId, String readingType,
                                                               LocalDateTime before) {
        return healthReadingRepository
                .findPreviousTrustedReadings(patientId, readingType, before, LATEST_ONE)
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertResponse> listRecentForPatient(String email, Pageable pageable) {
        UUID userId = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundByEmail(email)))
                .getId();
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException(
                        "Paciente não encontrado para o usuário autenticado"));

        return alertRepository
                .findByPatientIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        patient.getId(), LocalDateTime.now().minusDays(7), pageable)
                .map(alertMapper::toResponse);
    }

    /**
     * Registra a resposta da paciente a um alerta de leitura grave.
     *
     * <p>O escopo é aplicado por comparação de paciente, não só pela role: a
     * paciente autenticada alcança apenas alerta dela. Alerta de outra paciente é
     * recusado como não autorizado, sem revelar nada sobre o alerta.
     *
     * <p>{@code NOT_OK} leva o alerta a PENDING com {@code confirmedAt} igual ao
     * {@code measuredAt} da leitura grave — e não ao horário da resposta: a
     * referência clínica é quando a medição aconteceu, que é também de onde a janela
     * de 4h é contada. {@code OK} leva a UNCONFIRMED, devolvendo o alerta ao fluxo
     * comum: a próxima leitura fora da faixa, em até 2h, confirma por duas leituras.
     *
     * <p>A troca de status é condicional no banco, então a resposta da paciente e o
     * agendador nunca vencem os dois. Perdida a corrida, o alerta já não está
     * AWAITING_PATIENT e o 409 explica o que aconteceu.
     */
    @Override
    @Transactional
    public PatientAlertAnswerResponse registerPatientResponse(String email, UUID alertId,
                                                              PatientAlertAnswer answer) {
        Patient patient = resolvePatient(email);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException(alertNotFoundById(alertId)));

        if (!alert.getPatient().getId().equals(patient.getId())) {
            log.warn("Paciente {} tentou responder o alerta {}, que e de outro paciente.",
                    patient.getId(), alertId);
            throw new UnauthorizedException("Alerta não pertence ao paciente autenticado");
        }

        if (!AlertStatus.AWAITING_PATIENT.equals(alert.getStatus())) {
            throw closedWindowFor(alert);
        }

        LocalDateTime respondedAt = LocalDateTime.now(clock);
        boolean notOk = answer == PatientAlertAnswer.NOT_OK;
        String targetStatus = notOk ? AlertStatus.PENDING : AlertStatus.UNCONFIRMED;

        // Em NOT_OK o alerta é confirmado, então confirmedAt recebe o horário da
        // medição grave. Em OK ele volta ao fluxo comum e continua não confirmado,
        // então confirmedAt fica nulo — um valor ali faria a janela de 4h silenciar
        // as leituras seguintes de uma paciente que disse estar bem.
        HealthReading severeReading = alert.getHealthReading();
        LocalDateTime confirmedAt =
                notOk && severeReading != null ? severeReading.getMeasuredAt() : null;

        int changed = alertRepository.leaveAwaitingPatient(
                alertId, targetStatus, confirmedAt, answer, respondedAt);

        if (changed == 0) {
            log.info("Resposta do paciente {} ao alerta {} perdeu a corrida com o agendador.",
                    patient.getId(), alertId);
            throw AlertResponseWindowClosedException.deadlineExpired();
        }

        if (notOk) {
            // Só quem venceu a troca de status publica, então o médico recebe no
            // máximo um e-mail por alerta.
            eventPublisher.publishEvent(AlertConfirmedEvent.bySevereReading(
                    alert, severeReading, AlertConfirmationReason.PACIENTE_NAO_ESTA_BEM));

            log.info("Alerta {} do paciente {} passou a {}: paciente respondeu que nao esta bem.",
                    alertId, patient.getId(), AlertStatus.PENDING);
        } else {
            log.info("Alerta {} do paciente {} passou a {}: paciente respondeu que esta bem.",
                    alertId, patient.getId(), AlertStatus.UNCONFIRMED);
        }

        return new PatientAlertAnswerResponse(alertId, answer, respondedAt, targetStatus, notOk);
    }

    /**
     * Ids dos alertas AWAITING_PATIENT cujo prazo já venceu, no instante do
     * {@link Clock} injetado.
     *
     * <p>Devolve ids, e não entidades, porque quem chama é o agendador: ele usa cada
     * id para abrir uma transação própria em
     * {@link #confirmAlertWithoutPatientResponse}. Entidade carregada aqui estaria
     * desanexada lá, e o status lido poderia já estar velho no momento da troca.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UUID> findAlertIdsAwaitingPatientPastDeadline() {
        return alertRepository.findIdsAwaitingPatientPastDeadline(
                LocalDateTime.now(clock), EXPIRED_BATCH);
    }

    /**
     * Confirma um alerta cujo prazo venceu sem resposta da paciente: ele passa a
     * PENDING e o médico é avisado, com {@code confirmedAt} igual ao
     * {@code measuredAt} da leitura grave. O silêncio dela é tratado como motivo de
     * preocupação, não como "está tudo bem".
     *
     * <p>Um alerta por chamada, com transação própria e curta: ela abre, disputa a
     * linha e fecha. É o que faz a falha em um alerta não desfazer a confirmação dos
     * outros do mesmo ciclo, e é o que mantém cada disputa com a resposta da
     * paciente isolada.
     *
     * <p>{@code REQUIRES_NEW} é efetivo porque o agendador chama este método pela
     * interface, atravessando o proxy do Spring. Chamada de dentro desta própria
     * classe não abriria transação nova.
     *
     * @return {@code true} quando esta chamada fez a transição; {@code false} quando
     *         outro caminho chegou primeiro ou o alerta não existe mais — nos dois
     *         casos nada é publicado
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean confirmAlertWithoutPatientResponse(UUID alertId) {
        Alert alert = alertRepository.findById(alertId).orElse(null);

        if (alert == null || !AlertStatus.AWAITING_PATIENT.equals(alert.getStatus())) {
            return false;
        }

        HealthReading severeReading = alert.getHealthReading();
        LocalDateTime confirmedAt =
                severeReading == null ? null : severeReading.getMeasuredAt();

        // A paciente não respondeu, então patient_response e patient_responded_at
        // continuam nulos: eles registram resposta, e não houve resposta.
        int changed = alertRepository.leaveAwaitingPatient(
                alertId, AlertStatus.PENDING, confirmedAt, null, null);

        if (changed == 0) {
            log.info("Alerta {} saiu de {} antes da varredura. Nenhum aviso duplicado.",
                    alertId, AlertStatus.AWAITING_PATIENT);
            return false;
        }

        eventPublisher.publishEvent(AlertConfirmedEvent.bySevereReading(
                alert, severeReading, AlertConfirmationReason.SEM_RESPOSTA));

        log.info("Alerta {} passou a {}: prazo de {} min venceu sem resposta do paciente.",
                alertId, AlertStatus.PENDING, PATIENT_RESPONSE_WINDOW.toMinutes());

        return true;
    }

    /**
     * Distingue os dois desfechos de janela fechada, para a paciente entender por que
     * a resposta não foi aceita. Resposta já gravada é um reenvio do app; sem
     * resposta gravada, o prazo venceu e o agendador já avisou o médico.
     */
    private AlertResponseWindowClosedException closedWindowFor(Alert alert) {
        if (alert.getPatientResponse() != null) {
            log.info("Alerta {} ja tinha resposta registrada. Nova resposta recusada.", alert.getId());
            return AlertResponseWindowClosedException.alreadyAnswered();
        }

        log.info("Alerta {} nao esta mais em {}. Resposta recusada.",
                alert.getId(), AlertStatus.AWAITING_PATIENT);
        return AlertResponseWindowClosedException.deadlineExpired();
    }

    /** Mesmo caminho de {@link #resolveDoctor}: usuário ativo, depois paciente. */
    private Patient resolvePatient(String email) {
        UUID userId = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundByEmail(email)))
                .getId();

        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException(
                        "Paciente não encontrado para o usuário autenticado"));
    }

    /**
     * Marca o alerta como RESOLVED.
     *
     * <p>O escopo é aplicado na consulta de vínculo, não só pela role: o médico
     * alcança apenas alerta de paciente vinculado a ele.
     */
    @Override
    @Transactional
    public AlertResponse resolveAlert(String email, UUID alertId) {
        Doctor doctor = resolveDoctor(email);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException(alertNotFoundById(alertId)));

        if (!doctorPatientRepository.existsByDoctorIdAndPatientId(
                doctor.getId(), alert.getPatient().getId())) {
            throw new UnauthorizedException(ErrorMessages.patientNotLinkedToDoctor());
        }

        // Alerta já resolvido: operação idempotente. Devolve o estado atual sem
        // gravar de novo, para não sobrescrever o registro de quem resolveu antes.
        if (AlertStatus.RESOLVED.equals(alert.getStatus())) {
            log.info("Alerta {} ja estava {}. Nada alterado.", alertId, AlertStatus.RESOLVED);
            return alertMapper.toResponse(alert);
        }

        alert.setStatus(AlertStatus.RESOLVED);
        Alert saved = alertRepository.save(alert);

        log.info("Alerta {} marcado como {} pelo medico {}.",
                alertId, AlertStatus.RESOLVED, doctor.getId());

        return alertMapper.toResponse(saved);
    }

    /**
     * Converte o horário informado para UTC, que é como a coluna
     * {@code measured_at} guarda o valor.
     *
     * <p>É isso que faz {@code 14:32Z} e {@code 11:32-03:00} virarem a mesma
     * leitura: os dois descrevem o mesmo instante, e depois da normalização
     * produzem o mesmo {@code LocalDateTime}. A conversão não consulta o fuso da
     * JVM — o deslocamento vem do próprio valor recebido.
     */
    private LocalDateTime toUtc(OffsetDateTime measuredAt) {
        return measuredAt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /**
     * Persiste a leitura recebida, com {@code measuredAt} já normalizado em UTC. O
     * dispositivo fica nulo: o contrato da integração não informa dispositivo, e a
     * coluna passou a aceitar nulo na V34.
     *
     * <p>A verificação de idempotência acontece antes desta chamada, mas duas
     * requisições idênticas em paralelo passam as duas por lá. A restrição UNIQUE da
     * V33 barra a segunda, e o flush explícito faz a violação aparecer aqui, onde
     * ainda é possível traduzi-la em {@link AlertDuplicateReadingException} — leitura
     * repetida é sucesso para quem chama, não erro.
     */
    private HealthReading saveReading(Patient patient, AlertEvaluationRequest request,
                                     LocalDateTime measuredAtUtc, boolean suspect) {
        HealthReading reading = new HealthReading();
        reading.setPatient(patient);
        reading.setPatientDevice(null);
        reading.setReadingType(request.readingType());
        reading.setValue(String.valueOf(request.value()));
        reading.setUnit(request.unit());
        reading.setMeasuredAt(measuredAtUtc);
        reading.setSuspect(suspect);

        try {
            return healthReadingRepository.saveAndFlush(reading);
        } catch (DataIntegrityViolationException e) {
            log.info("Leitura concorrente do paciente {} no tipo {} barrada pela restricao de unicidade.",
                    patient.getId(), request.readingType());
            throw new AlertDuplicateReadingException(e);
        }
    }

    /**
     * Compara o valor com a faixa normal e descreve o desvio encontrado.
     * Retorna null quando o valor está dentro da faixa.
     *
     * <p>Os limites são inclusivos no normal: a comparação usa estritamente menor e
     * estritamente maior, então valor igual a um dos limites não gera alerta.
     */
    private String buildReason(Double value, ReadingThreshold range) {
        Double min = range.getNormalMin();
        Double max = range.getNormalMax();

        if (min != null && value < min) {
            return "Valor abaixo do mínimo normal de " + min + " para " + range.getReadingType();
        }
        if (max != null && value > max) {
            return "Valor acima do máximo normal de " + max + " para " + range.getReadingType();
        }
        return null;
    }

    /**
     * Descreve por que a leitura foi considerada impossível. Vai no campo
     * {@code reason} da resposta, para quem chama saber que a leitura foi descartada
     * por implausibilidade e não por estar dentro do normal.
     */
    private String buildImplausibleReason(ReadingThreshold range) {
        return "Valor fora da faixa plausível de " + range.getReadingType()
                + " (" + range.getPlausibleMin() + " a " + range.getPlausibleMax()
                + "). Leitura registrada como suspeita e não avaliada.";
    }

    /**
     * Monta o alerta reaproveitando o AlertMapper já existente, para não duplicar a
     * atribuição de campos da entidade. A leitura que originou o alerta vai no
     * relacionamento, então health_reading_id fica preenchido.
     *
     * <p>O status vem do chamador: {@code UNCONFIRMED} no fluxo comum, que só vira
     * PENDING quando uma segunda leitura seguida confirmar o desvio, e
     * {@code AWAITING_PATIENT} na leitura grave, que espera a resposta da paciente.
     */
    private Alert buildAlert(Patient patient, HealthReading reading,
                             ReadingThreshold range, String reason, String status) {
        AlertRequest alertRequest = new AlertRequest(
                patient.getId(),
                reading.getId(),
                range.getSeverity(),
                buildTitle(range),
                reason,
                status
        );
        return alertMapper.toEntity(alertRequest, patient, reading);
    }

    private String buildTitle(ReadingThreshold range) {
        String label = range.getLabel();
        String title = "Leitura fora da faixa normal: " + range.getReadingType()
                + (label != null ? " (" + label + ")" : "");
        return title.length() > 255 ? title.substring(0, 255) : title;
    }

    /** Mesmo caminho usado pelos outros services: usuário ativo, depois médico. */
    private Doctor resolveDoctor(String email) {
        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundByEmail(email)));

        return doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException(ErrorMessages.doctorProfileNotFound()));
    }

    private static String alertNotFoundById(UUID id) {
        return "Alerta não encontrado com ID: " + id;
    }
}
