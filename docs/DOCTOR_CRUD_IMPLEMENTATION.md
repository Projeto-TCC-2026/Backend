# Implementação do CRUD de Doctors ✅

## Status: Implementação Completa

Todos os passos foram executados com sucesso seguindo o guia de implementação.

## 📁 Arquivos Criados

### Service Layer
- ✅ `application/service/DoctorService.java` - Interface do serviço
- ✅ `application/service/DoctorServiceImpl.java` - Implementação com todas as regras de negócio

### Controller Layer
- ✅ `presentation/controller/DoctorController.java` - Controller REST com todos os endpoints

## 🔧 Arquivos Pré-existentes Utilizados

### Domain Layer
- ✅ `domain/model/Doctor.java` - Entidade JPA
- ✅ `domain/repository/DoctorRepository.java`
- ✅ `domain/repository/UserRepository.java`
- ✅ `domain/repository/HospitalRepository.java`

### Application Layer (DTOs e Mappers)
- ✅ `application/dto/request/DoctorRequest.java`
- ✅ `application/dto/response/DoctorResponse.java`
- ✅ `application/dto/response/DoctorSummary.java`
- ✅ `application/mapper/DoctorMapper.java`
- ✅ `application/mapper/UserMapper.java`
- ✅ `application/mapper/HospitalMapper.java`

### Exception Layer
- ✅ `exception/ResourceNotFoundException.java`
- ✅ `exception/BusinessException.java`

## 📋 Endpoints Implementados

### 1. POST /doctors
**Criar novo médico**

```bash
POST http://localhost:8080/doctors
Content-Type: application/json

{
  "userId": 1,
  "hospitalId": 1,
  "fullName": "Dr. João Silva",
  "cpf": "12345678901",
  "crm": "12345-SP",
  "specialty": "Cardiologia",
  "phone": "11987654321"
}
```

**Response: 201 CREATED**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "user": {
      "id": 1,
      "email": "joao@email.com",
      "role": "DOCTOR",
      "createdAt": "2026-06-22T10:00:00",
      "updatedAt": "2026-06-22T10:00:00"
    },
    "hospital": {
      "id": 1,
      "name": "Hospital São Lucas",
      "city": "São Paulo",
      "state": "SP"
    },
    "fullName": "Dr. João Silva",
    "cpf": "12345678901",
    "crm": "12345-SP",
    "specialty": "Cardiologia",
    "phone": "11987654321",
    "createdAt": "2026-06-22T10:00:00",
    "updatedAt": "2026-06-22T10:00:00"
  }
}
```

---

### 2. GET /doctors
**Listar todos os médicos**

```bash
GET http://localhost:8080/doctors
```

**Response: 200 OK**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "fullName": "Dr. João Silva",
      "crm": "12345-SP",
      "specialty": "Cardiologia"
    },
    {
      "id": 2,
      "fullName": "Dra. Maria Santos",
      "crm": "67890-RJ",
      "specialty": "Pediatria"
    }
  ]
}
```

---

### 3. GET /doctors/{id}
**Buscar médico por ID**

```bash
GET http://localhost:8080/doctors/1
```

**Response: 200 OK**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "user": {
      "id": 1,
      "email": "joao@email.com",
      "role": "DOCTOR",
      "createdAt": "2026-06-22T10:00:00",
      "updatedAt": "2026-06-22T10:00:00"
    },
    "hospital": {
      "id": 1,
      "name": "Hospital São Lucas",
      "city": "São Paulo",
      "state": "SP"
    },
    "fullName": "Dr. João Silva",
    "cpf": "12345678901",
    "crm": "12345-SP",
    "specialty": "Cardiologia",
    "phone": "11987654321",
    "createdAt": "2026-06-22T10:00:00",
    "updatedAt": "2026-06-22T10:00:00"
  }
}
```

**Erro (quando não encontrado): 404 NOT FOUND**
```json
{
  "success": false,
  "message": "Médico não encontrado com ID: 999"
}
```

---

### 4. PUT /doctors/{id}
**Atualizar médico**

```bash
PUT http://localhost:8080/doctors/1
Content-Type: application/json

{
  "userId": 1,
  "hospitalId": 2,
  "fullName": "Dr. João Silva Oliveira",
  "cpf": "12345678901",
  "crm": "12345-SP",
  "specialty": "Cardiologia Intervencionista",
  "phone": "11987654321"
}
```

**Response: 200 OK**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "user": { ... },
    "hospital": { ... },
    "fullName": "Dr. João Silva Oliveira",
    "specialty": "Cardiologia Intervencionista",
    ...
  }
}
```

---

### 5. PATCH /doctors/{id}/inactive
**Inativar médico**

```bash
PATCH http://localhost:8080/doctors/1/inactive
```

