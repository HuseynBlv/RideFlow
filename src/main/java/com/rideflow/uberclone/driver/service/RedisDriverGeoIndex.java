package com.rideflow.uberclone.driver.service;

import com.rideflow.uberclone.driver.dto.NearbyDriverResponse;
import com.rideflow.uberclone.driver.entity.DriverProfile;
import com.rideflow.uberclone.driver.entity.DriverStatus;
import com.rideflow.uberclone.driver.repository.DriverProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "app.dispatch.geo-provider", havingValue = "redis", matchIfMissing = true)
public class RedisDriverGeoIndex implements DriverGeoIndex {

    private static final String DRIVER_GEO_KEY = "drivers:geo";
    private static final String DRIVER_HEARTBEAT_KEY_PREFIX = "drivers:heartbeat:";

    private final StringRedisTemplate redisTemplate;
    private final DriverProfileRepository driverProfileRepository;
    private final long heartbeatTtlSeconds;

    public RedisDriverGeoIndex(
            StringRedisTemplate redisTemplate,
            DriverProfileRepository driverProfileRepository,
            @Value("${app.dispatch.heartbeat-ttl-seconds}") long heartbeatTtlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.driverProfileRepository = driverProfileRepository;
        this.heartbeatTtlSeconds = heartbeatTtlSeconds;
    }

    @Override
    public void updateDriverLocation(DriverProfile driverProfile) {
        String driverId = driverProfile.getId().toString();
        redisTemplate.opsForGeo().add(
                DRIVER_GEO_KEY,
                new Point(driverProfile.getCurrentLongitude(), driverProfile.getCurrentLatitude()),
                driverId
        );
        redisTemplate.opsForValue().set(heartbeatKey(driverId), "1", Duration.ofSeconds(heartbeatTtlSeconds));
    }

    @Override
    public void removeDriver(UUID driverId) {
        redisTemplate.opsForZSet().remove(DRIVER_GEO_KEY, driverId.toString());
        redisTemplate.delete(heartbeatKey(driverId.toString()));
    }

    @Override
    public List<NearbyDriverResponse> findNearbyAvailableDrivers(double latitude, double longitude, double radiusKm, int limit) {
        GeoResults<org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(
                        DRIVER_GEO_KEY,
                        new Circle(new Point(longitude, latitude), new Distance(radiusKm, Metrics.KILOMETERS)),
                        GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().sortAscending().limit(limit)
                );

        if (results == null || results.getContent().isEmpty()) {
            return List.of();
        }

        Map<UUID, Double> distanceByDriver = new LinkedHashMap<>();
        for (GeoResult<org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<String>> result : results) {
            String driverId = result.getContent().getName();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(heartbeatKey(driverId)))) {
                distanceByDriver.put(UUID.fromString(driverId), result.getDistance() == null ? 0.0 : result.getDistance().getValue());
            }
        }

        if (distanceByDriver.isEmpty()) {
            return List.of();
        }

        Instant freshnessCutoff = Instant.now().minusSeconds(heartbeatTtlSeconds);
        List<DriverProfile> availableDrivers = driverProfileRepository.findByIdInAndStatusAndLastLocationAtAfter(
                distanceByDriver.keySet(),
                DriverStatus.AVAILABLE,
                freshnessCutoff
        );

        Map<UUID, DriverProfile> driverById = availableDrivers.stream()
                .collect(Collectors.toMap(DriverProfile::getId, driver -> driver));

        List<NearbyDriverResponse> response = new ArrayList<>();
        distanceByDriver.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .forEach(entry -> {
                    DriverProfile driver = driverById.get(entry.getKey());
                    if (driver != null) {
                        response.add(new NearbyDriverResponse(
                                driver.getId(),
                                driver.getUser().getName(),
                                driver.getVehicleType(),
                                driver.getPlateNumber(),
                                entry.getValue(),
                                driver.getCurrentLatitude(),
                                driver.getCurrentLongitude()
                        ));
                    }
                });
        return response;
    }

    private String heartbeatKey(String driverId) {
        return DRIVER_HEARTBEAT_KEY_PREFIX + driverId;
    }
}
