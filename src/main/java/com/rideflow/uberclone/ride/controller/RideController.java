package com.rideflow.uberclone.ride.controller;

import com.rideflow.uberclone.auth.security.AuthenticatedUser;
import com.rideflow.uberclone.ride.dto.RideRequestRequest;
import com.rideflow.uberclone.ride.dto.RideResponse;
import com.rideflow.uberclone.ride.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping("/rides/request")
    @PreAuthorize("hasRole('RIDER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RideResponse requestRide(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody RideRequestRequest request
    ) {
        return rideService.requestRide(user.getUserId(), request);
    }

    @PostMapping("/rides/{rideId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public RideResponse acceptRide(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return rideService.acceptRide(rideId, user.getUserId());
    }

    @PostMapping("/rides/{rideId}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public RideResponse startRide(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return rideService.startRide(rideId, user.getUserId());
    }

    @PostMapping("/rides/{rideId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public RideResponse completeRide(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return rideService.completeRide(rideId, user.getUserId());
    }

    @GetMapping("/rides/{rideId}")
    public RideResponse getRide(
            @PathVariable UUID rideId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return rideService.getRide(rideId, user.getUserId());
    }

    @GetMapping("/riders/me/rides")
    @PreAuthorize("hasRole('RIDER')")
    public List<RideResponse> riderHistory(@AuthenticationPrincipal AuthenticatedUser user) {
        return rideService.getRiderRides(user.getUserId());
    }

    @GetMapping("/drivers/me/rides")
    @PreAuthorize("hasRole('DRIVER')")
    public List<RideResponse> driverHistory(@AuthenticationPrincipal AuthenticatedUser user) {
        return rideService.getDriverRides(user.getUserId());
    }
}
