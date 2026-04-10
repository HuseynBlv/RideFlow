package com.rideflow.uberclone.auth.service;

import com.rideflow.uberclone.auth.dto.AuthResponse;
import com.rideflow.uberclone.auth.dto.LoginRequest;
import com.rideflow.uberclone.auth.dto.RegisterRequest;
import com.rideflow.uberclone.auth.security.AuthenticatedUser;
import com.rideflow.uberclone.auth.security.JwtService;
import com.rideflow.uberclone.common.exception.ConflictException;
import com.rideflow.uberclone.driver.entity.DriverProfile;
import com.rideflow.uberclone.driver.entity.DriverStatus;
import com.rideflow.uberclone.driver.repository.DriverProfileRepository;
import com.rideflow.uberclone.user.entity.RiderProfile;
import com.rideflow.uberclone.user.entity.Role;
import com.rideflow.uberclone.user.entity.UserAccount;
import com.rideflow.uberclone.user.repository.RiderProfileRepository;
import com.rideflow.uberclone.user.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RiderProfileRepository riderProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            RiderProfileRepository riderProfileRepository,
            DriverProfileRepository driverProfileRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.riderProfileRepository = riderProfileRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByPhone(request.phone())) {
            throw new ConflictException("Phone number is already registered");
        }
        if (request.role() == Role.ADMIN) {
            throw new ConflictException("Admin registration is not enabled");
        }
        if (request.role() == Role.DRIVER && (request.vehicleType() == null || request.plateNumber() == null)) {
            throw new ConflictException("Driver registration requires vehicle type and plate number");
        }

        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setName(request.name());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setCreatedAt(Instant.now());
        userAccountRepository.save(user);

        if (request.role() == Role.RIDER) {
            RiderProfile riderProfile = new RiderProfile();
            riderProfile.setId(UUID.randomUUID());
            riderProfile.setUser(user);
            riderProfileRepository.save(riderProfile);
        } else if (request.role() == Role.DRIVER) {
            DriverProfile driverProfile = new DriverProfile();
            driverProfile.setId(UUID.randomUUID());
            driverProfile.setUser(user);
            driverProfile.setVehicleType(request.vehicleType());
            driverProfile.setPlateNumber(request.plateNumber());
            driverProfile.setStatus(DriverStatus.OFFLINE);
            driverProfileRepository.save(driverProfile);
        }

        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getPhone(), user.getPasswordHash(), user.getRole());
        return new AuthResponse(user.getId(), user.getRole(), jwtService.generateToken(principal));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.phone(), request.password())
        );
        UserAccount user = userAccountRepository.findByPhone(request.phone())
                .orElseThrow(() -> new ConflictException("User not found"));
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getPhone(), user.getPasswordHash(), user.getRole());
        return new AuthResponse(user.getId(), user.getRole(), jwtService.generateToken(principal));
    }
}
