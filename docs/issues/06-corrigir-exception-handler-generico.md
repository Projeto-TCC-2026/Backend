# 06 — Corrigir tratamento genérico de exceções

**Prioridade:** Média (robustez/segurança)
**Esforço:** Baixo
**Bloqueada por:** Nenhuma — pode começar imediatamente

## Contexto

`GlobalExceptionHandler.handleRuntime(RuntimeException ex)` devolve `ex.getMessage()` como corpo de uma resposta `400 Bad Request` para **qualquer** `RuntimeException` não mapeada explicitamente — incluindo exceções inesperadas como `NullPointerException` ou erros do Hibernate/driver JDBC. Isso mistura "erro de negócio esperado" (ex: `BusinessException`) com "bug interno inesperado" sob o mesmo formato de resposta, e pode vazar detalhes internos (nomes de coluna, mensagens de driver) para o cliente da API.

## O que precisa ser feito

- Remover (ou restringir bastante) o `@ExceptionHandler(RuntimeException.class)` genérico.
- Exceções de negócio conhecidas continuam sendo tratadas pelos handlers específicos já existentes (`BusinessException`, `ResourceNotFoundException`, `UnauthorizedException`, `InvalidTokenException`).
- Qualquer exceção não mapeada (incluindo `RuntimeException` genérica) deve cair no handler de `Exception.class`, retornando `500` com uma mensagem genérica (sem `ex.getMessage()` bruto), e logando o stack trace completo no servidor para investigação.

## Critérios de aceite

- [ ] Exceções de negócio conhecidas continuam retornando o status HTTP correto com a mensagem de negócio.
- [ ] Exceções inesperadas (`RuntimeException` não mapeada, `NullPointerException` etc.) retornam `500` com mensagem genérica ao cliente, sem detalhes internos.
- [ ] O stack trace completo de exceções inesperadas é logado no servidor (não descartado silenciosamente).
