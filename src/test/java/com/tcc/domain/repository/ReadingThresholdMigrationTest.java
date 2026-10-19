package com.tcc.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.tcc.domain.model.ReadingThreshold;

/**
 * Lê reading_thresholds no H2 depois de todas as migrations e confere as faixas que o
 * AlertServiceImpl vai usar.
 *
 * <p>Diferente do AlertServiceImplTest, que monta a faixa no próprio teste, aqui os
 * valores vêm do banco: é o único teste que pega uma migration que deixou de aplicar
 * ou que alterou coluna além da pretendida.
 *
 * <p>Mesmo perfil e mesmas propriedades do TccApplicationTests, para o Spring
 * reaproveitar o contexto em cache em vez de subir outro.
 */
@ActiveProfiles("dev")
@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=dGVzdC1qd3Qtc2VjcmV0LXBhcmEtY2FycmVnYXItY29udGV4dG8tMTIzNDU2Nzg5MA==",
        "app.password-reset.frontend-base-url=http://localhost:4200",
        "app.account-activation.frontend-base-url=http://localhost:4200",
        "app.password-reset.sqs-enabled=false",
        "app.account-activation.sqs-enabled=false",
        "app.password-reset.aws-region=us-east-1",
        "app.dashboard-cache.aws-region=us-east-1",
        "app.dashboard-cache.s3-bucket=fake-test-bucket"
})
@DisplayName("Faixas de reading_thresholds após as migrations")
class ReadingThresholdMigrationTest {

    private final ReadingThresholdRepository readingThresholdRepository;

    @Autowired
    ReadingThresholdMigrationTest(ReadingThresholdRepository readingThresholdRepository) {
        this.readingThresholdRepository = readingThresholdRepository;
    }

    private ReadingThreshold thresholdOf(String readingType) {
        return readingThresholdRepository.findByReadingType(readingType).orElseThrow();
    }

    @Test
    @DisplayName("SPO2: normal a partir de 91 (V39), sem máximo; grave e plausível inalterados")
    void spo2RangesAfterV39() {
        ReadingThreshold spo2 = thresholdOf("SPO2");

        assertThat(spo2.getNormalMin()).isEqualTo(91.0);
        assertThat(spo2.getNormalMax()).isNull();
        assertThat(spo2.getSevereMin()).isEqualTo(84.0);
        assertThat(spo2.getSevereMax()).isNull();
        assertThat(spo2.getPlausibleMin()).isEqualTo(50.0);
        assertThat(spo2.getPlausibleMax()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("TEMPERATURE: normal de 36.2 (V39) a 38.0; grave e plausível inalterados")
    void temperatureRangesAfterV39() {
        ReadingThreshold temperature = thresholdOf("TEMPERATURE");

        assertThat(temperature.getNormalMin()).isEqualTo(36.2);
        assertThat(temperature.getNormalMax()).isEqualTo(38.0);
        assertThat(temperature.getSevereMin()).isEqualTo(35.0);
        assertThat(temperature.getSevereMax()).isNull();
        assertThat(temperature.getPlausibleMin()).isEqualTo(30.0);
        assertThat(temperature.getPlausibleMax()).isEqualTo(45.0);
    }

    @Test
    @DisplayName("HEART_RATE não é tocado pela V39")
    void heartRateUnchanged() {
        ReadingThreshold heartRate = thresholdOf("HEART_RATE");

        assertThat(heartRate.getNormalMin()).isEqualTo(50.0);
        assertThat(heartRate.getNormalMax()).isEqualTo(120.0);
        assertThat(heartRate.getSevereMin()).isEqualTo(40.0);
        assertThat(heartRate.getSevereMax()).isEqualTo(131.0);
        assertThat(heartRate.getPlausibleMin()).isEqualTo(25.0);
        assertThat(heartRate.getPlausibleMax()).isEqualTo(250.0);
    }

    @Test
    @DisplayName("labels de SPO2 e TEMPERATURE citam os limites da V39")
    void labelsUpdatedByV39() {
        assertThat(thresholdOf("SPO2").getLabel())
                .isEqualTo("Saturação de oxigênio normal a partir de 91 por cento");
        assertThat(thresholdOf("TEMPERATURE").getLabel())
                .isEqualTo("Temperatura corporal normal entre 36,2 e 38,0 graus Celsius");
    }

    @Test
    @DisplayName("severity continua CRITICAL nos três tipos e nenhuma linha foi criada")
    void severityAndRowCountUnchanged() {
        assertThat(readingThresholdRepository.count()).isEqualTo(3);
        assertThat(thresholdOf("SPO2").getSeverity()).isEqualTo("CRITICAL");
        assertThat(thresholdOf("TEMPERATURE").getSeverity()).isEqualTo("CRITICAL");
        assertThat(thresholdOf("HEART_RATE").getSeverity()).isEqualTo("CRITICAL");
    }
}
