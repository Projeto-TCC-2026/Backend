# 03 — Remover ExampleController

**Prioridade:** Baixa
**Esforço:** Baixo
**Bloqueada por:** Nenhuma — pode começar imediatamente

## Contexto

`ExampleController` (`/api/example/**`) é um resíduo de scaffolding para demonstrar o Swagger UI. Não tem relação com o domínio do sistema (pacientes, médicos, saúde) e está registrado e documentado como se fosse um recurso real da API.

## O que precisa ser feito

- Remover `ExampleController` e qualquer DTO/config exclusiva dele, se houver.
- Confirmar que nenhuma outra parte do projeto (testes, documentação `.http`, Swagger tags) referencia esse controller.

## Critérios de aceite

- [ ] `ExampleController.java` removido.
- [ ] `/api/example/**` não aparece mais no Swagger UI.
- [ ] Build continua passando após a remoção.
