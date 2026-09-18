package com.apihub.repository;

import com.apihub.entity.ApiDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApiDefinitionRepository extends JpaRepository<ApiDefinition, Long> {

    List<ApiDefinition> findByStatus(ApiDefinition.Status status);

    Optional<ApiDefinition> findByNameIgnoreCase(String name);

    List<ApiDefinition> findByNameContainingIgnoreCase(String term);

    @Query("select a from ApiDefinition a left join fetch a.endpoints where a.id = :id")
    Optional<ApiDefinition> findByIdWithEndpoints(Long id);
}
