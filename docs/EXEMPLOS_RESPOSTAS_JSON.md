# 📄 Exemplos de Respostas JSON - Hospital e Doctor API

## 📋 Índice
1. [Hospital Responses](#hospital-responses)
2. [Doctor Responses](#doctor-responses)
3. [Respostas Paginadas](#respostas-paginadas)
4. [Respostas de Erro](#respostas-de-erro)

---

## 🏥 Hospital Responses

### ✅ Criar Hospital - POST /api/hospitals

**Request:**
```json
{
  "name": "Hospital Santa Maria",
  "cnpj": "12345678901234",
  "phone": "(11) 98765-4321",
  "email": "contato@santamaria.com.br",
  "address": "Rua das Flores, 123",
  "city": "São Paulo",
  "state": "SP"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Hospital Santa Maria",
    "cnpj": "12345678901234",
    "phone": "(11) 98765-4321",
    "email": "contato@santamaria.com.br",
    "address": "Rua das Flores, 123",
    "city": "São Paulo",
    "state": "SP",
    "createdAt": "2026-06-29T10:30:00",
    "updatedAt": "2026-06-29T10:30:00"
  },
  "timestamp": "2026-06-29T10:30:00"
}
```

### ✅ Buscar Hospital - GET /api/hospitals/1

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Hospital Santa Maria",
    "cnpj": "12345678901234",
    "phone": "(11) 98765-4321",
    "email": "contato@santamaria.com.br",
    "address": "Rua das Flores, 123",
    "city": "São Paulo",
    "state": "SP",
    "createdAt": "2026-06-29T10:30:00",
    "updatedAt": "2026-06-29T10:30:00"
  },
  "timestamp": "2026-06-29T11:00:00"
}
```

### ✅ Listar Hospitais - GET /api/hospitals?page=0&size=2

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Hospital Santa Maria",
        "cnpj": "12345678901234",
        "phone": "(11) 98765-4321",
        "email": "contato@santamaria.com.br",
        "address": "Rua das Flores, 123",
        "city": "São Paulo",
        "state": "SP",
        "createdAt": "2026-06-29T10:30:00",
        "updatedAt": "2026-06-29T10:30:00"
      },
      {
        "id": 2,
        "name": "Hospital São Lucas",
        "cnpj": "98765432109876",
        "phone": "(11) 99999-8888",
        "email": "contato@saolucas.com.br",
        "address": "Avenida Paulista, 1000",
        "city": "São Paulo",
        "state": "SP",
        "createdAt": "2026-06-29T11:00:00",
        "updatedAt": "2026-06-29T11:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 2,
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 15,
    "totalPages": 8,
    "last": false,
    "size": 2,
    "number": 0,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "numberOfElements": 2,
    "first": true,
    "empty": false
  },
  "timestamp": "2026-06-29T11:15:00"
}
```

### ✅ Buscar por Nome - GET /api/hospitals/search/name?name=Santa

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Hospital Santa Maria",
        "cnpj": "12345678901234",
        "phone": "(11) 98765-4321",
        "email": "contato@santamaria.com.br",
        "address": "Rua das Flores, 123",
        "city": "São Paulo",
        "state": "SP",
        "createdAt": "2026-06-29T10:30:00",
        "updatedAt": "2026-06-29T10:30:00"
      },
      {
        "id": 5,
        "name": "Hospital Santa Casa",
        "cnpj": "11111111111111",
        "phone": "(11) 88888-7777",
        "email": "contato@santacasa.com.br",
        "address": "Rua Central, 500",
        "city": "São Paulo",
        "state": "SP",
        "createdAt": "2026-06-29T12:00:00",
        "updatedAt": "2026-06-29T12:00:00"
      }
    ],
    "totalElements": 2,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  },
  "timestamp": "2026-06-29T12:30:00"
}
```

### ✅ Filtrar Hospitais - GET /api/hospitals/filter?city=São Paulo&state=SP

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Hospital Santa Maria",
        "cnpj": "12345678901234",
        "phone": "(11) 98765-4321",
        "email": "contato@santamaria.com.br",
        "address": "Rua das Flores, 123",
        "city": "São Paulo",
        "state": "SP",
        "createdAt": "2026-06-29T10:30:00",
        "updatedAt": "2026-06-29T10:30:00"
      }
    ],
    "totalElements": 8,
    "totalPages": 1,
    "empty": false
  },
  "timestamp": "2026-06-29T13:00:00"
}
```

### ✅ Atualizar Hospital - PUT /api/hospitals/1

