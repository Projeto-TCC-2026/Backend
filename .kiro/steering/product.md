---
inclusion: always
---

# Produto

TCC Saúde é uma API REST backend para usuários, hospitais, médicos e pacientes. Também tem modelos persistentes para procedimentos, execuções, dispositivos, leituras de saúde, alertas e notificações — parte desses fluxos ainda não tem endpoint nem service correspondente.

Dado de saúde e dado de identificação são sensíveis. Toda regra deste arquivo existe por causa disso.

## Log

- **Pode logar:** apenas identificadores técnicos, como `hospitalId`, `patientId`, `doctorId`, `procedureId` e `userId`.
- **Nunca logar:** nome, CPF, e-mail, telefone, endereço, data de nascimento, leitura de saúde, senha, JWT ou qualquer credencial.
- Em log de erro, registre o ID da entidade e o tipo da exceção. Nunca o payload da requisição nem a exceção completa.

## Perfis e autorização

Perfis: `ADMIN`, `DOCTOR`, `PATIENT`.

**`DOCTOR` acessa apenas pacientes do próprio hospital.** Todo endpoint que devolva paciente, ou dado derivado de paciente, filtra pelo hospital do médico autenticado. `@PreAuthorize` com a role não é suficiente: o escopo é aplicado na consulta ao repository.

**`ADMIN` é global**, sem vínculo nem escopo por hospital: administra todos os hospitais e usuários, e os relatórios agregam a plataforma inteira. `ADMIN` não acessa o CRUD de pacientes; esse CRUD é exclusivo de `DOCTOR`.

**`PATIENT` não acessa registro de paciente**, nem o próprio. `/api/patients/**` exige `DOCTOR`.

**Identidade do usuário autenticado** é derivada só em `/auth/me` e `/api/admin/profile`. Nenhum endpoint de paciente deriva o `patientId` do usuário autenticado; quando é necessário, o identificador vem pela URL, pela query string ou pelo corpo da requisição.

**Usuário inativo não autentica** e não pode usar JWT emitido antes da inativação. O mesmo vale para médico de hospital inativo.

**Nenhum endpoint público emite JWT** sem validar credencial pelo fluxo normal de autenticação.

**`/auth/me` exige usuário autenticado.** A regra dele precede qualquer matcher mais abrangente de `/auth/**` no `SecurityConfig` — o Spring Security para no primeiro match.

Todo endpoint novo ou alterado leva `@PreAuthorize` explícito. Endpoint público de propósito (login, health check) pode não ter — nesse caso, deixe um comentário dizendo por quê.

## Persistência

Estratégia de exclusão, por entidade:

| Entidade | Comportamento |
|---|---|
| `User` | soft delete (`active = false`) |
| `Hospital` | hard delete se não houver médicos, mais inativação separada |
| `Patient` | hard delete se não houver execuções nem leituras, mais inativação separada |
| `Doctor` | hard delete, bloqueado se houver pacientes ou procedimentos |
| `RefreshToken` | revogação (`revoked = true`), sem remoção |
| demais entidades | sem delete próprio; podem cair em cascata via `CascadeType.ALL` |

`Patient.active` filtra as principais consultas do CRUD.

Ao criar entidade nova, siga o padrão da entidade-pai correspondente e diga qual escolheu.

## Funcionalidades fora do escopo atual

Não implemente estes fluxos sem requisito explícito do usuário, e não assuma comportamento existente neles:

- **Execução de procedimento:** só leitura por paciente. Não existe criação, atualização nem exclusão.
- **Alertas:** não existe regra que dispare alerta — nenhum limite, nenhuma faixa por paciente, nenhuma avaliação de leitura.
- **Leituras de saúde:** não existe escrita.
- **Dashboard:** `DashboardServiceImpl` existe e não é chamado por nenhum controller.

## Idioma

- Descrição de API, mensagem de validação e mensagem de erro de domínio: **português**.
- Identificador Java, nome de pacote, nome de tabela e de coluna: **inglês**.

## Ao mudar contrato de endpoint

Na mesma mudança, atualize também:

- as anotações OpenAPI do endpoint;
- os exemplos em `docs/*.http`.

A descrição OpenAPI precisa corresponder ao que o endpoint faz. Não descreva efeito que o código não tem.
