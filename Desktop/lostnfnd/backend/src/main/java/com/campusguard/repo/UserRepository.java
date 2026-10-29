package com.campusguard.repo;

import com.campusguard.domain.AppUser;
import com.campusguard.domain.Enums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRole(Enums.Role role);
}
