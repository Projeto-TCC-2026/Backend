# Issues — Backend TCC

Cada arquivo desta pasta é uma issue individual, derivada da auditoria em `docs/AUDITORIA_ARQUITETURA.md`, já ajustada com as decisões que você tomou:

- ✅ Aceito: o projeto é uma **arquitetura em camadas** (não Clean Architecture) — não haverá issue de "migrar para Clean Architecture".
- ✅ Aceito: manter `ServiceImpl` como convenção, **mas separado do path das interfaces** de Service.
- ✅ Aceito: entidades ainda não usadas (Alert, Notification, ProcedureExecution, HealthReading, PatientDevice, ReadingImport, ProcedurePhoto) **não são um problema** — ficam para uso futuro, sem issue de remoção/limpeza.

Nenhuma issue foi implementada ainda — este é só o planejamento. Trabalhe uma por vez.

## Índice por prioridade

| # | Issue | Prioridade | Esforço | Bloqueada por |
|---|---|---|---|---|
| 01 | [Remover segredo JWT hardcoded do repositório](01-seguranca-jwt-secret-hardcoded.md) | Alta | Baixo | Nenhuma |
| 02 | [Proteger/remover endpoint público de geração de token](02-seguranca-endpoint-teste-gera-token.md) | Alta | Baixo | Nenhuma |
| 10 | [Adicionar testes unitários das regras de negócio](10-testes-unitarios-regras-negocio.md) | Alta | Médio | Nenhuma |
| 07 | [Separar interface de Service da implementação Impl](07-separar-service-de-serviceimpl.md) | Média-Alta | Médio | Nenhuma |
| 04 | [Padronizar resposta do AuthController com ApiResponse](04-padronizar-authcontroller-apiresponse.md) | Média | Baixo | Nenhuma |
| 05 | [Validar DTOs de login e refresh token](05-validar-dtos-login-refresh-token.md) | Média | Baixo | Nenhuma |
| 06 | [Corrigir tratamento genérico de exceções](06-corrigir-exception-handler-generico.md) | Média | Baixo | Nenhuma |
| 08 | [Atualizar documentação para refletir arquitetura em camadas](08-atualizar-documentacao-arquitetura.md) | Média | Baixo | Nenhuma |
| 03 | [Remover ExampleController](03-remover-example-controller.md) | Baixa | Baixo | Nenhuma |
| 09 | [Resolver pastas vazias de scaffold](09-limpar-pastas-vazias-scaffold.md) | Baixa | Baixo | 07 |
| 11 | [Corrigir regra permitAll de /api/health](11-corrigir-permitall-health.md) | Baixa | Baixo | Nenhuma |
| 12 | [Centralizar mensagens de erro duplicadas](12-centralizar-mensagens-erro.md) | Baixa | Baixo | Nenhuma |
| 13 | [Clarificar nomes de métodos sobrecarregados no repository](13-clarificar-nomes-metodos-repository.md) | Baixa | Baixo | Nenhuma |

## Sugestão de ordem de execução

1. Comece pelas de segurança (01, 02) — são as de maior risco real e mais rápidas de resolver.
2. Em seguida, consistência de API e validação (04, 05, 06) — rápidas e reduzem confusão para quem consome a API.
3. Depois a reorganização estrutural (07) e a atualização da documentação (08), que andam juntas.
4. Feche com testes (10) cobrindo as regras de negócio já existentes — idealmente depois de 07, para os testes já nascerem apontando para a estrutura final.
5. As de limpeza/nomenclatura (03, 09, 11, 12, 13) podem ser feitas a qualquer momento, intercaladas — são baixo risco e baixo esforço.
