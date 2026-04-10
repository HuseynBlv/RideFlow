package com.rideflow.uberclone.ride.service;

import com.rideflow.uberclone.driver.entity.DriverProfile;
import com.rideflow.uberclone.ride.entity.Ride;
import com.rideflow.uberclone.ride.entity.RideStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Service
public class RideStateService {

    public void markMatching(Ride ride) {
        transition(ride, Set.of(RideStatus.REQUESTED), RideStatus.MATCHING);
    }

    public void assignDriver(Ride ride, DriverProfile driver, Instant acceptedAt) {
        transition(ride, Set.of(RideStatus.MATCHING), RideStatus.DRIVER_ASSIGNED);
        ride.setDriver(driver);
        ride.setAcceptedAt(acceptedAt);
    }

    public void markDriverArriving(Ride ride) {
        transition(ride, Set.of(RideStatus.DRIVER_ASSIGNED), RideStatus.DRIVER_ARRIVING);
    }

    public void startRide(Ride ride, Instant startedAt) {
        transition(ride, Set.of(RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVING), RideStatus.IN_PROGRESS);
        ride.setStartedAt(startedAt);
    }

    public void completeRide(Ride ride, Instant endedAt, BigDecimal finalFare) {
        transition(ride, Set.of(RideStatus.IN_PROGRESS), RideStatus.COMPLETED);
        ride.setEndedAt(endedAt);
        ride.setFinalFare(finalFare);
    }

    public void cancelByRider(Ride ride) {
        transition(ride, Set.of(RideStatus.REQUESTED, RideStatus.MATCHING, RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVING), RideStatus.CANCELLED_BY_RIDER);
    }

    public void cancelByDriver(Ride ride) {
        transition(ride, Set.of(RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVING), RideStatus.CANCELLED_BY_DRIVER);
    }

    public void expireRide(Ride ride) {
        transition(ride, Set.of(RideStatus.REQUESTED, RideStatus.MATCHING), RideStatus.EXPIRED);
    }

    private void transition(Ride ride, Set<RideStatus> allowedSources, RideStatus target) {
        if (!allowedSources.contains(ride.getStatus())) {
            throw new IllegalStateException("Ride cannot transition from " + ride.getStatus() + " to " + target);
        }
        ride.setStatus(target);
    }
}
