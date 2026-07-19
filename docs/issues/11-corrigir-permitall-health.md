# 11 — Corrigir regra permitAll de /api/health

**Prioridade:** Baixa
**Esforço:** Baixo
**Bloqueada por:** Nenhuma — pode começar imediatamente

## Contexto

`SecurityConfig` libera `"/api/health"` como público, mas o Actuator (sem `management.endpoints.web-base-path` customizado) expõe o health check em `/actuator/health`, não em `/api/health`. Essa regra de `permitAll` hoje não corresponde a nenhum endpoint real — é uma configuração morta que pode confundir quem for revisar as regras de segurança depois.

## O que precisa ser feito

- Decidir o path real desejado para o health check: manter o padrão do Actuator (`/actuator/health`) e ajustar a regra em `SecurityConfig`, ou configurar `management.endpoints.web-base-path=/api` para que `/api/health` passe a existir de fato.
- Ajustar `SecurityConfig` para liberar o path correto.

## Critérios de aceite

- [ ] O path liberado em `SecurityConfig` corresponde a um endpoint real do Actuator.
- [ ] `curl` (ou equivalente) no path liberado retorna o health check sem autenticação.
