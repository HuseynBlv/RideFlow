package com.rideflow.uberclone.driver.service;

import com.rideflow.uberclone.driver.dto.NearbyDriverResponse;
import com.rideflow.uberclone.driver.entity.DriverProfile;
import com.rideflow.uberclone.driver.entity.DriverStatus;
import com.rideflow.uberclone.driver.repository.DriverProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "app.dispatch.geo-provider", havingValue = "in-memory")
public class InMemoryDriverGeoIndex implements DriverGeoIndex {

    private final DriverProfileRepository driverProfileRepository;
    private final long heartbeatTtlSeconds;
    private final Map<UUID, IndexedDriverLocation> indexedDrivers = new ConcurrentHashMap<>();

    public InMemoryDriverGeoIndex(
            DriverProfileRepository driverProfileRepository,
            @Value("${app.dispatch.heartbeat-ttl-seconds}") long heartbeatTtlSeconds
    ) {
        this.driverProfileRepository = driverProfileRepository;
        this.heartbeatTtlSeconds = heartbeatTtlSeconds;
    }

    @Override
    public void updateDriverLocation(DriverProfile driverProfile) {
        if (driverProfile.getCurrentLatitude() == null || driverProfile.getCurrentLongitude() == null) {
            return;
        }
        indexedDrivers.put(driverProfile.getId(), new IndexedDriverLocation(
                driverProfile.getCurrentLatitude(),
                driverProfile.getCurrentLongitude(),
                Instant.now()
        ));
    }

    @Override
    public void removeDriver(UUID driverId) {
        indexedDrivers.remove(driverId);
    }

    @Override
    public List<NearbyDriverResponse> findNearbyAvailableDrivers(double latitude, double longitude, double radiusKm, int limit) {
        Instant freshnessCutoff = Instant.now().minusSeconds(heartbeatTtlSeconds);
        Map<UUID, DriverProfile> driversById = driverProfileRepository.findAll()
                .stream()
                .filter(driver -> driver.getStatus() == DriverStatus.AVAILABLE)
                .filter(driver -> driver.getLastLocationAt() != null && !driver.getLastLocationAt().isBefore(freshnessCutoff))
                .collect(java.util.stream.Collectors.toMap(DriverProfile::getId, driver -> driver));

        return indexedDrivers.entrySet().stream()
                .filter(entry -> entry.getValue().updatedAt().isAfter(freshnessCutoff))
                .map(entry -> toNearby(entry.getKey(), entry.getValue(), driversById.get(entry.getKey()), latitude, longitude))
                .filter(response -> response != null)
                .filter(response -> response.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyDriverResponse::distanceKm))
                .limit(limit)
                .toList();
    }

    private NearbyDriverResponse toNearby(UUID driverId, IndexedDriverLocation indexedLocation, DriverProfile driver, double latitude, double longitude) {
        if (driver == null) {
            return null;
        }
        double distanceKm = haversineKm(latitude, longitude, indexedLocation.latitude(), indexedLocation.longitude());
        return new NearbyDriverResponse(
                driverId,
                driver.getUser().getName(),
                driver.getVehicleType(),
                driver.getPlateNumber(),
                distanceKm,
                indexedLocation.latitude(),
                indexedLocation.longitude()
        );
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private record IndexedDriverLocation(double latitude, double longitude, Instant updatedAt) {
    }
}
