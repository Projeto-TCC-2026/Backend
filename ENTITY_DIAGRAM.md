# Diagrama de Relacionamento das Entidades

## Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────────────┐
│                        SISTEMA DE GESTÃO MÉDICA                      │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐
│     User     │
│──────────────│
│ id (PK)      │◄────────────┐
│ email        │             │ 1:1
│ passwordHash │             │
│ role         │             │
└──────────────┘             │
       │                     │
       │ 1:1                 │
       │                     │
       ├─────────────────────┼──────────────┐
       │                     │              │
       ▼                     ▼              │
┌──────────────┐      ┌──────────────┐     │
│   Doctor     │      │   Patient    │     │
│──────────────│      │──────────────│     │
│ id (PK)      │      │ id (PK)      │     │
│ userId (FK)  │──┐   │ userId (FK)  │─────┘
│ hospitalId   │  │   │ fullName     │
│ fullName     │  │   │ cpf          │
│ cpf          │  │   │ birthDate    │
│ crm          │  │   │ gender       │
│ specialty    │  │   │ phone        │
└──────────────┘  │   │ weight       │
       │          │   │ height       │
       │ N:1      │   └──────────────┘
       │          │          │
       ▼          │          │ 1:N
┌──────────────┐  │          ├────────────────┬────────────┐
│   Hospital   │  │          │                │            │
│──────────────│  │          ▼                ▼            ▼
│ id (PK)      │  │   ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ name         │  │   │PatientDevice │ │HealthReading │ │    Alert     │
│ cnpj         │  │   │──────────────│ │──────────────│ │──────────────│
│ phone        │  │   │ id (PK)      │ │ id (PK)      │ │ id (PK)      │
│ email        │  │   │ patientId    │ │ patientId    │ │ patientId    │
│ address      │  │   │ deviceId     │ │ deviceId     │ │ readingId    │
└──────────────┘  │   │ deviceType   │ │ readingType  │ │ severity     │
                  │   │ active       │ │ value        │ │ title        │
                  │   └──────────────┘ │ unit         │ │ status       │
                  │          │         │ measuredAt   │ └──────────────┘
                  │          │ 1:N     └──────────────┘        │
                  │          │                │                │ 1:N
                  │          ▼                │ 1:N            │
                  │   ┌──────────────┐        │                ▼
                  │   │ReadingImport │◄───────┘         ┌──────────────┐
                  │   │──────────────│                  │ Notification │
                  │   │ id (PK)      │                  │──────────────│
                  │   │ patientId    │                  │ id (PK)      │
                  │   │ deviceId     │                  │ alertId (FK) │
                  │   │ sourceFile   │                  │ doctorId     │
                  │   │ importedAt   │                  │ message      │
                  │   └──────────────┘                  │ sentAt       │
                  │                                     │ readAt       │
                  │                                     └──────────────┘
                  │                                            ▲
                  │                                            │ N:1
                  └────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    RELACIONAMENTO DOCTOR-PATIENT                     │
└─────────────────────────────────────────────────────────────────────┘

      ┌──────────────┐         ┌──────────────────┐         ┌──────────────┐
      │   Doctor     │   N:N   │  DoctorPatient   │   N:N   │   Patient    │
      │──────────────│◄────────│──────────────────│────────►│──────────────│
      │ id (PK)      │         │ id (PK)          │         │ id (PK)      │
      └──────────────┘         │ doctorId (FK)    │         └──────────────┘
                               │ patientId (FK)   │
                               │ createdAt        │
                               └──────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    FLUXO DE PROCEDIMENTOS MÉDICOS                    │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐
│   Doctor     │
│──────────────│
│ id (PK)      │
└──────────────┘
       │
       │ 1:N (cria procedimentos)
       │
       ▼
┌──────────────────┐
│   Procedure      │
│──────────────────│
│ id (PK)          │
│ doctorId (FK)    │
│ title            │
│ description      │
│ estimatedDuration│
│ active           │
└──────────────────┘
       │
       │ 1:N (associado a pacientes)
       │
       ▼
