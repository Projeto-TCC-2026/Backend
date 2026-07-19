# Auditoria de Arquitetura — Backend TCC

> Documento gerado a partir da leitura completa do código-fonte (`src/main/java/com/tcc/**`), configs (`application*.properties`), `pom.xml`, `CONTEXT.md`, `.gitignore` e CI (`.github/workflows/ci.yml`).
> Nenhum arquivo de código foi alterado — este é um relatório **somente leitura**, para servir de base ao seu plano de correção.

---

## 1. Resumo executivo

O projeto **não implementa Clean Architecture**, embora os nomes dos pacotes (`domain`, `application`, `infrastructure`, `presentation`) sugiram isso. O que existe hoje é uma **arquitetura em camadas (Layered/N-Tier) do estilo Spring MVC clássico** — Controller → Service → Repository → Entity JPA — com uma nomenclatura de pastas "emprestada" da Clean Architecture, mas sem a regra que a define: **a Regra de Dependência** (camadas internas não podem depender de frameworks ou de camadas externas).

Os dois sintomas mais claros disso:

1. **`domain/model`** contém entidades anotadas com `@Entity`, `@Table`, `@Column`, `@OneToMany` etc. (JPA/Hibernate). Isso amarra o "domínio" ao framework de persistência — o oposto do que a Clean Architecture pede (entidades de domínio deveriam ser POJOs puros, sem `import jakarta.persistence.*`).
2. **`domain/repository`** contém interfaces que **estendem `JpaRepository`** (Spring Data). Ou seja, o "domínio" depende diretamente do Spring Data JPA — outra inversão da regra de dependência.

Isso não é incomum em projetos Spring Boot — é, na verdade, o padrão mais comum do ecossistema — mas **não é Clean Architecture**. É importante alinhar a expectativa: ou (a) você aceita que este é um projeto em arquitetura em camadas pragmática (o que é uma escolha legítima e muito usada no mercado), ou (b) você reestrutura para separar de fato regras de negócio de detalhes de framework. As duas opções são discutidas nas seções 10 e 12.

Achados adicionais relevantes: segredo JWT hardcoded e versionado no Git, inconsistência de padrões de DTO (record vs classe mutável), controller de autenticação que quebra o padrão de resposta usado no resto da API, ausência de validação (`@Valid`) no fluxo de login, zero testes automatizados além do teste gerado pelo Spring Initializr, e uma pasta de persistência "fantasma" que sugere uma tentativa anterior de separar camadas que foi abandonada.

---

## 2. Estrutura de pastas atual (mapa completo)

```
src/main/java/com/tcc/
├── TccApplication.java
├── application/
│   ├── dto/
│   │   ├── request/       (16 DTOs de entrada)
│   │   └── response/      (23 DTOs de saída)
│   ├── mapper/             (14 mappers manuais Entity <-> DTO)
│   └── service/            (10 arquivos: 5 interfaces + 5 *Impl)
├── domain/
│   ├── model/               (16 entidades @Entity JPA)
│   └── repository/          (15 interfaces extends JpaRepository)
├── exception/                (exceptions + @RestControllerAdvice)
├── infrastructure/
│   ├── config/               (OpenApiConfig)
│   ├── persistence/repository/  ⚠️ VAZIA (só .gitkeep)
│   └── security/             (JWT, SecurityConfig, UserDetailsService)
├── presentation/
│   └── controller/           (7 controllers, incluindo 2 de teste/exemplo)
└── util/                      ⚠️ VAZIA (só .gitkeep)
```

**Cobertura real de funcionalidades:** existem `domain/model` e `domain/repository` para `Alert`, `Notification`, `ProcedureExecution`, `ProcedurePhoto`, `ReadingImport`, `HealthReading`, `PatientDevice` etc., mas **não existem services nem controllers para essas entidades** (apenas para `User`, `Doctor`, `Hospital`, `Patient`, `Auth`). Ou seja, boa parte do domínio modelado (Alertas, Notificações, Procedimentos, Leituras de Saúde) tem persistência pronta mas nenhuma camada de aplicação/apresentação — funcionalidade incompleta, não um problema arquitetural em si, mas afeta a avaliação de "o que está pronto".

