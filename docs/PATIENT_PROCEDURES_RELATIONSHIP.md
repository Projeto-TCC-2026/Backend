# 🔗 Relacionamento Paciente - Procedimentos Realizados

## ✅ Status: IMPLEMENTADO

Implementação do relacionamento bidirecional entre Paciente e Procedimentos Realizados (ProcedureExecution), preparando a estrutura para integração futura com o módulo de Procedimentos.

---

## 📋 Requisitos Implementados

### ✅ Regras de Negócio
- ✅ **Um Paciente pode possuir vários Procedimentos Realizados** - Relacionamento @OneToMany
- ✅ **Todo Procedimento Realizado obrigatoriamente associado a um Paciente** - Campo patient com @ManyToOne (nullable = false)
- ✅ **Estrutura preparada para integração** - Endpoints, services e repositórios prontos

### ✅ Funcionalidades Implementadas
- ✅ Relacionamento bidirecional entre Patient e ProcedureExecution
- ✅ Métodos de negócio para gerenciar procedimentos na entidade Patient
- ✅ Validação de integridade ao excluir paciente com procedimentos
- ✅ Endpoints para listar procedimentos de um paciente
- ✅ Endpoint para contar procedimentos de um paciente
- ✅ Repository com métodos paginados e queries personalizadas
- ✅ Service layer com lógica de negócio

---

## 🏗️ Arquitetura da Implementação

### 1. Domain Layer - Entidades

#### Patient.java
```java
@Entity
@Table(name = "patients")
public class Patient {
    // ... outros campos
    
    // Relacionamento OneToMany com ProcedureExecution
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProcedureExecution> procedureExecutions = new ArrayList<>();
    
    // Métodos de negócio para gerenciar procedimentos
    public void addProcedureExecution(ProcedureExecution procedureExecution) {
        this.procedureExecutions.add(procedureExecution);
        procedureExecution.setPatient(this);
    }
    
    public void removeProcedureExecution(ProcedureExecution procedureExecution) {
        this.procedureExecutions.remove(procedureExecution);
        procedureExecution.setPatient(null);
    }
    
    public List<ProcedureExecution> getActiveProcedureExecutions() {
        return procedureExecutions.stream()
                .filter(pe -> !"CANCELLED".equals(pe.getStatus()))
                .toList();
    }
    
    public long countProcedureExecutions() {
        return procedureExecutions.size();
    }
    
    public boolean hasProcedureExecutions() {
        return !procedureExecutions.isEmpty();
    }
}
```

#### ProcedureExecution.java
```java
@Entity
@Table(name = "procedure_executions")
public class ProcedureExecution {
    // ... outros campos
    
    // Relacionamento ManyToOne com Patient (obrigatório)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    // ... outros relacionamentos e métodos
}
```

### 2. Repository Layer

#### ProcedureExecutionRepository.java
```java
@Repository
public interface ProcedureExecutionRepository extends JpaRepository<ProcedureExecution, Long> {
    
    // Métodos de busca simples
    List<ProcedureExecution> findByPatientId(Long patientId);
    
    // Métodos com paginação
    Page<ProcedureExecution> findByPatientId(Long patientId, Pageable pageable);
    Page<ProcedureExecution> findByPatientIdAndStatus(Long patientId, String status, Pageable pageable);
    
    // Métodos de contagem
    long countByPatientId(Long patientId);
    long countByPatientIdAndStatus(Long patientId, String status);
    
    // Verificações de existência
    boolean existsByPatientId(Long patientId);
    boolean existsByPatientIdAndStatus(Long patientId, String status);
    
    // Consultas personalizadas
    @Query("SELECT pe FROM ProcedureExecution pe WHERE pe.patient.id = :patientId " +
           "AND pe.executionDate BETWEEN :startDate AND :endDate")
    List<ProcedureExecution> findByPatientIdAndDateRange(
        @Param("patientId") Long patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT pe FROM ProcedureExecution pe WHERE pe.patient.id = :patientId " +
           "ORDER BY pe.executionDate DESC")
    List<ProcedureExecution> findLatestByPatientId(
        @Param("patientId") Long patientId,
        Pageable pageable
    );
}
```

### 3. Service Layer

#### PatientService.java
```java
public interface PatientService {
    // ... métodos CRUD existentes
    
    // Métodos de relacionamento com Procedimentos Realizados
    Page<ProcedureExecutionResponse> getPatientProcedureExecutions(Long patientId, Pageable pageable);
    Long countPatientProcedureExecutions(Long patientId);
}
```