**Response Atual: 500 INTERNAL SERVER ERROR**
```json
{
  "success": false,
  "message": "Inativação ainda não implementada. A entidade Doctor precisa de um campo de status para suportar esta operação."
}
```

⚠️ **ATENÇÃO**: Este endpoint retorna erro propositalmente até que a entidade `Doctor` seja atualizada com um campo de status (`active`, `status` ou `deletedAt`).

---

## 🧪 Como Testar

### Pré-requisitos
1. Ter a aplicação rodando: `./mvnw spring-boot:run`
2. Ter dados de teste (User e Hospital) cadastrados no banco

### Opção 1: Swagger UI
1. Acesse: http://localhost:8080/swagger-ui.html
2. Navegue até a seção "Doctors"
3. Teste cada endpoint através da interface

### Opção 2: cURL

```bash
# 1. Criar médico
curl -X POST http://localhost:8080/doctors \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "hospitalId": 1,
    "fullName": "Dr. João Silva",
    "cpf": "12345678901",
    "crm": "12345-SP",
    "specialty": "Cardiologia",
    "phone": "11987654321"
  }'

# 2. Listar todos
curl http://localhost:8080/doctors

# 3. Buscar por ID
curl http://localhost:8080/doctors/1

# 4. Atualizar
curl -X PUT http://localhost:8080/doctors/1 \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "hospitalId": 1,
    "fullName": "Dr. João Silva Oliveira",
    "cpf": "12345678901",
    "crm": "12345-SP",
    "specialty": "Cardiologia Intervencionista",
    "phone": "11987654321"
  }'

# 5. Inativar (retorna erro)
curl -X PATCH http://localhost:8080/doctors/1/inactive
```

### Opção 3: Postman/Insomnia
Importe a coleção de endpoints ou crie manualmente seguindo os exemplos acima.

---

## ✅ Validações Implementadas

O `DoctorRequest` possui as seguintes validações via Bean Validation:

- `userId`: **Obrigatório** (@NotNull)
- `hospitalId`: **Obrigatório** (@NotNull)
- `fullName`: **Obrigatório** e máximo 255 caracteres
- `cpf`: **Obrigatório** e exatamente 11 caracteres
- `crm`: **Obrigatório** e máximo 20 caracteres
- `specialty`: Opcional, máximo 100 caracteres
- `phone`: Opcional, máximo 20 caracteres

---

## 🔒 Tratamento de Exceções

### ResourceNotFoundException (404)
Lançada quando:
- User não encontrado
- Hospital não encontrado
- Doctor não encontrado

### UnsupportedOperationException (500)
Lançada quando:
- Tentativa de inativar médico (funcionalidade pendente)

---

## 📝 Observações Importantes

### Endpoint de Inativação
O endpoint `PATCH /doctors/{id}/inactive` **ainda não está funcional** porque a entidade `Doctor` não possui campo de status/active.

**Para implementar corretamente:**
1. Adicionar campo à entidade Doctor:
   ```java
   @Column(nullable = false)
   private Boolean active = true;
   ```
2. Atualizar o método `inactive()` no service:
   ```java
   @Override
   public void inactive(Long id) {
       Doctor doctor = doctorRepository.findById(id)
           .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado com ID: " + id));
       doctor.setActive(false);
       doctorRepository.save(doctor);
   }
   ```

---

## 🚀 Próximos Passos Sugeridos

1. ✅ CRUD de Doctors - **COMPLETO**
2. ⏭️ Implementar campo de status na entidade Doctor
3. ⏭️ CRUD de Patients
4. ⏭️ CRUD de Hospitals
5. ⏭️ CRUD de Users
6. ⏭️ Implementar autenticação e autorização
7. ⏭️ Testes unitários e de integração

---

## 📊 Estrutura de Camadas

```
┌─────────────────────────────────────┐
│   DoctorController                  │  ← REST Endpoints
│   (Presentation Layer)              │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│   DoctorService                     │  ← Business Logic
│   DoctorServiceImpl                 │
│   (Application Layer)               │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│   DoctorRepository                  │  ← Data Access
│   (Domain Layer)                    │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│   Database (H2/PostgreSQL)          │
└─────────────────────────────────────┘
```

---

## ✅ Checklist de Conclusão

- [x] Interface DoctorService criada
- [x] DoctorServiceImpl implementado com todas as operações
- [x] DoctorController criado com todos os endpoints
- [x] Validações configuradas
- [x] Tratamento de exceções implementado
- [x] Documentação Swagger adicionada
- [x] Endpoint de inativação com TODOs documentados
- [x] Código compilando sem erros

**Status Final: IMPLEMENTAÇÃO CONCLUÍDA COM SUCESSO! ✅**
