package com.rideflow.uberclone.ride.dto;

import com.rideflow.uberclone.ride.entity.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RideResponse(
        UUID rideId,
        UUID riderId,
        UUID driverId,
        RideStatus status,
        double pickupLatitude,
        double pickupLongitude,
        double dropoffLatitude,
        double dropoffLongitude,
        BigDecimal estimatedFare,
        BigDecimal finalFare,
        Instant requestedAt,
        Instant acceptedAt,
        Instant startedAt,
        Instant endedAt,
        List<UUID> candidateDriverIds
) {
}
