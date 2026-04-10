package com.rideflow.uberclone.ride.service;

import com.rideflow.uberclone.common.exception.ConflictException;
import com.rideflow.uberclone.common.exception.NotFoundException;
import com.rideflow.uberclone.driver.dto.NearbyDriverResponse;
import com.rideflow.uberclone.driver.entity.DriverProfile;
import com.rideflow.uberclone.driver.entity.DriverStatus;
import com.rideflow.uberclone.driver.repository.DriverProfileRepository;
import com.rideflow.uberclone.driver.service.DriverGeoIndexService;
import com.rideflow.uberclone.notification.service.NotificationService;
import com.rideflow.uberclone.payment.entity.Payment;
import com.rideflow.uberclone.payment.entity.PaymentStatus;
import com.rideflow.uberclone.payment.repository.PaymentRepository;
import com.rideflow.uberclone.pricing.service.PricingService;
import com.rideflow.uberclone.ride.dto.RideRequestRequest;
import com.rideflow.uberclone.ride.dto.RideResponse;
import com.rideflow.uberclone.ride.entity.Ride;
import com.rideflow.uberclone.ride.entity.RideEvent;
import com.rideflow.uberclone.ride.entity.RideEventType;
import com.rideflow.uberclone.ride.entity.RideStatus;
import com.rideflow.uberclone.ride.repository.RideEventRepository;
import com.rideflow.uberclone.ride.repository.RideRepository;
import com.rideflow.uberclone.user.entity.RiderProfile;
import com.rideflow.uberclone.user.service.ProfileService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RideService {

    private final ProfileService profileService;
    private final RideRepository rideRepository;
    private final RideEventRepository rideEventRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final PaymentRepository paymentRepository;
    private final PricingService pricingService;
    private final DriverGeoIndexService driverGeoIndexService;
    private final RideStateService rideStateService;
    private final NotificationService notificationService;
    private final double searchRadiusKm;
    private final int maxCandidateDrivers;

    public RideService(
            ProfileService profileService,
            RideRepository rideRepository,
            RideEventRepository rideEventRepository,
            DriverProfileRepository driverProfileRepository,
            PaymentRepository paymentRepository,
            PricingService pricingService,
            DriverGeoIndexService driverGeoIndexService,
            RideStateService rideStateService,
            NotificationService notificationService,
            @Value("${app.dispatch.search-radius-km}") double searchRadiusKm,
            @Value("${app.dispatch.max-candidate-drivers}") int maxCandidateDrivers
    ) {
        this.profileService = profileService;
        this.rideRepository = rideRepository;
        this.rideEventRepository = rideEventRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.paymentRepository = paymentRepository;
        this.pricingService = pricingService;
        this.driverGeoIndexService = driverGeoIndexService;
        this.rideStateService = rideStateService;
        this.notificationService = notificationService;
        this.searchRadiusKm = searchRadiusKm;
        this.maxCandidateDrivers = maxCandidateDrivers;
    }

    @Transactional
    public RideResponse requestRide(UUID riderUserId, RideRequestRequest request) {
        RiderProfile rider = profileService.requireRider(riderUserId);
        Instant now = Instant.now();

        Ride ride = new Ride();
        ride.setId(UUID.randomUUID());
        ride.setRider(rider);
        ride.setPickupLatitude(request.pickupLatitude());
        ride.setPickupLongitude(request.pickupLongitude());
        ride.setDropoffLatitude(request.dropoffLatitude());
        ride.setDropoffLongitude(request.dropoffLongitude());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setRequestedAt(now);
        ride.setEstimatedFare(pricingService.estimateFare(
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.dropoffLatitude(),
                request.dropoffLongitude()
        ));
        rideRepository.save(ride);
        logEvent(ride, RideEventType.REQUESTED, "Ride requested by rider " + rider.getId());

        rideStateService.markMatching(ride);
        rideRepository.save(ride);
        logEvent(ride, RideEventType.MATCHING_STARTED, "Matching started");

        List<NearbyDriverResponse> candidates = driverGeoIndexService.findNearbyAvailableDrivers(
                request.pickupLatitude(),
                request.pickupLongitude(),
                searchRadiusKm,
                maxCandidateDrivers
        );

        if (candidates.isEmpty()) {
            rideStateService.expireRide(ride);
            rideRepository.save(ride);
            logEvent(ride, RideEventType.EXPIRED, "No drivers found");
            RideResponse response = toResponse(ride, List.of());
            notificationService.publishRideUpdate(response);
            notificationService.publishSystemMessage(rider.getId(), "No nearby drivers were available", ride.getId());
            return response;
        }

        List<UUID> candidateIds = candidates.stream().map(NearbyDriverResponse::driverId).toList();
        RideResponse response = toResponse(ride, candidateIds);
        candidates.forEach(candidate -> {
            logEvent(ride, RideEventType.DRIVER_OFFERED, "Offered to driver " + candidate.driverId());
            notificationService.publishRideOffer(candidate.driverId(), response);
        });
        notificationService.publishRideUpdate(response);
        return response;
    }

    @Transactional
    public RideResponse acceptRide(UUID rideId, UUID driverUserId) {
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new NotFoundException("Ride not found"));
        DriverProfile driver = driverProfileRepository.findByUserIdForUpdate(driverUserId)
                .orElseThrow(() -> new NotFoundException("Driver not found"));

        if (isSameDriverIdempotent(ride, driver) && ride.getStatus() != RideStatus.MATCHING) {
            return toResponse(ride, List.of());
        }
        if (ride.getStatus() != RideStatus.MATCHING) {
            throw new ConflictException("Ride is no longer available for acceptance");
        }
        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new ConflictException("Driver is not available");
        }

        Instant now = Instant.now();
        rideStateService.assignDriver(ride, driver, now);
        driver.setStatus(DriverStatus.BUSY);
        driverProfileRepository.save(driver);
        rideRepository.save(ride);
        driverGeoIndexService.removeDriver(driver.getId());
        logEvent(ride, RideEventType.DRIVER_ASSIGNED, "Driver " + driver.getId() + " accepted the ride");

        RideResponse response = toResponse(ride, List.of());
        notificationService.publishRideUpdate(response);
        return response;
    }

    @Transactional
    public RideResponse startRide(UUID rideId, UUID driverUserId) {
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new NotFoundException("Ride not found"));
        DriverProfile driver = driverProfileRepository.findByUserIdForUpdate(driverUserId)
                .orElseThrow(() -> new NotFoundException("Driver not found"));
        assertAssignedDriver(ride, driver);

        if (ride.getStatus() == RideStatus.IN_PROGRESS) {
            return toResponse(ride, List.of());
        }

        rideStateService.startRide(ride, Instant.now());
        rideRepository.save(ride);
        logEvent(ride, RideEventType.STARTED, "Ride started by driver " + driver.getId());

        RideResponse response = toResponse(ride, List.of());
        notificationService.publishRideUpdate(response);
        return response;
    }

    @Transactional
    public RideResponse completeRide(UUID rideId, UUID driverUserId) {
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new NotFoundException("Ride not found"));
        DriverProfile driver = driverProfileRepository.findByUserIdForUpdate(driverUserId)
                .orElseThrow(() -> new NotFoundException("Driver not found"));
        assertAssignedDriver(ride, driver);

        if (ride.getStatus() == RideStatus.COMPLETED) {
            return toResponse(ride, List.of());
        }

        Instant completedAt = Instant.now();
        rideStateService.completeRide(ride, completedAt, pricingService.calculateFinalFare(ride, completedAt));
        rideRepository.save(ride);

        driver.setStatus(DriverStatus.AVAILABLE);
        driverProfileRepository.save(driver);
        if (driver.getCurrentLatitude() != null && driver.getCurrentLongitude() != null) {
            driverGeoIndexService.updateDriverLocation(driver);
        }

        logEvent(ride, RideEventType.COMPLETED, "Ride completed");
        Payment payment = paymentRepository.findByRideId(ride.getId()).orElseGet(Payment::new);
        payment.setId(payment.getId() == null ? UUID.randomUUID() : payment.getId());
        payment.setRide(ride);
        payment.setAmount(ride.getFinalFare());
        payment.setMethod("SIMULATED_CARD");
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setProcessedAt(completedAt);
        paymentRepository.save(payment);
        logEvent(ride, RideEventType.PAYMENT_CAPTURED, "Simulated payment captured");

        RideResponse response = toResponse(ride, List.of());
        notificationService.publishRideUpdate(response);
        return response;
    }

    public RideResponse getRide(UUID rideId, UUID requesterUserId) {
        Ride ride = rideRepository.findDetailedById(rideId)
                .orElseThrow(() -> new NotFoundException("Ride not found"));
        authorizeRideAccess(ride, requesterUserId);
        return toResponse(ride, List.of());
    }

    public List<RideResponse> getRiderRides(UUID riderUserId) {
        return rideRepository.findAllByRider_User_IdOrderByRequestedAtDesc(riderUserId)
                .stream()
                .map(ride -> toResponse(ride, List.of()))
                .toList();
    }

    public List<RideResponse> getDriverRides(UUID driverUserId) {
        return rideRepository.findAllByDriver_User_IdOrderByRequestedAtDesc(driverUserId)
                .stream()
                .map(ride -> toResponse(ride, List.of()))
                .toList();
    }

    private void authorizeRideAccess(Ride ride, UUID requesterUserId) {
        boolean riderOwnsRide = ride.getRider().getUser().getId().equals(requesterUserId);
        boolean driverOwnsRide = ride.getDriver() != null && ride.getDriver().getUser().getId().equals(requesterUserId);
        if (!riderOwnsRide && !driverOwnsRide) {
            throw new ConflictException("User is not allowed to access this ride");
        }
    }

    private void assertAssignedDriver(Ride ride, DriverProfile driver) {
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driver.getId())) {
            throw new ConflictException("Ride is not assigned to this driver");
        }
    }

    private boolean isSameDriverIdempotent(Ride ride, DriverProfile driver) {
        return ride.getDriver() != null && ride.getDriver().getId().equals(driver.getId());
    }

    private void logEvent(Ride ride, RideEventType type, String metadata) {
        RideEvent event = new RideEvent();
        event.setId(UUID.randomUUID());
        event.setRide(ride);
        event.setType(type);
        event.setMetadata(metadata);
        event.setCreatedAt(Instant.now());
        rideEventRepository.save(event);
    }

    private RideResponse toResponse(Ride ride, List<UUID> candidateDriverIds) {
        return new RideResponse(
                ride.getId(),
                ride.getRider().getId(),
                ride.getDriver() == null ? null : ride.getDriver().getId(),
                ride.getStatus(),
                ride.getPickupLatitude(),
                ride.getPickupLongitude(),
                ride.getDropoffLatitude(),
                ride.getDropoffLongitude(),
                ride.getEstimatedFare(),
                ride.getFinalFare(),
                ride.getRequestedAt(),
                ride.getAcceptedAt(),
                ride.getStartedAt(),
                ride.getEndedAt(),
                candidateDriverIds
        );
    }
}
