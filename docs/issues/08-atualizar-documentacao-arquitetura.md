# 08 — Atualizar documentação para refletir arquitetura em camadas

**Prioridade:** Média
**Esforço:** Baixo
**Bloqueada por:** Nenhuma — pode começar imediatamente (mas faz sentido revisar depois da issue 07, para descrever o path final correto)

## Contexto

`CONTEXT.md` descreve a arquitetura do projeto como "Layered Clean Architecture". Após a auditoria, você decidiu conscientemente aceitar que o projeto é uma **arquitetura em camadas** inspirada em Clean Architecture, não uma Clean Architecture de fato (o `domain/model` depende de JPA, `domain/repository` estende `JpaRepository`). Isso é uma escolha válida — mas a documentação deve dizer isso corretamente, para não gerar expectativa equivocada (inclusive numa eventual defesa de TCC).

## O que precisa ser feito

- Atualizar a seção "Architecture" de `CONTEXT.md` para descrever o projeto como arquitetura em camadas (Controller → Service → Repository → Entity JPA), citando as camadas reais (`presentation`, `application`, `domain`, `infrastructure`) sem afirmar Clean Architecture.
- Revisar `README.md` e demais docs em `docs/` que também mencionem "Clean Architecture" e ajustar a linguagem.
- Documentar explicitamente a decisão: por que optou por camadas em vez de Clean Architecture "de livro" (pragmatismo, escopo de TCC, tempo).

## Critérios de aceite

- [ ] `CONTEXT.md` não afirma mais "Clean Architecture" — descreve a arquitetura em camadas real.
- [ ] Nenhum outro documento em `docs/` contradiz essa descrição.
- [ ] A decisão de manter arquitetura em camadas está registrada por escrito (mesmo que em uma frase).
