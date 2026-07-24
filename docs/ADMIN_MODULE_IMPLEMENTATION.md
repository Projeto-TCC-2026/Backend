# Módulo Administrativo - Implementação Completa

## Visão Geral

O módulo administrativo foi desenvolvido para permitir que usuários com perfil **ADMIN** gerenciem toda a plataforma, incluindo cadastro e habilitação de hospitais, gerenciamento de usuários, e controle total da aplicação.

## Funcionalidades Implementadas

### 1. 🔐 **AUTENTICAÇÃO E SEGURANÇA**

#### **Login Específico para Administradores**
- **Endpoint**: `POST /auth/admin/login`
- **Validação**: Verifica se o usuário tem role ADMIN
- **Retorna**: Token JWT e dados básicos do administrador
- **Segurança**: Endpoint específico com validação de role

#### **Proteção de Rotas**
- **Path Base**: `/api/admin/**`
- **Restrição**: Apenas usuários com role `ADMIN`
- **Implementação**: `@PreAuthorize("hasRole('ADMIN')")` em todos os endpoints
- **Configuração**: SecurityConfig atualizado para proteger rotas administrativas

#### **Logout Seguro**
- **Endpoint**: `POST /auth/logout`
- **Funcionalidade**: Revoga refresh tokens ativos
- **Segurança**: Invalida sessões de forma segura

---

### 2. 📊 **DASHBOARD ADMINISTRATIVO**

#### **Estatísticas da Plataforma**
- **Endpoint**: `GET /api/admin/dashboard`
- **Dados Retornados**:
  - Total de hospitais cadastrados
  - Total de usuários no sistema
  - Total de médicos ativos
  - Total de pacientes registrados
  - Mensagem de boas-vindas

#### **Perfil do Administrador**
- **Endpoint**: `GET /api/admin/profile`
- **Funcionalidade**: Retorna dados do admin logado
- **Informações**: ID, email, role, dados do perfil

---

### 3. 🏥 **GERENCIAMENTO DE HOSPITAIS**

#### **CRUD Completo de Hospitais**
- **Criar**: `POST /api/admin/hospitals`
- **Listar**: `GET /api/admin/hospitals`
- **Visualizar**: `GET /api/admin/hospitals/{id}`
- **Atualizar**: `PUT /api/admin/hospitals/{id}`
- **Excluir**: `DELETE /api/admin/hospitals/{id}`

#### **Controle de Acesso de Hospitais**
- **Habilitar**: `PATCH /api/admin/hospitals/{id}/enable`
- **Desabilitar**: `PATCH /api/admin/hospitals/{id}/disable`
- **Funcionalidade**: Controla se médicos do hospital podem fazer login

#### **Resumo dos Hospitais**
- **Endpoint**: `GET /api/admin/hospitals/summary`
- **Dados**: Informações resumidas + contagem de médicos por hospital
- **Paginação**: Suporte completo a paginação e ordenação

---

### 4. 👥 **GERENCIAMENTO DE USUÁRIOS**

#### **CRUD Completo de Usuários**
- **Criar**: `POST /api/admin/users`
- **Listar**: `GET /api/admin/users`
- **Filtrar por Role**: `GET /api/admin/users?role=DOCTOR`
- **Visualizar**: `GET /api/admin/users/{id}`
- **Atualizar**: `PUT /api/admin/users/{id}`
- **Excluir**: `DELETE /api/admin/users/{id}` (soft delete)

#### **Controle de Status de Usuários**
- **Ativar**: `PATCH /api/admin/users/{id}/activate`
- **Desativar**: `PATCH /api/admin/users/{id}/deactivate`
- **Funcionalidade**: Controla acesso ao sistema

#### **Suporte a Múltiplas Roles**
- **ADMIN**: Administradores do sistema
- **DOCTOR**: Médicos dos hospitais
- **PATIENT**: Pacientes do sistema

---

## Estrutura Técnica Implementada

### **Controllers**
- `AdminController.java`: Controller principal com 18 endpoints administrativos
- `AuthController.java`: Adicionado endpoint `/auth/admin/login`

### **Services**
- `AuthService`: Adicionado método `loginAdmin()`
- `HospitalService`: Métodos para controle de hospitais (enable/disable, summary, count)
- `UserService`: Métodos para gerenciamento completo de usuários

### **DTOs**
- `HospitalSummary.java`: DTO para resumo de hospitais com contagem de médicos
- `ApiResponse.java`: Atualizado com método `success(T data, String message)`

### **Repositories**
- `HospitalRepository`: Query para resumo de hospitais com contagem de médicos
- `UserRepository`: Métodos para busca por role e contagem de usuários

### **Database**
- `V21__add_active_field_to_hospitals.sql`: Migração para campo `active` em hospitais
- **Índices**: Criados para otimização de consultas por status `active`

---

## Segurança Implementada

