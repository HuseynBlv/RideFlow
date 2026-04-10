package com.rideflow.uberclone.user.service;

import com.rideflow.uberclone.common.exception.NotFoundException;
import com.rideflow.uberclone.driver.entity.DriverProfile;
import com.rideflow.uberclone.driver.repository.DriverProfileRepository;
import com.rideflow.uberclone.user.entity.RiderProfile;
import com.rideflow.uberclone.user.entity.UserAccount;
import com.rideflow.uberclone.user.repository.RiderProfileRepository;
import com.rideflow.uberclone.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProfileService {

    private final UserAccountRepository userAccountRepository;
    private final RiderProfileRepository riderProfileRepository;
    private final DriverProfileRepository driverProfileRepository;

    public ProfileService(
            UserAccountRepository userAccountRepository,
            RiderProfileRepository riderProfileRepository,
            DriverProfileRepository driverProfileRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.riderProfileRepository = riderProfileRepository;
        this.driverProfileRepository = driverProfileRepository;
    }

    public UserAccount requireUser(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public RiderProfile requireRider(UUID userId) {
        return riderProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Rider profile not found"));
    }

    public DriverProfile requireDriver(UUID userId) {
        return driverProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Driver profile not found"));
    }
}
