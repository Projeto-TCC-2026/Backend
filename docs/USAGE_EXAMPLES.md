# Exemplos de Uso das Entidades

Este documento fornece exemplos práticos de como utilizar as entidades criadas.

## Configuração Inicial

Todas as entidades estão configuradas com JPA/Hibernate e podem ser utilizadas através dos repositories criados.

## Exemplos por Funcionalidade

### 1. Criar um Novo Usuário e Paciente

```java
// UserService.java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Transactional
    public Patient createPatientUser(String email, String password, String fullName, 
                                      String cpf, LocalDate birthDate) {
        // Criar usuário
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(hashPassword(password)); // implementar hash
        user.setRole("PATIENT");
        user = userRepository.save(user);
        
        // Criar paciente
        Patient patient = new Patient();
        patient.setUser(user);
        patient.setFullName(fullName);
        patient.setCpf(cpf);
        patient.setBirthDate(birthDate);
        
        return patientRepository.save(patient);
    }
}
```

### 2. Criar um Médico Vinculado a um Hospital

```java
// DoctorService.java
@Service
public class DoctorService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private HospitalRepository hospitalRepository;
    
    @Transactional
    public Doctor createDoctor(String email, String password, Long hospitalId,
                                String fullName, String cpf, String crm, String specialty) {
        // Buscar hospital
        Hospital hospital = hospitalRepository.findById(hospitalId)
            .orElseThrow(() -> new ResourceNotFoundException("Hospital não encontrado"));
        
        // Criar usuário
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(hashPassword(password));
        user.setRole("DOCTOR");
        user = userRepository.save(user);
        
        // Criar médico
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setHospital(hospital);
        doctor.setFullName(fullName);
        doctor.setCpf(cpf);
        doctor.setCrm(crm);
        doctor.setSpecialty(specialty);
        
        return doctorRepository.save(doctor);
    }
}
```

### 3. Associar Médico a Paciente

```java
// DoctorPatientService.java
@Service
public class DoctorPatientService {
    
    @Autowired
    private DoctorPatientRepository doctorPatientRepository;
    
    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Transactional
    public DoctorPatient associateDoctorToPatient(Long doctorId, Long patientId) {
        // Verificar se já existe associação
        if (doctorPatientRepository.existsByDoctorIdAndPatientId(doctorId, patientId)) {
            throw new BusinessException("Médico já está associado a este paciente");
        }
        
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado"));
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        
        DoctorPatient doctorPatient = new DoctorPatient();
        doctorPatient.setDoctor(doctor);
        doctorPatient.setPatient(patient);
        
        return doctorPatientRepository.save(doctorPatient);
    }
    
    public List<Patient> findPatientsByDoctorId(Long doctorId) {
        return doctorPatientRepository.findByDoctorId(doctorId)
            .stream()
            .map(DoctorPatient::getPatient)
            .collect(Collectors.toList());
    }
}
```

### 4. Registrar Dispositivo Médico para Paciente

```java
// PatientDeviceService.java
@Service
public class PatientDeviceService {
    
    @Autowired
    private PatientDeviceRepository deviceRepository;
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Transactional
    public PatientDevice registerDevice(Long patientId, String deviceIdentifier,
                                        String deviceType, String manufacturer, String model) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        
        PatientDevice device = new PatientDevice();
        device.setPatient(patient);
        device.setDeviceIdentifier(deviceIdentifier);
        device.setDeviceType(deviceType);
        device.setManufacturer(manufacturer);
        device.setModel(model);
        device.setActive(true);
        
        return deviceRepository.save(device);
    }
    
    public List<PatientDevice> getActiveDevicesByPatient(Long patientId) {
        return deviceRepository.findByPatientIdAndActive(patientId, true);
    }
}
```

### 5. Importar Leituras de Saúde

```java
// HealthReadingService.java
@Service
public class HealthReadingService {
    
    @Autowired
    private HealthReadingRepository healthReadingRepository;
    
    @Autowired
    private ReadingImportRepository importRepository;
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private PatientDeviceRepository deviceRepository;
    
    @Transactional
    public ReadingImport importHealthReadings(Long patientId, Long deviceId, 
                                              String sourceFile, List<ReadingData> readings) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        
        PatientDevice device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Dispositivo não encontrado"));
        
        // Criar registro de importação
        ReadingImport readingImport = new ReadingImport();
        readingImport.setPatient(patient);
        readingImport.setPatientDevice(device);
        readingImport.setSourceFile(sourceFile);
        readingImport = importRepository.save(readingImport);
        
        // Criar leituras individuais
        for (ReadingData data : readings) {
            HealthReading reading = new HealthReading();
            reading.setPatient(patient);
            reading.setPatientDevice(device);
            reading.setReadingImport(readingImport);
            reading.setReadingType(data.getType());
            reading.setValue(data.getValue());
            reading.setUnit(data.getUnit());
            reading.setMeasuredAt(data.getMeasuredAt());
            
            healthReadingRepository.save(reading);
        }
        
        return readingImport;
    }
    
    public List<HealthReading> getPatientReadingsByType(Long patientId, String readingType) {
        return healthReadingRepository.findByPatientIdAndReadingType(patientId, readingType);
    }
}
```

### 6. Criar e Gerenciar Alertas