`src/test/java/com/tcc/` contém **apenas** o `TccApplicationTests.java` gerado pelo Spring Initializr (teste de contexto vazio). Não há nenhum teste unitário ou de integração no projeto.

---

## 3. Achados por camada

### 3.1 `domain/model` — Entidades

- Todas as 16 entidades são classes JPA (`@Entity`) com getters/setters públicos para **todos** os campos, incluindo coleções (`setDoctorPatients`, `setHealthReadings` etc.). Isso é um **modelo de domínio anêmico**: a "entidade" é apenas um saco de dados: comportamento de negócio (validações, invariantes) vive quase inteiramente nos `*ServiceImpl`.
  - Exceção parcial: `Patient.java` tem alguns métodos de negócio (`inactivate()`, `addProcedureExecution()`, `hasProcedureExecutions()`, `countProcedureExecutions()`) — é o único modelo com comportamento próprio. Os outros 15 são puramente getters/setters.
- `Patient.java` tem 337 linhas majoritariamente boilerplate de getters/setters (17 campos escalares + 6 coleções). Isso por si não é "errado", mas é o tipo de classe que se beneficiaria de Lombok (`@Getter @Setter`) só para reduzir ruído — hoje cada entidade nova exige repetir esse boilerplate manualmente.
- Nenhuma entidade usa `record` (o que é esperado — JPA não é totalmente compatível com records) nem value objects para conceitos como `Cpf`, `Email`, `Cnpj`, `Crm`. Essas validações (formato de CPF, e-mail etc.) estão hoje espalhadas entre Bean Validation nos DTOs de request (`@Pattern`) e checagens manuais no Service (`request.email().trim().isEmpty()`). É duplicação de responsabilidade: a regra "o que é um CPF válido" existe em mais de um lugar.

### 3.2 `domain/repository` — Portas de persistência

- As 15 interfaces estendem `JpaRepository<T, Long>` diretamente. Isso é o padrão-padrão do Spring Data e funciona bem — mas tecnicamente é uma dependência de "domínio" em relação a um framework de infraestrutura (Spring Data JPA), o que a Clean Architecture proíbe (o `domain` deveria depender só de abstrações que ele mesmo define, e a implementação Spring Data ficaria em `infrastructure`).
- Existe uma pasta **`infrastructure/persistence/repository/`** vazia (só `.gitkeep`). Isso é um forte indício de que em algum momento você (ou alguém) tentou separar "porta" (`domain/repository`) de "adaptador" (`infrastructure/persistence/repository`), mas a separação nunca foi concluída — hoje as duas pastas de repositório coexistem, uma cheia e uma vazia, o que é confuso para quem lê o projeto pela primeira vez.
- `ProcedureExecutionRepository.java`: tem `findByPatientId(Long)` (retorna `List`) e `findByPatientId(Long, Pageable)` (retorna `Page`) — sobrecarga válida, mas o nome não deixa claro a diferença até se olhar a assinatura. Um nome como `findAllByPatientId` vs `findByPatientId(..., Pageable)` ajudaria a legibilidade.

### 3.3 `application/service` — Casos de uso