**Request:**
```json
{
  "name": "Hospital Santa Maria - Unidade Central",
  "cnpj": "12345678901234",
  "phone": "(11) 98765-4321",
  "email": "contato@santamaria.com.br",
  "address": "Rua das Flores, 123 - Sala 101",
  "city": "São Paulo",
  "state": "SP"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Hospital Santa Maria - Unidade Central",
    "cnpj": "12345678901234",
    "phone": "(11) 98765-4321",
    "email": "contato@santamaria.com.br",
    "address": "Rua das Flores, 123 - Sala 101",
    "city": "São Paulo",
    "state": "SP",
    "createdAt": "2026-06-29T10:30:00",
    "updatedAt": "2026-06-29T14:00:00"
  },
  "timestamp": "2026-06-29T14:00:00"
}
```

### ✅ Excluir Hospital - DELETE /api/hospitals/1

**Response (200 OK):**
```json
{
  "success": true,
  "data": null,
  "message": "Hospital excluído com sucesso",
  "timestamp": "2026-06-29T14:30:00"
}
```

---

## 👨‍⚕️ Doctor Responses

### ✅ Criar Doctor - POST /api/doctors

**Request:**
```json
{
  "userId": 5,
  "hospitalId": 1,
  "fullName": "Dr. João Silva",
  "cpf": "12345678901",
  "crm": "12345-SP",
  "specialty": "Cardiologia",
  "phone": "11987654321"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "user": {
      "id": 5,
      "email": "joao.silva@example.com",
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
    "phone": "11987654321",
    "createdAt": "2026-06-29T10:00:00",
    "updatedAt": "2026-06-29T10:00:00"
  },
  "timestamp": "2026-06-29T10:00:00"
}
```

### ✅ Buscar Doctor - GET /api/doctors/1

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "user": {
      "id": 5,
      "email": "joao.silva@example.com",
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
    "phone": "11987654321",
    "createdAt": "2026-06-29T10:00:00",
    "updatedAt": "2026-06-29T10:00:00"
  },
  "timestamp": "2026-06-29T11:00:00"
}
```

### ✅ Listar Doctors - GET /api/doctors?page=0&size=2

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "user": {
          "id": 5,
          "email": "joao.silva@example.com",
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
        "phone": "11987654321",
        "createdAt": "2026-06-29T10:00:00",
        "updatedAt": "2026-06-29T10:00:00"
      },
      {
        "id": 2,
        "user": {
          "id": 8,
          "email": "maria.santos@example.com",
          "role": "DOCTOR",
          "createdAt": "2026-06-29T10:30:00",
          "updatedAt": "2026-06-29T10:30:00"
        },
        "hospital": {
          "id": 1,
          "name": "Hospital Santa Maria",
          "city": "São Paulo",
          "state": "SP"
        },
        "fullName": "Dra. Maria Santos",
        "cpf": "98765432100",
        "crm": "67890-SP",
        "specialty": "Pediatria",
        "phone": "11999999999",
        "createdAt": "2026-06-29T11:00:00",
        "updatedAt": "2026-06-29T11:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 2,
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 25,
    "totalPages": 13,
    "last": false,
    "size": 2,
    "number": 0,
    "numberOfElements": 2,
    "first": true,
    "empty": false
  },
  "timestamp": "2026-06-29T12:00:00"
}
```

### ✅ Buscar por Nome - GET /api/doctors/search/name?name=João

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "user": {
          "id": 5,
          "email": "joao.silva@example.com",
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
        "phone": "11987654321",
        "createdAt": "2026-06-29T10:00:00",
        "updatedAt": "2026-06-29T10:00:00"
      }
    ],
    "totalElements": 3,
    "totalPages": 1,
    "empty": false
  },
  "timestamp": "2026-06-29T13:00:00"
}
```

### ✅ Buscar por CRM - GET /api/doctors/search/crm?crm=12345-SP

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "user": {
      "id": 5,
      "email": "joao.silva@example.com",
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
    "phone": "11987654321",
    "createdAt": "2026-06-29T10:00:00",
    "updatedAt": "2026-06-29T10:00:00"
  },
  "timestamp": "2026-06-29T13:30:00"
}
```

### ✅ Buscar por Especialidade - GET /api/doctors/search/specialty?specialty=Cardiologia

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "user": {
          "id": 5,
          "email": "joao.silva@example.com",
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
        "phone": "11987654321",
        "createdAt": "2026-06-29T10:00:00",
        "updatedAt": "2026-06-29T10:00:00"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "empty": false
  },
  "timestamp": "2026-06-29T14:00:00"
}
```

### ✅ Filtrar Doctors - GET /api/doctors/filter?hospitalId=1&specialty=Cardiologia

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "user": {
          "id": 5,
          "email": "joao.silva@example.com",
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
        "phone": "11987654321",
        "createdAt": "2026-06-29T10:00:00",
        "updatedAt": "2026-06-29T10:00:00"
      }
    ],
    "totalElements": 3,
    "totalPages": 1,
    "empty": false
  },
  "timestamp": "2026-06-29T14:30:00"
}
```

---

## 📄 Respostas Paginadas

### Estrutura Completa de Page

