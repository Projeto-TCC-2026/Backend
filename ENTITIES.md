# Documentação das Entidades

Este documento descreve todas as entidades criadas no sistema e seus relacionamentos.

## Entidades Criadas

### 1. User
**Localização:** `src/main/java/com/tcc/domain/model/User.java`

Entidade que representa um usuário do sistema.

**Campos:**
- `id` (PK) - Long
- `email` - String (unique, required)
- `passwordHash` - String (required)
- `role` - String (required)
- `createdAt` - LocalDateTime (auto)
- `updatedAt` - LocalDateTime (auto)

**Relacionamentos:**
- 1:1 com Doctor
- 1:1 com Patient

---

### 2. Hospital
**Localização:** `src/main/java/com/tcc/domain/model/Hospital.java`

Entidade que representa um hospital.

**Campos:**
- `id` (PK) - Long
- `name` - String (required)
- `cnpj` - String (unique, required)
- `phone` - String
- `email` - String
- `address` - String
- `city` - String
- `state` - String
- `createdAt` - LocalDateTime (auto)
- `updatedAt` - LocalDateTime (auto)

**Relacionamentos:**
- 1:N com Doctor

---

### 3. Doctor
**Localização:** `src/main/java/com/tcc/domain/model/Doctor.java`

Entidade que representa um médico.

**Campos:**
- `id` (PK) - Long
- `userId` (FK) - Long
- `hospitalId` (FK) - Long
- `fullName` - String (required)
- `cpf` - String (unique, required)
- `crm` - String (unique, required)
- `specialty` - String
- `phone` - String
- `createdAt` - LocalDateTime (auto)
- `updatedAt` - LocalDateTime (auto)

**Relacionamentos:**
- N:1 com Hospital
- 1:1 com User
- N:N com Patient (via DoctorPatient)
- 1:N com Procedure
- 1:N com Notification

---

### 4. Patient
**Localização:** `src/main/java/com/tcc/domain/model/Patient.java`

Entidade que representa um paciente.

**Campos:**
- `id` (PK) - Long
- `userId` (FK) - Long
- `fullName` - String (required)
- `cpf` - String (unique, required)
- `birthDate` - LocalDate (required)
- `gender` - String
- `phone` - String
- `weight` - Double
- `height` - Double
- `createdAt` - LocalDateTime (auto)
- `updatedAt` - LocalDateTime (auto)

**Relacionamentos:**
- 1:1 com User
- N:N com Doctor (via DoctorPatient)
- 1:N com PatientDevice
- 1:N com PatientProcedure
- 1:N com HealthReading
- 1:N com Alert

---

### 5. DoctorPatient
**Localização:** `src/main/java/com/tcc/domain/model/DoctorPatient.java`

Tabela de associação entre Doctor e Patient.

**Campos:**
- `id` (PK) - Long
- `doctorId` (FK) - Long
- `patientId` (FK) - Long
- `createdAt` - LocalDateTime (auto)

**Relacionamentos:**
- N:1 com Doctor
- N:1 com Patient

---

### 6. PatientDevice
**Localização:** `src/main/java/com/tcc/domain/model/PatientDevice.java`

Entidade que representa dispositivos médicos associados a pacientes.

**Campos:**
- `id` (PK) - Long
- `patientId` (FK) - Long
- `deviceIdentifier` - String (required)
- `deviceType` - String (required)
- `manufacturer` - String
- `model` - String
- `active` - Boolean (default: true)
- `createdAt` - LocalDateTime (auto)

**Relacionamentos:**
- N:1 com Patient
- 1:N com ReadingImport
- 1:N com HealthReading

---

### 7. Procedure
**Localização:** `src/main/java/com/tcc/domain/model/Procedure.java`

Entidade que representa procedimentos médicos.

**Campos:**
- `id` (PK) - Long
- `doctorId` (FK) - Long
- `title` - String (required)
- `description` - String (TEXT)
- `estimatedDuration` - Integer
- `active` - Boolean (default: true)
- `createdAt` - LocalDateTime (auto)

**Relacionamentos:**
- N:1 com Doctor
- 1:N com PatientProcedure
- 1:N com ProcedureExecution

---

### 8. PatientProcedure
**Localização:** `src/main/java/com/tcc/domain/model/PatientProcedure.java`

Entidade que vincula procedimentos a pacientes.

**Campos:**
- `id` (PK) - Long
- `patientId` (FK) - Long
- `procedureId` (FK) - Long
- `doctorId` (FK) - Long
- `startDate` - LocalDate (required)
- `endDate` - LocalDate
- `status` - String (required)
- `notes` - String (TEXT)

**Relacionamentos:**
- N:1 com Patient
- N:1 com Doctor
- N:1 com Procedure
- 1:N com ProcedureExecution

---

### 9. ProcedureExecution
**Localização:** `src/main/java/com/tcc/domain/model/ProcedureExecution.java`

Entidade que representa execuções específicas de procedimentos.

