package com.apihub.repository;

import com.apihub.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Interests are LAZY, so a plain findById would blow up with
     * LazyInitializationException once the transaction closes (open-in-view is off).
     * JOIN FETCH loads them in the same query.
     */
    @Query("select u from User u left join fetch u.interests where u.id = :id")
    Optional<User> findByIdWithInterests(Long id);
}
