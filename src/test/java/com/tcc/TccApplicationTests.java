package com.tcc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.tcc.domain.model.AlertStatus;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.PatientRepository;

/**
 * Sobe o contexto completo no perfil {@code dev}.
 *
 * <p>Os placeholders sem valor padrão em {@code application.properties} e
 * {@code application-dev.properties} são intencionais: em produção a aplicação
 * deve falhar ao subir se a variável de ambiente faltar. Para que
 * {@code ./mvnw clean package} rode sem nenhuma variável de ambiente (como no
 * GitHub Actions), este teste injeta valores falsos apenas no contexto de teste.
 *
 * <p>Nenhum valor aqui alcança a AWS. As duas flags {@code sqs-enabled} ficam
 * em {@code false}, então entram os publishers NoOp e nenhuma mensagem é
 * publicada. Os beans {@code SqsClient} e {@code S3Client} são criados porque
 * as suas {@code @Configuration} não são condicionais, mas o builder do SDK só
 * resolve credencial e endpoint na primeira chamada de API — que não acontece
 * durante o carregamento do contexto.
 */
@ActiveProfiles("dev")
@SpringBootTest
@TestPropertySource(properties = {
        // JWT_SECRET: chave falsa, válida só dentro do teste.
        "jwt.secret=dGVzdC1qd3Qtc2VjcmV0LXBhcmEtY2FycmVnYXItY29udGV4dG8tMTIzNDU2Nzg5MA==",

        // FRONTEND_BASE_URL: base dos links de reset de senha e de ativação.
        "app.password-reset.frontend-base-url=http://localhost:4200",
        "app.account-activation.frontend-base-url=http://localhost:4200",

        // SQS desligada nos dois fluxos: garante os publishers NoOp.
        "app.password-reset.sqs-enabled=false",
        "app.account-activation.sqs-enabled=false",

        // AWS_REGION: lida pelos beans SqsClient e S3Client, que não são
        // condicionais. Região falsa, sem chamada de API no teste.
        "app.password-reset.aws-region=us-east-1",
        "app.dashboard-cache.aws-region=us-east-1",

        // DASHBOARD_CACHE_S3_BUCKET: bucket falso; só o job agendado o usaria.
        "app.dashboard-cache.s3-bucket=fake-test-bucket"
})
class TccApplicationTests {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private AlertRepository alertRepository;

	@Test
	void contextLoads() {
	}

    /**
     * Executa de fato a consulta de alertas do médico, contra o H2 com as migrations
     * aplicadas.
     *
     * <p>Subir o contexto já valida o JPQL, mas não a execução: esta consulta tem
     * subconsulta EXISTS, JOIN FETCH e uma {@code countQuery} escrita à mão, e um erro
     * em qualquer um dos três só aparece quando o SQL roda. Como a página é paginada,
     * a chamada dispara as duas consultas, principal e de contagem.
     */
    @Test
    void doctorAlertQueryRunsAgainstTheSchema() {
        var result = alertRepository.findForDoctor(
                UUID.randomUUID(),
                List.of(AlertStatus.PENDING, AlertStatus.AWAITING_PATIENT, AlertStatus.RESOLVED),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    /** Mesma razão: valida a execução do filtro de status da planilha de alertas. */
    @Test
    void alertReportQueryRunsWithStatusFilter() {
        var result = alertRepository.findForReport(
                LocalDate.of(2026, 8, 5).atStartOfDay(),
                LocalDate.of(2026, 8, 6).atStartOfDay(),
                List.of(AlertStatus.PENDING, AlertStatus.RESOLVED));

        assertThat(result).isEmpty();
    }

    @Test
    void patientStatusFilterAcceptsInactiveStatusWithoutTextFilters() {
        var result = patientRepository.findVisible(
                null, null, null, null, null, null, null, null, null, false, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void hospitalStatusFilterAcceptsAllStatusesWithoutTextFilters() {
        var result = hospitalRepository.findByFilters(null, null, null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
    }

    @Test
    void hospitalStatusFilterAcceptsUnsetOptionalTextFilters() {
        var result = hospitalRepository.findByFiltersAndActive(
                "Central", null, null, true, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
    }

}
