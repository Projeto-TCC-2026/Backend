# Validação de Perfis de Acesso

Documento de análise comparando os requisitos de acesso por perfil com a implementação atual do backend.

---

## Perfis existentes no sistema

```java
public enum Role {
    ADMIN,
    DOCTOR,
    PATIENT
}
```

---

## 1. ADMIN

**Requisito:** Visualiza tudo, CRUD de hospitais e CRUD de doutores.

| Requisito | Status | Implementação |
|---|---|---|
| CRUD de Hospitais | ✅ Implementado | `AdminController` + `HospitalManagementController` (`@PreAuthorize("hasRole('ADMIN')")`) |
| CRUD de Doutores | ✅ Implementado | `DoctorController`: create/update/delete exigem `hasRole('ADMIN')` |
| Visualiza tudo (dashboard global) | ✅ Implementado | `DashboardController` com métricas de hospitais, doutores e pacientes |
| Gerencia usuários | ✅ Implementado | `AdminController` com CRUD de usuários e ativação/desativação |
| Não acessa CRUD de pacientes | ✅ Correto | `SecurityConfig` → `/api/patients/**` exige `hasRole('DOCTOR')` |

**Resultado: FUNCIONAL** ✅

---

## 2. HOSPITAL

**Requisito:** Cadastro próprio, ambiente com Dashboard do hospital e capacidade de cadastrar doutores.

| Requisito | Status | Observação |
|---|---|---|
| Role `HOSPITAL` no enum | ❌ Não existe | Apenas `ADMIN`, `DOCTOR`, `PATIENT` |
| Login como hospital | ❌ Não existe | Sem endpoint de autenticação para hospital |
| Dashboard do hospital | ⚠️ Parcial | `DashboardServiceImpl.getHospitalDashboard()` existe mas é acessível por `ADMIN` ou `DOCTOR` — não há role hospital |
| Hospital cadastrar seus doutores | ❌ Não existe | Criar doutor é exclusivo `ADMIN` |
| Hospital fazer auto-cadastro | ❌ Não existe | Criação de hospital é exclusiva do admin |
| Hospital ver apenas seus próprios dados | ❌ Não existe | Sem controller de contexto hospital |

**Resultado: NÃO IMPLEMENTADO** ❌

### O que precisa ser feito

1. Criar role `HOSPITAL` (ou `HOSPITAL_ADMIN`) no enum `Role`
2. Migration para suportar a nova role
3. Controller de contexto hospital com:
   - Dashboard do próprio hospital (doutores, pacientes vinculados)
   - CRUD de doutores **apenas do próprio hospital** (escopo por `hospital_id`)
4. Garantir que hospital só visualiza/manipula seus próprios doutores
5. Endpoint de login específico (ou usar o genérico com a nova role)
6. Decidir: hospital se auto-cadastra ou admin cadastra e depois hospital gerencia?

---

## 3. DOCTOR

**Requisito:** Acesso próprio ao site com Dashboard individual e capacidade de cadastrar pacientes.

| Requisito | Status | Implementação |
|---|---|---|
| Login como doutor | ✅ Implementado | `AuthController.loginDoctor()` retorna `DoctorAuthResponse` |
| Cadastrar pacientes | ✅ Implementado | `PatientController` inteiro exige `hasRole('DOCTOR')` |
| Dashboard individual do doutor | ⚠️ Parcial | `HospitalDashboardResponse` mostra dados do hospital, não do médico individual |
| Ver apenas seus próprios pacientes | ⚠️ Não implementado | Controller lista TODOS os pacientes ativos, sem filtro por hospital/médico |
| Escopo por hospital | ⚠️ Não implementado | Regra documentada mas não aplicada no código |

**Resultado: PARCIAL** ⚠️

### O que precisa ser feito

1. Dashboard individual do doutor:
   - Seus pacientes (via `doctor_patients`)
   - Seus procedimentos
   - Estatísticas pessoais
2. Filtro de escopo nos endpoints de paciente:
   - Doutor só vê pacientes vinculados a ele via `doctor_patients`
   - Ou, no mínimo, pacientes do mesmo hospital
3. Endpoint `/api/doctors/me` ou similar para o doutor ver seu próprio perfil

---

## 4. PATIENT

**Requisito:** Acesso somente ao aplicativo mobile.

| Requisito | Status | Implementação |
|---|---|---|
| Login como paciente | ✅ Implementado | Login genérico retorna token com role `PATIENT` |
| Bloqueado de endpoints de gestão | ✅ Correto | `/api/patients/**` exige `DOCTOR`, paciente não acessa |
| Endpoints para o app mobile | ❌ Não existe | Nenhum controller com `hasRole('PATIENT')` |

**Resultado: PARCIAL** ⚠️

### O que precisa ser feito (quando o app mobile for desenvolvido)

1. Controller com endpoints exclusivos para `PATIENT`:
   - Ver seus próprios dados (`/api/me/profile`)
   - Ver seus procedimentos (`/api/me/procedures`)
   - Ver suas leituras de saúde (`/api/me/readings`)
   - Ver seus alertas (`/api/me/alerts`)
2. Todos os endpoints derivam o paciente do token JWT (não do path)
3. Paciente nunca vê dados de outros pacientes

---

## Resumo Geral

| Perfil | Status | Prioridade de Implementação |
|---|---|---|
| ADMIN | ✅ Funcional | — |
| HOSPITAL | ❌ Não existe | **Alta** — precisa de nova role + controller + escopo |
| DOCTOR | ⚠️ Parcial | **Média** — falta escopo por hospital e dashboard individual |
| PATIENT | ⚠️ Só bloqueio | **Baixa** — endpoints mobile vêm quando o app for desenvolvido |

---

## SecurityConfig atual (referência)

```
/auth/**                    → público (login, refresh)
/auth/me                    → autenticado
/api/doctors/**             → ADMIN ou DOCTOR
/api/patients/**            → DOCTOR
/api/users/**               → ADMIN
/api/admin/**               → ADMIN
/api/dashboard/**           → ADMIN
qualquer outro              → autenticado
```

---

## Decisões pendentes

1. **Nome da role hospital:** `HOSPITAL` ou `HOSPITAL_ADMIN`?
2. **Auto-cadastro de hospital:** O hospital se registra sozinho ou o admin cria e entrega credenciais?
3. **Escopo do doutor:** Filtra por vínculo direto (`doctor_patients`) ou por hospital?
4. **Endpoints mobile:** Prefixo `/api/me/` ou `/api/patient/`?