┌─────────────────────┐
│ PatientProcedure    │
│─────────────────────│
│ id (PK)             │
│ patientId (FK)      │
│ procedureId (FK)    │
│ doctorId (FK)       │
│ startDate           │
│ endDate             │
│ status              │
│ notes               │
└─────────────────────┘
       │
       │ 1:N (múltiplas execuções)
       │
       ▼
┌───────────────────────┐
│ ProcedureExecution    │
│───────────────────────│
│ id (PK)               │
│ patientProcedureId    │
│ procedureId (FK)      │
│ doctorId (FK)         │
│ patientId (FK)        │
│ executionDate         │
│ status                │
│ observations          │
└───────────────────────┘
       │
       │ 1:N (fotos do procedimento)
       │
       ▼
┌───────────────────┐
│ ProcedurePhoto    │
│───────────────────│
│ id (PK)           │
│ executionId (FK)  │
│ imageUrl          │
│ fileName          │
│ uploadedAt        │
└───────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    FLUXO DE LEITURAS DE SAÚDE                        │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐       ┌──────────────────┐
│   Patient    │       │  PatientDevice   │
│──────────────│◄──────│──────────────────│
│ id (PK)      │ 1:N   │ id (PK)          │
└──────────────┘       │ patientId (FK)   │
       │               │ deviceIdentifier │
       │               │ deviceType       │
       │               └──────────────────┘
       │                      │
       │                      │ 1:N
       │                      │
       │                      ▼
       │               ┌──────────────────┐
       │               │  ReadingImport   │
       │               │──────────────────│
       │               │ id (PK)          │
       │               │ patientId (FK)   │
       │               │ deviceId (FK)    │
       │               │ sourceFile       │
       │               └──────────────────┘
       │                      │
       │                      │ 1:N
       │                      │
       │                      ▼
       │               ┌──────────────────┐
       └──────────────►│ HealthReading    │
                 1:N   │──────────────────│
                       │ id (PK)          │
                       │ patientId (FK)   │
                       │ deviceId (FK)    │
                       │ importId (FK)    │
                       │ readingType      │
                       │ value            │
                       │ unit             │
                       │ measuredAt       │
                       └──────────────────┘
                              │
                              │ 1:N (gera alertas)
                              │
                              ▼
                       ┌──────────────────┐
                       │     Alert        │
                       │──────────────────│
                       │ id (PK)          │
                       │ patientId (FK)   │
                       │ readingId (FK)   │
                       │ severity         │
                       │ title            │
                       │ status           │
                       └──────────────────┘
                              │
                              │ 1:N (notifica médicos)
                              │
                              ▼
                       ┌──────────────────┐
                       │  Notification    │
                       │──────────────────│
                       │ id (PK)          │
                       │ alertId (FK)     │
                       │ doctorId (FK)    │
                       │ message          │
                       │ sentAt           │
                       │ readAt           │
                       └──────────────────┘
```

## Legenda

- **PK** = Primary Key (Chave Primária)
- **FK** = Foreign Key (Chave Estrangeira)
- **1:1** = Relacionamento Um para Um
- **1:N** = Relacionamento Um para Muitos
- **N:1** = Relacionamento Muitos para Um
- **N:N** = Relacionamento Muitos para Muitos

## Características Principais

### Autenticação e Autorização
- **User** é a entidade base para autenticação
- Cada usuário pode ser Doctor ou Patient (1:1)
- O campo `role` define o tipo de acesso

### Gestão Hospitalar
- **Hospital** pode ter múltiplos médicos
- **Doctor** pertence a um hospital específico

### Monitoramento de Pacientes
- **PatientDevice** registra dispositivos médicos
- **HealthReading** armazena leituras de sinais vitais
- **Alert** é gerado automaticamente para leituras anormais
- **Notification** notifica médicos sobre alertas críticos

### Procedimentos Médicos
- **Procedure** define templates de procedimentos
- **PatientProcedure** vincula procedimentos a pacientes
- **ProcedureExecution** registra cada execução
- **ProcedurePhoto** armazena evidências fotográficas

### Importação de Dados
- **ReadingImport** rastreia arquivos de leitura importados
- **HealthReading** vincula cada leitura à sua importação
