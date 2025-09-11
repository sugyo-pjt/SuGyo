package com.sugyo.auth;

import com.sugyo.auth.domain.RefreshToken;
import com.sugyo.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUserAndIssuedIpAndIssuedUserAgent(User user, String issuedIp, String issuedUserAgent);
}

