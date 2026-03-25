CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    available_seats INT NOT NULL,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    event_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_event FOREIGN KEY (event_id) REFERENCES events(id)
);

INSERT INTO events (name, capacity, available_seats, version)
VALUES ('Spring Boot Demo Event', 1, 1, 0)