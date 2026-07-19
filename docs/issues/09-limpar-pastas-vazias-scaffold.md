# 09 — Resolver pastas vazias de scaffold

**Prioridade:** Baixa
**Esforço:** Baixo
**Bloqueada por:** 07 — Separar interface de Service da implementação Impl

## Contexto

Duas pastas existem só com `.gitkeep`, sem uso real: `infrastructure/persistence/repository/` e `util/`. A primeira sugere uma tentativa anterior de separar "porta" (`domain/repository`) de "adapter" (`infrastructure/persistence/repository`) que nunca foi concluída — hoje ela só confunde quem lê a estrutura, já que os repositórios reais estão todos em `domain/repository`.

## O que precisa ser feito

- Como você aceitou a arquitetura em camadas (issue 08), a pasta `infrastructure/persistence/repository/` não tem função — remover, ou documentar explicitamente que fica reservada para uma futura separação (se for uma intenção real e não só um resquício).
- `util/` está vazia — remover se não há plano concreto de uso, ou adicionar um `README.md` de uma linha explicando para que serve, se for mantida de propósito.

## Critérios de aceite

- [ ] `infrastructure/persistence/repository/` foi removida ou tem uma justificativa documentada para existir vazia.
- [ ] `util/` foi removida ou tem uma justificativa documentada para existir vazia.
- [ ] Estrutura de pastas final não tem diretórios "fantasma" sem explicação.
