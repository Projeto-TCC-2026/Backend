# Implementação Completa - Hospital e Doctor

## Visão Geral

Este documento descreve a implementação completa das funcionalidades de Hospital e Doctor, incluindo CRUD, relacionamentos, buscas, filtros, paginação e ordenação.

## 📋 Arquivos Criados

### Services
- `HospitalService.java` - Interface do serviço de Hospital
- `HospitalServiceImpl.java` - Implementação do serviço de Hospital
- `DoctorService.java` - Interface do serviço de Doctor
- `DoctorServiceImpl.java` - Implementação do serviço de Doctor

### Controllers
- `HospitalController.java` - Controller REST para Hospital
- `DoctorController.java` - Controller REST para Doctor

### Repositories (Atualizados)
- `HospitalRepository.java` - Adicionados métodos de busca e filtros
- `DoctorRepository.java` - Adicionados métodos de busca e filtros

### Documentação
- `hospitals.http` - Exemplos de requisições HTTP para Hospital
- `HOSPITAL_DOCTOR_IMPLEMENTATION.md` - Este documento

## 🏗️ Arquitetura

### Camada de Service

#### HospitalService
Operações disponíveis:
- `createHospital(HospitalRequest)` - Criar novo hospital
- `getAllHospitals(Pageable)` - Listar todos com paginação
- `getHospitalById(Long)` - Buscar por ID
- `updateHospital(Long, HospitalRequest)` - Atualizar hospital
- `deleteHospital(Long)` - Excluir hospital
- `searchByName(String, Pageable)` - Buscar por nome
- `filterHospitals(...)` - Filtrar por múltiplos critérios

#### DoctorService
Operações disponíveis:
- `createDoctor(DoctorRequest)` - Criar novo doutor
- `getAllDoctors(Pageable)` - Listar todos com paginação
- `getDoctorById(Long)` - Buscar por ID
- `updateDoctor(Long, DoctorRequest)` - Atualizar doutor
- `deleteDoctor(Long)` - Excluir doutor
- `searchByName(String, Pageable)` - Buscar por nome
- `searchByCrm(String)` - Buscar por CRM
- `searchBySpecialty(String, Pageable)` - Buscar por especialidade
- `filterDoctors(...)` - Filtrar por múltiplos critérios

### Camada de Controller

#### HospitalController
Endpoints REST:
- `POST /api/hospitals` - Criar hospital
- `GET /api/hospitals` - Listar todos (paginado)
- `GET /api/hospitals/{id}` - Buscar por ID
- `PUT /api/hospitals/{id}` - Atualizar hospital
- `DELETE /api/hospitals/{id}` - Excluir hospital
- `GET /api/hospitals/search/name` - Buscar por nome
- `GET /api/hospitals/filter` - Filtrar hospitais

#### DoctorController
Endpoints REST:
- `POST /api/doctors` - Criar doutor
- `GET /api/doctors` - Listar todos (paginado)
- `GET /api/doctors/{id}` - Buscar por ID
- `PUT /api/doctors/{id}` - Atualizar doutor
- `DELETE /api/doctors/{id}` - Excluir doutor
- `GET /api/doctors/search/name` - Buscar por nome
- `GET /api/doctors/search/crm` - Buscar por CRM
- `GET /api/doctors/search/specialty` - Buscar por especialidade
- `GET /api/doctors/filter` - Filtrar doutores

## 🔐 Segurança e Permissões

### Hospital
- **Criar/Atualizar**: `ADMIN` ou `DOCTOR`
- **Buscar/Listar**: `ADMIN` ou `DOCTOR`
- **Excluir**: Apenas `ADMIN`

### Doctor
- **Criar/Atualizar/Excluir**: Apenas `ADMIN`
- **Buscar/Listar**: `ADMIN` ou `DOCTOR`

## ✅ Validações Implementadas

### Hospital
- ✅ CNPJ único no sistema
- ✅ Nome obrigatório
- ✅ CNPJ obrigatório (14 caracteres)
- ✅ Validação de formato de email
- ✅ Estado deve ter 2 caracteres
- ✅ Não permite exclusão se houver doutores associados

### Doctor
- ✅ CRM único no sistema
- ✅ CPF único no sistema
- ✅ Hospital obrigatório e deve existir
- ✅ Usuário obrigatório e deve existir
- ✅ Nome completo obrigatório
- ✅ CPF obrigatório (11 caracteres)
- ✅ CRM obrigatório
- ✅ Não permite um usuário associado a múltiplos doutores
- ✅ Não permite exclusão se houver pacientes ou procedimentos associados

## 🔍 Funcionalidades de Busca e Filtro

### Hospital

#### Busca por Nome
```http
GET /api/hospitals/search/name?name=Santa
```
- Busca parcial (case-insensitive)
- Suporta paginação e ordenação

#### Filtros Combinados
```http
GET /api/hospitals/filter?name=Santa&city=São Paulo&state=SP
```
Filtros disponíveis:
- `name` - Nome do hospital (busca parcial)
- `city` - Cidade (busca parcial)
- `state` - Estado (busca exata, case-insensitive)

### Doctor

#### Busca por Nome
```http
GET /api/doctors/search/name?name=João
```

#### Busca por CRM
```http
GET /api/doctors/search/crm?crm=12345-SP
```

#### Busca por Especialidade
```http
GET /api/doctors/search/specialty?specialty=Cardiologia
```

#### Filtros Combinados
```http
GET /api/doctors/filter?hospitalId=1&specialty=Cardiologia&name=João&crm=12345
```
Filtros disponíveis:
- `hospitalId` - ID do hospital
- `specialty` - Especialidade (busca parcial)
- `name` - Nome (busca parcial)
- `crm` - CRM (busca parcial)