```java
// AlertService.java
@Service
public class AlertService {
    
    @Autowired
    private AlertRepository alertRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private DoctorPatientRepository doctorPatientRepository;
    
    @Transactional
    public Alert createAlert(Long patientId, Long healthReadingId, 
                            String severity, String title, String description) {
        Alert alert = new Alert();
        alert.setPatient(patientRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado")));
        
        if (healthReadingId != null) {
            alert.setHealthReading(healthReadingRepository.findById(healthReadingId)
                .orElseThrow(() -> new ResourceNotFoundException("Leitura não encontrada")));
        }
        
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setDescription(description);
        alert.setStatus("PENDING");
        
        alert = alertRepository.save(alert);
        
        // Notificar médicos associados ao paciente
        notifyDoctors(alert);
        
        return alert;
    }
    
    private void notifyDoctors(Alert alert) {
        List<DoctorPatient> associations = doctorPatientRepository
            .findByPatientId(alert.getPatient().getId());
        
        for (DoctorPatient dp : associations) {
            Notification notification = new Notification();
            notification.setAlert(alert);
            notification.setDoctor(dp.getDoctor());
            notification.setMessage(buildNotificationMessage(alert));
            notification.setStatus("SENT");
            
            notificationRepository.save(notification);
        }
    }
    
    public List<Alert> getPatientAlertsByStatus(Long patientId, String status) {
        return alertRepository.findByPatientIdAndStatus(patientId, status);
    }
}
```

### 7. Criar e Executar Procedimentos

```java
// ProcedureService.java
@Service
public class ProcedureService {
    
    @Autowired
    private ProcedureRepository procedureRepository;
    
    @Autowired
    private PatientProcedureRepository patientProcedureRepository;
    
    @Autowired
    private ProcedureExecutionRepository executionRepository;
    
    @Transactional
    public Procedure createProcedure(Long doctorId, String title, 
                                     String description, Integer estimatedDuration) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado"));
        
        Procedure procedure = new Procedure();
        procedure.setDoctor(doctor);
        procedure.setTitle(title);
        procedure.setDescription(description);
        procedure.setEstimatedDuration(estimatedDuration);
        procedure.setActive(true);
        
        return procedureRepository.save(procedure);
    }
    
    @Transactional
    public PatientProcedure assignProcedureToPatient(Long procedureId, Long patientId, 
                                                     Long doctorId, LocalDate startDate) {
        Procedure procedure = procedureRepository.findById(procedureId)
            .orElseThrow(() -> new ResourceNotFoundException("Procedimento não encontrado"));
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado"));
        
        PatientProcedure patientProcedure = new PatientProcedure();
        patientProcedure.setProcedure(procedure);
        patientProcedure.setPatient(patient);
        patientProcedure.setDoctor(doctor);
        patientProcedure.setStartDate(startDate);
        patientProcedure.setStatus("SCHEDULED");
        
        return patientProcedureRepository.save(patientProcedure);
    }
    
    @Transactional
    public ProcedureExecution executeProcedure(Long patientProcedureId, 
                                               String observations) {
        PatientProcedure pp = patientProcedureRepository.findById(patientProcedureId)
            .orElseThrow(() -> new ResourceNotFoundException("Procedimento do paciente não encontrado"));
        
        ProcedureExecution execution = new ProcedureExecution();
        execution.setPatientProcedure(pp);
        execution.setProcedure(pp.getProcedure());
        execution.setDoctor(pp.getDoctor());
        execution.setPatient(pp.getPatient());
        execution.setExecutionDate(LocalDateTime.now());
        execution.setStatus("COMPLETED");
        execution.setObservations(observations);
        
        return executionRepository.save(execution);
    }
}
```

### 8. Upload de Fotos de Procedimentos

```java
// ProcedurePhotoService.java
@Service
public class ProcedurePhotoService {
    
    @Autowired
    private ProcedurePhotoRepository photoRepository;
    
    @Autowired
    private ProcedureExecutionRepository executionRepository;
    
    @Transactional
    public ProcedurePhoto uploadPhoto(Long executionId, MultipartFile file) {
        ProcedureExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new ResourceNotFoundException("Execução não encontrada"));
        
        // Upload do arquivo (implementar upload para S3, Azure Storage, etc.)
        String imageUrl = uploadFileToStorage(file);
        
        ProcedurePhoto photo = new ProcedurePhoto();
        photo.setProcedureExecution(execution);
        photo.setImageUrl(imageUrl);
        photo.setFileName(file.getOriginalFilename());
        
        return photoRepository.save(photo);
    }
    
    public List<ProcedurePhoto> getPhotosByExecution(Long executionId) {
        return photoRepository.findByProcedureExecutionId(executionId);
    }
}
```

## Queries Úteis

### Buscar Notificações Não Lidas de um Médico

```java
List<Notification> unreadNotifications = 
    notificationRepository.findByDoctorIdAndReadAtIsNull(doctorId);
```

### Buscar Leituras de Saúde em um Período

```java
LocalDateTime startDate = LocalDateTime.now().minusDays(7);
LocalDateTime endDate = LocalDateTime.now();

List<HealthReading> readings = 
    healthReadingRepository.findByPatientIdAndMeasuredAtBetween(
        patientId, startDate, endDate
    );
```

### Buscar Procedimentos Ativos de um Paciente

```java
List<PatientProcedure> activeProcedures = 
    patientProcedureRepository.findByPatientIdAndStatus(patientId, "IN_PROGRESS");
```

### Buscar Alertas por Severidade

```java
List<Alert> criticalAlerts = alertRepository.findBySeverity("CRITICAL");
```

## Transações e Boas Práticas

1. **Use @Transactional** em métodos que modificam dados
2. **Valide entradas** antes de persistir
3. **Trate exceções** apropriadamente
4. **Use DTOs** para comunicação com controllers
5. **Implemente paginação** para listas grandes
6. **Evite N+1 queries** usando fetch joins quando necessário

## Próximos Passos

1. Implementar validações com Bean Validation (@NotNull, @Email, etc.)
2. Criar DTOs para Request e Response
3. Implementar Mappers (MapStruct ou ModelMapper)
4. Adicionar testes unitários e de integração
5. Implementar autenticação e autorização com Spring Security
6. Adicionar documentação da API com Swagger
