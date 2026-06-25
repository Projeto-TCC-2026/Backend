# Estrutura Completa do Projeto

> **Última atualização:** reflete o estado real do código após adição de Actuator,
> Bean Validation, Flyway, configuração Docker e reorganização da documentação.

---

## 📁 Visão Geral da Raiz

```
tcc/
├── src/
│   ├── main/
│   │   ├── java/com/tcc/
│   │   │   ├── application/           # Camada de Aplicação
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/       # DTOs de requisição (Bean Validation)
│   │   │   │   │   └── response/      # DTOs de saída (inclui ApiResponse<T>)
│   │   │   │   ├── mapper/            # Conversores Entity <-> DTO
│   │   │   │   └── service/           # Serviços de negócio
│   │   │   │       ├── DoctorService.java       ✅ Interface
│   │   │   │       └── DoctorServiceImpl.java   ✅ Implementação
│   │   │   │
│   │   │   ├── domain/                # Camada de Domínio
│   │   │   │   ├── model/             # ✅ 14 Entidades JPA
│   │   │   │   └── repository/        # ✅ 14 Repositories (JpaRepository)
│   │   │   │
│   │   │   ├── exception/             # Exceções customizadas
│   │   │   │   ├── BusinessException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   │
│   │   │   ├── infrastructure/        # Camada de Infraestrutura
│   │   │   │   ├── config/
│   │   │   │   │   └── OpenApiConfig.java
│   │   │   │   ├── persistence/       # Implementações de persistência
│   │   │   │   └── security/          # (placeholder — Spring Security planejado)
│   │   │   │
│   │   │   ├── presentation/          # Camada de Apresentação
│   │   │   │   └── controller/
│   │   │   │       ├── DoctorController.java    ✅ CRUD completo (5 endpoints)
│   │   │   │       └── ExampleController.java   ✅ Health check manual (legado)
│   │   │   │
│   │   │   ├── util/                  # Classes utilitárias
│   │   │   └── TccApplication.java    # Classe principal
│   │   │
│   │   └── resources/
│   │       ├── application.properties          # Configurações base (H2 + dev)
│   │       ├── application-prod.properties     # Configurações de produção (PostgreSQL)
│   │       └── db/migration/                   # ✅ 14 scripts Flyway (V1–V14)
│   │
│   └── test/
│       └── java/com/tcc/
│           └── TccApplicationTests.java
│
├── Dockerfile                  # Multi-stage: build (JDK 17) → runtime (JRE 17)
├── docker-compose.yml          # Serviços: app + postgres, healthcheck, volume
├── .dockerignore               # Exclui target/, .git/, docs/, *.md do contexto Docker
├── .env                        # Variáveis de ambiente locais (NÃO comitar — ver .gitignore)
├── pom.xml                     # Dependências Maven
├── mvnw / mvnw.cmd             # Maven Wrapper
├── README.md                   # Documentação principal
└── docs/                       # Documentação do projeto
    ├── ENTITIES.md
    ├── ENTITY_DIAGRAM.md
    ├── USAGE_EXAMPLES.md
    ├── DOCTOR_CRUD_IMPLEMENTATION.md
    ├── IMPLEMENTATION_SUMMARY.md
    ├── HELP.md
    ├── PROJECT_STRUCTURE.md    # ← este arquivo
    └── doctors.http
```

---

## 📦 Dependências — estado real (pom.xml)

| Dependência | Versão | Status |
|---|---|---|
| `spring-boot-starter-parent` | 4.0.6 | ✅ |
| `spring-boot-starter-data-jpa` | gerenciada pelo parent | ✅ |
| `spring-boot-starter-webmvc` | gerenciada pelo parent | ✅ |
| `spring-boot-starter-validation` | gerenciada pelo parent | ✅ Bean Validation ativo |
| `spring-boot-starter-actuator` | gerenciada pelo parent | ✅ Health check via `/actuator/health` |
| `spring-boot-starter-flyway` | gerenciada pelo parent | ✅ Migrations ativas |
| `flyway-database-postgresql` | gerenciada pelo parent | ✅ Dialeto PostgreSQL |
| `springdoc-openapi-starter-webmvc-ui` | **3.0.3** | ✅ Swagger UI |
| `postgresql` | gerenciada pelo parent | ✅ runtime |
| `h2` | gerenciada pelo parent | ✅ runtime (dev) |
| `spring-boot-devtools` | gerenciada pelo parent | ✅ optional/runtime |
| `spring-boot-starter-test` | gerenciada pelo parent | ✅ test |

---

## 🔧 Configuração de Banco de Dados

### Desenvolvimento (padrão — `application.properties`)
```properties
spring.jpa.hibernate.ddl-auto=none      # Flyway gerencia o schema
spring.jpa.show-sql=true
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```
> Sem datasource explícito → Spring Boot usa H2 em memória automaticamente.

### Produção (`application-prod.properties` / variáveis do docker-compose)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tccdb
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=false
```
> Em Docker, `SPRING_DATASOURCE_URL` sobrescreve esse valor apontando para o
> container `postgres` interno.

---

## 🏥 Health Check — Actuator (✅ implementado)

O health check manual (`ExampleController`) foi mantido por compatibilidade, mas
o endpoint canônico agora é provido pelo Spring Boot Actuator:

```
GET /actuator/health
```

Configuração em `application.properties`:
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=always
```

> `show-details=always` é adequado para desenvolvimento. Quando Spring Security
> for adicionado, alterar para `when-authorized`.

---

## 🐳 Docker — arquivos e funções

### `Dockerfile` (multi-stage)
- **Estágio 1 — build** (`eclipse-temurin:17-jdk`): copia `.mvn/`, `mvnw`, `pom.xml`
  e baixa dependências em camada separada (cache). Em seguida copia `src/` e executa
  `mvnw clean package -DskipTests`.
