package com.rideflow.uberclone.ride.repository;

import com.rideflow.uberclone.ride.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r left join fetch r.rider rider left join fetch rider.user left join fetch r.driver driver left join fetch driver.user where r.id = :rideId")
    Optional<Ride> findByIdForUpdate(UUID rideId);

    @Query("select r from Ride r left join fetch r.rider rider left join fetch rider.user left join fetch r.driver driver left join fetch driver.user where r.id = :rideId")
    Optional<Ride> findDetailedById(UUID rideId);

    List<Ride> findAllByRider_User_IdOrderByRequestedAtDesc(UUID userId);

    List<Ride> findAllByDriver_User_IdOrderByRequestedAtDesc(UUID userId);
}
