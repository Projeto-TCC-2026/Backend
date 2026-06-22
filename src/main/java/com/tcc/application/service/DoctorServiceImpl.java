package com.tcc.application.service;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.dto.response.DoctorSummary;
import com.tcc.application.mapper.DoctorMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final DoctorMapper doctorMapper;

    public DoctorServiceImpl(DoctorRepository doctorRepository,
                             UserRepository userRepository,
                             HospitalRepository hospitalRepository,
                             DoctorMapper doctorMapper) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
        this.doctorMapper = doctorMapper;
    }

    @Override
    public DoctorResponse create(DoctorRequest request) {
        // Buscar User pelo userId
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + request.userId()));

        // Buscar Hospital pelo hospitalId
        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital não encontrado com ID: " + request.hospitalId()));

        // Criar entidade usando doctorMapper
        Doctor doctor = doctorMapper.toEntity(request, user, hospital);

        // Salvar no banco de dados
        doctor = doctorRepository.save(doctor);

        // Retornar response
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorSummary> findAll() {
        // Buscar todos os médicos
        List<Doctor> doctors = doctorRepository.findAll();

        // Converter para DoctorSummary
        return doctors.stream()
                .map(doctorMapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse findById(Long id) {
        // Buscar Doctor por ID
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado com ID: " + id));

        // Retornar response
        return doctorMapper.toResponse(doctor);
    }

    @Override
    public DoctorResponse update(Long id, DoctorRequest request) {
        // Buscar Doctor por ID
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado com ID: " + id));

        // Buscar Hospital pelo hospitalId
        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital não encontrado com ID: " + request.hospitalId()));

        // Atualizar entidade usando doctorMapper
        doctorMapper.updateEntity(doctor, request, hospital);

        // Salvar alterações
        doctor = doctorRepository.save(doctor);

        // Retornar response
        return doctorMapper.toResponse(doctor);
    }

    @Override
    public void inactive(Long id) {
        // ATENÇÃO: A entidade Doctor atualmente NÃO possui campo 'active', 'status' ou 'deletedAt'
        // TODO: Adicionar campo de status/active na entidade Doctor para suportar inativação lógica
        throw new UnsupportedOperationException(
                "Inativação ainda não implementada. " +
                "A entidade Doctor precisa de um campo de status para suportar esta operação."
        );
    }
}
