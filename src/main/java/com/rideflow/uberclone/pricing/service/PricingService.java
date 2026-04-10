package com.rideflow.uberclone.pricing.service;

import com.rideflow.uberclone.ride.entity.Ride;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Service
public class PricingService {

    private static final BigDecimal BASE_FARE = new BigDecimal("2.50");
    private static final BigDecimal RATE_PER_KM = new BigDecimal("1.20");
    private static final BigDecimal RATE_PER_MIN = new BigDecimal("0.35");

    public BigDecimal estimateFare(double pickupLat, double pickupLon, double dropoffLat, double dropoffLon) {
        double distanceKm = haversineKm(pickupLat, pickupLon, dropoffLat, dropoffLon);
        long estimatedMinutes = Math.max(5, Math.round((distanceKm / 25.0) * 60.0));
        return BASE_FARE
                .add(RATE_PER_KM.multiply(BigDecimal.valueOf(distanceKm)))
                .add(RATE_PER_MIN.multiply(BigDecimal.valueOf(estimatedMinutes)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateFinalFare(Ride ride, Instant completedAt) {
        double distanceKm = haversineKm(
                ride.getPickupLatitude(),
                ride.getPickupLongitude(),
                ride.getDropoffLatitude(),
                ride.getDropoffLongitude()
        );
        long durationMinutes = ride.getStartedAt() == null
                ? Math.max(5, Math.round((distanceKm / 25.0) * 60.0))
                : Math.max(1, Duration.between(ride.getStartedAt(), completedAt).toMinutes());
        return BASE_FARE
                .add(RATE_PER_KM.multiply(BigDecimal.valueOf(distanceKm)))
                .add(RATE_PER_MIN.multiply(BigDecimal.valueOf(durationMinutes)))
                .setScale(2, RoundingMode.HALF_UP);
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
}
