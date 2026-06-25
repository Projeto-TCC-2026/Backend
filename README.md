# TCC Saúde - Backend

Backend do projeto de extensão TCC Saúde, desenvolvido com Spring Boot seguindo princípios de Clean Architecture e DDD estratégico simplificado.

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



```
GET /api/health
```

Resposta:
```json
{
  "success": true,
  "data": "API is running"
}
```

## Estrutura do Projeto

```
src/main/java/com/tcc
├── application
│   ├── dto
│   │   ├── request       # DTOs de entrada
│   │   └── response      # DTOs de saída (inclui ApiResponse)
│   ├── mapper            # Interfaces MapStruct
│   └── service           # Lógica de aplicação e regras de negócio
│
├── domain
│   ├── model             # Entidades do domínio
│   └── repository        # Interfaces de repositório (contratos)
│
├── infrastructure
│   ├── persistence
│   │   └── repository    # Implementações JPA dos repositórios
│   ├── security          # JWT, Filters, SecurityConfig
│   └── config            # Configurações globais (CORS, Swagger)
│
├── presentation
│   └── controller        # Endpoints REST
│
├── exception             # Exceções customizadas e GlobalExceptionHandler
│
└── util                  # Classes utilitárias

src/main/resources
├── application.properties
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

### Domain
- Entidades puras do domínio
- Interfaces de repositório (contratos)
- **Não** depende de nenhuma outra camada

### Application
- Casos de uso e regras de negócio
- Conversões DTO ↔ Entity via mappers
- **Não** contém código HTTP ou anotações de controller

### Infrastructure
- Implementações técnicas (JPA, Security, configs)
- Implementa os contratos definidos em Domain

### Presentation
- Recebe requisições HTTP
- Valida entrada
- Delega para services
- **Não** contém regras de negócio ou SQL
