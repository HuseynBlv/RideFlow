package com.rideflow.uberclone.pricing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.pricing")
public class PricingProperties {

    private BigDecimal baseFare = new BigDecimal("2.50");
    private BigDecimal bookingFee = new BigDecimal("1.00");
    private BigDecimal minimumFare = new BigDecimal("5.50");
    private BigDecimal perKmRate = new BigDecimal("1.20");
    private BigDecimal perMinuteRate = new BigDecimal("0.35");
    private double estimatedAverageSpeedKph = 24.0;
    private double routeDistanceMultiplier = 1.25;
    private long minimumEstimatedDurationMinutes = 5;
    private long minimumFinalDurationMinutes = 1;

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(BigDecimal baseFare) {
        this.baseFare = baseFare;
    }

    public BigDecimal getBookingFee() {
        return bookingFee;
    }

    public void setBookingFee(BigDecimal bookingFee) {
        this.bookingFee = bookingFee;
    }

    public BigDecimal getMinimumFare() {
        return minimumFare;
    }

    public void setMinimumFare(BigDecimal minimumFare) {
        this.minimumFare = minimumFare;
    }

    public BigDecimal getPerKmRate() {
        return perKmRate;
    }

    public void setPerKmRate(BigDecimal perKmRate) {
        this.perKmRate = perKmRate;
    }

    public BigDecimal getPerMinuteRate() {
        return perMinuteRate;
    }

    public void setPerMinuteRate(BigDecimal perMinuteRate) {
        this.perMinuteRate = perMinuteRate;
    }

    public double getEstimatedAverageSpeedKph() {
        return estimatedAverageSpeedKph;
    }

    public void setEstimatedAverageSpeedKph(double estimatedAverageSpeedKph) {
        this.estimatedAverageSpeedKph = estimatedAverageSpeedKph;
    }

    public double getRouteDistanceMultiplier() {
        return routeDistanceMultiplier;
    }

    public void setRouteDistanceMultiplier(double routeDistanceMultiplier) {
        this.routeDistanceMultiplier = routeDistanceMultiplier;
    }

    public long getMinimumEstimatedDurationMinutes() {
        return minimumEstimatedDurationMinutes;
    }

    public void setMinimumEstimatedDurationMinutes(long minimumEstimatedDurationMinutes) {
        this.minimumEstimatedDurationMinutes = minimumEstimatedDurationMinutes;
    }

    public long getMinimumFinalDurationMinutes() {
        return minimumFinalDurationMinutes;
    }

    public void setMinimumFinalDurationMinutes(long minimumFinalDurationMinutes) {
        this.minimumFinalDurationMinutes = minimumFinalDurationMinutes;
    }
}
