# 🚀 Como Executar o Projeto - Sprint Hospital e Doutores

## 📋 Pré-requisitos

### Obrigatório
- ☕ **Java 17** ou superior
- 🗄️ **PostgreSQL** instalado e rodando
- 🔧 **Maven** (ou use o wrapper incluído: `mvnw`)

### Opcional
- 🐳 **Docker** (para rodar PostgreSQL via container)
- 📮 **Postman** ou extensão **REST Client** (VSCode)

---

## 🔧 Configuração Inicial

### 1. Configurar Banco de Dados

#### Opção A: PostgreSQL Local
```sql
-- Criar banco de dados
CREATE DATABASE tcc_db;

-- Criar usuário (se necessário)
CREATE USER tcc_user WITH PASSWORD 'tcc_password';
GRANT ALL PRIVILEGES ON DATABASE tcc_db TO tcc_user;
```

#### Opção B: PostgreSQL via Docker
```bash
docker run --name postgres-tcc \
  -e POSTGRES_DB=tcc_db \
  -e POSTGRES_USER=tcc_user \
  -e POSTGRES_PASSWORD=tcc_password \
  -p 5432:5432 \
  -d postgres:15
```

### 2. Configurar application.properties

Edite `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/tcc_db
spring.datasource.username=tcc_user
spring.datasource.password=tcc_password

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# JWT
jwt.secret=YOUR_SECRET_KEY_HERE_CHANGE_IN_PRODUCTION
jwt.expiration=86400000
```

---

## 🏗️ Compilar e Executar

### Windows

#### 1. Compilar o Projeto
```cmd
.\mvnw clean compile
```

#### 2. Executar Testes (opcional)
```cmd
.\mvnw test
```

#### 3. Executar a Aplicação
```cmd
.\mvnw spring-boot:run
```

### Linux/Mac

#### 1. Compilar o Projeto
```bash
./mvnw clean compile
```

#### 2. Executar Testes (opcional)
```bash
./mvnw test
```

#### 3. Executar a Aplicação
```bash
./mvnw spring-boot:run
```

### Alternativa: Gerar JAR e Executar

```bash
# Gerar JAR
./mvnw clean package -DskipTests

# Executar JAR
java -jar target/tcc-0.0.1-SNAPSHOT.jar
```

---

## ✅ Verificar se está Funcionando

### 1. Health Check
```bash
curl http://localhost:8080/actuator/health
```

Resposta esperada:
```json
{
  "status": "UP"
}
```

### 2. Acessar Swagger
Abra no navegador:
```
http://localhost:8080/swagger-ui.html
```

### 3. Acessar API Docs (JSON)
```
http://localhost:8080/v3/api-docs
```

---

## 🔐 Autenticação

### 1. Criar Usuário Admin (via SQL)

Execute no PostgreSQL:
```sql
-- Inserir usuário admin (senha: admin123)
INSERT INTO users (email, password, role, created_at, updated_at)
VALUES (
  'admin@example.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'ADMIN',
  NOW(),
  NOW()
);
```

> **Nota:** A senha acima é `admin123` criptografada com BCrypt.

### 2. Fazer Login

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "admin123"
}
```

Resposta:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "expiresIn": 86400000
  }
}
```

### 3. Usar o Token

Copie o token e use no header `Authorization`:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 🧪 Testar os Endpoints

### Opção 1: Swagger UI
1. Acesse: `http://localhost:8080/swagger-ui.html`
2. Clique em **Authorize** (cadeado no topo)
3. Cole o token: `Bearer SEU_TOKEN_AQUI`
4. Teste os endpoints diretamente na interface

### Opção 2: Arquivos HTTP

#### VSCode (REST Client)
1. Instale a extensão: **REST Client**
2. Abra os arquivos:
   - `docs/hospitals.http`
   - `docs/doctors.http`
3. Substitua `YOUR_JWT_TOKEN_HERE` pelo seu token
4. Clique em **Send Request** acima de cada requisição

#### IntelliJ IDEA
1. Abra os arquivos `.http` (suporte nativo)
2. Atualize o token
3. Clique no ícone de play ao lado de cada requisição

### Opção 3: cURL

