# 10 — Adicionar testes unitários das regras de negócio

**Prioridade:** Alta (qualidade/confiabilidade)
**Esforço:** Médio
**Bloqueada por:** Nenhuma — pode começar imediatamente (mas idealmente depois da issue 07, para os testes já nascerem apontando para o path final dos Services)

## Contexto

O único teste do projeto é `TccApplicationTests.java`, o teste padrão do Spring Initializr (`contextLoads()`, sem asserção de negócio). Não há nenhum teste cobrindo as regras de negócio já implementadas nos Services (unicidade de CPF/e-mail, associação de usuário a paciente/médico, bloqueio de exclusão com procedimentos associados, inativação). O CI (`ci.yml`) roda `mvnw clean package`, mas como não há testes de negócio, o pipeline "passa" mesmo que uma regra seja quebrada por acidente.

## O que precisa ser feito

Escrever testes unitários (com mocks de repositório, sem subir contexto Spring completo) cobrindo pelo menos:

- `PatientServiceImpl`: criação com CPF duplicado (deve lançar `BusinessException`), criação com e-mail duplicado, criação com usuário já associado a outro paciente, exclusão bloqueada quando há `ProcedureExecution` ou `HealthReading` associados, inativação bem-sucedida.
- `DoctorServiceImpl` e `HospitalServiceImpl`: mesma lógica de unicidade (CRM/CNPJ conforme aplicável).
- `UserServiceImpl`: criação com e-mail duplicado, validação de role inválida.
- `AuthServiceImpl`: login com credenciais inválidas (`UnauthorizedException`), login de médico com usuário que não é `DOCTOR`, refresh token expirado/revogado (`InvalidTokenException`).

## Critérios de aceite

- [ ] Cada regra de negócio listada acima tem pelo menos um teste unitário cobrindo o caminho de sucesso e o caminho de exceção.
- [ ] Testes usam mocks (ex: Mockito) para os repositórios — não dependem de banco real.
- [ ] `mvnw clean package` no CI passa com os novos testes incluídos e falha se uma dessas regras for quebrada.
