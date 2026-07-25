# Contexto do Projeto

## Architecture

O TCC Saúde adota uma **arquitetura em camadas**, com o fluxo principal de dependências e chamadas:

```text
Controller → Service → Repository → Entity JPA → Banco de dados
```

As camadas reais são organizadas nos seguintes pacotes:

- `presentation`: controllers REST, validação da entrada HTTP e autorização;
- `application`: services, DTOs e mappers, concentrando casos de uso e regras de negócio;
- `domain`: entidades JPA e repositories Spring Data que estendem `JpaRepository`;
- `infrastructure`: configurações técnicas de segurança, JWT e OpenAPI.

Essa separação mantém controllers enxutos, concentra a orquestração nos services e isola o acesso a dados nos repositories. As entidades e os contratos de persistência, porém, utilizam diretamente JPA e Spring Data; portanto, as camadas não são independentes dos frameworks de persistência.

### Decisão arquitetural

A arquitetura em camadas foi mantida conscientemente por pragmatismo, considerando o escopo acadêmico do TCC, o prazo disponível e a simplicidade de implementação e manutenção. A estrutura aproveita princípios de separação de responsabilidades sem exigir o isolamento e as abstrações adicionais de uma arquitetura orientada a domínio independente de frameworks.