## 📄 Paginação e Ordenação

Todos os endpoints de listagem suportam:

### Parâmetros de Paginação
- `page` - Número da página (inicia em 0)
- `size` - Tamanho da página (padrão: 10)

### Parâmetros de Ordenação
- `sort` - Campo e direção: `campo,direção`
  - Exemplo: `sort=name,asc` ou `sort=createdAt,desc`

### Exemplos

#### Hospital - Ordenar por Nome (crescente)
```http
GET /api/hospitals?page=0&size=20&sort=name,asc
```

#### Hospital - Ordenar por Data de Criação (decrescente)
```http
GET /api/hospitals?page=0&size=10&sort=createdAt,desc
```

#### Doctor - Ordenar por Nome (crescente)
```http
GET /api/doctors?page=0&size=20&sort=fullName,asc
```

#### Doctor - Ordenar por CRM
```http
GET /api/doctors?page=0&size=10&sort=crm,asc
```

#### Doctor - Múltiplas ordenações
```http
GET /api/doctors?sort=specialty,asc&sort=fullName,asc
```

## 🔗 Relacionamentos

### Hospital → Doctor (One-to-Many)
- Um Hospital pode ter vários Doutores
- Todo Doctor pertence obrigatoriamente a um Hospital
- Ao buscar um Doctor, o Hospital é retornado como `HospitalSummary`
- Ao tentar excluir um Hospital com Doutores, retorna erro de negócio

### Doctor → User (One-to-One)
- Todo Doctor está associado a um User
- Um User só pode estar associado a um Doctor
- A validação é feita na criação e atualização

## 📊 Estrutura de Resposta

### HospitalResponse
```json
{
  "id": 1,
  "name": "Hospital Santa Maria",
  "cnpj": "12345678901234",
  "phone": "(11) 98765-4321",
  "email": "contato@santamaria.com.br",
  "address": "Rua das Flores, 123",
  "city": "São Paulo",
  "state": "SP",
  "createdAt": "2026-06-29T10:00:00",
  "updatedAt": "2026-06-29T10:00:00"
}
```

### DoctorResponse
```json
{
  "id": 1,
  "user": {
    "id": 5,
    "email": "doutor@example.com",
    "role": "DOCTOR",
    "createdAt": "2026-06-29T09:00:00",
    "updatedAt": "2026-06-29T09:00:00"
  },
  "hospital": {
    "id": 1,
    "name": "Hospital Santa Maria",
    "city": "São Paulo",
    "state": "SP"
  },
  "fullName": "Dr. João Silva",
  "cpf": "12345678901",
  "crm": "12345-SP",
  "specialty": "Cardiologia",
  "phone": "(11) 99999-9999",
  "createdAt": "2026-06-29T10:00:00",
  "updatedAt": "2026-06-29T10:00:00"
}
```

### Resposta Paginada
```json
{
  "success": true,
  "data": {
    "content": [...],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {...},
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 50,
    "totalPages": 5,
    "last": false,
    "size": 10,
    "number": 0,
    "sort": {...},
    "numberOfElements": 10,
    "first": true,
    "empty": false
  }
}
```

## 🚨 Tratamento de Erros

### Exceções de Negócio
- `BusinessException` - Violações de regras de negócio
  - CNPJ duplicado
  - CRM duplicado
  - CPF duplicado
  - Hospital com doutores associados (ao excluir)
  - Doctor com pacientes/procedimentos (ao excluir)

### Exceções de Recursos
- `ResourceNotFoundException` - Recurso não encontrado
  - Hospital não encontrado
  - Doctor não encontrado
  - User não encontrado

### Exemplo de Resposta de Erro
```json
{
  "success": false,
  "error": {
    "message": "Já existe um hospital cadastrado com o CNPJ: 12345678901234",
    "timestamp": "2026-06-29T10:30:00"
  }
}
```

## 📝 Documentação Swagger

Todos os endpoints estão documentados com Swagger/OpenAPI:
- Acesse: `http://localhost:8080/swagger-ui.html`

Cada endpoint possui:
- ✅ Resumo e descrição detalhada
- ✅ Parâmetros com exemplos
- ✅ Códigos de resposta HTTP
- ✅ Schemas de requisição e resposta
- ✅ Informações de segurança (Bearer Token)

## 🎯 Próximos Passos Sugeridos

1. **Testes Automatizados**
   - Testes unitários dos Services
   - Testes de integração dos Controllers
   - Testes dos Repositories

2. **Melhorias de Performance**
   - Adicionar cache (Redis) para buscas frequentes
   - Otimizar queries com @EntityGraph para reduzir N+1

3. **Auditoria**
   - Adicionar logs de auditoria para operações críticas
   - Implementar @CreatedBy e @ModifiedBy

4. **Validações Avançadas**
   - Validação de CNPJ com dígitos verificadores
   - Validação de CPF com dígitos verificadores
   - Validação de formato de CRM por estado

5. **Relatórios**
   - Endpoint para relatório de doutores por hospital
   - Endpoint para estatísticas de hospitais por região

## ✨ Conclusão

A implementação está completa e segue todos os requisitos da Sprint:
- ✅ CRUD completo de Hospital e Doctor
- ✅ Relacionamento Hospital x Doctor
- ✅ Buscas e pesquisas
- ✅ Paginação e ordenação
- ✅ Filtros combinados
- ✅ Validações de negócio
- ✅ Tratamento de exclusão com relacionamentos
- ✅ Documentação Swagger completa
- ✅ Reutilização de DTOs, Mappers e Repositories existentes
- ✅ Padrão arquitetural do projeto mantido
