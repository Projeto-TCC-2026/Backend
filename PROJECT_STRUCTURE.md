# Estrutura Completa do Projeto

## 📁 Visão Geral

```
tcc/
├── src/
│   ├── main/
│   │   ├── java/com/tcc/
│   │   │   ├── application/           # Camada de Aplicação
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/       # DTOs de requisição
│   │   │   │   │   └── response/      # DTOs de resposta
│   │   │   │   ├── mapper/            # Conversores Entity <-> DTO
│   │   │   │   └── service/           # Serviços de negócio
│   │   │   │
│   │   │   ├── domain/                # Camada de Domínio
│   │   │   │   ├── model/             # ✅ 14 Entidades JPA
│   │   │   │   └── repository/        # ✅ 14 Repositories
│   │   │   │
│   │   │   ├── exception/             # Exceções customizadas
│   │   │   │   ├── BusinessException.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   │
│   │   │   ├── infrastructure/        # Camada de Infraestrutura
│   │   │   │   ├── config/           # Configurações
│   │   │   │   │   └── OpenApiConfig.java
│   │   │   │   ├── persistence/      # Implementações de persistência
│   │   │   │   └── security/         # Configurações de segurança
│   │   │   │
│   │   │   ├── presentation/          # Camada de Apresentação
│   │   │   │   └── controller/       # REST Controllers
│   │   │   │       ├── ExampleController.java
│   │   │   │       └── HealthController.java
│   │   │   │
│   │   │   ├── util/                  # Classes utilitárias
│   │   │   └── TccApplication.java    # Classe principal
│   │   │
│   │   └── resources/
│   │       ├── application.properties  # ✅ Configurações da aplicação
│   │       └── db/migration/          # Scripts Flyway (opcional)
│   │
│   └── test/                          # Testes
│       └── java/com/tcc/
│           └── TccApplicationTests.java
│
├── pom.xml                            # Dependências Maven
├── README.md                          # Documentação principal
├── ENTITIES.md                        # ✅ Documentação das entidades
├── ENTITY_DIAGRAM.md                  # ✅ Diagrama visual
├── USAGE_EXAMPLES.md                  # ✅ Exemplos de uso
└── PROJECT_STRUCTURE.md               # ✅ Este arquivo
```

## ✅ Entidades Criadas (14 no total)

### Módulo de Autenticação e Usuários
1. **User** - `domain/model/User.java`
   - Usuários do sistema (base para Doctor e Patient)

### Módulo Hospitalar
2. **Hospital** - `domain/model/Hospital.java`
   - Hospitais cadastrados no sistema
3. **Doctor** - `domain/model/Doctor.java`
   - Médicos vinculados aos hospitais
4. **DoctorPatient** - `domain/model/DoctorPatient.java`
   - Relacionamento N:N entre médicos e pacientes

### Módulo de Pacientes
5. **Patient** - `domain/model/Patient.java`
   - Pacientes cadastrados no sistema
6. **PatientDevice** - `domain/model/PatientDevice.java`
   - Dispositivos médicos dos pacientes

### Módulo de Procedimentos Médicos
7. **Procedure** - `domain/model/Procedure.java`
   - Templates de procedimentos médicos
8. **PatientProcedure** - `domain/model/PatientProcedure.java`
   - Procedimentos agendados para pacientes
9. **ProcedureExecution** - `domain/model/ProcedureExecution.java`
   - Execuções reais dos procedimentos
10. **ProcedurePhoto** - `domain/model/ProcedurePhoto.java`
    - Fotos dos procedimentos executados

### Módulo de Monitoramento de Saúde
11. **ReadingImport** - `domain/model/ReadingImport.java`
    - Importações de leituras de dispositivos
12. **HealthReading** - `domain/model/HealthReading.java`
    - Leituras individuais de sinais vitais

### Módulo de Alertas e Notificações
13. **Alert** - `domain/model/Alert.java`
    - Alertas gerados por leituras anormais
14. **Notification** - `domain/model/Notification.java`
    - Notificações enviadas aos médicos

## ✅ Repositories Criados (14 no total)

Todos os repositories estão em `domain/repository/` e estendem `JpaRepository`:

1. `UserRepository.java`
2. `HospitalRepository.java`
3. `DoctorRepository.java`
4. `PatientRepository.java`
5. `DoctorPatientRepository.java`
6. `PatientDeviceRepository.java`
7. `ProcedureRepository.java`
8. `PatientProcedureRepository.java`
9. `ProcedureExecutionRepository.java`
10. `ProcedurePhotoRepository.java`
11. `ReadingImportRepository.java`
12. `HealthReadingRepository.java`
13. `AlertRepository.java`
14. `NotificationRepository.java`

