package com.surocksang.domain.user.repository;

import com.surocksang.domain.user.domain.UserAgreement;
import com.surocksang.domain.user.domain.UserAgreementId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAgreementRepository extends JpaRepository<UserAgreement, UserAgreementId> {
}
