# TCC Saúde - Backend

Backend do projeto de extensão TCC Saúde, desenvolvido com Spring Boot em uma arquitetura em camadas, inspirada em princípios de separação de responsabilidades e DDD simplificado.

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.6 |
| Spring Data JPA | incluso no starter |
| H2 Database | runtime (dev) |
| Maven Wrapper | incluso |

### Planejadas

- Spring Security + JWT
- PostgreSQL (produção)
- Flyway (versionamento de banco)
- MapStruct (mapeamento DTO ↔ Entity)
- Swagger/OpenAPI (documentação)
- Docker

## Pré-requisitos

- Java 17+ instalado
- `JAVA_HOME` configurado

## Como rodar

```bash
# Na raiz do projeto
.\mvnw.cmd spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

### Compilar sem rodar

```bash
.\mvnw.cmd clean package
```

### Rodar o JAR gerado

```bash
java -jar target\tcc-0.0.1-SNAPSHOT.jar
```

## Documentação Adicional

A documentação detalhada do projeto está organizada na pasta [`docs/`](docs/):

- [`docs/ENTITIES.md`](docs/ENTITIES.md) — Documentação das entidades
- [`docs/ENTITY_DIAGRAM.md`](docs/ENTITY_DIAGRAM.md) — Diagrama de relacionamentos
- [`docs/PROJECT_STRUCTURE.md`](docs/PROJECT_STRUCTURE.md) — Estrutura detalhada do projeto
- [`docs/DOCTOR_CRUD_IMPLEMENTATION.md`](docs/DOCTOR_CRUD_IMPLEMENTATION.md) — Implementação do CRUD de Doctors
- [`docs/IMPLEMENTATION_SUMMARY.md`](docs/IMPLEMENTATION_SUMMARY.md) — Resumo da implementação
- [`docs/USAGE_EXAMPLES.md`](docs/USAGE_EXAMPLES.md) — Exemplos de uso das entidades
- [`docs/doctors.http`](docs/doctors.http) — Casos de teste HTTP

## Estrutura do Projeto

```
src/main/java/com/tcc
├── presentation
│   └── controller        # Endpoints REST, validação e autorização
│
├── application
│   ├── dto
│   │   ├── request       # DTOs de entrada
│   │   └── response      # DTOs de saída (inclui ApiResponse)
│   ├── mapper            # Componentes de conversão Entity ↔ DTO
│   └── service           # Casos de uso e regras de negócio
│
├── domain
│   ├── model             # Entidades JPA
│   └── repository        # Repositories Spring Data JPA
│
├── infrastructure
│   ├── security          # JWT, filtros e SecurityConfig
│   └── config            # Configurações técnicas, como OpenAPI
│
├── exception             # Exceções customizadas e GlobalExceptionHandler
└── TccApplication.java

src/main/resources
├── application.properties
├── application-dev.properties
├── application-prod.properties
└── db/migration          # Scripts Flyway
```

## Padrão de Resposta da API

Todas as respostas seguem o formato padronizado via `ApiResponse<T>`:

**Sucesso:**
```json
{
  "success": true,
  "data": {}
}
```

**Erro:**
```json
{
  "success": false,
  "message": "Descrição do erro"
}
```

## Tratamento de Exceções

Centralizado no `GlobalExceptionHandler`:

| Exceção | HTTP Status |
|---|---|
| `ResourceNotFoundException` | 404 Not Found |
| `BusinessException` | 422 Unprocessable Entity |
| `Exception` (genérica) | 500 Internal Server Error |

## Camadas — Responsabilidades

O fluxo principal é `Controller → Service → Repository → Entity JPA`.

### Presentation
- Recebe e valida requisições HTTP
- Aplica as regras de autorização dos endpoints
- Delega a execução aos services

### Application
- Implementa casos de uso, transações e regras de negócio
- Mantém DTOs e mappers para não expor entidades pela API
- Não contém consultas SQL nem detalhes de resposta de erro

### Domain
- Contém as entidades anotadas com JPA
- Contém repositories que estendem interfaces do Spring Data, como `JpaRepository`
- Possui, portanto, dependência explícita das tecnologias de persistência

### Infrastructure
- Reúne configurações técnicas de segurança, JWT e OpenAPI
- Dá suporte transversal às demais camadas

## Decisão Arquitetural

A arquitetura em camadas foi escolhida conscientemente por pragmatismo, considerando o escopo do TCC, o prazo e a facilidade de implementação e manutenção. O projeto aplica separação de responsabilidades, mas não busca isolar o domínio dos frameworks de persistência.
