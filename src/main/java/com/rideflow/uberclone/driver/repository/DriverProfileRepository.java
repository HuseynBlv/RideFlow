package com.rideflow.uberclone.driver.repository;

import com.rideflow.uberclone.driver.entity.DriverProfile;
import com.rideflow.uberclone.driver.entity.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {

    Optional<DriverProfile> findByUser_Id(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DriverProfile d where d.user.id = :userId")
    Optional<DriverProfile> findByUserIdForUpdate(UUID userId);

    List<DriverProfile> findByIdInAndStatusAndLastLocationAtAfter(Collection<UUID> ids, DriverStatus status, Instant lastLocationAt);
}
