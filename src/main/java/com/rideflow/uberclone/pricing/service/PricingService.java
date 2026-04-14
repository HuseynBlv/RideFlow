package com.rideflow.uberclone.pricing.service;

import com.rideflow.uberclone.pricing.config.PricingProperties;
import com.rideflow.uberclone.ride.entity.Ride;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Service
public class PricingService {

    private final PricingProperties pricingProperties;

    public PricingService(PricingProperties pricingProperties) {
        this.pricingProperties = pricingProperties;
    }

    public BigDecimal estimateFare(double pickupLat, double pickupLon, double dropoffLat, double dropoffLon) {
        double distanceKm = haversineKm(pickupLat, pickupLon, dropoffLat, dropoffLon);
        double billableDistanceKm = billableDistanceKm(distanceKm);
        long estimatedMinutes = estimatedDurationMinutes(billableDistanceKm);
        return totalFare(billableDistanceKm, estimatedMinutes);
    }

    public BigDecimal calculateFinalFare(Ride ride, Instant completedAt) {
        double directDistanceKm = haversineKm(
                ride.getPickupLatitude(),
                ride.getPickupLongitude(),
                ride.getDropoffLatitude(),
                ride.getDropoffLongitude()
        );
        double billableDistanceKm = billableDistanceKm(directDistanceKm);
        long durationMinutes = ride.getStartedAt() == null
                ? estimatedDurationMinutes(billableDistanceKm)
                : finalDurationMinutes(ride.getStartedAt(), completedAt);
        return totalFare(billableDistanceKm, durationMinutes);
    }

    private BigDecimal totalFare(double billableDistanceKm, long durationMinutes) {
        BigDecimal subtotal = pricingProperties.getBaseFare()
                .add(pricingProperties.getBookingFee())
                .add(pricingProperties.getPerKmRate().multiply(BigDecimal.valueOf(billableDistanceKm)))
                .add(pricingProperties.getPerMinuteRate().multiply(BigDecimal.valueOf(durationMinutes)));
        return subtotal.max(pricingProperties.getMinimumFare()).setScale(2, RoundingMode.HALF_UP);
    }

    private double billableDistanceKm(double directDistanceKm) {
        double adjustedDistanceKm = directDistanceKm * pricingProperties.getRouteDistanceMultiplier();
        return BigDecimal.valueOf(adjustedDistanceKm).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private long estimatedDurationMinutes(double billableDistanceKm) {
        double averageSpeedKph = Math.max(1.0, pricingProperties.getEstimatedAverageSpeedKph());
        long minutes = (long) Math.ceil((billableDistanceKm / averageSpeedKph) * 60.0);
        return Math.max(pricingProperties.getMinimumEstimatedDurationMinutes(), minutes);
    }

    private long finalDurationMinutes(Instant startedAt, Instant completedAt) {
        long seconds = Math.max(0, Duration.between(startedAt, completedAt).getSeconds());
        long roundedMinutes = seconds == 0 ? 0 : (long) Math.ceil(seconds / 60.0);
        return Math.max(pricingProperties.getMinimumFinalDurationMinutes(), roundedMinutes);
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