- Padrão: para cada entidade principal existe `XService` (interface) + `XServiceImpl` (implementação), ambos no mesmo pacote `application/service`. Isso é abordado em detalhe na seção 10 (é a sua pergunta original).
- `AuthServiceImpl` (187 linhas) concentra: autenticação, emissão de JWT, geração/rotação de refresh token, montagem de 4 tipos de resposta diferentes (`AuthResponse`, `DoctorAuthResponse`, `PatientAuthResponse`, `RefreshTokenResponse`) e busca de perfil. Isso são pelo menos 3 responsabilidades distintas (autenticação, gestão de sessão/token, leitura de perfil) numa única classe — candidato a violação de **SRP** (ver seção 6).
- `PatientServiceImpl` e `DoctorServiceImpl` repetem a mesma receita: buscar entidade relacionada, checar unicidade de CPF, checar unicidade de e-mail, checar associação de usuário, mapear, salvar. É duplicação de estrutura entre os 4 services de CRUD (`User`, `Doctor`, `Hospital`, `Patient`) — não é grave, mas é a definição de "boilerplate repetido" que normalmente seria extraído para um método utilitário ou uma classe base, se o projeto crescer.
- Regras de negócio como "não é possível excluir paciente com procedimentos associados" (`PatientServiceImpl.deletePatient`) estão no Service, não na entidade `Patient`. Do ponto de vista de Clean Code/DDD isso é discutível dos dois lados — mas é inconsistente com o fato de `Patient` já ter `hasProcedureExecutions()` como método de negócio: a entidade "sabe" a resposta, mas é o Service que decide o que fazer com ela. Ok como está, mas mostra a linha borrada entre "onde mora a regra".

### 3.4 `application/dto` — DTOs de entrada/saída

- **Inconsistência de estilo:** a maioria dos `request` DTOs são `record` com Bean Validation (`PatientRequest`, `DoctorRequest`, `HospitalRequest`) — bom, moderno, imutável. Mas `LoginRequest.java` e `RefreshTokenRequest.java` são classes mutáveis (construtor vazio + getters/setters), **sem nenhuma anotação de validação** (`@NotBlank`, `@Email`).
  - Consequência prática: `AuthController.login()` não usa `@Valid` — um `email` ou `password` nulo/vazio passa direto para `AuthServiceImpl.findAndValidateUser`, chegando ao `passwordEncoder.matches(...)` ou à query do repositório sem qualquer validação de formato. Não é uma vulnerabilidade grave (o fluxo falha com "Credenciais inválidas"), mas é uma inconsistência de padrão que vale corrigir: ou todos os DTOs de request são records validados, ou nenhum é.
- 23 DTOs de resposta, a maioria records simples — consistentes entre si.

### 3.5 `application/mapper` — Conversão Entity ↔ DTO

- Os 14 mappers seguem um padrão consistente (`toResponse`, `toSummary`, `toEntity`, `updateEntity`) — este é o ponto **mais consistente** do projeto e está bem feito. Nenhuma crítica relevante aqui além do volume de código manual que uma lib como MapStruct eliminaria (troca de trade-off: menos boilerplate vs. mais uma dependência/anotação a aprender — decisão de time, não um erro).

### 3.6 `infrastructure/security`

- `JwtService`, `JwtAuthFilter`, `SecurityConfig`, `UserDetailsServiceImpl`, `PasswordConfig` — bem organizados e coesos, cada um com responsabilidade única. Este é o pacote mais próximo de um bom exemplo de SRP no projeto.
- Bug de configuração (não é arquitetural, mas vale registrar): `SecurityConfig` libera `"/api/health"` como público, mas o Actuator (sem `management.endpoints.web-base-path` customizado) expõe o health check em `/actuator/health`, não em `/api/health`. Ou seja, essa regra de `permitAll` hoje não protege nem libera nada real — é uma entrada morta na configuração.
- `TestController` também expõe `/api/test/generate-token?email=...` como **público** (liberado em `SecurityConfig`) e permite gerar um JWT válido para **qualquer e-mail informado**, sem autenticação nem verificação de que o e-mail existe. Isso é um endpoint de desenvolvimento esquecido em produção: hoje qualquer pessoa sem autenticação pode gerar um token JWT válido para se autenticar como qualquer usuário (ex: `admin@empresa.com`) e usá-lo nos demais endpoints. **Isso é uma falha de segurança real, não só de organização de código.**

