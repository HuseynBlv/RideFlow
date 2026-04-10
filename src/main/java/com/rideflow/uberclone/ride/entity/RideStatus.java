package com.rideflow.uberclone.ride.entity;

public enum RideStatus {
    REQUESTED,
    MATCHING,
    DRIVER_ASSIGNED,
    DRIVER_ARRIVING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED_BY_RIDER,
    CANCELLED_BY_DRIVER,
    EXPIRED
}
