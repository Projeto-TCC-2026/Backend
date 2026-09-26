package com.tcc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

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

	@Test
	void contextLoads() {
	}

}
