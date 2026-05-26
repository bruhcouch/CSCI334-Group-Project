# UOW Parking Lot System
The UOW Parking Lot System is a Smart Lot, equipped with parking space sensors, availability forecasting and a Traffic Prediction Model.

## Running the application

For the dashboard demo on Windows, double-click:

```
start-dashboard.bat
```

It opens Kafka, Accounts, API Gateway, Spotter, Parking, Occupancy, Admin Stats and the frontend in separate terminal windows, then opens `http://localhost:3000`.

Open up the backend directory
```
cd backend
```

Startup docker instance for kafka servers
```
docker compose up -d
docker ps
docker logs kafka
docker logs zookeeper
```

Startup at the api gateway since some microservices rely on it
```
cd api-gateway
mvn spring-boot:run
cd ..
```

Make sure to operate terminal commands inputs using Git Bash.

# Accounts Microservice

Startup accounts microservice
```
cd accounts
mvn spring-boot:run
cd ..
```

Make sure the api gateway is running since all request go through the gateway.
This was done so that cookies can automatically be set with the JWT token which is necessary for the frontend.

## Environment variables

For demo purposes, default values for environment variables have been provided.

For production, make sure to set:

JWT_SECRET
JWT_EXPIRATION

ADMIN_EMAIL
ADMIN_PASSWORD
ADMIN_USERNAME

KAFKA_SERVER

After logging into your account a cookie will be set containing your JWT token.
This is used to authorise your account and check you have the privelige to send commands.

## Commands

### User Commands

Registering a user


```
curl -i -c cookies.txt -X POST http://localhost:8081/accounts/register \
-H "Content-Type: application/json" \
-d '{
  "username": "Tom",
  "email": "tom@ross.com",
  "password": "something1"
}'
```

Logging in as a user

```
curl -i -c cookies.txt -X POST http://localhost:8081/accounts/login \
-H "Content-Type: application/json" \
-d '{
  "email": "tom@ross.com",
  "password": "something1"
}'
```

Logging in as admin

```
curl -i -c cookies.txt -X POST http://localhost:8089/api/accounts/login \
-H "Content-Type: application/json" \
-d '{
  "email": "admin@uowmail.edu.au",
  "password": "test123"
}'
```



Viewing account details (except for password)

```
curl -i -b cookies.txt -G "http://localhost:8089/api/accounts"
```

Update account details (will need to login and export token again afterwards)

```
curl -i -b cookies.txt -X PATCH "http://localhost:8089/api/accounts" \
-H "Content-Type: application/json" \
-d '{
  "username": "Michael Fazbender",
  "email": "fazbend@gmail.com",
  "password": "yodellingman2500"
}'
```

Upgrading or downgrading your subscription
```
curl -i -b cookies.txt -X PATCH "http://localhost:8089/api/accounts/subscription/upgrade"
curl -i -b cookies.txt -X PATCH "http://localhost:8089/api/accounts/subscription/downgrade"
```

### Staff Commands

This will enable you to use the following commands

Querying accounts for analytics using different syntax

```
curl -i -b cookies.txt -G "http://localhost:8089/api/staff/accounts"


curl -i -b cookies.txt -G "http://localhost:8089/api/staff/accounts" \
  -d "enabled=true" \
  -d "role=STAFF" \
  -d "startDate=2025-01-01" \
  -d "endDate=2026-12-31"

curl -i -b cookies.txt -G "http://localhost:8089/api/staff/accounts" -d "enabled=false&role=USER"
```

### Admin Commands

Registering staff and admin accounts

```
curl -i -b cookies.txt -X POST http://localhost:8089/api/admin/accounts/staff \
-H "Content-Type: application/json " \
-d '{
  "username": "Billy Jean",
  "email": "thekidisnotmyown@gmail.com",
  "password": "moviestar25"
}'

curl -i -b cookies.txt -X POST http://localhost:8089/api/admin/accounts/admin \
-H "Content-Type: application/json" \
-d '{
  "username": "Bob rossy",
  "email": "whateveweqafwar@gmail.com",
  "password": "mirfed256"
}'
```

Querying all and specific users with admin privliges

```
curl -i -b cookies.txt -G "http://localhost:8089/api/admin/accounts"
curl -i -b cookies.txt -G "http://localhost:8089/api/admin/accounts/72"
```

Enabling and disabling accounts

```
curl -i -b cookies.txt -X PATCH "http://localhost:8089/api/admin/accounts/72/enable"
curl -i -b cookies.txt -X PATCH "http://localhost:8089/api/admin/accounts/21/disable"
```

Deleting accounts

```
curl -i -b cookies.txt -X DELETE "http://localhost:8089/api/admin/accounts/89"
```

# Spotter Microservice

The Spotter service provides UOW parking space detection, 100 seeded UOW parking spaces, and a repeatable sensor simulation feed for frontend and analytics work.

It runs on port `8085` and loads its datasets from inside the service:

```
project/spotter/src/main/resources/data/uow-parking-spaces.csv
project/spotter/src/main/resources/data/uow-spotter-feed.csv
```

The service publishes JSON Kafka events when spaces are created or updated:

```
spotter.created
spotter.updated
```

For frontend-only development without Kafka running, start Spotter with `SPOTTER_KAFKA_ENABLED=false`.

## Spotter commands

Run only the Spotter service:

```
cd project
mvn -pl spotter spring-boot:run
```

Use these endpoints from the frontend:

```
curl http://localhost:8085/api/spotter/health
curl http://localhost:8085/api/spotter/spaces
curl http://localhost:8085/api/spotter/lots
curl http://localhost:8085/api/spotter/zones
curl http://localhost:8085/api/spotter/summary
curl http://localhost:8085/api/spotter/events
```

Filter spaces by lot, zone, occupancy, or disability permit requirement:

```
curl "http://localhost:8085/api/spotter/spaces?lotName=P1&zone=General&occupied=false"
```

Advance the simulation by one sensor event:

```
curl -X POST http://localhost:8085/api/spotter/simulation/next
```

Run several simulation events at once:

```
curl -X POST http://localhost:8085/api/spotter/simulation/run \
-H "Content-Type: application/json" \
-d '{
  "eventCount": 5,
  "publishEvents": true
}'
```

Record a manual sensor reading:

```
curl -X POST http://localhost:8085/api/spotter/sensors/UOW-P1-GEN-004/detect \
-H "Content-Type: application/json" \
-d '{
  "occupied": true,
  "confidence": 0.98,
  "source": "frontend-demo"
}'
```

Reset the in-memory database back to the CSV dataset and restart the simulation feed:

```
curl -X POST http://localhost:8085/api/spotter/simulation/reset
```

# Parking Bookings

The Parking service runs on port `8082`. Bookings reserve real Spotter sensor IDs, prevent overlapping active/reserved bookings for the same space, and automatically expire old bookings.

Create a booking through the API gateway:

```
curl -X POST http://localhost:8089/api/parking \
-H "Content-Type: application/json" \
-d '{
  "accountId": 1,
  "parkingLot": "P1",
  "parkingSpace": "UOW-P1-GEN-004",
  "vehicle": "ABC123",
  "startTime": "2026-05-27T09:00",
  "endTime": "2026-05-27T11:00"
}'
```

Cancel a booking:

```
curl -X PATCH http://localhost:8089/api/parking/1/cancel
```

# Occupancy Predictions

The Occupancy service runs on port `8083` and exposes current occupancy, historical occupancy, and prediction endpoints.

```
curl http://localhost:8089/api/occupancy/predictions
curl http://localhost:8089/api/occupancy/P1/predict
```
  
