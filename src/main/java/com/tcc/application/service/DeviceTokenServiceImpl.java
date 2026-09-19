package com.tcc.application.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.application.dto.request.DeviceTokenRequest;
import com.tcc.domain.model.DeviceToken;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DeviceTokenRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;

@Service
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenServiceImpl.class);

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    public DeviceTokenServiceImpl(DeviceTokenRepository deviceTokenRepository,
                                  UserRepository userRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Upsert pela chave de unicidade real, que é o token. Um usuário pode ter
     * vários dispositivos; um token pertence a um único usuário por vez. Quando o
     * token reaparece, o registro existente é reaproveitado e revinculado, o que
     * também cobre o caso de troca de conta no mesmo aparelho.
     */
    @Override
    @Transactional
    public void register(String email, DeviceTokenRequest request) {
        User user = findActiveUser(email);

        Optional<DeviceToken> existing = deviceTokenRepository.findByToken(request.token());

        if (existing.isPresent()) {
            DeviceToken deviceToken = existing.get();
            deviceToken.setUser(user);
            deviceToken.setPlatform(request.platform());
            deviceToken.setDeviceId(request.deviceId());
            deviceTokenRepository.save(deviceToken);
            log.info("Token de push atualizado para o usuario {}.", user.getId());
            return;
        }

        deviceTokenRepository.save(new DeviceToken(
                user, request.token(), request.platform(), request.deviceId()));
        log.info("Token de push registrado para o usuario {}.", user.getId());
    }

    /**
     * A busca inclui o usuário para que um token só possa ser removido por quem o
     * registrou. Token inexistente e token de outro usuário produzem o mesmo erro,
     * de propósito: a resposta não revela que o token existe.
     */
    @Override
    @Transactional
    public void unregister(String email, String token) {
        User user = findActiveUser(email);

        DeviceToken deviceToken = deviceTokenRepository.findByTokenAndUserId(token, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Token de dispositivo não encontrado para o usuário autenticado"));

        deviceTokenRepository.delete(deviceToken);
        log.info("Token de push removido do usuario {}.", user.getId());
    }

    private User findActiveUser(String email) {
        return userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.userNotFoundByEmail(email)));
    }
}
