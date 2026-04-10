package com.rideflow.uberclone.ride.repository;

import com.rideflow.uberclone.ride.entity.RideEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RideEventRepository extends JpaRepository<RideEvent, UUID> {
}