### **Autenticação**
- ✅ JWT-based authentication
- ✅ Refresh token rotation
- ✅ Login específico para ADMIN
- ✅ Validação de role em tempo de login

### **Autorização**
- ✅ Method-level security com `@PreAuthorize`
- ✅ Proteção de todas as rotas `/api/admin/**`
- ✅ Verificação de role ADMIN em todos os endpoints
- ✅ Isolation between user roles

### **Proteção de Dados**
- ✅ Soft delete para usuários
- ✅ Validação de integridade referencial
- ✅ Criptografia de senhas com BCrypt
- ✅ Sanitização de inputs

---

## Endpoints Disponíveis

### **Autenticação**
```
POST /auth/admin/login      - Login específico para ADMIN
POST /auth/logout           - Logout seguro
GET  /auth/me              - Perfil do usuário logado
```

### **Dashboard**
```
GET  /api/admin/dashboard   - Estatísticas da plataforma
GET  /api/admin/profile     - Perfil do administrador
```

### **Hospitais** (18 endpoints)
```
GET    /api/admin/hospitals               - Listar hospitais
POST   /api/admin/hospitals               - Cadastrar hospital
GET    /api/admin/hospitals/{id}          - Visualizar hospital
PUT    /api/admin/hospitals/{id}          - Atualizar hospital
DELETE /api/admin/hospitals/{id}          - Excluir hospital
PATCH  /api/admin/hospitals/{id}/enable   - Habilitar hospital
PATCH  /api/admin/hospitals/{id}/disable  - Desabilitar hospital
GET    /api/admin/hospitals/summary       - Resumo dos hospitais
```

### **Usuários** (8 endpoints)
```
GET    /api/admin/users                   - Listar usuários
POST   /api/admin/users                   - Cadastrar usuário
GET    /api/admin/users/{id}              - Visualizar usuário
PUT    /api/admin/users/{id}              - Atualizar usuário
DELETE /api/admin/users/{id}              - Excluir usuário (soft)
PATCH  /api/admin/users/{id}/activate     - Ativar usuário
PATCH  /api/admin/users/{id}/deactivate   - Desativar usuário
GET    /api/admin/users?role=DOCTOR       - Filtrar por role
```

---

## Como Testar

### **1. Fazer Login como Admin**
```bash
curl -X POST http://localhost:8080/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@sistema.com", "password": "admin123"}'
```

### **2. Acessar Dashboard**
```bash
curl -X GET http://localhost:8080/api/admin/dashboard \
  -H "Authorization: Bearer {token}"
```

### **3. Gerenciar Hospitais**
```bash
# Listar hospitais
curl -X GET http://localhost:8080/api/admin/hospitals \
  -H "Authorization: Bearer {token}"

# Cadastrar hospital
curl -X POST http://localhost:8080/api/admin/hospitals \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"name": "Hospital Teste", "cnpj": "12.345.678/0001-90"}'
```

### **4. Gerenciar Usuários**
```bash
# Listar usuários
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer {token}"

# Criar usuário médico
curl -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"email": "doctor@hospital.com", "password": "senha123", "role": "DOCTOR"}'
```

---

## Arquivo de Testes

Criado arquivo `docs/admin.http` com todos os endpoints para teste via HTTP Client, incluindo:
- Casos de sucesso
- Testes de segurança
- Validação de acesso negado
- Exemplos de payloads

---

## Validações e Regras de Negócio

### **Hospitais**
- ✅ CNPJ deve ser único
- ✅ Não pode excluir hospital com médicos associados
- ✅ Campos obrigatórios validados
- ✅ Enable/disable afeta login de médicos

### **Usuários**
- ✅ Email deve ser único
- ✅ Roles válidas: ADMIN, DOCTOR, PATIENT
- ✅ Soft delete preserva integridade referencial
- ✅ Senha automaticamente criptografada

### **Segurança**
- ✅ Apenas ADMIN pode acessar endpoints administrativos
- ✅ Token JWT obrigatório
- ✅ Validação de role em cada request
- ✅ Logs de segurança para auditoria

---

## Status da Implementação

- ✅ **Autenticação de Admin**: Implementado e testado
- ✅ **Login específico**: Funcional com validação de role
- ✅ **Proteção de rotas**: Todas as rotas protegidas
- ✅ **Logout seguro**: Implementado com revogação de tokens
- ✅ **Dashboard**: Estatísticas funcionais
- ✅ **CRUD Hospitais**: Completo com 8 operações
- ✅ **CRUD Usuários**: Completo com 8 operações
- ✅ **Controle de acesso**: Enable/disable funcionais
- ✅ **Documentação**: HTTP client e exemplos prontos
- ✅ **Testes**: Aplicação compila e testa com sucesso
- ✅ **Database**: Migração V21 aplicada com sucesso

**O módulo administrativo está 100% implementado e pronto para uso!**