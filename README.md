# TCC Saúde – Backend

API REST em Spring Boot que atende o painel web (admin, hospital, médico) e o app mobile do paciente. Cuida de hospitais, médicos, pacientes, procedimentos, check-ins, leituras de saúde e alertas.

## Pré-requisitos

- Java 17 (`java.version` no `pom.xml`). Não precisa instalar o Maven: use o wrapper (`./mvnw`, ou `.\mvnw.cmd` no Windows).
- Docker, se for subir com o `docker-compose.yml` (PostgreSQL 17).

Stack: Spring Boot 4.1.1, Spring Security + JJWT, Spring Data JPA, Flyway, springdoc-openapi, Actuator, PostgreSQL (prod) e H2 (dev).

## Variáveis de ambiente

O `spring-boot:run` não lê o `.env`, que só é usado pelo Docker Compose. Rodando direto pelo Maven, defina as variáveis no terminal.

Obrigatórias em qualquer perfil (sem elas a aplicação não sobe):

| Variável | Para quê |
|---|---|
| `JWT_SECRET` | assinatura dos tokens. Precisa de 32+ caracteres (o JJWT recusa chave menor para HMAC-SHA) |
| `FRONTEND_BASE_URL` | links de reset de senha e de ativação de conta |
| `AWS_REGION` | clientes de SQS e S3 |
| `APP_PASSWORD_RESET_SQS_QUEUE_URL` | fila de recuperação de senha |

Só no perfil `prod`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `DASHBOARD_CACHE_S3_BUCKET` (no `dev` o bucket é fixo em `dev-local-sem-cache`).

Para subir localmente, qualquer valor serve nas variáveis da AWS. A aplicação sobe sem credenciais da AWS. Exemplo no PowerShell:

```powershell
$env:JWT_SECRET = "uma-chave-local-com-pelo-menos-32-caracteres"
$env:FRONTEND_BASE_URL = "http://localhost:4200"
$env:AWS_REGION = "us-east-1"
$env:APP_PASSWORD_RESET_SQS_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/000000000000/fila-teste"
```

Opcionais (padrão entre parênteses):

- `APP_CORS_ALLOWED_ORIGINS` (`http://localhost:*,http://127.0.0.1:*`). O curinga `*` é recusado.
- `APP_ACCOUNT_ACTIVATION_SQS_ENABLED` (`false`) e `APP_ACCOUNT_ACTIVATION_SQS_QUEUE_URL`
- `APP_PUSH_ENABLED` (`false`)
- `APP_ALERT_EMAIL_ENABLED` (`false`) e `APP_ALERT_EMAIL_SENDER`
- `APP_INTEGRATION_SERVICE_KEY` (vazio): chave esperada no header `X-Integration-Key`. Sem ela, `/api/integration/**` recusa todas as requisições.

## Como rodar

### Perfil `dev`

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Sobe na porta `8080` com um H2 em memória (`jdbc:h2:mem:<uuid>`), e o Flyway aplica todas as migrations nele. O banco é zerado a cada reinício.

### Perfil `prod`

Exige `SPRING_PROFILES_ACTIVE=prod` e as variáveis `DB_*` apontando para um PostgreSQL. Na EC2 os valores vêm de `/etc/tccapp/app.env`.

### Docker Compose

```bash
docker compose up --build
```

Sobe o `postgres:17` (porta `5434` no host) e a aplicação no perfil `prod` (porta `8080`). As variáveis vêm do `.env`, que não é versionado: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET`, as da AWS etc.

Para debugar, use o `docker-compose.debug.yml` junto com o principal. Sozinho ele não funciona: só define o serviço `app` e aponta para o host `postgres`, que existe apenas no `docker-compose.yml`.

```bash
docker compose -f docker-compose.yml -f docker-compose.debug.yml up --build
```

Assim a aplicação roda com o `Dockerfile.dev` no perfil `dev`, usando o Postgres do compose, com `./src` montado no container e o debugger na porta `5005`.

### Testes e build

```bash
./mvnw test
./mvnw clean package      # gera target/tcc-0.0.1-SNAPSHOT.jar
```

O deploy roda a cada push na `main` pelo `.github/workflows/deploy-backend.yml` (EC2, via S3 e SSM). Também dá para disparar manualmente pela aba Actions.

## Swagger e health check

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Health: `http://localhost:8080/actuator/health`. É público, mas os detalhes (banco, disco) só aparecem para `ADMIN`.

Nos endpoints protegidos, faça login, copie o `accessToken` e use `Authorize` no Swagger com `Bearer <token>`.

## Endpoints principais

Os perfis abaixo vêm do `SecurityConfig` e dos `@PreAuthorize`.

| Caminho | Quem acessa | O que tem |
|---|---|---|
| `/auth/login`, `/auth/{doctor,patient,admin,hospital}/login`, `/auth/refresh`, `/auth/logout`, `/auth/register/hospital` | público | login, renovação de token, cadastro de hospital |
| `/auth/me`, `/auth/change-password`, `/auth/profile/{doctor,hospital,patient}` | autenticado | perfil do usuário logado |
| `/forgot-password/request`, `/forgot-password/reset` | público | recuperação de senha por código |
| `/account-activation/activate` | público | primeiro acesso por link |
| `/api/admin/**` (`hospitals`, `users`, `procedures`, `hospital-management`) | ADMIN | gestão de hospitais, usuários e procedimentos |
| `/api/users` | ADMIN | CRUD de usuários |
| `/api/dashboard/admin` | ADMIN | métricas da plataforma |
| `/api/hospitals` | ADMIN, DOCTOR (exclusão só ADMIN) | CRUD e busca de hospitais |
| `/api/doctors` | ADMIN, DOCTOR (parte só ADMIN) | CRUD e busca de médicos |
| `/api/hospital/**` (`profile`, `dashboard`, `doctors`, `procedures`) | HOSPITAL | portal do hospital |
| `/api/doctor/**` (`dashboard`, `procedures`, `my-procedures`, `alerts`) | DOCTOR | portal do médico e resolução de alertas |
| `/api/patients/**` (inclui `/{patientId}/assigned-procedures`) | DOCTOR, HOSPITAL | CRUD de pacientes, cada um no seu escopo |
| `/api/reports/**` | ADMIN ou HOSPITAL/DOCTOR, conforme o relatório | relatórios e exportação de check-ins e alertas |
| `/api/mobile/**` (`patient-procedures`, `checkins`, `alerts`, `devices`) | PATIENT | app do paciente |
| `/api/integration/alerts/evaluate` | integração (header `X-Integration-Key`) | avaliação de leituras e geração de alertas |
| `GET /api/public/stats` | público | números da landing page |

Exemplos de requisições prontas para rodar ficam em `docs/*.http`.
