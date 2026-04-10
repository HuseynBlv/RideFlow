package com.rideflow.uberclone.auth.dto;

import com.rideflow.uberclone.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 32) String phone,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull Role role,
        @Size(max = 50) String vehicleType,
        @Size(max = 30) String plateNumber
) {
}
