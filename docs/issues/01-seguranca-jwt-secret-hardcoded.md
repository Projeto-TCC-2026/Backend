# 01 — Remover segredo JWT hardcoded do repositório

**Prioridade:** Alta (segurança)
**Esforço:** Baixo
**Bloqueada por:** Nenhuma — pode começar imediatamente

## Contexto

`jwt.secret` está com valor fixo em `application.properties` e `application-dev.properties`, e ambos os arquivos estão versionados no Git (confirmado via `git ls-files`). `application-prod.properties` não sobrescreve `jwt.secret`, ou seja, **produção herda a mesma chave que está no histórico do repositório**. Qualquer pessoa com acesso ao código (inclusive um fork, se o repositório for ou vier a ser público) pode forjar tokens JWT válidos para qualquer usuário.

## O que precisa ser feito

- Remover o valor literal de `jwt.secret` de `application.properties` e `application-dev.properties`.
- Definir `jwt.secret` via variável de ambiente em todos os profiles (`${JWT_SECRET}`), incluindo produção.
- Gerar uma nova chave secreta (a atual deve ser considerada comprometida, já que está no histórico do Git) e guardá-la fora do repositório (variável de ambiente, secret manager, `.env` já ignorado pelo Git).
- Revogar/expirar todos os refresh tokens existentes emitidos com a chave antiga, já que a rotação de chave invalida a verificação de tokens antigos.

## Critérios de aceite

- [ ] Nenhum arquivo versionado no Git contém o valor real de `jwt.secret`.
- [ ] `application.properties`, `application-dev.properties` e `application-prod.properties` referenciam `jwt.secret` via `${...}` (variável de ambiente), sem fallback com valor literal sensível.
- [ ] Aplicação sobe normalmente em dev e prod com a variável de ambiente configurada.
- [ ] Nova chave gerada e antiga considerada comprometida/descartada.
