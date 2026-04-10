package com.rideflow.uberclone.ride.service;

import com.rideflow.uberclone.driver.entity.DriverProfile;
import com.rideflow.uberclone.ride.entity.Ride;
import com.rideflow.uberclone.ride.entity.RideStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RideStateServiceTest {

    private final RideStateService rideStateService = new RideStateService();

    @Test
    void shouldFollowExpectedTransitions() {
        Ride ride = new Ride();
        ride.setId(UUID.randomUUID());
        ride.setStatus(RideStatus.REQUESTED);

        rideStateService.markMatching(ride);
        assertEquals(RideStatus.MATCHING, ride.getStatus());

        DriverProfile driver = new DriverProfile();
        driver.setId(UUID.randomUUID());
        rideStateService.assignDriver(ride, driver, Instant.now());
        assertEquals(RideStatus.DRIVER_ASSIGNED, ride.getStatus());

        rideStateService.startRide(ride, Instant.now());
        assertEquals(RideStatus.IN_PROGRESS, ride.getStatus());

        rideStateService.completeRide(ride, Instant.now(), new BigDecimal("10.00"));
        assertEquals(RideStatus.COMPLETED, ride.getStatus());
    }

    @Test
    void shouldRejectInvalidTransition() {
        Ride ride = new Ride();
        ride.setStatus(RideStatus.REQUESTED);

        assertThrows(IllegalStateException.class, () -> rideStateService.completeRide(ride, Instant.now(), BigDecimal.ONE));
    }
}
