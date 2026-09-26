package com.tcc.presentation.controller;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.tcc.application.dto.request.PatientUpdateRequest;
import com.tcc.application.dto.response.PatientResponse;
import com.tcc.application.service.PatientService;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.UserRepository;
import com.tcc.infrastructure.security.JwtService;

/**
 * Matriz de permissão de {@code /api/patients/**}.
 *
 * <p>O CRUD de pacientes é exclusivo de {@code DOCTOR} e {@code HOSPITAL}. {@code ADMIN}
 * e {@code PATIENT} não acessam. Este teste prova a matriz chamando os endpoints reais,
 * atravessando a cadeia de filtros de verdade do {@code SecurityConfig} — inclusive o
 * {@code JwtAuthFilter} e o {@code @PreAuthorize} do controller.
 *
 * <p>Sobre a abordagem: o projeto está em Spring Boot 4.1, onde {@code @WebMvcTest} e
 * {@code @AutoConfigureMockMvc} saíram para o módulo {@code spring-boot-webmvc-test}, e
 * {@code @WithMockUser} exige {@code spring-security-test}. Nenhum dos dois está no
 * classpath, e a tarefa pede para não adicionar dependência sem necessidade. Então o
 * {@code MockMvc} é montado à mão a partir do {@code FilterChainProxy} já existente no
 * contexto, usando só o que o {@code spring-test} oferece. O papel de cada requisição
 * vem de um token opaco resolvido pelo {@code JwtService} mockado, e o usuário
 * correspondente vem do {@code UserRepository} mockado — ou seja, a role percorre o
 * mesmo caminho de produção ({@code UserDetailsServiceImpl} monta {@code ROLE_<role>}).
 *
 * <p>Não há banco: {@code PatientService} é mockado, então um 200 só acontece se a
 * autorização liberar a chamada.
 */
@ActiveProfiles("dev")
@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=dGVzdC1qd3Qtc2VjcmV0LXBhcmEtbWF0cml6LWRlLXBlcm1pc3Nhby0xMjM0NTY3ODkw",
        "app.password-reset.frontend-base-url=http://localhost:4200",
        "app.account-activation.frontend-base-url=http://localhost:4200",
        "app.password-reset.sqs-enabled=false",
        "app.account-activation.sqs-enabled=false",
        "app.password-reset.aws-region=us-east-1",
        "app.dashboard-cache.aws-region=us-east-1",
        "app.dashboard-cache.s3-bucket=fake-test-bucket"
})
@DisplayName("Matriz de permissão de /api/patients/**")
class PatientControllerSecurityTest {

    private static final UUID PATIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    /** Token opaco por perfil: o JwtService mockado traduz cada um no e-mail do usuário. */
    private static final String TOKEN_ADMIN = "token-admin";
    private static final String TOKEN_DOCTOR = "token-doctor";
    private static final String TOKEN_PATIENT = "token-patient";
    private static final String TOKEN_HOSPITAL = "token-hospital";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    /** Mockado: a matriz é sobre autorização, não sobre regra de negócio nem banco. */
    @MockitoBean
    private PatientService patientService;

    /** Mockado para resolver o usuário de cada token sem depender do banco. */
    @MockitoBean
    private UserRepository userRepository;

    /** Mockado para que o token opaco do teste dispense assinatura real. */
    @MockitoBean
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        when(jwtService.isTokenValid(anyString())).thenReturn(true);
        stubToken(TOKEN_ADMIN, "admin@tcc.local", Role.ADMIN);
        stubToken(TOKEN_DOCTOR, "doctor@tcc.local", Role.DOCTOR);
        stubToken(TOKEN_PATIENT, "patient@tcc.local", Role.PATIENT);
        stubToken(TOKEN_HOSPITAL, "hospital@tcc.local", Role.HOSPITAL);

