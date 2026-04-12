# RideFlow

RideFlow is a simplified Java/Spring Boot ride-hailing backend inspired by Uber. The first milestone focuses on:

- JWT-based rider and driver authentication
- Driver online/offline status and live location updates
- Redis-backed nearest-driver lookup
- Ride request flow with a strict ride state machine
- Concurrency-safe ride acceptance with database locking
- Trip start and completion with pricing and simulated payment
- WebSocket/STOMP ride updates and driver offer notifications
- A built-in rider/driver demo UI served from `/`

## Stack

- Java 21
- Spring Boot 3.5
- PostgreSQL + PostGIS
- Redis
- Flyway
- Spring Security + JWT
- Spring WebSocket/STOMP
- OpenAPI via Springdoc

## Run locally

1. Start infrastructure:

```bash
docker compose up -d
```

2. Start the application:

```bash
mvn spring-boot:run
```

The default local credentials are configured in [application.yml](/Users/huseynbva/RideFlow/src/main/resources/application.yml).

3. Open the demo UI:

```text
http://localhost:8080/
```

## Key API endpoints

- `POST /auth/register`
- `POST /auth/login`
- `POST /drivers/me/online`
- `POST /drivers/me/offline`
- `POST /drivers/me/location`
- `GET /drivers/nearby`
- `POST /rides/request`
- `POST /rides/{rideId}/accept`
- `POST /rides/{rideId}/start`
- `POST /rides/{rideId}/complete`
- `GET /rides/{rideId}`
- `GET /riders/me/rides`
- `GET /drivers/me/rides`

## WebSocket topics

- `/topic/rides/{rideId}`
- `/topic/drivers/{driverId}/offers`
- `/topic/riders/{riderId}`

## Demo flow

1. Register one rider and one or more drivers.
2. Put drivers online and push driver locations.
3. Request a ride as the rider.
4. Observe the candidate driver offer topic.
5. Accept the ride as a driver.
6. Start and complete the ride as the assigned driver.

## Notes

- PostgreSQL remains the source of truth for ride ownership and ride state.
- Redis is used for fast geospatial lookup and driver heartbeats.
- PostGIS is enabled in the database and the schema stores PostGIS geometry alongside numeric coordinates through triggers.
