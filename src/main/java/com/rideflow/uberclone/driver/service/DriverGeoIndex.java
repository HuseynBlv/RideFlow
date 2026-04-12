package com.rideflow.uberclone.driver.service;

import com.rideflow.uberclone.driver.dto.NearbyDriverResponse;
import com.rideflow.uberclone.driver.entity.DriverProfile;

import java.util.List;
import java.util.UUID;

public interface DriverGeoIndex {

    void updateDriverLocation(DriverProfile driverProfile);

    void removeDriver(UUID driverId);

    List<NearbyDriverResponse> findNearbyAvailableDrivers(double latitude, double longitude, double radiusKm, int limit);
}