### 3.7 `presentation/controller`

- Todos os controllers de CRUD (`UserController`, `DoctorController`, `HospitalController`, `PatientController`) retornam `ResponseEntity<ApiResponse<T>>` — padrão de envelope consistente.
- `AuthController` **quebra esse padrão**: retorna `ResponseEntity<AuthResponse>`, `ResponseEntity<DoctorAuthResponse>` etc. diretamente, sem o envelope `ApiResponse`. Um cliente da API (mobile/web) precisa tratar `/auth/login` de forma diferente de `/api/patients`, o que é uma inconsistência de contrato de API.
- `ExampleController` (`/api/example/**`) e `TestController` (`/api/test/**`) são resíduos de scaffolding/aprendizado do Swagger e do JWT. Estão registrados como controllers reais, documentados no Swagger, e (no caso do `TestController`) **acessíveis publicamente em produção** conforme a `SecurityConfig` atual.

### 3.8 `exception/`

- `GlobalExceptionHandler` é pequeno e direto — ok. Um ponto de atenção: `handleRuntime(RuntimeException ex)` devolve `ex.getMessage()` como `400 Bad Request` para **qualquer** `RuntimeException` não mapeada explicitamente (ex: `NullPointerException`, exceções do Hibernate). Isso pode vazar detalhes internos (nomes de coluna, stacktrace parcial, mensagens do driver JDBC) para o cliente da API — mistura "erro de negócio esperado" com "bug interno inesperado" sob o mesmo status HTTP e mesmo formato de mensagem.

### 3.9 `util/`

- Pasta vazia (só `.gitkeep`). Não é um problema, mas também não cumpre função nenhuma hoje — é um placeholder sem uso.

---

## 4. Análise SOLID

| Princípio | Onde é respeitado | Onde é violado / tensionado |
|---|---|---|
| **S — Single Responsibility** | `infrastructure/security/*` (cada classe faz uma coisa: filtro, geração de token, config, user details) | `AuthServiceImpl` mistura autenticação + emissão/rotação de refresh token + composição de 4 DTOs de resposta distintos. `SecurityConfig.securityFilterChain` também define o handler JSON de erro 401/403 inline (mistura configuração de segurança com formatação de resposta HTTP) |
| **O — Open/Closed** | Mappers e Services são fáceis de estender com novos métodos sem alterar os existentes | `GlobalExceptionHandler.handleRuntime` captura genericamente `RuntimeException` — qualquer nova exceção de runtime não intencional cai nesse handler por padrão, ao invés de estender o comportamento de forma explícita |
| **L — Liskov Substitution** | Não há herança de entidades/classes de domínio no projeto, então não há como violar (nem demonstrar) este princípio hoje | — |
| **I — Interface Segregation** | Interfaces de Service são pequenas e focadas por entidade (`PatientService`, `DoctorService` etc. — não há uma interface "genérica" inflada) | — |
| **D — Dependency Inversion** | Controllers dependem de interfaces de Service (`PatientController` depende de `PatientService`, não de `PatientServiceImpl`) — isso está correto | `domain/repository` depende de `JpaRepository` (Spring Data) em vez de o domínio definir sua própria abstração; `domain/model` depende de `jakarta.persistence.*`. A camada nomeada "domain" depende de frameworks externos — o núcleo do que a DIP (e a Clean Architecture) tentam evitar |

**Conclusão SOLID:** o princípio mais respeitado é o **DIP na fronteira Controller→Service** (via interface); o mais violado é o **DIP na fronteira Domain→Framework** (JPA e Spring Data infiltrados no "domínio"). O SRP tem um ponto de atenção real em `AuthServiceImpl`.

---

## 5. Clean Code — inconsistências e code smells

