---
inclusion: always
---

# Estrutura do projeto

**Este projeto usa arquitetura em camadas**, de forma pragmática, sob `com.tcc`. **Não é Clean Architecture nem Hexagonal** — não trate como se fosse.

Fluxo: `Controller → Service → Repository → Entidade JPA`

Os nomes de pacote expressam responsabilidade, mas as camadas **não** são independentes de framework: entidades usam Jakarta Persistence e repositories estendem interfaces do Spring Data. Isso é intencional, não é dívida a corrigir.

## Árvore

```text
src/main/java/com/tcc/
├── presentation/controller/    # endpoints REST, HTTP, autorização
├── application/
│   ├── dto/request/            # DTOs de entrada, validados
│   ├── dto/response/           # DTOs de saída e ApiResponse
│   ├── mapper/                 # conversão entidade/DTO, escrita à mão
│   └── service/                # interfaces e *Impl, no mesmo pacote
├── domain/
│   ├── model/                  # entidades JPA e relacionamentos
│   └── repository/             # contratos Spring Data e queries
├── infrastructure/
│   ├── security/               # filtro/serviço de JWT e config do Spring Security
│   └── config/                 # OpenAPI e configuração técnica
├── exception/                  # exceções e handler global
└── TccApplication.java

src/main/resources/
├── application.properties      # configuração compartilhada
├── application-dev.properties  # perfil H2
├── application-prod.properties # perfil PostgreSQL
└── db/migration/               # migrations Flyway ordenadas

src/test/java/com/tcc/          # teste usa o mesmo pacote da unidade testada
docs/                           # guias e exemplos .http executáveis
```

Não existe pacote `application/service/impl`. Interface e implementação ficam juntas em `application/service/`.

## Feature de referência

Hospital é a feature modelo. Ao criar feature nova, siga a forma destes arquivos:

- `src/main/java/com/tcc/domain/model/Hospital.java`
- `src/main/resources/db/migration/V2__create_hospitals_table.sql`
- `src/main/java/com/tcc/domain/repository/HospitalRepository.java`
- `src/main/java/com/tcc/application/dto/request/` e `src/main/java/com/tcc/application/dto/response/` (DTOs do Hospital)
- `src/main/java/com/tcc/application/mapper/HospitalMapper.java`
- `src/main/java/com/tcc/application/service/HospitalService.java` e `HospitalServiceImpl.java`
- `src/main/java/com/tcc/presentation/controller/HospitalController.java`
- `src/test/java/com/tcc/application/service/HospitalServiceImplTest.java`
- `docs/hospitals.http`

Essa também é a ordem em que criar os arquivos. A migration de uma feature nova recebe o próximo número livre, no formato `V<n>__create_<feature>_table.sql`. Nunca reaproveite um número já usado.

Para carregar esses arquivos no contexto, adicione-os com `#File` ou o pacote correspondente com `#Folder` antes de começar.

## ApiResponse: qual é qual

Existem dois tipos com o nome simples `ApiResponse`:

- `com.tcc.application.dto.response.ApiResponse` — o wrapper de resposta do projeto (classe)
- `io.swagger.v3.oas.annotations.responses.ApiResponse` — a annotation de documentação do springdoc

Regra:

- Importe o DTO: `import com.tcc.application.dto.response.ApiResponse;`
- Escreva a annotation singular por extenso: `@io.swagger.v3.oas.annotations.responses.ApiResponse(...)`
- Nunca importe `io.swagger.v3.oas.annotations.responses.ApiResponse`.
- `ApiResponses`, no plural, é outro tipo e não colide — pode importar normalmente.

## Formato de resposta

Toda resposta de **sucesso** usa o envelope `ApiResponse`.

| Situação | Status | Retorno |
|---|---|---|
| Leitura com dado | 200 | `ApiResponse<T>` |
| Leitura paginada | 200 | `ApiResponse<Page<T>>` |
| Criação | 201 | `ApiResponse<T>`, sem header `Location` |
| Atualização | 200 | `ApiResponse<T>` |
| Exclusão | 200 | `ApiResponse<Void>` via `ApiResponse.success()` |

Não use `204` e não devolva body cru em endpoint novo. Não altere o contrato de endpoint existente sem solicitação explícita — isso afeta os clientes da API.

**Erro:** controller nunca monta resposta de erro. Lance exceção tipada (`ResourceNotFoundException`, `BusinessException`, exceções de autorização/token) e deixe o `GlobalExceptionHandler` produzir o contrato de erro do projeto.

## Regras por camada

**Controller** — fica fino: valida com `@Valid`, aplica `@PreAuthorize`, documenta com OpenAPI, chama o service e embrulha o resultado em `ApiResponse<T>`. Todo endpoint novo ou alterado leva `@PreAuthorize` explícito. Endpoint intencionalmente público, como login e health check, é exceção e leva comentário justificando a exposição.

**Service** — dono das regras de negócio, do limite da transação, das checagens de "não encontrado" e "duplicado", e da orquestração. Sempre uma interface mais uma classe `*Impl` correspondente, no mesmo pacote.

**Repository** — interface Spring Data. Pode ter derived query, JPQL e projeção para DTO.

**DTO de request** — usa Jakarta Validation, normalmente `record`.

**Mapper** — concentra as conversões explícitas entre entidade e DTO. Projeção DTO produzida diretamente por query de repository é permitida e não exige mapper. Mantenha o mesmo tratamento de nulo e o mesmo padrão de update-in-place dos mappers vizinhos.

**Entidade JPA** — mapeamento explícito de tabela e coluna, acessores mutáveis, coleções inicializadas, callbacks de ciclo de vida para timestamp.

**Exceção** — lance tipada e deixe o `GlobalExceptionHandler` traduzir.

**Nomes** — pacote minúsculo, classe PascalCase, membro camelCase, método de service e de repository com nome descritivo. Nome de tabela no plural (`hospitals`), e o arquivo em `docs/` acompanha esse nome (`docs/hospitals.http`).

## Não faça

- Não injete `Repository` em controller. Controller chama service.
- Não devolva entidade JPA por endpoint REST. Sempre DTO de resposta.
- Não coloque SQL nem query de repository em controller ou service.
- Não coloque regra de negócio em controller nem em mapper.
- Não monte resposta de erro dentro do controller.
- Não coloque `@Transactional` em controller.
- Não use `@Autowired` em campo.
- Não crie pacote `service/impl`. Implementação fica junto da interface.
- Não crie ports/adapters, gateway de caso de uso, abstração de repository de domínio nem segunda implementação de persistência.
- Não crie estrutura paralela para feature nova. Encaixe no padrão que já existe.

## Quando o pedido conflitar com uma regra

- Nunca quebre uma regra em silêncio.
- Se a mudança foi pedida de propósito, implemente — mas diga qual regra está sendo quebrada e qual o impacto.
- Se a intenção não estiver clara, pergunte antes de escolher.
- Não crie caminho alternativo para contornar a regra sem avisar.