Cada repository contém:
- Métodos padrão do JpaRepository (save, findById, findAll, delete, etc.)
- Métodos customizados de consulta (findBy..., existsBy..., etc.)

## 📊 Diagrama de Dependências

```
┌────────────────────────────────────────────┐
│         Presentation Layer                 │
│  (Controllers - REST Endpoints)            │
└─────────────────┬──────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────┐
│         Application Layer                  │
│  (Services, DTOs, Mappers)                 │
└─────────────────┬──────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────┐
│         Domain Layer                       │
│  (Entities, Repositories)                  │
│  ✅ 14 Entidades                           │
│  ✅ 14 Repositories                        │
└─────────────────┬──────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────┐
│         Infrastructure Layer               │
│  (Config, Security, Persistence)           │
└────────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────┐
│         Database (H2/PostgreSQL)           │
└────────────────────────────────────────────┘
```

## 🔧 Configuração do Banco de Dados

### H2 Database (Desenvolvimento)
```properties
spring.datasource.url=jdbc:h2:mem:tccdb
spring.datasource.username=sa
spring.datasource.password=

# Console H2
http://localhost:8080/h2-console
```

### Hibernate
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## 📚 Documentos de Referência

1. **ENTITIES.md** - Documentação completa de todas as entidades
2. **ENTITY_DIAGRAM.md** - Diagramas visuais dos relacionamentos
3. **USAGE_EXAMPLES.md** - Exemplos práticos de código
4. **PROJECT_STRUCTURE.md** - Este arquivo

## 🚀 Próximos Passos Sugeridos

### Fase 1: Camada de Aplicação
- [ ] Criar DTOs de Request em `application/dto/request/`
- [ ] Criar DTOs de Response em `application/dto/response/`
- [ ] Implementar Mappers em `application/mapper/`
- [ ] Implementar Services em `application/service/`

### Fase 2: Camada de Apresentação
- [ ] Criar Controllers REST em `presentation/controller/`
- [ ] Adicionar validações com Bean Validation
- [ ] Implementar tratamento global de exceções

### Fase 3: Segurança
- [ ] Configurar Spring Security
- [ ] Implementar autenticação JWT
- [ ] Adicionar controle de autorização por roles

### Fase 4: Qualidade
- [ ] Escrever testes unitários
- [ ] Escrever testes de integração
- [ ] Adicionar logs com SLF4J/Logback
- [ ] Implementar auditoria de entidades

### Fase 5: Produção
- [ ] Configurar PostgreSQL para produção
- [ ] Adicionar Flyway para migrations
- [ ] Configurar profiles (dev, test, prod)
- [ ] Implementar cache com Redis
- [ ] Adicionar monitoramento com Actuator

## 📦 Dependências Principais

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- API Documentation -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.2.0</version>
    </dependency>
    
    <!-- DevTools -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 🎯 Características Implementadas

✅ Arquitetura em camadas (Clean Architecture)
✅ 14 Entidades JPA com relacionamentos completos
✅ 14 Repositories com queries customizadas
✅ Configuração H2 Database
✅ Swagger/OpenAPI configurado
✅ Tratamento de exceções básico
✅ Timestamps automáticos (@PrePersist, @PreUpdate)
✅ Relacionamentos bidirecionais configurados
✅ Lazy loading para otimização

## 🌐 Endpoints Disponíveis

### Swagger UI
- **URL:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/api-docs

### H2 Console
- **URL:** http://localhost:8080/h2-console
- **JDBC URL:** jdbc:h2:mem:tccdb

### Health Check
- **URL:** http://localhost:8080/health

## 📝 Notas Importantes

1. **Relacionamentos Bidirecionais**: Todas as entidades com relacionamentos possuem navegação nos dois sentidos
2. **Cascade**: Configurado apropriadamente para evitar deleções acidentais
3. **Fetch Type**: LAZY por padrão para otimizar performance
4. **Unique Constraints**: CPF, CNPJ, CRM, Email são únicos
5. **Timestamps**: CreatedAt e UpdatedAt são gerenciados automaticamente

## 🔍 Como Explorar o Código

1. **Entidades**: Comece por `domain/model/` para entender o modelo de dados
2. **Repositories**: Veja `domain/repository/` para queries disponíveis
3. **Relacionamentos**: Consulte `ENTITY_DIAGRAM.md` para visualizar
4. **Exemplos**: Leia `USAGE_EXAMPLES.md` para ver código em ação

## 📞 Contato e Suporte

Para dúvidas ou sugestões sobre a estrutura do projeto, consulte a documentação adicional ou revise os exemplos de uso.
