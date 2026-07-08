package com.tcc.application.service;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.response.PatientResponse;
import com.tcc.application.dto.response.ProcedureExecutionResponse;
import com.tcc.application.mapper.PatientMapper;
import com.tcc.application.mapper.ProcedureExecutionMapper;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureExecutionRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ProcedureExecutionRepository procedureExecutionRepository;
    private final PatientMapper patientMapper;
    private final ProcedureExecutionMapper procedureExecutionMapper;

    public PatientServiceImpl(PatientRepository patientRepository, 
                             UserRepository userRepository,
                             ProcedureExecutionRepository procedureExecutionRepository,
                             PatientMapper patientMapper,
                             ProcedureExecutionMapper procedureExecutionMapper) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.procedureExecutionRepository = procedureExecutionRepository;
        this.patientMapper = patientMapper;
        this.procedureExecutionMapper = procedureExecutionMapper;
    }

    @Override
    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        // Verificar se o usuário existe
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + request.userId()));

        // Verificar se já existe paciente com esse CPF ativo
        if (patientRepository.existsByCpfAndActiveTrue(request.cpf())) {
            throw new BusinessException("Já existe um paciente ativo cadastrado com o CPF: " + request.cpf());
        }

        // Verificar se o usuário já está associado a outro paciente
        if (patientRepository.findByUserId(request.userId()).isPresent()) {
            throw new BusinessException("Usuário já está associado a um paciente");
        }

        // Verificar se já existe paciente com esse e-mail ativo (se fornecido)
        if (request.email() != null && !request.email().trim().isEmpty()) {
            if (patientRepository.existsByEmailAndActiveTrue(request.email())) {
                throw new BusinessException("Já existe um paciente ativo cadastrado com o e-mail: " + request.email());
            }
        }

        Patient patient = patientMapper.toEntity(request, user);
        Patient savedPatient = patientRepository.save(patient);
        
        return patientMapper.toResponse(savedPatient);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> getAllActivePatients(Pageable pageable) {
        return patientRepository.findByActiveTrue(pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        Patient patient = patientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com ID: " + id));
        
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        Patient existingPatient = patientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com ID: " + id));

        // Verificar se o CPF já existe em outro paciente ativo (exceto o atual)
        if (!existingPatient.getCpf().equals(request.cpf()) && 
            patientRepository.existsByCpfAndActiveTrue(request.cpf())) {
            throw new BusinessException("Já existe um paciente ativo cadastrado com o CPF: " + request.cpf());
        }

        // Verificar se o e-mail já existe em outro paciente ativo (exceto o atual)
        if (request.email() != null && !request.email().trim().isEmpty()) {
            if (existingPatient.getEmail() == null || !existingPatient.getEmail().equals(request.email())) {
                if (patientRepository.existsByEmailAndActiveTrue(request.email())) {
                    throw new BusinessException("Já existe um paciente ativo cadastrado com o e-mail: " + request.email());
                }
            }
        }

        // Verificar se o usuário mudou e se já está associado a outro paciente
        if (!existingPatient.getUser().getId().equals(request.userId())) {
            User newUser = userRepository.findById(request.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + request.userId()));
            
            if (patientRepository.findByUserId(request.userId()).isPresent()) {
                throw new BusinessException("Usuário já está associado a um paciente");
            }
            
            existingPatient.setUser(newUser);
        }

        patientMapper.updateEntity(existingPatient, request);
        Patient updatedPatient = patientRepository.save(existingPatient);
        
        return patientMapper.toResponse(updatedPatient);
    }

    @Override
    @Transactional
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com ID: " + id));

        // Verificar se há procedimentos realizados associados
        if (patient.hasProcedureExecutions()) {
            throw new BusinessException("Não é possível excluir o paciente. Existem " + 
                    patient.countProcedureExecutions() + " procedimento(s) realizado(s) associado(s). " +
                    "Use a inativação ao invés da exclusão para manter o histórico.");
        }

        // Verificar outros relacionamentos críticos se necessário
        if (!patient.getHealthReadings().isEmpty()) {
            throw new BusinessException("Não é possível excluir o paciente. Existem leituras de saúde associadas.");
        }

        patientRepository.delete(patient);
    }

    @Override
    @Transactional
    public void inactivatePatient(Long id) {
        Patient patient = patientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com ID: " + id));

        patient.inactivate();
        patientRepository.save(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> searchByName(String name, Pageable pageable) {
        return patientRepository.findByFullNameContainingIgnoreCaseAndActiveTrue(name, pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> searchByCpf(String cpf, Pageable pageable) {
        return patientRepository.findByCpfContainingAndActiveTrue(cpf, pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> searchByEmail(String email, Pageable pageable) {
        return patientRepository.findByEmailContainingIgnoreCaseAndActiveTrue(email, pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> searchByPhone(String phone, Pageable pageable) {
        return patientRepository.findByPhoneContainingAndActiveTrue(phone, pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> filterPatients(String name, String gender, String city, String state, Pageable pageable) {
        return patientRepository.findByFilters(name, gender, city, state, pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcedureExecutionResponse> getPatientProcedureExecutions(Long patientId, Pageable pageable) {
        // Verificar se o paciente existe e está ativo
        Patient patient = patientRepository.findByIdAndActiveTrue(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com ID: " + patientId));

        // Buscar os procedimentos realizados do paciente com paginação
        return procedureExecutionRepository.findByPatientId(patientId, pageable)
                .map(procedureExecutionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countPatientProcedureExecutions(Long patientId) {
        // Verificar se o paciente existe e está ativo
        Patient patient = patientRepository.findByIdAndActiveTrue(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com ID: " + patientId));

        // Contar os procedimentos realizados do paciente
        return procedureExecutionRepository.countByPatientId(patientId);
    }
}