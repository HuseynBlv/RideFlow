package com.rideflow.uberclone.user.repository;

import com.rideflow.uberclone.user.entity.RiderProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiderProfileRepository extends JpaRepository<RiderProfile, UUID> {

    Optional<RiderProfile> findByUser_Id(UUID userId);
}