1. **Segredo JWT hardcoded e versionado no Git** (`application.properties` e `application-dev.properties`, chave `jwt.secret`). Confirmado via `git ls-files`: ambos os arquivos estão rastreados pelo Git. `application-prod.properties` **não sobrescreve `jwt.secret`**, ou seja, o valor de produção herda a chave hardcoded do `application.properties` padrão — a chave que assina os tokens JWT de produção está no histórico do repositório. Isso é crítico: qualquer pessoa com acesso ao repositório (inclusive um fork público, se houver) pode forjar tokens válidos.
2. **Mistura de convenções em DTOs**: `record` imutável + Bean Validation (maioria) vs. classe mutável sem validação (`LoginRequest`, `RefreshTokenRequest`). Escolha uma convenção e aplique em todo o pacote `dto/request`.
3. **Sufixo `Impl`** (`PatientServiceImpl`, `AuthServiceImpl` etc.) é um padrão comum no ecossistema Spring, mas é considerado code smell por Clean Code (Robert C. Martin cita isso: o nome deveria revelar intenção, não implementação). Ver discussão dedicada na seção 6.
4. **Controllers "de exemplo" em produção**: `ExampleController` e `TestController` não têm relação com o domínio do sistema (pacientes, médicos, saúde) e estão publicamente acessíveis. Deveriam ser removidos ou isolados sob um profile de desenvolvimento (`@Profile("dev")`) que nunca é ativado em produção.
5. **Comentários em português explicando o "o quê" em vez do "porquê"** (ex: `// Verificar se o usuário existe` acima de uma linha autoexplicativa `userRepository.findById(...)`). Não é grave, mas é ruído — Clean Code prega comentários que expliquem decisão/racional, não parafraseiam o código.
6. **Duplicação estrutural entre os 4 Services de CRUD** (User, Doctor, Hospital, Patient): mesmo esqueleto de "buscar → validar unicidade → mapear → salvar" repetido manualmente 4 vezes.
7. **Mensagens de exceção como strings inline concatenadas** (`"Já existe um paciente ativo cadastrado com o CPF: " + request.cpf()`) repetidas com pequenas variações em `PatientServiceImpl`, `DoctorServiceImpl`, `HospitalServiceImpl`, `UserServiceImpl`. Nenhum catálogo central de mensagens — se o texto mudar (ex: por causa de i18n futuro), é preciso caçar todas as ocorrências manualmente.

---

## 6. Sua pergunta central: Service + Interface + sufixo `Impl` no mesmo diretório

Respondendo diretamente ao que você citou como exemplo:

**Isso é uma violação de Clean Architecture?** Não, isoladamente não é. É simplesmente a convenção mais comum em projetos Spring (interface pública + implementação com sufixo `Impl`), e ter as duas no mesmo pacote (`application/service`) é aceitável — muitos projetos Spring de referência fazem exatamente isso.

**Mas há dois problemas reais aqui, e vale separá-los:**

1. **A interface hoje não cumpre a função que normalmente justifica sua existência.** O motivo mais comum para se criar `XService` + `XServiceImpl` é permitir troca de implementação (ex: testes com mock, ou múltiplas estratégias). Neste projeto:
   - Não há testes que mockem `PatientService`, `DoctorService` etc.
   - Não há mais de uma implementação de nenhuma dessas interfaces.
   - Ou seja, hoje a interface é *generalização especulativa* (YAGNI violado): existe "porque é o padrão Spring", não porque resolve um problema concreto agora. Isso não é um erro grave, mas é importante você saber que a interface, por si, não te dá nenhum benefício de Clean Architecture enquanto não houver testes ou implementações alternativas usando essa abstração.

2. **O sufixo `Impl` é, tecnicamente, um code smell de nomenclatura** (não uma violação arquitetural). Clean Code recomenda nomes que revelem *intenção*, não que descrevam "isto é uma implementação". Alternativas comuns quando você tem só uma implementação e quer manter a interface por causa de testes: nomear a implementação por sua estratégia real (ex: `JpaPatientService`, `DefaultPatientService`) — ou, mais simples, remover a interface e ter apenas `PatientService` como classe concreta até que exista uma segunda implementação real.

