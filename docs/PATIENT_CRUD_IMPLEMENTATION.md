# 🏥 Implementação CRUD de Pacientes

## ✅ Status: CONCLUÍDO

Implementação completa do módulo CRUD de Pacientes seguindo os requisitos especificados.

---

## 📋 Requisitos Implementados

### ✅ Funcionalidades
- **Cadastro de Paciente** - POST /api/patients
- **Edição de Paciente** - PUT /api/patients/{id}  
- **Busca de Paciente** - GET /api/patients/{id}
- **Listagem de Pacientes** - GET /api/patients
- **Inativação de Paciente** - PATCH /api/patients/{id}/inactive

### ✅ Regras de Negócio
- ✅ **Somente Doutor pode cadastrar paciente** - Implementado com `@PreAuthorize("hasRole('DOCTOR')")`
- ✅ **Validação de CPF único** - Verificação de CPF duplicado entre pacientes ativos
- ✅ **Soft Delete** - Campo `active` para inativação sem exclusão física
- ✅ **Associação com User** - Cada paciente vinculado a um usuário único
- ✅ **Validações de entrada** - Bean Validation nos DTOs

---

## 🏗️ Arquitetura Implementada

### 1. **Camada de Domínio (Domain)**

#### Patient.java
```java
@Entity
@Table(name = "patients")
public class Patient {
    // Campos básicos + campo 'active' adicionado
    private Boolean active = true;
    
    // Métodos de negócio
    public void inactivate() { this.active = false; }
    public boolean isActive() { return active != null && active; }
}
```

#### PatientRepository.java
```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    // Métodos específicos para pacientes ativos
    List<Patient> findByActiveTrue();
    Optional<Patient> findByIdAndActiveTrue(Long id);
    boolean existsByCpfAndActiveTrue(String cpf);
}
```

### 2. **Camada de Aplicação (Application)**

#### PatientService.java / PatientServiceImpl.java
```java
@Service
public class PatientServiceImpl implements PatientService {
    // CRUD completo com regras de negócio
    // Validações de CPF, usuário e status ativo
}
```

#### DTOs
- **PatientRequest.java** - DTO para entrada com validações
- **PatientResponse.java** - DTO para saída com campo `active`
- **PatientMapper.java** - Conversões entre Entity e DTO

### 3. **Camada de Apresentação (Presentation)**

#### PatientController.java
```java
@RestController
@RequestMapping("/api/patients")
@PreAuthorize("hasRole('DOCTOR')") // Segurança em todos endpoints
public class PatientController {
    // 5 endpoints implementados
}
```

---

## 🛡️ Segurança e Validações

### Segurança
- **JWT Authentication** - Todos endpoints protegidos
- **Role-Based Access** - Apenas `ROLE_DOCTOR` pode acessar
- **Spring Security** - `@PreAuthorize` em todos métodos

### Validações
```java
public record PatientRequest(
    @NotNull Long userId,
    @NotBlank @Size(max = 255) String fullName,
    @NotBlank @Size(min = 11, max = 11) String cpf,
    @NotNull @Past LocalDate birthDate,
    // ... outros campos com validações
) {}
```

### Regras de Negócio
1. **CPF único** - Não permite CPF duplicado entre pacientes ativos
2. **Usuário único** - Cada usuário pode estar associado a apenas um paciente
3. **Soft Delete** - Inativação preserva dados históricos
4. **Transações** - Operações atômicas com `@Transactional`

---

## 📊 Base de Dados

### Migration V16
```sql
-- Adiciona coluna 'active' na tabela patients
ALTER TABLE patients 
ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;

-- Índice para otimizar consultas por status ativo
CREATE INDEX idx_patients_active ON patients(active);
```

---

## 🔗 Endpoints da API

| Método | Endpoint | Descrição | Status Code |
|--------|----------|-----------|-------------|
| POST | `/api/patients` | Criar paciente | 201 Created |
| GET | `/api/patients` | Listar ativos | 200 OK |
| GET | `/api/patients/{id}` | Buscar por ID | 200 OK |
| PUT | `/api/patients/{id}` | Atualizar | 200 OK |
| PATCH | `/api/patients/{id}/inactive` | Inativar | 200 OK |

### Exemplo de Uso
```bash
# Criar paciente
curl -X POST http://localhost:8080/api/patients \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "fullName": "João Silva",
    "cpf": "12345678901",
    "birthDate": "1990-05-15"
  }'
```

---

## 🧪 Testes

### Arquivo de Testes HTTP
- **docs/patients.http** - Casos de teste completos
- Exemplos de requisições e respostas
- Cenários de sucesso e erro

### Cenários Testados
- ✅ Criação com dados válidos
- ✅ Validação de CPF duplicado  
- ✅ Busca por ID existente/inexistente
- ✅ Atualização de dados
- ✅ Inativação de paciente
- ✅ Controle de acesso (role DOCTOR)

---

## 📚 Documentação

### Swagger/OpenAPI
- **Tags** organizadas por módulo
- **Security Requirements** documentados  
- **Parâmetros** e **Responses** detalhados
- **Exemplos** de uso incluídos

### Acessar Documentação
1. Iniciar aplicação: `./mvnw spring-boot:run`
2. Abrir: `http://localhost:8080/swagger-ui.html`
3. Seção: **"Pacientes - CRUD de Pacientes"**

---

## 🔄 Próximos Passos Sugeridos

1. **Testes Unitários** - JUnit + Mockito
2. **Testes de Integração** - TestContainers
3. **Auditoria** - Spring Data Envers para histórico
4. **Cache** - Redis para consultas frequentes
5. **Métricas** - Micrometer + Prometheus

---

## ✅ Conclusão

O módulo CRUD de Pacientes foi implementado com sucesso seguindo:
- ✅ **Clean Architecture** - Separação clara de responsabilidades  
- ✅ **Domain-Driven Design** - Regras de negócio no domínio
- ✅ **Security First** - Controle rigoroso de acesso
- ✅ **API RESTful** - Padrões HTTP corretos
- ✅ **Documentação Completa** - OpenAPI + Markdown
- ✅ **Migrations** - Versionamento de banco

**Resultado:** API robusta, segura e bem documentada pronta para uso em produção! 🚀