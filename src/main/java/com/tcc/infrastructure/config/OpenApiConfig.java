package com.tcc.infrastructure.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    // URL de produção opcional — se definida em tcc.openapi.prod-url aparece como
    // servidor extra no seletor do Swagger UI.
    @Value("${tcc.openapi.prod-url:}")
    private String prodUrl;

    @Bean
    public OpenAPI myOpenAPI() {
        // URL relativa "/" faz o Swagger UI usar o mesmo host/porta de onde a página
        // foi carregada, funcionando tanto em localhost quanto em qualquer IP/domínio.
        Server defaultServer = new Server();
        defaultServer.setUrl("/");
        defaultServer.setDescription("Servidor atual");

        Contact contact = new Contact();
        contact.setEmail("contato@tcc.com");
        contact.setName("Equipe TCC");
        contact.setUrl("https://github.com/Projeto-TCC-2026");

        License mitLicense = new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("TCC 2026 - Backend API")
                .version("1.0")
                .contact(contact)
                .description("Esta é a documentação da API do projeto TCC 2026. " +
                           "A API fornece endpoints para gerenciar todas as funcionalidades do sistema. " +
                           "Para testar endpoints protegidos: faça login em /auth/doctor/login ou /auth/patient/login, " +
                           "copie o accessToken e clique em 'Authorize' inserindo: Bearer {token}")
                .license(mitLicense);

        // Configuração do esquema de segurança JWT
        SecurityScheme securityScheme = new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Insira o token JWT obtido no login. Exemplo: Bearer eyJhbGci...");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Authentication");

        OpenAPI openAPI = new OpenAPI()
                .info(info)
                .servers(List.of(defaultServer))
                .addSecurityItem(securityRequirement)
                .schemaRequirement("Bearer Authentication", securityScheme);

        // Adiciona servidor de produção como opção extra se configurado via propriedade
        if (prodUrl != null && !prodUrl.isEmpty()) {
            Server prodServer = new Server();
            prodServer.setUrl(prodUrl);
            prodServer.setDescription("Servidor de Produção");
            openAPI.getServers().add(prodServer);
        }

        return openAPI;
    }
}