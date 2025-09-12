package com.sugyo.domain.user.repository;

import com.sugyo.domain.user.domain.UserAgreement;
import com.sugyo.domain.user.domain.UserAgreementId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAgreementRepository extends JpaRepository<UserAgreement, UserAgreementId> {
}