        Page<PatientResponse> emptyPage = new PageImpl<>(List.of(), Pageable.ofSize(10), 0);
        when(patientService.getAllActivePatients(anyString(), any())).thenReturn(emptyPage);
        when(patientService.searchByCpf(anyString(), anyString(), any())).thenReturn(emptyPage);
        when(patientService.updatePatient(anyString(), eq(PATIENT_ID), any()))
                .thenReturn(null);
    }

    /** Liga o token ao usuário, do jeito que o JwtAuthFilter + UserDetailsServiceImpl fazem. */
    private void stubToken(String token, String email, Role role) {
        when(jwtService.extractUsername(token)).thenReturn(email);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash("irrelevante-para-autorizacao");
        user.setRole(role);
        user.setActive(true);

        when(userRepository.findByEmailAndActiveTrue(email)).thenReturn(Optional.of(user));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /**
     * Corpo válido para o PUT, escrito à mão. O JSON é literal de propósito: serializar
     * o record exigiria o módulo JSR-310 do Jackson para o {@code LocalDate}, e o objetivo
     * aqui é autorização, não serialização. Os campos atendem a validação de
     * {@link PatientUpdateRequest} para que um 400 não se disfarce de resultado da matriz.
     */
    private String updateBody() {
        return """
                {
                  "fullName": "Paciente de Teste",
                  "cpf": "12345678901",
                  "birthDate": "1990-01-01",
                  "gender": "MALE",
                  "phone": "11999999999",
                  "email": "paciente.teste@tcc.local",
                  "address": "Rua Teste, 100",
                  "city": "Sao Paulo",
                  "state": "SP",
                  "zipCode": "01000000",
                  "bloodType": "O+",
                  "weight": 70.0,
                  "height": 1.75
                }
                """;
    }

    @Nested
    @DisplayName("GET /api/patients (listagem)")
    class ListPatients {

        @Test
        @DisplayName("sem autenticação retorna 401")
        void semAutenticacaoRetorna401() throws Exception {
            mockMvc.perform(get("/api/patients"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("ADMIN retorna 403")
        void adminRetorna403() throws Exception {
            mockMvc.perform(get("/api/patients").header("Authorization", bearer(TOKEN_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PATIENT retorna 403")
        void patientRetorna403() throws Exception {
            mockMvc.perform(get("/api/patients").header("Authorization", bearer(TOKEN_PATIENT)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DOCTOR retorna 200")
        void doctorRetorna200() throws Exception {
            mockMvc.perform(get("/api/patients").header("Authorization", bearer(TOKEN_DOCTOR)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HOSPITAL retorna 200")
        void hospitalRetorna200() throws Exception {
            mockMvc.perform(get("/api/patients").header("Authorization", bearer(TOKEN_HOSPITAL)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/patients/search/cpf (busca por CPF)")
    class SearchByCpf {

        @Test
        @DisplayName("sem autenticação retorna 401")
        void semAutenticacaoRetorna401() throws Exception {
            mockMvc.perform(get("/api/patients/search/cpf").param("cpf", "123"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("ADMIN retorna 403")
        void adminRetorna403() throws Exception {
            mockMvc.perform(get("/api/patients/search/cpf")
                            .param("cpf", "123")
                            .header("Authorization", bearer(TOKEN_ADMIN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PATIENT retorna 403")
        void patientRetorna403() throws Exception {
            mockMvc.perform(get("/api/patients/search/cpf")
                            .param("cpf", "123")
                            .header("Authorization", bearer(TOKEN_PATIENT)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DOCTOR retorna 200")
        void doctorRetorna200() throws Exception {
            mockMvc.perform(get("/api/patients/search/cpf")
                            .param("cpf", "123")
                            .header("Authorization", bearer(TOKEN_DOCTOR)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HOSPITAL retorna 200")
        void hospitalRetorna200() throws Exception {
            mockMvc.perform(get("/api/patients/search/cpf")
                            .param("cpf", "123")
                            .header("Authorization", bearer(TOKEN_HOSPITAL)))
                    .andExpect(status().isOk());
        }
    }

    /**
     * A matriz por HTTP acima é atendida por duas camadas independentes: o matcher de
     * {@code /api/patients/**} no {@code SecurityConfig} e o {@code @PreAuthorize} de cada
     * método do controller. Como o matcher nega antes, ele mascara um {@code @PreAuthorize}
     * que volte a aceitar {@code ADMIN} — o teste por HTTP continuaria verde.
     *
     * <p>Esta verificação fecha essa brecha olhando direto para a anotação de cada método
     * público do controller, garantindo que a segunda camada também exclui {@code ADMIN}.
     */
    @Nested
    @DisplayName("@PreAuthorize do controller (segunda camada)")
    class PreAuthorizeExpressions {

        @Test
        @DisplayName("nenhum endpoint aceita ADMIN")
        void nenhumEndpointAceitaAdmin() {
            List<String> comAdmin = new ArrayList<>();

            for (Method method : PatientController.class.getDeclaredMethods()) {
                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                if (annotation != null && annotation.value().contains("ADMIN")) {
                    comAdmin.add(method.getName() + " -> " + annotation.value());
                }
            }

            assertThat(comAdmin)
                    .as("endpoints de /api/patients/** que ainda aceitam ADMIN")
                    .isEmpty();
        }

        @Test
        @DisplayName("todo endpoint exige DOCTOR ou HOSPITAL explicitamente")
        void todoEndpointExigeDoctorOuHospital() {
            List<String> semPreAuthorize = new ArrayList<>();
            int anotados = 0;

            for (Method method : PatientController.class.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) {
                    continue;
                }

                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

                if (annotation == null) {
                    semPreAuthorize.add(method.getName());
                    continue;
                }

                anotados++;
                assertThat(annotation.value())
                        .as("expressão de %s", method.getName())
                        .contains("DOCTOR")
                        .contains("HOSPITAL");
            }

            assertThat(semPreAuthorize)
                    .as("endpoints públicos sem @PreAuthorize")
                    .isEmpty();
            assertThat(anotados)
                    .as("quantidade de endpoints protegidos no PatientController")
                    .isEqualTo(13);
        }
    }

    @Nested
    @DisplayName("PUT /api/patients/{id} (atualização)")
    class UpdatePatient {

        @Test
        @DisplayName("sem autenticação retorna 401")
        void semAutenticacaoRetorna401() throws Exception {
            mockMvc.perform(put("/api/patients/{id}", PATIENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("ADMIN retorna 403")
        void adminRetorna403() throws Exception {
            mockMvc.perform(put("/api/patients/{id}", PATIENT_ID)
                            .header("Authorization", bearer(TOKEN_ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PATIENT retorna 403")
        void patientRetorna403() throws Exception {
            mockMvc.perform(put("/api/patients/{id}", PATIENT_ID)
                            .header("Authorization", bearer(TOKEN_PATIENT))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DOCTOR retorna 200")
        void doctorRetorna200() throws Exception {
            mockMvc.perform(put("/api/patients/{id}", PATIENT_ID)
                            .header("Authorization", bearer(TOKEN_DOCTOR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HOSPITAL retorna 200")
        void hospitalRetorna200() throws Exception {
            mockMvc.perform(put("/api/patients/{id}", PATIENT_ID)
                            .header("Authorization", bearer(TOKEN_HOSPITAL))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody()))
                    .andExpect(status().isOk());
        }
    }
}
