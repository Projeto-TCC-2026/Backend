---
inclusion: always
---

# Tecnologia

## Stack

Versões exatas estão em `#[[file:pom.xml]]` — essa é a fonte de verdade. Não fixe número de versão neste arquivo.

- Java 17, Spring Boot 4.x, build com Maven Wrapper.
- Spring MVC para REST; Spring Data JPA/Hibernate para persistência.
- PostgreSQL em produção e no Docker Compose; H2 em memória no perfil `dev`.
- Flyway para migrations, com `ddl-auto=none` (Hibernate não gera schema).
- Spring Security com JJWT para autenticação e autorização via JWT.
- Jakarta Bean Validation, springdoc OpenAPI/Swagger UI, Spring Boot Actuator.
- JUnit 5, Mockito e AssertJ via `spring-boot-starter-test`.
- Imagem Docker multi-stage sobre Eclipse Temurin 17; GitHub Actions roda o ciclo `package` do Maven.

## Comandos

Use sempre o wrapper do repositório, nunca um Maven instalado na máquina.

```bash
# Git Bash / Linux / macOS
./mvnw test
./mvnw clean package
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw -Dtest=HospitalServiceImplTest test
docker compose up --build
```

No Windows (PowerShell ou CMD), troque `./mvnw` por `.\mvnw.cmd`.

- Aplicação: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- O Compose expõe o PostgreSQL na porta `5434` do host.

## Regras de engenharia

### Migrations

- Toda mudança de schema é um arquivo **novo**. Nunca edite uma migration já aplicada. Nem para corrigir typo. Crie outra.
- Nome: `V<n>__create_<tabela>_table.sql` para criação, `V<n>__<verbo>_<alvo>.sql` para alteração. Tabela no plural.
- Antes de escolher `<n>`, liste `src/main/resources/db/migration/` e use o próximo número livre. Não deduza pelo que está no contexto.
- As migrations rodam em PostgreSQL e em H2 (perfil `dev`). Prefira SQL padrão; se precisar de recurso específico do PostgreSQL, confirme que o H2 aceita, senão o perfil de desenvolvimento quebra.
- Migration não insere usuário, senha nem token. Ela roda em todos os ambientes.

### Segredos

- Senha de infraestrutura, token estático de integração, chave de API e chave de assinatura do JWT vêm de variável de ambiente, em **todos** os perfis. Nunca com valor literal em `application*.properties`, em código, em migration ou em arquivo de steering.
- Token de autenticação emitido pela aplicação (access e refresh) é gerado em tempo de execução. Nunca venha de configuração nem de variável de ambiente.
- Identificador não secreto — como o usuário padrão do H2 — pode ficar na configuração de desenvolvimento.
- Dado local de desenvolvimento entra por script não versionado. Não use endpoint, migration Flyway nem inicialização automática da aplicação para criar usuário ou credencial. Se versionar um script de exemplo, ele não pode ter senha funcional.

### Endpoints de conveniência

Não crie endpoint para facilitar teste manual — gerador de token, atalho de login, rota de seed, bypass de autenticação. Nem sob `@Profile`. Para testar à mão, use o Swagger UI com o fluxo de login real.

### Injeção de dependência

- Dependência entre componentes: sempre por construtor. Proibido `@Autowired` em campo.
- Propriedade de configuração: `@Value` no parâmetro do construtor, não em campo.

### Transações

- Use sempre `org.springframework.transaction.annotation.Transactional`. Nunca `jakarta.transaction.Transactional` — ela não tem `readOnly`.
- Método de escrita: `@Transactional`.
- Método de leitura: `@Transactional(readOnly = true)`. Inclui método que só consulta.
- A anotação fica nos **métodos** da classe `*Impl` do service. Nunca no controller, nunca no repository.

### Data e hora

O projeto usa `LocalDateTime` na aplicação e `TIMESTAMP` sem fuso no banco, e não define timezone em nenhum ambiente. Não crie coluna nova de data/hora sem alinhar antes — se for pedido, avise o impacto.

### Mappers

- São classes `@Component` escritas à mão.
- MapStruct **não** é dependência do projeto. Não gere código que dependa dele.

### Testes

- Teste de service com Mockito e AssertJ, um cenário por teste.
- Use `@Nested` e `@DisplayName` quando melhorar a leitura do cenário.
- Rode teste direcionado durante o desenvolvimento e `./mvnw clean package` antes de fechar mudança ampla.
