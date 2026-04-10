package com.rideflow.uberclone.notification.service;

import com.rideflow.uberclone.ride.dto.RideResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishRideUpdate(RideResponse rideResponse) {
        messagingTemplate.convertAndSend("/topic/rides/" + rideResponse.rideId(), rideResponse);
        messagingTemplate.convertAndSend("/topic/riders/" + rideResponse.riderId(), rideResponse);
        if (rideResponse.driverId() != null) {
            messagingTemplate.convertAndSend("/topic/drivers/" + rideResponse.driverId(), rideResponse);
        }
    }

    public void publishRideOffer(UUID driverId, RideResponse rideResponse) {
        messagingTemplate.convertAndSend("/topic/drivers/" + driverId + "/offers", rideResponse);
    }

    public void publishSystemMessage(UUID riderId, String message, UUID rideId) {
        messagingTemplate.convertAndSend("/topic/riders/" + riderId, Map.of(
                "rideId", rideId,
                "message", message
        ));
    }
}
