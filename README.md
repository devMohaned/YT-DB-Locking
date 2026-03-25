# Database Locking Demo with Spring Boot and PostgreSQL

This project demonstrates how different database concurrency strategies behave when two users try to book the last available seat for the same event at nearly the same time.

It is built to make race conditions visible and easy to explain in a live demo, especially for comparing:

- `PLAIN`
- `OPTIMISTIC`
- `PESSIMISTIC`
- `SERIALIZABLE` (ISOLATION_LEVEL)

## What This Project Shows

The application exposes a simple booking API:

- Create an event with a limited number of seats
- Read event details
- Book a seat using a specific locking/isolation strategy
- List the bookings created for an event

The booking flow intentionally waits for `5` seconds before writing, which makes concurrent requests easier to reproduce during demos.

## Tech Stack

- Java `21`
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Docker Compose
- Maven Wrapper

## How the Demo Works

Every event has:

- `capacity`
- `availableSeats`
- `version`

When two users try to book the same event with capacity `1`, each locking mode behaves differently:

| Mode | Strategy | Expected Demo Outcome |
| --- | --- | --- |
| `PLAIN` | No lock and no version check | Both requests may succeed, causing overbooking |
| `OPTIMISTIC` | JPA `@Version` check | One request succeeds, the other fails with `409 Conflict` |
| `PESSIMISTIC` | Row-level lock with `PESSIMISTIC_WRITE` | One request books first, the other waits and then fails if no seats remain |
| `SERIALIZABLE` | Transaction isolation level `SERIALIZABLE` | One transaction may be aborted by PostgreSQL to preserve serial execution |

## Project Structure

```text
src/main/java/com/db/lock
|-- controller
|-- entity
|-- exception
|-- repository
|-- service
`-- util
```

Important parts:

- `BookingController` exposes the REST API
- `BookingService` contains `PLAIN`, `OPTIMISTIC`, and `PESSIMISTIC` flows
- `SerializableBookingService` handles the `SERIALIZABLE` transaction
- `Event` uses `@Version` for optimistic locking
- `ApiExceptionHandler` converts conflicts into `409` responses

## Prerequisites

Before running the project, make sure you have:

- Java `21`
- Docker and Docker Compose

## Running the Project

### 1. Start PostgreSQL

```bash
docker compose up -d --build
```

This starts PostgreSQL with:

- database: `booking_demo`
- username: `postgres`
- password: `postgres`
- port: `5432`

### 2. Run the Spring Boot app

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

## Configuration

Current application settings in `application.yaml`:

- server port: `8080`
- datasource URL: `jdbc:postgresql://localhost:5432/booking_demo`
- datasource username: `postgres`
- datasource password: `postgres`
- Hibernate DDL mode: `update`
- SQL logging: enabled

## Seed Data and Event ID Note

The database initialization script inserts one event automatically the first time PostgreSQL starts:

- `Spring Boot Demo Event`

Because of that:

- event IDs in your environment may not match the hardcoded IDs in a demo script
- the `postgres_data` Docker volume keeps data between restarts

Safest approach:

1. Call `POST /api/events`
2. Copy the returned `id`
3. Use that `id` in the next requests

If you want a fully clean database for a fresh demo, you can reset the containers and volume:

```bash
docker compose down -v
docker compose up -d --build
```

## API Overview

Base URL:

```text
http://localhost:8080/api
```

### Create Event

`POST /events`

Request body:

```json
{
  "name": "Plain Demo Event",
  "capacity": 1
}
```

Typical response:

```json
{
  "id": 2,
  "name": "Plain Demo Event",
  "capacity": 1,
  "availableSeats": 1,
  "version": 0
}
```

### Get Event

`GET /events/{eventId}`

Typical response:

```json
{
  "id": 2,
  "name": "Plain Demo Event",
  "capacity": 1,
  "availableSeats": 0,
  "version": 0
}
```

### Book Event

`POST /events/{eventId}/book?userId={userId}&mode={mode}`

Supported modes:

