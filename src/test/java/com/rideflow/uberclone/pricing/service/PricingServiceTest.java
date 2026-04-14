package com.rideflow.uberclone.pricing.service;

import com.rideflow.uberclone.pricing.config.PricingProperties;
import com.rideflow.uberclone.ride.entity.Ride;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService(defaultProperties());

    @Test
    void shouldApplyMinimumFareForVeryShortTripEstimate() {
        BigDecimal fare = pricingService.estimateFare(40.4093, 49.8671, 40.4093, 49.8671);

        assertEquals(new BigDecimal("5.50"), fare);
    }

    @Test
    void shouldRoundFinalDurationUpToNextMinute() {
        Ride ride = new Ride();
        ride.setPickupLatitude(40.4093);
        ride.setPickupLongitude(49.8671);
        ride.setDropoffLatitude(40.4200);
        ride.setDropoffLongitude(49.9400);
        ride.setStartedAt(Instant.parse("2026-04-14T12:00:00Z"));

        BigDecimal fare = pricingService.calculateFinalFare(ride, Instant.parse("2026-04-14T12:01:01Z"));

        assertEquals(new BigDecimal("13.63"), fare);
    }

    @Test
    void shouldFallbackToEstimatedDurationWhenRideNeverStarted() {
        Ride ride = new Ride();
        ride.setPickupLatitude(40.4093);
        ride.setPickupLongitude(49.8671);
        ride.setDropoffLatitude(40.3777);
        ride.setDropoffLongitude(49.8920);

        BigDecimal fare = pricingService.calculateFinalFare(ride, Instant.parse("2026-04-14T12:10:00Z"));

        assertEquals(new BigDecimal("14.20"), fare);
    }

    private PricingProperties defaultProperties() {
        PricingProperties properties = new PricingProperties();
        properties.setBaseFare(new BigDecimal("2.50"));
        properties.setBookingFee(new BigDecimal("1.00"));
        properties.setMinimumFare(new BigDecimal("5.50"));
        properties.setPerKmRate(new BigDecimal("1.20"));
        properties.setPerMinuteRate(new BigDecimal("0.35"));
        properties.setEstimatedAverageSpeedKph(24.0);
        properties.setRouteDistanceMultiplier(1.25);
        properties.setMinimumEstimatedDurationMinutes(5);
        properties.setMinimumFinalDurationMinutes(1);
        return properties;
    }
}
