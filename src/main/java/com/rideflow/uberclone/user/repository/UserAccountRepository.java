package com.rideflow.uberclone.user.repository;

import com.rideflow.uberclone.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByPhone(String phone);

    boolean existsByPhone(String phone);
}
