package com.rideflow.uberclone.auth.security;

import com.rideflow.uberclone.user.entity.UserAccount;
import com.rideflow.uberclone.user.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public AuthenticatedUser loadUserByUsername(String phone) {
        UserAccount user = userAccountRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new AuthenticatedUser(user.getId(), user.getPhone(), user.getPasswordHash(), user.getRole());
    }
}