- `PLAIN`
- `OPTIMISTIC`
- `PESSIMISTIC`
- `SERIALIZABLE`

Success response:

```json
{
  "status": "SUCCESS",
  "message": "Booked with PLAIN mode",
  "eventId": 2,
  "userId": "user1"
}
```

Possible conflict response:

```json
{
  "status": 409,
  "error": "No seats left"
}
```

Other conflict responses:

```json
{
  "status": 409,
  "error": "Concurrent update detected in OPTIMISTIC mode. Please retry."
}
```

```json
{
  "status": 409,
  "error": "Serializable transaction conflict detected. Retry the request."
}
```

### Get Bookings for an Event

`GET /events/{eventId}/bookings`

This returns the bookings recorded for the event.

## Demo Walkthrough

For a good demo:

1. Create an event with `capacity: 1`
2. Trigger two booking requests quickly for the same event
3. Compare the final event state and the bookings list
4. Explain how each strategy handled the race

## Demo cURL Commands

The commands below preserve your original demo flow. If the event IDs in your environment are different, replace `1`, `2`, `3`, and `4` with the actual IDs returned by `POST /api/events`.

### Plain Demo

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"name":"Plain Demo Event","capacity":1}'

curl http://localhost:8080/api/events/1

curl -X POST "http://localhost:8080/api/events/1/book?userId=user1&mode=PLAIN"
curl -X POST "http://localhost:8080/api/events/1/book?userId=user2&mode=PLAIN"

curl http://localhost:8080/api/events/1
curl http://localhost:8080/api/events/1/bookings
```

Expected explanation:

- both users can observe the seat as available
- both requests may succeed
- bookings can become greater than available capacity

### Optimistic Demo

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"name":"Optimistic Demo Event","capacity":1}'

curl http://localhost:8080/api/events/2

curl -X POST "http://localhost:8080/api/events/2/book?userId=user1&mode=OPTIMISTIC"
curl -X POST "http://localhost:8080/api/events/2/book?userId=user2&mode=OPTIMISTIC"

curl http://localhost:8080/api/events/2
curl http://localhost:8080/api/events/2/bookings
```

Expected explanation:

- both users may read the same version initially
- one update succeeds first
- the second request fails with an optimistic locking conflict

### Pessimistic Demo

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"name":"Pessimistic Demo Event","capacity":1}'

curl http://localhost:8080/api/events/3

curl -X POST "http://localhost:8080/api/events/3/book?userId=user1&mode=PESSIMISTIC"
curl -X POST "http://localhost:8080/api/events/3/book?userId=user2&mode=PESSIMISTIC"

curl http://localhost:8080/api/events/3
curl http://localhost:8080/api/events/3/bookings
```

Expected explanation:

- the first transaction acquires the row lock
- the second request waits
- once the lock is released, the second request sees no seats left and fails

### Serializable Demo

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"name":"Serializable Demo Event","capacity":1}'

curl http://localhost:8080/api/events/4

curl -X POST "http://localhost:8080/api/events/4/book?userId=user1&mode=SERIALIZABLE"
curl -X POST "http://localhost:8080/api/events/4/book?userId=user2&mode=SERIALIZABLE"

curl http://localhost:8080/api/events/4
curl http://localhost:8080/api/events/4/bookings
```

Expected explanation:

- both transactions may begin normally
- PostgreSQL detects that both cannot commit as if they ran one after the other
- one transaction is rolled back with a serialization conflict

## Suggested Demo Talking Points

- `PLAIN` is useful for showing how overbooking happens without coordination
- `OPTIMISTIC` is good when conflicts are rare and retries are acceptable
- `PESSIMISTIC` is useful when you prefer blocking over conflicting updates
- `SERIALIZABLE` gives the strongest isolation but can abort transactions under contention

## Logging

The service logs each stage of the booking flow, including:

- request arrival
- seat checks
- artificial delay
- lock acquisition in pessimistic mode
- update attempts
- conflict handling

This makes the application useful for live coding sessions, demos, and YouTube explanations.

## YouTube Video

```text
https://youtu.be/P2X4Hr4zKcQ
```