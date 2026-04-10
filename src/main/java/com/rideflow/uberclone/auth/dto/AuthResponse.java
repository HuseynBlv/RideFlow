package com.rideflow.uberclone.auth.dto;

import com.rideflow.uberclone.user.entity.Role;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        Role role,
        String accessToken
) {
}
