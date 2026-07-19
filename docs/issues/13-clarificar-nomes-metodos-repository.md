# 13 — Clarificar nomes de métodos sobrecarregados no repository

**Prioridade:** Baixa
**Esforço:** Baixo
**Bloqueada por:** Nenhuma — pode começar imediatamente

## Contexto

`ProcedureExecutionRepository` (e outros repositórios com padrão semelhante) têm sobrecarga de método como `findByPatientId(Long)` (retorna `List`) e `findByPatientId(Long, Pageable)` (retorna `Page`). É uma sobrecarga válida em Java, mas o nome não deixa claro a diferença de comportamento até se olhar a assinatura completa — quem chama precisa saber de antemão qual variante está usando.

## O que precisa ser feito

- Renomear a variante paginada para deixar explícito o comportamento, ex: `findByPatientId(Long)` continua como está, e a paginada passa a se chamar algo como `findByPatientIdPaged(Long, Pageable)` — ou o inverso, mantendo a paginada com o nome mais "óbvio" já que é a mais usada nos Services atuais.
- Aplicar o mesmo critério de clareza nos demais repositórios que tenham sobrecarga semelhante.

## Critérios de aceite

- [ ] Não há dois métodos com o mesmo nome e comportamento visivelmente diferente (paginado vs. não paginado) sem distinção clara no nome.
- [ ] Todos os pontos de chamada foram atualizados para o novo nome, sem quebrar compilação.
