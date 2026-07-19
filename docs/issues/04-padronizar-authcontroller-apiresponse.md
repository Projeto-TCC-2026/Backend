# 04 — Padronizar resposta do AuthController com ApiResponse

**Prioridade:** Média (consistência de API)
**Esforço:** Baixo
**Bloqueada por:** Nenhuma — pode começar imediatamente

## Contexto

Todos os controllers de CRUD (`UserController`, `DoctorController`, `HospitalController`, `PatientController`) retornam `ResponseEntity<ApiResponse<T>>`, um envelope padrão com `success`, `data` e `message`. `AuthController` quebra esse padrão: retorna `ResponseEntity<AuthResponse>`, `ResponseEntity<DoctorAuthResponse>` etc. diretamente, sem o envelope. Isso obriga quem consome a API (mobile/web) a tratar `/auth/**` de forma diferente do resto dos endpoints.

## O que precisa ser feito

- Envolver todas as respostas de `AuthController` (`login`, `loginDoctor`, `loginPatient`, `refresh`, `logout`, `me`) no mesmo padrão `ApiResponse<T>` usado nos demais controllers.
- Ajustar os clientes da API (mobile/web) que hoje esperam o formato antigo do `/auth/**`, se já existirem em produção.

## Critérios de aceite

- [ ] Todos os endpoints de `AuthController` retornam `ResponseEntity<ApiResponse<T>>`.
- [ ] O formato de resposta de `/auth/**` é idêntico em estrutura ao de `/api/patients`, `/api/doctors` etc.
- [ ] Documentação Swagger dos endpoints de auth reflete o novo formato de resposta.
