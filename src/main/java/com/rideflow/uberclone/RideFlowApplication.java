package com.rideflow.uberclone;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RideFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(RideFlowApplication.class, args);
    }
}
