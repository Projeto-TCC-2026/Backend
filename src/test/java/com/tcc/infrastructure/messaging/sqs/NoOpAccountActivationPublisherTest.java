package com.tcc.infrastructure.messaging.sqs;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NoOpAccountActivationPublisherTest {

    @Test
    void shouldFailWhenActivationEmailCannotBePublished() {
        NoOpAccountActivationPublisher publisher = new NoOpAccountActivationPublisher();

        assertThatThrownBy(() ->
                publisher.publishAccountCreated(
                        "patient@example.test",
                        "Test Patient",
                        "activation-token",
                        "https://example.test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("desabilitado");
    }
}
