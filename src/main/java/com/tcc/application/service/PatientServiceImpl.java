package com.tcc.application.service;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.response.PatientResponse;
import com.tcc.application.mapper.PatientMapper;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientMapper patientMapper;

    public PatientServiceImpl(PatientRepository patientRepository, 
                             UserRepository userRepository,
                             PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.patientMapper = patientMapper;
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

        Patient patient = patientMapper.toEntity(request, user);
        Patient savedPatient = patientRepository.save(patient);
        
        return patientMapper.toResponse(savedPatient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getAllActivePatients() {
        return patientRepository.findByActiveTrue()
                .stream()
                .map(patientMapper::toResponse)
                .collect(Collectors.toList());
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
    public void inactivatePatient(Long id) {
        Patient patient = patientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com ID: " + id));

        patient.inactivate();
        patientRepository.save(patient);
    }
}