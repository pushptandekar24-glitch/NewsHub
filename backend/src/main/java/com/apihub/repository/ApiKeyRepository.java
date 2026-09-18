package com.apihub.repository;

import com.apihub.entity.ApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Lookup is by HASH — the raw key never touches the database. */
    Optional<ApiKey> findByKeyHashAndStatus(String keyHash, ApiKey.Status status);

    long countByUserIdAndStatus(Long userId, ApiKey.Status status);
}
