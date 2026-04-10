package com.rideflow.uberclone.driver.service;

import com.rideflow.uberclone.common.exception.ConflictException;
import com.rideflow.uberclone.driver.dto.DriverStatusResponse;
import com.rideflow.uberclone.driver.dto.LocationUpdateRequest;
import com.rideflow.uberclone.driver.dto.NearbyDriverResponse;
import com.rideflow.uberclone.driver.entity.DriverProfile;
import com.rideflow.uberclone.driver.entity.DriverStatus;
import com.rideflow.uberclone.driver.repository.DriverProfileRepository;
import com.rideflow.uberclone.user.service.ProfileService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DriverService {

    private final ProfileService profileService;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverGeoIndexService driverGeoIndexService;
    private final double defaultRadiusKm;
    private final int defaultLimit;

    public DriverService(
            ProfileService profileService,
            DriverProfileRepository driverProfileRepository,
            DriverGeoIndexService driverGeoIndexService,
            @Value("${app.dispatch.search-radius-km}") double defaultRadiusKm,
            @Value("${app.dispatch.max-candidate-drivers}") int defaultLimit
    ) {
        this.profileService = profileService;
        this.driverProfileRepository = driverProfileRepository;
        this.driverGeoIndexService = driverGeoIndexService;
        this.defaultRadiusKm = defaultRadiusKm;
        this.defaultLimit = defaultLimit;
    }

    @Transactional
    public DriverStatusResponse goOnline(UUID userId) {
        DriverProfile driver = profileService.requireDriver(userId);
        if (driver.getCurrentLatitude() == null || driver.getCurrentLongitude() == null) {
            throw new ConflictException("Driver must send a location before going online");
        }
        driver.setStatus(DriverStatus.AVAILABLE);
        driverProfileRepository.save(driver);
        driverGeoIndexService.updateDriverLocation(driver);
        return toResponse(driver);
    }

    @Transactional
    public DriverStatusResponse goOffline(UUID userId) {
        DriverProfile driver = profileService.requireDriver(userId);
        if (driver.getStatus() == DriverStatus.BUSY) {
            throw new ConflictException("Driver cannot go offline while busy");
        }
        driver.setStatus(DriverStatus.OFFLINE);
        driverProfileRepository.save(driver);
        driverGeoIndexService.removeDriver(driver.getId());
        return toResponse(driver);
    }

    @Transactional
    public DriverStatusResponse updateLocation(UUID userId, LocationUpdateRequest request) {
        DriverProfile driver = profileService.requireDriver(userId);
        driver.setCurrentLatitude(request.latitude());
        driver.setCurrentLongitude(request.longitude());
        driver.setLastLocationAt(Instant.now());
        driverProfileRepository.save(driver);
        if (driver.getStatus() == DriverStatus.AVAILABLE) {
            driverGeoIndexService.updateDriverLocation(driver);
        }
        return toResponse(driver);
    }

    public DriverStatusResponse getMyStatus(UUID userId) {
        return toResponse(profileService.requireDriver(userId));
    }

    public List<NearbyDriverResponse> findNearbyDrivers(double latitude, double longitude, Double radiusKm, Integer limit) {
        return driverGeoIndexService.findNearbyAvailableDrivers(
                latitude,
                longitude,
                radiusKm == null ? defaultRadiusKm : radiusKm,
                limit == null ? defaultLimit : limit
        );
    }

    private DriverStatusResponse toResponse(DriverProfile driver) {
        return new DriverStatusResponse(
                driver.getId(),
                driver.getStatus(),
                driver.getCurrentLatitude(),
                driver.getCurrentLongitude(),
                driver.getLastLocationAt()
        );
    }
}
