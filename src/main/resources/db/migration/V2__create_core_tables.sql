CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(32) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE rider_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id)
);

CREATE TABLE driver_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id),
    vehicle_type VARCHAR(50) NOT NULL,
    plate_number VARCHAR(30) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    current_latitude DOUBLE PRECISION,
    current_longitude DOUBLE PRECISION,
    current_location geography(Point, 4326),
    last_location_at TIMESTAMPTZ
);

CREATE TABLE rides (
    id UUID PRIMARY KEY,
    rider_id UUID NOT NULL REFERENCES rider_profiles (id),
    driver_id UUID REFERENCES driver_profiles (id),
    pickup_latitude DOUBLE PRECISION NOT NULL,
    pickup_longitude DOUBLE PRECISION NOT NULL,
    dropoff_latitude DOUBLE PRECISION NOT NULL,
    dropoff_longitude DOUBLE PRECISION NOT NULL,
    pickup_location geography(Point, 4326),
    dropoff_location geography(Point, 4326),
    status VARCHAR(40) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    estimated_fare NUMERIC(10, 2) NOT NULL,
    final_fare NUMERIC(10, 2),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE ride_events (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL REFERENCES rides (id),
    type VARCHAR(50) NOT NULL,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL UNIQUE REFERENCES rides (id),
    amount NUMERIC(10, 2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_driver_profiles_status ON driver_profiles (status);
CREATE INDEX idx_driver_profiles_last_location_at ON driver_profiles (last_location_at);
CREATE INDEX idx_driver_profiles_current_location ON driver_profiles USING GIST (current_location);
CREATE INDEX idx_rides_status ON rides (status);
CREATE INDEX idx_rides_rider_id ON rides (rider_id);
CREATE INDEX idx_rides_driver_id ON rides (driver_id);
CREATE INDEX idx_rides_pickup_location ON rides USING GIST (pickup_location);
CREATE INDEX idx_rides_dropoff_location ON rides USING GIST (dropoff_location);
CREATE INDEX idx_ride_events_ride_id ON ride_events (ride_id);

CREATE OR REPLACE FUNCTION sync_driver_profile_location()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.current_latitude IS NULL OR NEW.current_longitude IS NULL THEN
        NEW.current_location := NULL;
    ELSE
        NEW.current_location := ST_SetSRID(ST_MakePoint(NEW.current_longitude, NEW.current_latitude), 4326)::geography;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_driver_profiles_location
BEFORE INSERT OR UPDATE OF current_latitude, current_longitude
ON driver_profiles
FOR EACH ROW
EXECUTE FUNCTION sync_driver_profile_location();

CREATE OR REPLACE FUNCTION sync_ride_locations()
RETURNS TRIGGER AS $$
BEGIN
    NEW.pickup_location := ST_SetSRID(ST_MakePoint(NEW.pickup_longitude, NEW.pickup_latitude), 4326)::geography;
    NEW.dropoff_location := ST_SetSRID(ST_MakePoint(NEW.dropoff_longitude, NEW.dropoff_latitude), 4326)::geography;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_rides_locations
BEFORE INSERT OR UPDATE OF pickup_latitude, pickup_longitude, dropoff_latitude, dropoff_longitude
ON rides
FOR EACH ROW
EXECUTE FUNCTION sync_ride_locations();
