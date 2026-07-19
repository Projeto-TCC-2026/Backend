# 12 — Centralizar mensagens de erro duplicadas

**Prioridade:** Baixa
**Esforço:** Baixo
**Bloqueada por:** Nenhuma — pode começar imediatamente

## Contexto

Mensagens de exceção de negócio (ex: "Já existe um paciente ativo cadastrado com o CPF: ...") estão escritas como strings inline concatenadas, repetidas com pequenas variações em `PatientServiceImpl`, `DoctorServiceImpl`, `HospitalServiceImpl` e `UserServiceImpl`. Não há um catálogo central — se o texto precisar mudar (revisão de copy, i18n futuro), é preciso caçar todas as ocorrências manualmente.

## O que precisa ser feito

- Extrair as mensagens de erro repetidas para constantes (ex: classe `ErrorMessages` ou similar) ou para um arquivo de mensagens (`messages.properties`, se for adotar i18n).
- Atualizar os Services para referenciar essas constantes em vez de strings inline.

## Critérios de aceite

- [ ] Mensagens de erro de negócio não estão mais duplicadas como literais de string em múltiplos Services.
- [ ] Alterar uma mensagem de erro exige editar um único lugar.
