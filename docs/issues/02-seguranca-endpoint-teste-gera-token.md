# 02 — Proteger ou remover endpoint público de geração de token

**Prioridade:** Alta (segurança)
**Esforço:** Baixo
**Bloqueada por:** Nenhuma — pode começar imediatamente

## Contexto

`TestController./api/test/generate-token` gera um JWT válido para **qualquer e-mail informado via query param**, sem exigir autenticação e sem verificar se o e-mail corresponde a um usuário real. O path `/api/test/**` está liberado (`permitAll`) em `SecurityConfig`. Na prática, hoje qualquer pessoa sem login pode gerar um token válido para se autenticar como qualquer usuário (inclusive um e-mail de admin) e usá-lo nos demais endpoints da API.

## O que precisa ser feito

Escolher uma das opções:

- **Opção A (recomendada):** remover `TestController` do código de produção. Se ele ainda for útil para depuração local, isolá-lo sob um profile que nunca é ativado em produção (ex: `@Profile("dev")`), e remover `/api/test/**` do `permitAll` em `SecurityConfig` para qualquer ambiente que não seja dev local.
- **Opção B:** manter o endpoint, mas exigir autenticação de ADMIN para gerá-lo, e nunca habilitar o profile em produção.

## Critérios de aceite

- [ ] `/api/test/generate-token` não está acessível publicamente em produção.
- [ ] `SecurityConfig` não libera `/api/test/**` (ou libera apenas quando o profile de dev está ativo, de forma explícita e auditável).
- [ ] Build de produção não expõe nenhum endpoint capaz de emitir tokens sem autenticação.
