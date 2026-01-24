# Gather Service

A Spring Boot service that helps communities discover and gather at local bars and community spots. Each week, the service randomly selects a gathering location from Yelp and notifies all members via push notifications.

## Purpose

Bring people together by removing the "where should we meet?" decision. Every week, the service picks one bar or community gathering spot, and that's where the group meets. Simple, random, and fair.

## Features

- Weekly scheduled job that randomly selects a gathering spot (bar, pub, community space)
- Push notifications via Firebase Cloud Messaging to announce the week's location
- Starts with Seattle area, designed to expand to multiple cities
- RESTful API endpoints for Yelp business search and testing
- Health check endpoint
- Configurable scheduling and search parameters

## Prerequisites

- Java 17 or higher
- Gradle
- Yelp API Key (get one at https://www.yelp.com/developers)
- Firebase project with:
  - Cloud Messaging (FCM) enabled
  - Firestore Database enabled
  - Service account credentials JSON file

## Setup

1. Clone the repository
2. Get your Yelp API key from https://www.yelp.com/developers
3. Set up Firebase:
   - Create a Firebase project at https://console.firebase.google.com
   - Enable Cloud Firestore (Create Database → Start in production mode)
   - Enable Cloud Messaging
   - Download the service account JSON file (Project Settings → Service Accounts)
   - Save it as `src/main/resources/firebase-service-account.json`
4. Copy `.env.example` to `.env` and configure:
   ```bash
   export YELP_API_KEY=your-api-key-here
   export FIREBASE_CREDENTIALS_PATH=classpath:firebase-service-account.json
   export FIREBASE_ENABLED=true
   ```

## Configuration

Edit `src/main/resources/application.yml` to configure:
- Server port (default: 8080)
- Yelp job schedule (default: Monday at 9:00 AM)
- Default search location (default: Seattle, WA)
- Search term (default: bars)
- Number of spots to fetch (default: 50)
- Firebase notification topic
- Logging levels

## Running the Application

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

Or with environment variables:
```bash
YELP_API_KEY=your-key ./gradlew bootRun
```

## API Endpoints

### Health Check
```
GET /api/health
```

### Yelp API Endpoints
```
GET /api/yelp/search?location=Seattle&term=bars
GET /api/yelp/business/{business_id}
```

### City Management
```
GET    /api/cities              # Get all enabled cities
GET    /api/cities/{id}         # Get city by ID
POST   /api/cities              # Create new city
PUT    /api/cities/{id}         # Update city
DELETE /api/cities/{id}         # Delete city
```

### Gathering Spot History
```
GET /api/gathering-spots/city/{cityId}                    # All spots for city
GET /api/gathering-spots/city/{cityId}/recent?limit=10    # Recent spots
GET /api/gathering-spots/city/{cityId}/recent-ids?weeks=12 # Recent Yelp IDs
```

## Scheduled Job

The `YelpDataSyncJob` runs weekly (default: **Thursday at 9:00 AM**) to:
1. Fetch bars and gathering spots from Yelp API (currently Seattle area)
2. Randomly select one spot from the results
3. Send push notification to all subscribed users with Friday's gathering location

**Timeline**: Notification Thursday → Gather Friday

Configure in `application.yml`:
```yaml
yelp:
  job:
    enabled: true
    cron: "0 0 9 * * THU"  # Every Thursday at 9 AM (for Friday gathering)
    default-location: "Seattle, WA"
    default-term: "bars"
    search-limit: 50

firebase:
  notification-topic: weekly-gather
```

## Project Structure

```
src/main/java/com/gather/
├── Application.java                      # Main Spring Boot application
├── config/
│   ├── YelpApiConfig.java               # Yelp API configuration
│   ├── WebClientConfig.java             # WebClient bean configuration
│   └── FirebaseConfig.java              # Firebase & Firestore initialization
├── controller/
│   ├── HealthController.java            # Health check endpoint
│   ├── YelpController.java              # Yelp API endpoints
│   ├── CityController.java              # City management endpoints
│   └── GatheringSpotController.java     # Gathering spot history endpoints
├── service/
│   ├── YelpApiService.java              # Yelp API integration service
│   └── PushNotificationService.java     # Firebase push notifications
├── repository/
│   ├── CityRepository.java              # Firestore city data access
│   └── GatheringSpotRepository.java     # Firestore gathering spot data access
├── model/
│   ├── City.java                        # City entity (Firestore)
│   ├── GatheringSpot.java               # Gathering spot entity (Firestore)
│   ├── YelpBusiness.java                # Yelp business model
│   └── YelpSearchResponse.java          # Yelp API response model
└── job/
    └── YelpDataSyncJob.java             # Weekly gathering spot selection job
```

## How It Works

1. Every **Thursday at 9:00 AM**, the service queries Yelp for bars in Seattle
2. It checks Firestore to avoid spots selected in the last 12 weeks
3. It randomly selects one spot from available results
4. The selection is saved to Firestore for history tracking
5. A push notification goes out to all subscribed users with the location
6. Everyone meets on **Friday** at the announced spot - no planning needed!

## Frontend Integration

The mobile app (LoopIn) displays:
- This week's gathering spot
- Last 4 meetup locations (via `GET /api/gathering-spots/city/{cityId}/recent?limit=4`)
- User sign-up and city selection

## Documentation

See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) for detailed architecture, multi-city expansion plans, and future enhancements.

## Roadmap

**Phase 1 (Current)**:
- ✅ Seattle area with random selection
- ✅ Firebase Firestore for data persistence
- ✅ History tracking to avoid repeats (12 weeks)
- ✅ REST API for city and spot management
- ✅ Thursday notifications for Friday gatherings
- ⏳ Mobile app (LoopIn) integration

**Phase 2**:
- Multi-city support (Portland, San Francisco, etc.)
- City-specific FCM topics
- Mobile app with city selection
- Show last 4 meetups in app

**Phase 3**:
- User accounts and profiles
- RSVP functionality
- In-app chat for each gathering
- Community features, feedback, ratings

**Phase 4**:
- Private groups
- Smart selection algorithms
- Analytics dashboard
- Cross-city gatherings

## Next Steps

- Set up mobile app to subscribe to Firebase topic "weekly-gather"
- Add cities to Firestore via API (Portland, San Francisco)
- Implement multi-city scheduling
- Add user authentication with Firebase Auth
- Create admin dashboard for monitoring and manual overrides
