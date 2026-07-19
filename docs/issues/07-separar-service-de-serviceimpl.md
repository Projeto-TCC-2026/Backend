# 07 — Separar interface de Service da implementação Impl

**Prioridade:** Média-Alta (organização/decisão já tomada por você)
**Esforço:** Médio
**Bloqueada por:** Nenhuma — pode começar imediatamente

## Contexto

Hoje `application/service/` contém, no mesmo diretório, as interfaces (`PatientService`, `DoctorService`, `HospitalService`, `UserService`, `AuthService`) e suas implementações com sufixo `Impl` (`PatientServiceImpl`, `DoctorServiceImpl` etc.). Você decidiu manter a convenção `Impl`, mas **não quer implementação e interface no mesmo path**.

## O que precisa ser feito

- Definir um novo path para as implementações, mantendo as interfaces onde estão hoje (`application/service/`). Sugestão: `application/service/impl/` (implementação continua fazendo parte da camada de aplicação, só em subpacote próprio) — evite mover para `infrastructure`, pois essas classes não são adapters de framework, são orquestração de casos de uso.
- Mover `PatientServiceImpl`, `DoctorServiceImpl`, `HospitalServiceImpl`, `UserServiceImpl`, `AuthServiceImpl` para o novo path/pacote.
- Ajustar os imports afetados (controllers e testes futuros continuam dependendo apenas da interface, então o impacto de import deve ser mínimo — só a classe `@Service` concreta muda de pacote).
- Confirmar que a injeção de dependência do Spring continua funcionando (component scan cobre o novo subpacote automaticamente, já que está dentro do mesmo pacote-base escaneado).

## Critérios de aceite

- [ ] Nenhuma interface de Service e sua implementação estão no mesmo diretório.
- [ ] Todos os controllers continuam dependendo apenas das interfaces (`PatientService`, não `PatientServiceImpl`).
- [ ] Aplicação sobe normalmente e os beans de Service são injetados sem erro (Spring Context carrega OK).
- [ ] Estrutura final documentada (atualizar `docs/AUDITORIA_ARQUITETURA.md` ou `CONTEXT.md` com o novo path, se aplicável).
