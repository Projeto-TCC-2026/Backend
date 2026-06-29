# 🏗️ Arquitetura - Hospital e Doctor

## 📊 Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                         CLIENTE                              │
│  (Browser, Postman, Mobile App, etc.)                       │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/JSON
                         │ Bearer Token (JWT)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│                    (Controllers REST)                        │
│                                                              │
│  ┌────────────────────┐      ┌─────────────────────┐       │
│  │ HospitalController │      │  DoctorController   │       │
│  │                    │      │                     │       │
│  │ • POST /hospitals  │      │ • POST /doctors     │       │
│  │ • GET  /hospitals  │      │ • GET  /doctors     │       │
│  │ • GET  /{id}       │      │ • GET  /{id}        │       │
│  │ • PUT  /{id}       │      │ • PUT  /{id}        │       │
│  │ • DELETE /{id}     │      │ • DELETE /{id}      │       │
│  │ • GET  /search/*   │      │ • GET  /search/*    │       │
│  │ • GET  /filter     │      │ • GET  /filter      │       │
│  └──────────┬─────────┘      └──────────┬──────────┘       │
│             │                           │                   │
│             │ @PreAuthorize             │                   │
│             │ Security Check            │                   │
└─────────────┼───────────────────────────┼───────────────────┘
              │                           │
              ▼                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                         │
│                  (Services - Business Logic)                 │
│                                                              │
│  ┌───────────────────┐       ┌──────────────────┐          │
│  │ HospitalService   │       │  DoctorService   │          │
│  │ (Interface)       │       │  (Interface)     │          │
│  └────────┬──────────┘       └────────┬─────────┘          │
│           │                            │                    │
│           ▼                            ▼                    │
│  ┌────────────────────┐      ┌─────────────────────┐       │
│  │HospitalServiceImpl │      │ DoctorServiceImpl   │       │
│  │                    │      │                     │       │
│  │ • Validações       │      │ • Validações        │       │
│  │ • Regras Negócio   │◄─────┤ • Regras Negócio    │       │
│  │ • Transações       │      │ • Transações        │       │
│  │ • Conversões DTO   │      │ • Conversões DTO    │       │
│  └─────────┬──────────┘      └──────────┬──────────┘       │
│            │                            │                   │
│            │ usa                    usa │                   │
│            ▼                            ▼                   │
│  ┌────────────────────┐      ┌─────────────────────┐       │
│  │  HospitalMapper    │      │   DoctorMapper      │       │
│  │                    │      │                     │       │
│  │ • toEntity()       │      │ • toEntity()        │       │
│  │ • toResponse()     │      │ • toResponse()      │       │
│  │ • toSummary()      │      │ • toSummary()       │       │
│  │ • updateEntity()   │      │ • updateEntity()    │       │
│  └────────────────────┘      └─────────────────────┘       │
│                                                              │
└──────────────┬────────────────────────┬──────────────────────┘
               │                        │
               ▼                        ▼
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                            │
│                (Repositories - Data Access)                  │
│                                                              │
│  ┌──────────────────────┐    ┌──────────────────────┐      │
│  │ HospitalRepository   │    │  DoctorRepository    │      │
│  │ (JpaRepository)      │    │  (JpaRepository)     │      │
│  │                      │    │                      │      │
│  │ • findById()         │    │ • findById()         │      │
│  │ • findAll(Pageable)  │    │ • findAll(Pageable)  │      │
│  │ • save()             │    │ • save()             │      │
│  │ • delete()           │    │ • delete()           │      │
│  │ • existsByCnpj()     │    │ • existsByCrm()      │      │
│  │ • findByName...()    │    │ • findByName...()    │      │
│  │ • findByFilters()    │    │ • findByFilters()    │      │
│  └──────────┬───────────┘    └──────────┬───────────┘      │
│             │                           │                   │
│             ▼                           ▼                   │
│  ┌──────────────────────┐    ┌──────────────────────┐      │
│  │   Hospital Entity    │    │    Doctor Entity     │      │
│  │                      │    │                      │      │
│  │ • id                 │    │ • id                 │      │
│  │ • name               │    │ • fullName           │      │
│  │ • cnpj (unique)      │    │ • cpf (unique)       │      │
│  │ • phone              │    │ • crm (unique)       │      │
│  │ • email              │    │ • specialty          │      │
│  │ • address            │    │ • phone              │      │
│  │ • city               │    │ • user (OneToOne)    │      │
│  │ • state              │◄───┤ • hospital (ManyToOne)      │
│  │ • doctors (OneToMany)│    │ • doctorPatients     │      │
│  │ • createdAt          │    │ • procedures         │      │
│  │ • updatedAt          │    │ • notifications      │      │
│  └──────────────────────┘    └──────────────────────┘      │
│                                                              │
└──────────────────────┬───────────────────────────────────────┘
                       │ JPA/Hibernate
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    PERSISTENCE LAYER                         │
│                    (Database - PostgreSQL)                   │
│                                                              │
│  ┌──────────────────────┐    ┌──────────────────────┐      │
│  │   hospitals table    │    │    doctors table     │      │
│  │                      │    │                      │      │
│  │ • id (PK)            │    │ • id (PK)            │      │
│  │ • name               │    │ • full_name          │      │
│  │ • cnpj (unique)      │    │ • cpf (unique)       │      │
│  │ • phone              │    │ • crm (unique)       │      │
│  │ • email              │    │ • specialty          │      │
│  │ • address            │    │ • phone              │      │
│  │ • city               │    │ • user_id (FK)       │      │
│  │ • state              │◄───┤ • hospital_id (FK)   │      │
│  │ • created_at         │    │ • created_at         │      │
│  │ • updated_at         │    │ • updated_at         │      │
│  └──────────────────────┘    └──────────────────────┘      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 Fluxo de Dados

### Criação de Doctor

```
┌─────────┐                  ┌────────────┐
│ Cliente │                  │ Controller │
└────┬────┘                  └─────┬──────┘
     │                              │
     │ 1. POST /api/doctors         │
     │ {DoctorRequest + Token}      │
     ├─────────────────────────────►│
     │                              │ 2. @PreAuthorize
     │                              │    verifica ADMIN
     │                              │
     │                              ▼
     │                       ┌──────────────┐
     │                       │   Service    │
     │                       └──────┬───────┘
     │                              │
     │                              │ 3. Validações:
     │                              │    - User existe?
     │                              │    - Hospital existe?
     │                              │    - CPF único?
     │                              │    - CRM único?
     │                              │
     │                              ▼
     │                       ┌──────────────┐
     │                       │    Mapper    │
     │                       └──────┬───────┘
     │                              │
     │                              │ 4. Request → Entity
     │                              │
     │                              ▼
     │                       ┌──────────────┐
     │                       │  Repository  │
     │                       └──────┬───────┘
     │                              │
     │                              │ 5. save(doctor)
     │                              │
     │                              ▼
     │                       ┌──────────────┐
     │                       │   Database   │
     │                       └──────┬───────┘
     │                              │
     │                              │ 6. Doctor salvo
     │                              │
     │                              ▼
     │                       ┌──────────────┐
     │                       │    Mapper    │
     │                       └──────┬───────┘
     │                              │
     │                              │ 7. Entity → Response
     │                              │
     │                              ▼
     │                       ┌──────────────┐
     │                       │  Controller  │
     │                       └──────┬───────┘
     │                              │
     │ 8. DoctorResponse            │
     │    (201 Created)             │
     │◄─────────────────────────────┤
     │                              │
```

---

## 🔗 Relacionamentos de Entidades

```
┌────────────────────────────────────────────────────────────┐
│                      USER                                   │
│  - id (PK)                                                  │
│  - email (unique)                                           │
│  - password                                                 │
│  - role (ADMIN, DOCTOR, PATIENT)                           │
└────────────────┬───────────────────────────────────────────┘
                 │
                 │ OneToOne
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│                    DOCTOR                                   │
│  - id (PK)                                                  │
│  - user_id (FK) ───────────────► USER                      │
│  - hospital_id (FK) ───────────► HOSPITAL                  │
│  - fullName                                                 │
│  - cpf (unique)                                             │
│  - crm (unique)                                             │
│  - specialty                                                │
│  - phone                                                    │
└────────────────┬───────────────────────────────────────────┘
                 │
                 │ ManyToOne
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│                   HOSPITAL                                  │
│  - id (PK)                                                  │
│  - name                                                     │
│  - cnpj (unique)                                            │
│  - phone                                                    │
│  - email                                                    │
│  - address                                                  │
│  - city                                                     │
│  - state                                                    │
└────────────────────────────────────────────────────────────┘
      │
      │ OneToMany
      │
      └────────► List<Doctor> doctors


Regras:
✓ Um Hospital pode ter vários Doctors
✓ Um Doctor pertence a um Hospital (obrigatório)
✓ Um User pode ser um Doctor (OneToOne)
✓ Um Doctor não pode existir sem Hospital
✓ Um Doctor não pode existir sem User
```

---

## 📦 Estrutura de Pacotes

```
com.tcc
│
├── presentation
│   └── controller
│       ├── HospitalController.java
│       └── DoctorController.java
│
├── application
│   ├── service
│   │   ├── HospitalService.java (interface)
│   │   ├── HospitalServiceImpl.java
│   │   ├── DoctorService.java (interface)
│   │   └── DoctorServiceImpl.java
│   │
│   ├── dto
│   │   ├── request
│   │   │   ├── HospitalRequest.java
│   │   │   └── DoctorRequest.java
│   │   │
│   │   └── response
│   │       ├── HospitalResponse.java
│   │       ├── HospitalSummary.java
│   │       ├── DoctorResponse.java
│   │       └── DoctorSummary.java
│   │
│   └── mapper
│       ├── HospitalMapper.java
│       └── DoctorMapper.java
│
├── domain
│   ├── model
│   │   ├── Hospital.java
│   │   ├── Doctor.java
│   │   └── User.java
│   │
│   └── repository
│       ├── HospitalRepository.java
│       └── DoctorRepository.java
│
└── exception
    ├── BusinessException.java
    └── ResourceNotFoundException.java
```

---

## 🔐 Camada de Segurança

```
┌────────────────────────────────────────────────────────────┐
│                    REQUEST FLOW                             │
└────────────────────────────────────────────────────────────┘

1. Cliente envia requisição com Token JWT
   ↓
2. Spring Security intercepta
   ↓
3. JwtAuthenticationFilter valida token
   ↓
4. Extrai informações do usuário (email, role)
   ↓
5. Cria Authentication no SecurityContext
   ↓
6. @PreAuthorize no Controller verifica permissão
   ↓
7. Se autorizado, executa o método
   ↓
8. Retorna resposta ao cliente


Permissões por Role:
┌──────────────┬────────────────────────────────────┐
│ Recurso      │ ADMIN         │ DOCTOR            │
├──────────────┼───────────────┼───────────────────┤
│ Hospital     │               │                   │
│  - Create    │ ✓             │ ✓                 │
│  - Read      │ ✓             │ ✓                 │
│  - Update    │ ✓             │ ✓                 │
│  - Delete    │ ✓             │ ✗                 │
├──────────────┼───────────────┼───────────────────┤
│ Doctor       │               │                   │
│  - Create    │ ✓             │ ✗                 │
│  - Read      │ ✓             │ ✓                 │
│  - Update    │ ✓             │ ✗                 │
│  - Delete    │ ✓             │ ✗                 │
└──────────────┴───────────────┴───────────────────┘
```

---

## 🔍 Fluxo de Busca e Filtros

```
Busca Simples (por nome):
┌─────────┐
│ Cliente │  GET /api/hospitals/search/name?name=Santa
└────┬────┘
     │
     ▼
┌────────────┐
│ Controller │  searchByName(name, pageable)
└─────┬──────┘
      │
      ▼
┌──────────┐
│ Service  │  searchByName(name, pageable)
└─────┬────┘
      │
      ▼
┌──────────────┐
│ Repository   │  findByNameContainingIgnoreCase(name, pageable)
└──────┬───────┘
       │
       ▼
┌──────────┐
│ Database │  SELECT * FROM hospitals 
└─────┬────┘  WHERE LOWER(name) LIKE '%santa%'
      │       LIMIT 10 OFFSET 0
      │
      ▼
┌─────────────┐
│ Page<T>     │  {content: [...], totalPages: 5, ...}
└─────────────┘


Filtros Combinados:
┌─────────┐
│ Cliente │  GET /api/doctors/filter?hospitalId=1&specialty=Cardiologia
└────┬────┘
     │
     ▼
┌────────────┐
│ Controller │  filterDoctors(hospitalId, specialty, name, crm, pageable)
└─────┬──────┘
      │
      ▼
┌──────────┐
│ Service  │  filterDoctors(...)
└─────┬────┘
      │
      ▼
┌──────────────┐
│ Repository   │  @Query JPQL com filtros dinâmicos
└──────┬───────┘  WHERE (:hospitalId IS NULL OR d.hospital.id = :hospitalId)
       │          AND (:specialty IS NULL OR ...)
       │
       ▼
┌──────────┐
│ Database │  Query dinâmica baseada nos parâmetros não-nulos
└─────┬────┘
      │
      ▼
┌─────────────┐
│ Page<T>     │  Resultados filtrados
└─────────────┘
```

---

## 🎯 Padrões de Projeto Utilizados

### 1. **Repository Pattern**
```
DoctorRepository ──► JpaRepository<Doctor, Long>
                     ↓
              Spring Data JPA abstrai SQL
```

### 2. **Service Layer Pattern**
```
Controller ──► Service (Interface) ──► ServiceImpl
                                        ↓
                                   Business Logic
```

### 3. **DTO Pattern**
```
Client ──► DoctorRequest ──► Service ──► Doctor (Entity) ──► DB
                                ↓
Client ◄── DoctorResponse ◄── Mapper ◄── Doctor (Entity) ◄── DB
```

### 4. **Mapper Pattern**
```
DoctorMapper:
  - toEntity(request)    : Request → Entity
  - toResponse(entity)   : Entity → Response
  - toSummary(entity)    : Entity → Summary
  - updateEntity(...)    : Request → update Entity
```

### 5. **Dependency Injection**
```java
@Service
public class DoctorServiceImpl {
    
    private final DoctorRepository repository;
    private final DoctorMapper mapper;
    
    public DoctorServiceImpl(DoctorRepository repository, 
                             DoctorMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
}
```

---

## 📊 Diagrama de Sequência - Filtro de Doctors

```
Cliente  Controller  Service  Repository  Database
  │          │          │          │           │
  │──filter──►│          │          │           │
  │          │──filter──►│          │           │
  │          │          │─validate─►│           │
  │          │          │          │──SELECT──►│
  │          │          │          │           │
  │          │          │          │◄─Results──│
  │          │          │◄─Page<T>─│           │
  │          │◄─Page<T>─│          │           │
  │◄─200 OK──│          │          │           │
  │ (JSON)   │          │          │           │
```

---

## ✨ Conclusão

Esta arquitetura segue os princípios:
- ✅ **Separation of Concerns** - Cada camada tem uma responsabilidade
- ✅ **Dependency Inversion** - Dependência de abstrações (interfaces)
- ✅ **Single Responsibility** - Cada classe tem um propósito único
- ✅ **Open/Closed** - Aberto para extensão, fechado para modificação
- ✅ **DRY** - Não há duplicação de código
- ✅ **Clean Architecture** - Camadas bem definidas e desacopladas