#### PatientServiceImpl.java
```java
@Service
public class PatientServiceImpl implements PatientService {
    
    private final ProcedureExecutionRepository procedureExecutionRepository;
    private final ProcedureExecutionMapper procedureExecutionMapper;
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProcedureExecutionResponse> getPatientProcedureExecutions(Long patientId, Pageable pageable) {
        // Verificar se o paciente existe e está ativo
        Patient patient = patientRepository.findByIdAndActiveTrue(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        
        // Buscar os procedimentos realizados com paginação
        return procedureExecutionRepository.findByPatientId(patientId, pageable)
                .map(procedureExecutionMapper::toResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long countPatientProcedureExecutions(Long patientId) {
        // Verificar se o paciente existe
        Patient patient = patientRepository.findByIdAndActiveTrue(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        
        // Contar os procedimentos realizados
        return procedureExecutionRepository.countByPatientId(patientId);
    }
    
    @Override
    @Transactional
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        
        // Validação: não permite exclusão se houver procedimentos
        if (patient.hasProcedureExecutions()) {
            throw new BusinessException(
                "Não é possível excluir o paciente. Existem " + 
                patient.countProcedureExecutions() + " procedimento(s) realizado(s) associado(s). " +
                "Use a inativação ao invés da exclusão para manter o histórico."
            );
        }
        
        patientRepository.delete(patient);
    }
}
```

### 4. Controller Layer

#### PatientController.java
```java
@RestController
@RequestMapping("/api/patients")
public class PatientController {
    
    @GetMapping("/{id}/procedures")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Listar procedimentos realizados de um paciente",
        description = "Retorna todos os procedimentos realizados associados a um paciente. " +
                      "Suporta paginação. Preparado para integração com módulo de Procedimentos."
    )
    public ResponseEntity<ApiResponse<Page<ProcedureExecutionResponse>>> getPatientProcedures(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "executionDate", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        Page<ProcedureExecutionResponse> procedures = 
            patientService.getPatientProcedureExecutions(id, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(procedures));
    }
    
    @GetMapping("/{id}/procedures/count")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Contar procedimentos realizados de um paciente",
        description = "Retorna o total de procedimentos realizados associados a um paciente"
    )
    public ResponseEntity<ApiResponse<Long>> countPatientProcedures(@PathVariable Long id) {
        Long count = patientService.countPatientProcedureExecutions(id);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
```

---

## 🔐 Validações de Integridade

### Validação ao Excluir Paciente
```java
if (patient.hasProcedureExecutions()) {
    throw new BusinessException(
        "Não é possível excluir o paciente. Existem " + 
        patient.countProcedureExecutions() + " procedimento(s) realizado(s) associado(s). " +
        "Use a inativação ao invés da exclusão para manter o histórico."
    );
}
```

### Validação ao Buscar Procedimentos
```java
// Verifica se o paciente existe e está ativo antes de buscar procedimentos
Patient patient = patientRepository.findByIdAndActiveTrue(patientId)
    .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
```

---

## 🔗 Endpoints Disponíveis

| Método | Endpoint | Descrição | Paginação |
|--------|----------|-----------|-----------|
| GET | `/api/patients/{id}/procedures` | Listar procedimentos do paciente | ✅ Sim |
| GET | `/api/patients/{id}/procedures/count` | Contar procedimentos do paciente | Não |

### Detalhamento dos Endpoints

#### 1. GET /api/patients/{id}/procedures - Listar Procedimentos
**Autorização**: DOCTOR  
**Descrição**: Retorna todos os procedimentos realizados de um paciente com paginação

**Parâmetros de Path**:
- `id` (Long, obrigatório): ID do paciente

**Parâmetros de Query**:
- `page` (int): Número da página (default: 0)
- `size` (int): Tamanho da página (default: 10)
- `sort` (string): Campo e direção (default: executionDate,desc)

**Exemplo**:
```
GET /api/patients/1/procedures?page=0&size=10&sort=executionDate,desc
```

**Resposta**: Page<ProcedureExecutionResponse>
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "patientProcedure": {...},
        "procedure": {...},
        "doctor": {...},
        "patient": {...},
        "executionDate": "2024-01-15T10:30:00",
        "status": "COMPLETED",
        "observations": "Procedimento realizado com sucesso"
      }
    ],
    "totalElements": 25,
    "totalPages": 3,
    "size": 10,
    "number": 0
  }
}
```

---

#### 2. GET /api/patients/{id}/procedures/count - Contar Procedimentos
**Autorização**: DOCTOR  
**Descrição**: Retorna o total de procedimentos realizados de um paciente

**Exemplo**:
```
GET /api/patients/1/procedures/count
```

**Resposta**:
```json
{
  "success": true,
  "data": 25
}
```

---

## 📊 Diagrama de Relacionamento

```
┌─────────────────┐         ┌──────────────────────────┐
│     Patient     │1      * │   ProcedureExecution     │
│─────────────────│◄────────│──────────────────────────│
│ id              │         │ id                       │
│ fullName        │         │ patient_id (FK) NOT NULL │
│ cpf             │         │ patientProcedure_id (FK) │
│ ...             │         │ procedure_id (FK)        │
│ active          │         │ doctor_id (FK)           │
│                 │         │ executionDate            │
│                 │         │ status                   │
│                 │         │ observations             │
└─────────────────┘         └──────────────────────────┘
```

**Cardinalidade**: 
- 1 Patient → N ProcedureExecution (OneToMany)
- N ProcedureExecution → 1 Patient (ManyToOne, obrigatório)

**Cascade**: 
- CascadeType.ALL no lado Patient (ao excluir paciente, exclui procedimentos)

**Fetch**: 
- FetchType.LAZY em ambos os lados (carregamento sob demanda)

---

## 🧪 Casos de Uso

### Caso 1: Listar Procedimentos de um Paciente
```java
// Service
Page<ProcedureExecutionResponse> procedures = 
    patientService.getPatientProcedureExecutions(patientId, pageable);
