package com.rideflow.uberclone.driver.dto;

import java.util.UUID;

public record NearbyDriverResponse(
        UUID driverId,
        String driverName,
        String vehicleType,
        String plateNumber,
        double distanceKm,
        Double latitude,
        Double longitude
) {
}