```json
{
  "success": true,
  "data": {
    "content": [
      // Array com os elementos da página
    ],
    "pageable": {
      "pageNumber": 0,        // Número da página atual (começa em 0)
      "pageSize": 10,         // Tamanho da página
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      },
      "offset": 0,            // Offset no total de elementos
      "paged": true,
      "unpaged": false
    },
    "totalElements": 50,      // Total de elementos em todas as páginas
    "totalPages": 5,          // Total de páginas
    "last": false,            // É a última página?
    "size": 10,               // Tamanho da página
    "number": 0,              // Número da página atual
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "numberOfElements": 10,   // Número de elementos nesta página
    "first": true,            // É a primeira página?
    "empty": false            // Está vazia?
  },
  "timestamp": "2026-06-29T15:00:00"
}
```

---

## ❌ Respostas de Erro

### 🔴 Validação - CNPJ Duplicado

**Request:**
```json
POST /api/hospitals
{
  "name": "Outro Hospital",
  "cnpj": "12345678901234",  // CNPJ já existe
  ...
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "error": {
    "message": "Já existe um hospital cadastrado com o CNPJ: 12345678901234",
    "timestamp": "2026-06-29T15:30:00"
  }
}
```

### 🔴 Validação - CRM Duplicado

**Request:**
```json
POST /api/doctors
{
  "crm": "12345-SP",  // CRM já existe
  ...
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "error": {
    "message": "Já existe um doutor cadastrado com o CRM: 12345-SP",
    "timestamp": "2026-06-29T16:00:00"
  }
}
```

### 🔴 Recurso Não Encontrado

**Request:**
```
GET /api/hospitals/999
```

**Response (404 Not Found):**
```json
{
  "success": false,
  "error": {
    "message": "Hospital não encontrado com ID: 999",
    "timestamp": "2026-06-29T16:30:00"
  }
}
```

### 🔴 Hospital Obrigatório

**Request:**
```json
POST /api/doctors
{
  "userId": 5,
  "hospitalId": 999,  // Hospital não existe
  ...
}
```

**Response (404 Not Found):**
```json
{
  "success": false,
  "error": {
    "message": "Hospital não encontrado com ID: 999",
    "timestamp": "2026-06-29T17:00:00"
  }
}
```

### 🔴 Exclusão com Relacionamento

**Request:**
```
DELETE /api/hospitals/1
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "error": {
    "message": "Não é possível excluir o hospital. Existem 5 doutores associados.",
    "timestamp": "2026-06-29T17:30:00"
  }
}
```

### 🔴 Validação de Campos Obrigatórios

**Request:**
```json
POST /api/hospitals
{
  "name": "",  // Nome vazio
  "cnpj": "123"  // CNPJ inválido
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "errors": [
    {
      "field": "name",
      "message": "Nome é obrigatório"
    },
    {
      "field": "cnpj",
      "message": "CNPJ deve ter 14 caracteres"
    }
  ],
  "timestamp": "2026-06-29T18:00:00"
}
```

### 🔴 Não Autorizado

**Request:**
```
GET /api/hospitals
Authorization: Bearer INVALID_TOKEN
```

**Response (401 Unauthorized):**
```json
{
  "success": false,
  "error": {
    "message": "Token inválido ou expirado",
    "timestamp": "2026-06-29T18:30:00"
  }
}
```

### 🔴 Acesso Negado

**Request:**
```
DELETE /api/hospitals/1
Authorization: Bearer DOCTOR_TOKEN  // Doctor não pode excluir
```

**Response (403 Forbidden):**
```json
{
  "success": false,
  "error": {
    "message": "Acesso negado. Você não tem permissão para esta operação.",
    "timestamp": "2026-06-29T19:00:00"
  }
}
```

---

## 🎯 Códigos de Status HTTP

| Código | Descrição | Quando usar |
|--------|-----------|-------------|
| **200** | OK | Operação bem-sucedida (GET, PUT, DELETE) |
| **201** | Created | Recurso criado (POST) |
| **400** | Bad Request | Validação falhou ou regra de negócio violada |
| **401** | Unauthorized | Token ausente, inválido ou expirado |
| **403** | Forbidden | Usuário não tem permissão para a operação |
| **404** | Not Found | Recurso não encontrado |
| **500** | Internal Server Error | Erro interno do servidor |

---

## ✅ Conclusão

Todos os endpoints retornam respostas padronizadas com estrutura consistente:

**Sucesso:**
```json
{
  "success": true,
  "data": {...},
  "timestamp": "2026-06-29T10:00:00"
}
```

**Erro:**
```json
{
  "success": false,
  "error": {
    "message": "Mensagem de erro",
    "timestamp": "2026-06-29T10:00:00"
  }
}
```

Esta padronização facilita:
- ✅ Tratamento de erros no frontend
- ✅ Logs e debugging
- ✅ Monitoramento e métricas
- ✅ Experiência consistente para desenvolvedores