**Onde estaria fisicamente essa separação, numa Clean Architecture "de livro"?**
- A interface (o *caso de uso*, também chamado de *port*) pertenceria à camada de aplicação/domínio, pois é *quem consome* que deveria "possuir" o contrato (Dependency Inversion: o cliente define a interface, não quem implementa).
- A implementação, se ela dependesse de infraestrutura (banco, JWT, etc.), seria um *adapter* e moraria em `infrastructure`, não junto da interface.
- No seu caso, os `*ServiceImpl` dependem de `domain.repository` (que por sua vez depende de JPA) — então, tecnicamente, mesmo a implementação do "caso de uso" já está contaminada por infraestrutura. Separar a pasta física da interface e da implementação não resolveria o problema de fundo, que é a entidade e o repositório dependerem de JPA/Spring Data desde a raiz.

**Conclusão prática:** manter Service + ServiceImpl juntos em `application/service` é uma escolha de estilo aceitável e comum — não é isso que está impedindo o projeto de ser "Clean Architecture". O que impede de fato é o `domain/model` e o `domain/repository` dependerem de JPA/Spring Data. Se seu objetivo para o TCC é *defender* que a arquitetura segue Clean Architecture, o ponto que um banca/avaliador mais provavelmente vai questionar é esse — não o sufixo `Impl`.

---

## 7. Segurança — resumo dos achados

| Achado | Severidade | Local |
|---|---|---|
| `jwt.secret` hardcoded e versionado no Git; produção herda o valor de dev | Alta | `application.properties`, `application-dev.properties` |
| `TestController./api/test/generate-token` gera JWT válido para qualquer e-mail, sem autenticação, endpoint público | Alta | `SecurityConfig` (permitAll `/api/test/**`) + `TestController.java` |
| `handleRuntime` devolve `ex.getMessage()` de qualquer `RuntimeException` não tratada como resposta 400 ao cliente | Média | `GlobalExceptionHandler.java` |
| `LoginRequest`/`RefreshTokenRequest` sem `@Valid`/Bean Validation no `AuthController` | Baixa | `AuthController.java`, `LoginRequest.java` |
| Regra `permitAll("/api/health")` não corresponde ao path real do Actuator (`/actuator/health`) — configuração morta, não é vulnerabilidade, mas é confusa | Baixa | `SecurityConfig.java` |

`application-prod.properties` faz a coisa certa com `spring.datasource.password=${DB_PASSWORD}` (externalizado via variável de ambiente) — o problema de segredo hardcoded está isolado ao JWT, não ao banco de dados.

---

## 8. Testes

- Único arquivo de teste no projeto: `src/test/java/com/tcc/TccApplicationTests.java` — o teste padrão gerado pelo `Spring Initializr` (`contextLoads()`), sem nenhuma asserção de negócio.
- Não há testes unitários dos Services, dos Mappers, das regras de negócio (unicidade de CPF, inativação, etc.), nem testes de integração dos Controllers/Security.
- O CI (`ci.yml`) executa `./mvnw clean package`, que roda os testes existentes (só o de contexto) — ou seja, o pipeline "passa" hoje independentemente de qualquer regressão de lógica de negócio, porque não há teste que a cubra.

---

## 9. O que está bem feito (para não perder de vista)

- Separação de DTOs de request/response é consistente e bem nomeada.
- Mappers seguem um padrão único e previsível em todo o projeto.
- `infrastructure/security` é o pacote mais coeso e com melhor SRP do projeto.
- Uso de `record` para a maioria dos DTOs modernos, com Bean Validation aplicada corretamente.
- Migrations do Flyway numeradas e incrementais (20 versões), sem edição retroativa aparente.
- Externalização correta da senha do banco em produção via variável de ambiente.
- Uso de `@Transactional(readOnly = true)` em operações de leitura — detalhe de performance/clareza bem aplicado.