```

### Caso 2: Adicionar Procedimento a um Paciente
```java
// Domain
Patient patient = patientRepository.findById(patientId).orElseThrow();
ProcedureExecution execution = new ProcedureExecution(...);
patient.addProcedureExecution(execution);
patientRepository.save(patient);
```

### Caso 3: Verificar se Paciente tem Procedimentos
```java
// Service
if (patient.hasProcedureExecutions()) {
    // Não pode excluir
    throw new BusinessException("Paciente possui procedimentos associados");
}
```

### Caso 4: Contar Procedimentos Ativos
```java
// Domain
List<ProcedureExecution> activeProcedures = patient.getActiveProcedureExecutions();
long count = activeProcedures.size();
```

### Caso 5: Buscar Procedimentos por Data
```java
// Repository
List<ProcedureExecution> procedures = procedureExecutionRepository
    .findByPatientIdAndDateRange(patientId, startDate, endDate);
```

---

## 🎯 Benefícios da Implementação

### Para o Sistema
✅ **Integridade Referencial**: Garante que procedimentos sempre têm paciente associado  
✅ **Histórico Preservado**: Validação impede exclusão acidental de dados importantes  
✅ **Navegação Bidirecional**: Fácil acesso de Patient → Procedures e vice-versa  
✅ **Performance**: Fetch LAZY evita carregamento desnecessário  
✅ **Escalabilidade**: Paginação permite lidar com grandes volumes  

### Para a Integração Futura
✅ **Endpoints Prontos**: API já disponível para consumo pelo módulo de Procedimentos  
✅ **Repository Completo**: Queries prontas para diferentes cenários  
✅ **Service Layer**: Lógica de negócio centralizada e reutilizável  
✅ **DTOs Mapeados**: ProcedureExecutionResponse já existente  
✅ **Validações**: Regras de negócio implementadas e testadas  

---

## 🚀 Preparação para Integração com Módulo de Procedimentos

### O que está pronto:
✅ Relacionamento bidirecional configurado  
✅ Endpoints REST disponíveis  
✅ Repository com queries paginadas  
✅ Service com validações de negócio  
✅ DTOs e Mappers configurados  
✅ Documentação Swagger  

### O que o módulo de Procedimentos precisará fazer:
1. Criar endpoints para CRUD de ProcedureExecution
2. Implementar lógica de criação de procedimentos vinculados a pacientes
3. Adicionar validações específicas de procedimentos
4. Implementar workflow de status (PENDING → IN_PROGRESS → COMPLETED)
5. Adicionar upload de fotos dos procedimentos (ProcedurePhoto)

### Exemplo de Integração Futura:
```java
// POST /api/procedures/executions
{
  "patientId": 1,
  "procedureId": 5,
  "doctorId": 2,
  "executionDate": "2024-01-20T14:00:00",
  "status": "PENDING",
  "observations": "Preparação pré-operatória necessária"
}
```

---

## 📝 Queries Úteis Disponíveis

### 1. Listar todos os procedimentos de um paciente
```java
List<ProcedureExecution> procedures = 
    procedureExecutionRepository.findByPatientId(patientId);
```

### 2. Listar procedimentos com paginação
```java
Page<ProcedureExecution> procedures = 
    procedureExecutionRepository.findByPatientId(patientId, pageable);
```

### 3. Listar por paciente e status
```java
Page<ProcedureExecution> procedures = 
    procedureExecutionRepository.findByPatientIdAndStatus(patientId, "COMPLETED", pageable);
```

### 4. Buscar por período
```java
List<ProcedureExecution> procedures = 
    procedureExecutionRepository.findByPatientIdAndDateRange(
        patientId, startDate, endDate
    );
```

### 5. Buscar últimos N procedimentos
```java
Pageable top5 = PageRequest.of(0, 5);
List<ProcedureExecution> latest = 
    procedureExecutionRepository.findLatestByPatientId(patientId, top5);
```

---

## ✅ Conclusão

O relacionamento entre **Paciente** e **Procedimentos Realizados** foi **implementado com sucesso** seguindo as melhores práticas:

### Destaques:
- ✅ **Relacionamento Bidirecional** configurado corretamente
- ✅ **Regras de Negócio** implementadas (obrigatoriedade, validações)
- ✅ **Integridade Referencial** garantida
- ✅ **Endpoints REST** prontos para uso
- ✅ **Repository com Paginação** e queries otimizadas
- ✅ **Service Layer** com validações completas
- ✅ **Preparado para Integração** com módulo de Procedimentos
- ✅ **Documentação Completa** e exemplos de uso

**A estrutura está 100% pronta para a integração futura com o módulo de Procedimentos!** 🚀

---

**Data de implementação**: 02/07/2026  
**Versão**: 1.0.0  
**Status**: ✅ IMPLEMENTADO