- **Estágio 2 — runtime** (`eclipse-temurin:17-jre`): copia apenas o `.jar` gerado.
  Expõe porta `8080`. Imagem final não contém Maven nem código-fonte.

### `docker-compose.yml`
Dois serviços:

| Serviço | Imagem | Porta | Função |
|---|---|---|---|
| `postgres` | `postgres:17` | `5434:5432` | Banco de dados PostgreSQL com volume persistente |
| `app` | build local | `8080:8080` | Aplicação Spring Boot com profile `prod` |

- Credenciais lidas de variáveis de ambiente (arquivo `.env` local).
- `app` aguarda `postgres` via `healthcheck` (`pg_isready`) antes de subir.
- Volume `postgres_data` persiste os dados entre reinicializações.

### `.dockerignore`
Exclui do contexto de build: `target/`, `.git/`, `.idea/`, `docs/`, `*.md`,
`.gitignore`, `.gitattributes` e o jar do wrapper Maven.

### `.env`
Contém as variáveis `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` usadas
pelo `docker-compose.yml`. **Não comitar** — já está no `.gitignore`.
Crie sua cópia local a partir dos nomes de variável acima.

---

## 🗄️ Migrations Flyway (V1–V14)

Todos os scripts estão em `src/main/resources/db/migration/`:

| Script | Tabela criada |
|---|---|
| V1 | users |
| V2 | hospitals |
| V3 | doctors |
| V4 | patients |
| V5 | doctor_patients |
| V6 | patient_devices |
| V7 | procedures |
| V8 | patient_procedures |
| V9 | procedure_executions |
| V10 | procedure_photos |
| V11 | reading_imports |
| V12 | health_readings |
| V13 | alerts |
| V14 | notifications |

---

## 🌐 Endpoints Disponíveis

| Endpoint | Descrição |
|---|---|
| `GET /actuator/health` | Health check (Actuator) ✅ |
| `GET /api/health` | Health check manual — `ExampleController` (legado) |
| `GET /swagger-ui.html` | Swagger UI |
| `GET /api-docs` | OpenAPI JSON |
| `POST /doctors` | Criar médico |
| `GET /doctors` | Listar médicos |
| `GET /doctors/{id}` | Buscar médico por ID |
| `PUT /doctors/{id}` | Atualizar médico |
| `PATCH /doctors/{id}/inactive` | Inativar médico ⚠️ pendente (sem campo `active`) |

---

## 🎯 Estado de Implementação

### ✅ Implementado
- Arquitetura em camadas (Clean Architecture / DDD)
- 14 Entidades JPA com relacionamentos completos
- 14 Repositories com queries customizadas
- 14 Migrations Flyway (V1–V14)
- Bean Validation nos DTOs (`@NotNull`, `@NotBlank`, `@Size`, etc.)
- Spring Boot Actuator (`/actuator/health`)
- Swagger / OpenAPI 3 (springdoc 3.0.3)
- CRUD completo de Doctors (4 de 5 endpoints funcionais)
- Tratamento global de exceções (`GlobalExceptionHandler`)
- Padrão de resposta unificado (`ApiResponse<T>`)
- Docker: Dockerfile multi-stage + docker-compose com PostgreSQL
- Profile `prod` com PostgreSQL via `application-prod.properties`

### ⚠️ Parcialmente implementado
- CRUD de Doctors: endpoint `PATCH /doctors/{id}/inactive` pendente
  (entidade `Doctor` sem campo `active`/`status`)

### 🔲 Planejado
- Spring Security + JWT
- CRUDs de Patients, Hospitals, Users, Procedures
- Testes unitários e de integração
- Cache com Redis
- Monitoramento adicional via Actuator (metrics, info)
- Controle de autorização por roles

---

## 📊 Diagrama de Dependências entre Camadas

```
┌────────────────────────────────────────────┐
│         Presentation Layer                 │
│  (Controllers — REST Endpoints)            │
└─────────────────┬──────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────┐
│         Application Layer                  │
│  (Services, DTOs, Mappers)                 │
└─────────────────┬──────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────┐
│         Domain Layer                       │
│  (Entities, Repositories)                  │
│  ✅ 14 Entidades  ✅ 14 Repositories       │
└─────────────────┬──────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────┐
│         Infrastructure Layer               │
│  (Config, Security placeholder,            │
│   Flyway migrations, JPA/PostgreSQL)       │
└────────────────────────────────────────────┘
```

---

## 📝 Notas Importantes

1. **DDL gerenciado pelo Flyway** — `spring.jpa.hibernate.ddl-auto=none`. Nunca
   usar `create` ou `update` em produção.
2. **Fetch Type LAZY** — padrão em todos os relacionamentos para evitar N+1.
3. **Unique Constraints** — CPF, CRM e Email são únicos no banco (garantido pelas migrations).
4. **Timestamps automáticos** — `createdAt` / `updatedAt` via `@PrePersist` / `@PreUpdate`.
5. **Senha não documentada** — o arquivo `.env` não é versionado e seus valores
   não aparecem nesta documentação.

---

## 📚 Documentos de Referência

Todos em `docs/`:

| Arquivo | Conteúdo |
|---|---|
| `ENTITIES.md` | Documentação completa das 14 entidades |
| `ENTITY_DIAGRAM.md` | Diagramas ASCII de relacionamentos |
| `USAGE_EXAMPLES.md` | Exemplos práticos de código |
| `DOCTOR_CRUD_IMPLEMENTATION.md` | Detalhes do CRUD de Doctors |
| `IMPLEMENTATION_SUMMARY.md` | Resumo da implementação |
| `doctors.http` | Casos de teste HTTP |