---

## 10. Tabela de prioridades para correção

| # | Item | Severidade | Esforço estimado |
|---|---|---|---|
| 1 | Remover `jwt.secret` do código versionado; mover para variável de ambiente em todos os profiles | Alta (segurança) | Baixo |
| 2 | Restringir ou remover `TestController` (`/api/test/generate-token`) do build de produção | Alta (segurança) | Baixo |
| 3 | Remover `ExampleController` (não pertence ao domínio) | Baixa | Baixo |
| 4 | Padronizar `AuthController` para usar `ApiResponse<T>` como os demais controllers | Média (consistência de API) | Baixo |
| 5 | Adicionar `@Valid` + Bean Validation em `LoginRequest`/`RefreshTokenRequest` (ou convertê-los em `record`) | Média | Baixo |
| 6 | Separar `handleRuntime`/`handleGeneric` para não expor mensagens internas ao cliente | Média (segurança/robustez) | Baixo |
| 7 | Decidir conscientemente: manter arquitetura em camadas (documentar isso no `CONTEXT.md` em vez de "Clean Architecture") **ou** iniciar a separação real de domínio puro vs. JPA | Alta (estrutural) | Alto |
| 8 | Remover a pasta vazia `infrastructure/persistence/repository` ou usá-la de fato como camada de adapter | Baixa | Baixo/Alto (depende da decisão do item 7) |
| 9 | Adicionar testes unitários mínimos para as regras de negócio dos Services (unicidade de CPF/e-mail, inativação, exclusão bloqueada) | Alta (qualidade) | Médio |
| 10 | Avaliar remoção do sufixo `Impl` ou justificar sua permanência com testes que mockem as interfaces | Baixa (nomenclatura) | Baixo |

---

## 11. Duas rotas possíveis a partir daqui

**Rota A — Aceitar que é uma arquitetura em camadas pragmática (mais rápida, menos risco para um TCC perto do fim):**
Atualizar o `CONTEXT.md` para descrever a arquitetura real ("Arquitetura em Camadas inspirada em Clean Architecture") em vez de afirmar Clean Architecture, e focar as correções nos itens 1–6 e 9 da tabela acima (segurança, consistência, testes). É a rota de menor esforço e menor risco de regressão.

**Rota B — Migrar de fato para Clean Architecture (mais trabalho, mais correto academicamente):**
Passos, em ordem:
1. Criar `domain/model` como POJOs puros (sem `@Entity`), extraindo o mapeamento JPA para classes de persistência separadas em `infrastructure/persistence/entity` (padrão "Entity vs. Domain Model" / DTO de persistência).
2. Definir interfaces de repositório no domínio **sem** estender `JpaRepository` (ex: `interface PatientRepository { Optional<Patient> findById(Long id); ... }`), e implementá-las em `infrastructure/persistence` usando Spring Data internamente (adapter).
3. Mover regras de negócio hoje presas em `*ServiceImpl` (unicidade, invariantes) para dentro das entidades de domínio ou de serviços de domínio puros, deixando os `*ServiceImpl`/casos de uso apenas orquestrarem chamadas.
4. Só então a discussão sobre nome de pacote de "Service"/"Impl"/interface passa a ter um efeito real de isolamento, porque a camada de aplicação deixaria de depender de JPA transitivamente.

Essa rota é significativamente mais trabalhosa (reescreve o núcleo do projeto) e só compensa se o objetivo do TCC for demonstrar Clean Architecture na prática — se o objetivo é apenas "ter um backend funcional e defensável", a Rota A é mais realista dado o volume já implementado (16 entidades, 20 migrations, 4 CRUDs completos).

---

*Este relatório reflete o estado do código no momento da análise. Nenhuma alteração foi feita nos arquivos do projeto.*
