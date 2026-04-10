package com.rideflow.uberclone.driver.dto;

import com.rideflow.uberclone.driver.entity.DriverStatus;

import java.time.Instant;
import java.util.UUID;

public record DriverStatusResponse(
        UUID driverId,
        DriverStatus status,
        Double latitude,
        Double longitude,
        Instant lastLocationAt
) {
}
