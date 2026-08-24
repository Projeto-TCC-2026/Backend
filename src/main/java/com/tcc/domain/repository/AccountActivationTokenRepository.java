package com.tcc.domain.repository;

import com.tcc.domain.model.AccountActivationToken;
import com.tcc.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, UUID> {

    Optional<AccountActivationToken> findByTokenHash(String tokenHash);

    List<AccountActivationToken> findAllByUser(User user);

    void deleteAllByUser(User user);

    void deleteAllByExpiresAtBefore(LocalDateTime dateTime);

    @Modifying
    @Query("UPDATE AccountActivationToken a SET a.used = true WHERE a.user.id = :userId AND a.used = false")
    void invalidateAllByUserId(UUID userId);
}
