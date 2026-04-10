package com.rideflow.uberclone.driver.controller;

import com.rideflow.uberclone.auth.security.AuthenticatedUser;
import com.rideflow.uberclone.driver.dto.DriverStatusResponse;
import com.rideflow.uberclone.driver.dto.LocationUpdateRequest;
import com.rideflow.uberclone.driver.dto.NearbyDriverResponse;
import com.rideflow.uberclone.driver.service.DriverService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping("/me/online")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverStatusResponse goOnline(@AuthenticationPrincipal AuthenticatedUser user) {
        return driverService.goOnline(user.getUserId());
    }

    @PostMapping("/me/offline")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverStatusResponse goOffline(@AuthenticationPrincipal AuthenticatedUser user) {
        return driverService.goOffline(user.getUserId());
    }

    @PostMapping("/me/location")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverStatusResponse updateLocation(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody LocationUpdateRequest request
    ) {
        return driverService.updateLocation(user.getUserId(), request);
    }

    @GetMapping("/me/status")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverStatusResponse getMyStatus(@AuthenticationPrincipal AuthenticatedUser user) {
        return driverService.getMyStatus(user.getUserId());
    }

    @GetMapping("/nearby")
    public List<NearbyDriverResponse> nearbyDrivers(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) @Min(1) Integer limit
    ) {
        return driverService.findNearbyDrivers(latitude, longitude, radiusKm, limit);
    }
}