#### Criar Hospital
```bash
curl -X POST http://localhost:8080/api/hospitals \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN" \
  -d '{
    "name": "Hospital Santa Maria",
    "cnpj": "12345678901234",
    "phone": "(11) 98765-4321",
    "email": "contato@santamaria.com.br",
    "address": "Rua das Flores, 123",
    "city": "São Paulo",
    "state": "SP"
  }'
```

#### Listar Hospitais
```bash
curl -X GET "http://localhost:8080/api/hospitals?page=0&size=10" \
  -H "Authorization: Bearer SEU_TOKEN"
```

---

## 🐛 Solução de Problemas

### Erro: "JAVA_HOME not defined"
```bash
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17

# Linux/Mac
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

### Erro: "Connection refused" ao PostgreSQL
```bash
# Verificar se PostgreSQL está rodando
# Windows
sc query postgresql

# Linux
sudo systemctl status postgresql

# Docker
docker ps | grep postgres
```

### Erro: "Port 8080 already in use"
```bash
# Altere a porta em application.properties
server.port=8081
```

### Erro: "Flyway migration failed"
```bash
# Limpar banco e recriar
DROP DATABASE tcc_db;
CREATE DATABASE tcc_db;
```

### Logs não aparecem
```properties
# Adicione em application.properties
logging.level.com.tcc=DEBUG
logging.level.org.springframework.web=DEBUG
```

---

## 📊 Fluxo Completo de Teste

### 1. Criar Hospital
```http
POST /api/hospitals
{
  "name": "Hospital Santa Maria",
  "cnpj": "12345678901234",
  ...
}
```

### 2. Criar User para Doctor
```http
POST /api/users
{
  "email": "doctor@example.com",
  "password": "doctor123",
  "role": "DOCTOR"
}
```

### 3. Criar Doctor
```http
POST /api/doctors
{
  "userId": 1,
  "hospitalId": 1,
  "fullName": "Dr. João Silva",
  "cpf": "12345678901",
  "crm": "12345-SP",
  "specialty": "Cardiologia",
  "phone": "11987654321"
}
```

### 4. Buscar Doctor (com Hospital)
```http
GET /api/doctors/1
```

Resposta inclui:
```json
{
  "id": 1,
  "hospital": {
    "id": 1,
    "name": "Hospital Santa Maria",
    "city": "São Paulo",
    "state": "SP"
  },
  "fullName": "Dr. João Silva",
  ...
}
```

### 5. Filtrar Doctors por Hospital
```http
GET /api/doctors/filter?hospitalId=1
```

---

## 🔄 Comandos Úteis

### Limpar e Recompilar
```bash
./mvnw clean install
```

### Executar sem Testes
```bash
./mvnw spring-boot:run -DskipTests
```

### Ver Dependências
```bash
./mvnw dependency:tree
```

### Formatar Código
```bash
./mvnw spring-javaformat:apply
```

---

## 📚 Documentação Adicional

### Arquivos de Referência
- 📄 `docs/HOSPITAL_DOCTOR_IMPLEMENTATION.md` - Documentação detalhada
- 📄 `docs/SPRINT_HOSPITAL_DOCTOR_SUMMARY.md` - Resumo da Sprint
- 📄 `docs/hospitals.http` - Exemplos de requisições Hospital
- 📄 `docs/doctors.http` - Exemplos de requisições Doctor

### Endpoints Principais
- 🏥 **Hospitals**: `/api/hospitals`
- 👨‍⚕️ **Doctors**: `/api/doctors`
- 📖 **Swagger**: `/swagger-ui.html`
- 🔍 **API Docs**: `/v3/api-docs`
- ❤️ **Health**: `/actuator/health`

---

## 📞 Suporte

Em caso de dúvidas:
1. Verifique os logs: `logs/spring.log`
2. Consulte o Swagger: `http://localhost:8080/swagger-ui.html`
3. Revise a documentação em `docs/`

---

## ✅ Checklist de Execução

- [ ] Java 17+ instalado
- [ ] PostgreSQL rodando
- [ ] Banco de dados criado
- [ ] application.properties configurado
- [ ] Projeto compilado sem erros
- [ ] Aplicação iniciada com sucesso
- [ ] Health check retorna UP
- [ ] Swagger acessível
- [ ] Login funcionando
- [ ] Token JWT obtido
- [ ] Endpoints testados

---

**Pronto! Seu projeto está rodando! 🎉**
