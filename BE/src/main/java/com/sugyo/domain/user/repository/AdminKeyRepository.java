package com.sugyo.domain.user.repository;

import com.sugyo.domain.user.domain.AdminKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminKeyRepository extends JpaRepository<AdminKey, Long> {
    boolean existsByKey(String key);
}
