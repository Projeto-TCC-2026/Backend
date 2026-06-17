package com.tcc.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${tcc.openapi.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${tcc.openapi.prod-url:}")
    private String prodUrl;

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Servidor de Desenvolvimento");

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
                           "A API fornece endpoints para gerenciar todas as funcionalidades do sistema.")
                .license(mitLicense);

        OpenAPI openAPI = new OpenAPI().info(info).servers(List.of(devServer));

        // Adiciona servidor de produção se configurado
        if (prodUrl != null && !prodUrl.isEmpty()) {
            Server prodServer = new Server();
            prodServer.setUrl(prodUrl);
            prodServer.setDescription("Servidor de Produção");
            openAPI.servers(List.of(devServer, prodServer));
        }

        return openAPI;
    }
}