**Campos:**
- `id` (PK) - Long
- `patientProcedureId` (FK) - Long
- `procedureId` (FK) - Long
- `doctorId` (FK) - Long
- `patientId` (FK) - Long
- `executionDate` - LocalDateTime (required)
- `status` - String (required)
- `observations` - String (TEXT)

**Relacionamentos:**
- N:1 com PatientProcedure
- N:1 com Procedure
- N:1 com Doctor
- N:1 com Patient
- 1:N com ProcedurePhoto

---

### 10. ProcedurePhoto
**Localização:** `src/main/java/com/tcc/domain/model/ProcedurePhoto.java`

Entidade que armazena fotos de procedimentos médicos.

**Campos:**
- `id` (PK) - Long
- `procedureExecutionId` (FK) - Long
- `imageUrl` - String (required)
- `fileName` - String (required)
- `uploadedAt` - LocalDateTime (auto)

**Relacionamentos:**
- N:1 com ProcedureExecution

---

### 11. ReadingImport
**Localização:** `src/main/java/com/tcc/domain/model/ReadingImport.java`

Entidade que registra importações de leituras de dispositivos médicos.

**Campos:**
- `id` (PK) - Long
- `patientId` (FK) - Long
- `patientDeviceId` (FK) - Long
- `sourceFile` - String (required)
- `importedAt` - LocalDateTime (auto)

**Relacionamentos:**
- N:1 com Patient
- N:1 com PatientDevice
- 1:N com HealthReading

---

### 12. HealthReading
**Localização:** `src/main/java/com/tcc/domain/model/HealthReading.java`

Entidade que armazena leituras de saúde dos pacientes.

**Campos:**
- `id` (PK) - Long
- `patientId` (FK) - Long
- `patientDeviceId` (FK) - Long
- `readingImportId` (FK) - Long
- `readingType` - String (required)
- `value` - String (required)
- `unit` - String
- `measuredAt` - LocalDateTime (required)

**Exemplos de readingType:**
- frequência cardíaca
- pressão sistólica
- pressão diastólica
- glicemia
- oxigenação

**Relacionamentos:**
- N:1 com Patient
- N:1 com PatientDevice
- N:1 com ReadingImport
- 1:N com Alert

---

### 13. Alert
**Localização:** `src/main/java/com/tcc/domain/model/Alert.java`

Entidade que representa alertas gerados para condições de saúde anormais.

**Campos:**
- `id` (PK) - Long
- `patientId` (FK) - Long
- `healthReadingId` (FK) - Long
- `severity` - String (required)
- `title` - String (required)
- `description` - String (TEXT)
- `status` - String (required)
- `createdAt` - LocalDateTime (auto)

**Relacionamentos:**
- N:1 com Patient
- N:1 com HealthReading
- 1:N com Notification

---

### 14. Notification
**Localização:** `src/main/java/com/tcc/domain/model/Notification.java`

Entidade que representa notificações enviadas aos médicos.

**Campos:**
- `id` (PK) - Long
- `alertId` (FK) - Long
- `doctorId` (FK) - Long
- `message` - String (TEXT, required)
- `sentAt` - LocalDateTime (auto)
- `readAt` - LocalDateTime
- `status` - String (required)

**Relacionamentos:**
- N:1 com Alert
- N:1 com Doctor

---

## Repositories Criados

Todos os repositories estão localizados em `src/main/java/com/tcc/domain/repository/` e estendem `JpaRepository`:

1. **UserRepository** - Operações para User
2. **HospitalRepository** - Operações para Hospital
3. **DoctorRepository** - Operações para Doctor
4. **PatientRepository** - Operações para Patient
5. **DoctorPatientRepository** - Operações para DoctorPatient
6. **PatientDeviceRepository** - Operações para PatientDevice
7. **ProcedureRepository** - Operações para Procedure
8. **PatientProcedureRepository** - Operações para PatientProcedure
9. **ProcedureExecutionRepository** - Operações para ProcedureExecution
10. **ProcedurePhotoRepository** - Operações para ProcedurePhoto
11. **ReadingImportRepository** - Operações para ReadingImport
12. **HealthReadingRepository** - Operações para HealthReading
13. **AlertRepository** - Operações para Alert
14. **NotificationRepository** - Operações para Notification

Cada repository contém métodos customizados de consulta além dos métodos padrão do JpaRepository.

---

## Configuração do Banco de Dados

O projeto está configurado para usar H2 Database (banco em memória) por padrão. As configurações estão em:

**Arquivo:** `src/main/resources/application.properties`

**Acesso ao console H2:**
- URL: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:tccdb
- Username: sa
- Password: (vazio)

---

## Próximos Passos

1. Implementar Services na camada `application/service`
2. Criar DTOs em `application/dto/request` e `application/dto/response`
3. Criar Mappers em `application/mapper`
4. Implementar Controllers em `presentation/controller`
5. Adicionar validações com Bean Validation
6. Implementar tratamento de exceções global
7. Adicionar segurança com Spring Security
8. Criar migrations com Flyway (opcional)
