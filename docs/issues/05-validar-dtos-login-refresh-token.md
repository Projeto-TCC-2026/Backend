# 05 — Validar DTOs de login e refresh token

**Prioridade:** Média
**Esforço:** Baixo
**Bloqueada por:** Nenhuma — pode começar imediatamente

## Contexto

A maioria dos DTOs de request é `record` com Bean Validation (`@NotBlank`, `@Email`, `@Pattern` etc.), mas `LoginRequest` e `RefreshTokenRequest` são classes mutáveis sem nenhuma anotação de validação. `AuthController` também não usa `@Valid` nesses endpoints. Hoje um `email`/`password`/`refreshToken` nulo ou vazio passa direto para a camada de serviço sem qualquer validação de formato antes de chegar no banco/`PasswordEncoder`.

## O que precisa ser feito

- Converter `LoginRequest` e `RefreshTokenRequest` para `record`, seguindo o padrão já usado nos outros DTOs de request (ex: `PatientRequest`).
- Adicionar validação (`@NotBlank`, `@Email` onde aplicável) nos campos desses DTOs.
- Adicionar `@Valid` nos parâmetros `@RequestBody` de `AuthController` que hoje não têm.

## Critérios de aceite

- [ ] `LoginRequest` e `RefreshTokenRequest` seguem o mesmo padrão (record + Bean Validation) dos demais DTOs de request.
- [ ] Requisições de login/refresh com campos ausentes ou vazios retornam `400` com mensagem de validação, antes de qualquer lógica de negócio ser executada.
- [ ] `AuthController` usa `@Valid` em todos os endpoints que recebem `@RequestBody`.
