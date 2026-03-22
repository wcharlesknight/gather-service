# Gather Service

A Spring Boot backend for the Gatherus/LoopIn mobile app. Each week the service randomly selects a local gathering spot and notifies all members via push notification. It also handles user authentication and profile management so the mobile client does not talk to Firebase directly.

## Purpose

Bring people together by removing the "where should we meet?" decision. Every week the service picks one bar or community spot, and that's where the group meets — simple, random, and fair.

## Features

- User registration and login via Firebase Auth (Admin SDK)
- User profile and location management backed by Firestore
- Weekly scheduled job that randomly selects a gathering spot via Google Places
- Push notifications via Firebase Cloud Messaging
- City management API for configuring per-city search parameters
- Gathering spot history with repeat-avoidance (configurable window)
- Health check endpoint

## Prerequisites

- Java 21
- Gradle
- Google Places API key
- Firebase project with:
  - Authentication enabled (Email/Password provider)
  - Cloud Firestore enabled
  - Cloud Messaging (FCM) enabled
  - Service account credentials JSON file

## Setup

1. Clone the repository
2. Get a Google Places API key from the Google Cloud Console
3. Set up Firebase:
   - Create a Firebase project at https://console.firebase.google.com
   - Enable Authentication → Email/Password
   - Enable Cloud Firestore (production mode)
   - Enable Cloud Messaging
   - Download the service account JSON (Project Settings → Service Accounts)
   - Save it as `src/main/resources/firebase-service-account.json`
4. Copy `.env.example` to `.env` and configure:
   ```bash
   export GOOGLE_PLACES_API_KEY=your-api-key-here
   export FIREBASE_CREDENTIALS_PATH=classpath:firebase-service-account.json
   export FIREBASE_ENABLED=true
   ```

## Configuration

Edit `src/main/resources/application.yml` to configure:
- Server port (default: 8080)
- Google Places search parameters
- Job schedule (default: Thursday at 9:00 AM)
- Default search location and term
- Firebase notification topic
- Active place search provider

## Running the Application

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

## API Endpoints

### Health Check
```
GET /api/health
```

### Authentication
```
POST /api/auth/register    # Create account, returns Firebase custom token
POST /api/auth/login       # Sync last login timestamp (Bearer token required)
```

### User Profile
```
PUT  /api/users/location        # Save user's city selection (Bearer token required)
POST /api/users/ensure-profile  # Backfill missing profile fields (Bearer token required)
```

### City Management
```
GET    /api/cities          # Get all enabled cities (job configs)
GET    /api/cities/{id}     # Get city by ID
POST   /api/cities          # Create city job config
PUT    /api/cities/{id}     # Update city job config
DELETE /api/cities/{id}     # Delete city job config
```

### Gathering Spot History
```
GET /api/gathering-spots/city/{cityId}                         # All spots for city
GET /api/gathering-spots/city/{cityId}/recent?limit=10         # Recent spots
GET /api/gathering-spots/city/{cityId}/recent-ids?weeks=12     # Recent place IDs
```

## Scheduled Job

`GatheringSpotSyncJob` runs weekly (default: **Thursday at 9:00 AM**) to:

1. Load enabled cities from Firestore (falls back to default config if none found)
2. Search Google Places for bars/gathering spots in each city
3. Filter out spots selected in the last 12 weeks
4. Randomly select one spot and save it to Firestore
5. Send a push notification to the city's FCM topic

**Timeline**: Notification Thursday → Gather Friday

Configure in `application.yml`:
```yaml
place-service:
  active-provider: google
  job:
    enabled: true
    cron: "0 0 9 * * THU"
    default-location: "Seattle, WA"
    default-term: "bars"
    search-limit: 20
    avoid-repeat-weeks: 12

firebase:
  notification-topic: weekly-gather
```

## Project Structure

```
src/main/java/com/gather/
├── Application.java
├── config/
│   ├── FirebaseConfig.java          # FirebaseApp, Firestore, FirebaseAuth beans
│   ├── GooglePlacesApiConfig.java   # Google Places API key and field mask
│   ├── PlaceSearchServiceConfig.java # Selects active provider bean
│   └── WebClientConfig.java
├── controller/
│   ├── AuthController.java          # /api/auth/*
│   ├── CityController.java          # /api/cities/*
│   ├── GatheringSpotController.java # /api/gathering-spots/*
│   ├── HealthController.java
│   └── UserController.java          # /api/users/*
├── exception/
│   ├── InvalidTokenException.java
│   └── UnknownCityException.java
├── job/
│   └── GatheringSpotSyncJob.java    # Weekly selection + notification
├── model/
│   ├── domain/
│   │   ├── CityJobConfig.java       # Per-city job configuration (Firestore)
│   │   ├── GatheringSpot.java       # Selected spot record (Firestore)
│   │   └── Place.java               # Generic provider-agnostic place
│   ├── dto/
│   │   ├── request/
│   │   │   ├── RegisterRequest.java
│   │   │   └── UpdateLocationRequest.java
│   │   └── response/
│   │       ├── AuthResponse.java
│   │       └── LocationResponse.java
│   └── provider/
│       └── google/
│           ├── GooglePlace.java
│           ├── GooglePlacesSearchRequest.java
│           └── GooglePlacesSearchResponse.java
├── repository/
│   ├── CityRepository.java
│   └── GatheringSpotRepository.java
└── service/
    ├── AuthService.java
    ├── CityRegistry.java            # Authoritative city list for user location
    ├── CityService.java
    ├── GatheringSpotService.java
    ├── GooglePlaceSearchService.java
    ├── GooglePlacesApiService.java
    ├── PlaceSearchService.java      # Provider interface
    ├── PushNotificationService.java
    └── UserService.java
```

## How It Works

1. **User signs up** → mobile sends credentials to `POST /api/auth/register` → backend creates Firebase Auth user + Firestore profile → returns a custom token → mobile signs in with that token
2. **User selects city** → mobile sends `PUT /api/users/location` with `cityId` → backend validates against `CityRegistry` and writes authoritative location data to Firestore
3. **Every Thursday at 9 AM** → `GatheringSpotSyncJob` queries Google Places, filters recent repeats, picks a random spot, saves it, and fires an FCM push notification
4. **Mobile app** subscribes to the city's FCM topic to receive the weekly notification

## Roadmap

**Phase 1 (Current)**:
- ✅ User auth and profile management via backend
- ✅ Seattle and Tacoma with random spot selection
- ✅ Firebase Firestore for data persistence
- ✅ History tracking to avoid repeats (12 weeks)
- ✅ REST API for city and spot management
- ✅ Thursday notifications for Friday gatherings
- ⏳ Mobile app (LoopIn) full integration

**Phase 2**:
- Multi-city support (Portland, San Francisco, etc.)
- City-specific FCM topics
- Show last 4 meetups in app

**Phase 3**:
- RSVP functionality
- In-app chat for each gathering
- Community features, feedback, ratings

**Phase 4**:
- Private groups
- Smart selection algorithms
- Analytics dashboard
