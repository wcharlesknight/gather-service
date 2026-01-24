# Gather Service - Implementation Plan

## Overview
The Gather Service is a Spring Boot application that helps communities discover weekly gathering spots. It integrates with the Yelp API to find bars and community spaces, randomly selects one location each week, and sends push notifications to all members. The service eliminates the "where should we meet?" question by making the decision automatically and fairly.

## Mission
Bring people together by choosing a random bar or community gathering spot each week. No planning, no debates - just show up at the announced location and connect with your community.

**Weekly Schedule**:
- **Thursday 9:00 AM**: Notification sent with gathering spot
- **Friday evening**: Community gathers at the selected spot

**Mobile App**: LoopIn (React Native) - Located at `/Users/charlieknight/Projects/LoopIn`

## Core Features

### 1. Weekly Gathering Spot Selection
- **Schedule**: Runs every **Thursday at 9:00 AM** (configurable via cron expression)
- **Timeline**: Notification Thursday → Gathering Friday
- **Current Scope**: Seattle, WA area
- **Future Scope**: Expand to Portland, San Francisco, and other cities
- **Process**:
  1. Fetch up to 50 bars/gathering spots from Yelp API based on configured location
  2. Check Firestore history to avoid spots selected in last 12 weeks
  3. Randomly select one spot from available results
  4. Save selection to Firestore
  5. Send push notification to all subscribed users with Friday's gathering location

### 2. Database & Storage
- **Technology**: Firebase Firestore (NoSQL document database)
- **Collections**:
  - `cities` - City configurations and metadata
  - `gatheringSpots` - Historical record of selected spots per city
  - `userSubscriptions` - (Future) User preferences and subscriptions
- **Features**: Real-time sync, offline support, automatic scaling

### 3. Push Notifications
- **Technology**: Firebase Cloud Messaging (FCM)
- **Content**: Spot name, address, rating, and link to Yelp page
- **Delivery**: Sent to topic-based subscribers (default topic: "weekly-gather")
- **Platform Support**: iOS and Android with platform-specific configurations
- **Message**: "📍 This Week's Gather Spot! Meet Friday at [Spot Name] - [Address]"
- **Timing**: Thursday for Friday gathering

### 4. Multi-City Support (Planned)
- Cities stored in Firestore with individual configurations
- City-specific FCM topics (e.g., "gather-seattle", "gather-portland")
- Users subscribe to their city's topic
- Each city gets its own weekly gathering spot
- Configurable schedules per city

### 5. REST API Endpoints
- Health check endpoint for service monitoring
- Manual Yelp search endpoints for testing and on-demand queries
- Business details lookup by Yelp ID
- City management (CRUD operations)
- Gathering spot history queries

---

## Architecture Components

### Services Layer

#### YelpApiService
**Purpose**: Integration with Yelp Fusion API

**Methods**:
- `searchBusinesses(location, term, limit)`: Fetch businesses with structured response
- `searchBusinessesRaw(location, term)`: Fetch businesses as raw JSON (for API endpoints)
- `getBusinessDetails(businessId)`: Get detailed info for specific business

**Key Features**:
- Uses Spring WebClient for reactive HTTP calls
- Automatic API key injection via headers
- Error handling and logging

#### PushNotificationService
**Purpose**: Send push notifications via Firebase Cloud Messaging

**Methods**:
- `sendGatheringSpotNotification(business, topic)`: Send to topic subscribers
- `sendGatheringSpotNotificationToDevices(business, tokens)`: Send to specific devices

**Notification Content**:
- Title: "📍 This Week's Gather Spot!"
- Body: Spot name and address
- Data payload: Business ID, name, rating, address, URL, coordinates
- Platform-specific configurations (iOS sound, Android channel)

### Job Layer

#### YelpDataSyncJob
**Purpose**: Scheduled weekly task for gathering spot selection

**Configuration**:
- Cron schedule (default: `0 0 9 * * MON`)
- Default location (default: Seattle, WA) - used if no cities in Firestore
- Search term (default: bars)
- Number of results to fetch (default: 50)
- Weeks to avoid repeats (default: 12)
- FCM topic (default: weekly-gather)

**Workflow**:
1. Check if job is enabled
2. Query Firestore for enabled cities
3. For each city:
   - Fetch recent selections from Firestore (last 12 weeks)
   - Call Yelp API with city's configured parameters
   - Filter out recently selected spots
   - Randomly select one spot from available options
   - Save selection to Firestore
   - Send push notification to city-specific topic
   - Mark notification as sent in Firestore
4. If no cities in Firestore, use default configuration (Seattle)

**Multi-City Support**:
- Already implemented via Firestore city collection
- Each city processed independently
- City-specific notifications sent to city topics
- Full history tracking per city

### Configuration Layer

#### YelpApiConfig
- API key management
- Base URL configuration
- Loaded from environment variables

#### FirebaseConfig
- Initialize Firebase Admin SDK
- Initialize Firestore database client
- Load service account credentials from file
- Handle initialization errors gracefully
- Can be disabled via configuration

### Model Layer

#### YelpBusiness
**Properties**:
- id, name, url, phone
- rating, reviewCount, price
- location (address, city, state, coordinates)
- Formatted address helper method

#### YelpSearchResponse
**Properties**:
- List of businesses
- Total count of results

#### City (Firestore Entity)
**Properties**:
- id, name, location, topic
- cronSchedule, searchTerm, searchLimit
- enabled, createdAt

#### GatheringSpot (Firestore Entity)
**Properties**:
- id, cityId, yelpBusinessId
- businessName, address, rating
- selectedAt, notificationSent, notificationSentAt
- phoneNumber, yelpUrl

### Repository Layer

#### CityRepository
**Methods**:
- `findAllEnabled()` - Get all active cities
- `findById(id)` - Get specific city
- `findByName(name)` - Get city by name
- `save(city)` - Create or update city
- `delete(id)` - Remove city

#### GatheringSpotRepository
**Methods**:
- `save(spot)` - Save new gathering spot selection
- `findRecentByCityId(cityId, limit)` - Get recent spots
- `findRecentYelpIds(cityId, weeksBack)` - Get IDs to avoid repeats
- `findAllByCityId(cityId)` - Get full history for city
- `markNotificationSent(spotId)` - Update notification status

### Controller Layer

#### HealthController
- `GET /api/health`: Service health check

#### YelpController
- `GET /api/yelp/search?location={location}&term={term}`: Search businesses
- `GET /api/yelp/business/{id}`: Get business details

#### CityController
- `GET /api/cities`: Get all enabled cities
- `GET /api/cities/{id}`: Get specific city
- `POST /api/cities`: Create new city
- `PUT /api/cities/{id}`: Update city
- `DELETE /api/cities/{id}`: Delete city

#### GatheringSpotController
- `GET /api/gathering-spots/city/{cityId}`: Get all spots for city
- `GET /api/gathering-spots/city/{cityId}/recent`: Get recent spots
- `GET /api/gathering-spots/city/{cityId}/recent-ids`: Get recent Yelp IDs (debugging)

---

## Configuration

### application.yml
```yaml
server:
  port: 8080

yelp:
  api:
    api-key: ${YELP_API_KEY}
    base-url: https://api.yelp.com/v3
  job:
    enabled: true
    cron: "0 0 9 * * MON"  # Every Monday at 9 AM
    default-location: "Seattle, WA"
    default-term: "bars"
    search-limit: 50

firebase:
  credentials-path: ${FIREBASE_CREDENTIALS_PATH}
  enabled: true
  notification-topic: weekly-gather
```

### Future Multi-City Configuration
```yaml
yelp:
  job:
    cities:
      - name: Seattle
        location: "Seattle, WA"
        topic: gather-seattle
        cron: "0 0 9 * * MON"
      - name: Portland
        location: "Portland, OR"
        topic: gather-portland
        cron: "0 0 9 * * MON"
      - name: San Francisco
        location: "San Francisco, CA"
        topic: gather-sf
        cron: "0 0 9 * * MON"
```

### Environment Variables
- `YELP_API_KEY`: Your Yelp Fusion API key
- `FIREBASE_CREDENTIALS_PATH`: Path to Firebase service account JSON file
- `FIREBASE_ENABLED`: Toggle Firebase functionality

---

## Setup Requirements

### 1. Yelp API Access
1. Create a Yelp Fusion API account at https://www.yelp.com/developers
2. Create an app to get your API key
3. Set the `YELP_API_KEY` environment variable

### 2. Firebase Setup
1. Create a Firebase project at https://console.firebase.google.com
2. **Enable Firestore Database**:
   - Go to Firestore Database in Firebase Console
   - Click "Create Database"
   - Start in production mode (or test mode for development)
   - Choose a location (preferably close to your users)
3. **Enable Cloud Messaging**:
   - Go to Cloud Messaging in Firebase Console
   - Add Android and/or iOS apps to the project
4. **Download Service Account Credentials**:
   - Go to Project Settings → Service Accounts
   - Click "Generate New Private Key"
   - Save as `firebase-service-account.json` in `src/main/resources/`
5. Set up FCM topics in your mobile app
6. Subscribe users to the "weekly-gather" topic (or city-specific topics)

### 3. Mobile App Integration (Client Side)
**Not included in this service - requires separate mobile app development**

#### Mobile App: LoopIn (React Native)
**Repository**: `/Users/charlieknight/Projects/LoopIn`

**Core Features**:
- User sign-up with Firebase Auth
- City selection on onboarding
- Subscribe to city-specific FCM topic (e.g., "gather-seattle")
- Display current week's gathering spot
- Show last 4 meetup locations
- Map view with location pin
- "Add to Calendar" button (Friday evening event)
- "Get Directions" button

**iOS Requirements**:
- Configure APNs (Apple Push Notification service)
- Add Firebase SDK
- Subscribe to FCM topic: "gather-seattle"
- Handle notification payload
- React Native Maps integration

**Android Requirements**:
- Add Firebase SDK
- Create notification channel: "weekly_gather"
- Subscribe to FCM topic: "gather-seattle"
- Handle notification payload
- React Native Maps integration

**API Integration**:
- `GET /api/cities` - List available cities
- `GET /api/gathering-spots/city/{cityId}/recent?limit=4` - Show last 4 meetups
- Handle notification data payload for spot details

**City Selection**:
- User selects city on first launch
- App subscribes to city topic (e.g., "gather-seattle")
- User can change cities in settings
- Unsubscribe from old city, subscribe to new city

---

## Deployment Checklist

### Pre-Deployment
- [ ] Obtain Yelp API key
- [ ] Create Firebase project and download credentials
- [ ] Enable Firestore Database in Firebase Console
- [ ] Set Firestore security rules (see below)
- [ ] Add initial city to Firestore (Seattle)
- [ ] Configure Firebase in mobile apps
- [ ] Test notification delivery to test devices
- [ ] Set appropriate cron schedule for target timezone
- [ ] Configure desired location and search parameters

### Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Cities - read by anyone, write by admin only
    match /cities/{cityId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.token.admin == true;
    }

    // Gathering spots - read by anyone, write by service only
    match /gatheringSpots/{spotId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.token.admin == true;
    }

    // User subscriptions (future)
    match /userSubscriptions/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### Environment Configuration
- [ ] Set `YELP_API_KEY` environment variable
- [ ] Place `firebase-service-account.json` in correct location
- [ ] Set `FIREBASE_CREDENTIALS_PATH` if using custom location
- [ ] Configure logging levels for production

### Post-Deployment
- [ ] Verify health endpoint: `GET /api/health`
- [ ] Test manual search endpoint
- [ ] Monitor logs for scheduled job execution
- [ ] Verify push notifications are delivered
- [ ] Set up monitoring/alerting for job failures

---

## Future Enhancements

### Phase 2 - Multi-City Expansion
1. **City Management** ✅ (Already Implemented)
   - Add cities via Firestore or REST API
   - City-specific FCM topics
   - Configurable search parameters per city
   - City-specific schedules (timezone-aware) - TODO

2. **Database Integration** ✅ (Already Implemented)
   - Firestore stores gathering spot history per city
   - Track which spots have been selected
   - Avoid repeating locations for 12 weeks (configurable)
   - Analytics on popular spots via API

3. **Location History Tracking** ✅ (Already Implemented)
   - Don't repeat spots from last 12 weeks
   - Query Firestore before selection
   - Manual override capability for special occasions - TODO

4. **Next Steps for Phase 2**
   - Add Portland and San Francisco via API
   - Implement per-city cron scheduling
   - Create admin UI for city management
   - Add timezone-aware scheduling

### Phase 3 - Community Features
1. **User Management** ✅ (Partially - Firebase Auth)
   - ✅ User accounts via Firebase Auth
   - User profiles in Firestore
   - Join specific city communities
   - RSVP to weekly gatherings
   - See who's planning to attend

2. **In-App Chat** 🎯 (Priority Feature)
   - Real-time chat for each gathering
   - Firestore real-time listeners
   - Message history per gathering spot
   - Push notifications for new messages

3. **Feedback System**
   - Rate gathering spots after visit
   - Report closed or inappropriate venues
   - Suggest new spots to add to rotation
   - Community voting on spot categories

4. **User API Endpoints**
   - Subscribe/unsubscribe to city topics
   - View gathering spot history (✅ implemented)
   - RSVP to upcoming gatherings
   - Provide feedback on spots
   - Check-in at location

### Phase 4 - Advanced Features
1. **Spot Categories**
   - Different categories: Sports Bars, Dive Bars, Lounges, Coffee Shops
   - Category rotation (Sports Bar week, Dive Bar week, etc.)
   - User preferences for category frequency
   - Special themed weeks

2. **Smart Selection Algorithm**
   - Prefer higher-rated spots (weighted random)
   - Consider accessibility (public transit access)
   - Seasonal adjustments (outdoor patios in summer)
   - Balance between variety and quality

3. **Social Features**
   - In-app chat for each gathering
   - Photo sharing from gatherings
   - Community leaderboard (most active attendees)
   - Integration with social media for event sharing

### Phase 5 - Enterprise & Community Groups
1. **Private Groups**
   - Companies can create private gathering groups
   - Friend groups can have their own rotation
   - Custom spot lists per group
   - Custom schedules and notification preferences

2. **Analytics Dashboard**
   - Track attendance patterns
   - Monitor popular vs unpopular spots
   - User engagement metrics
   - City-by-city growth tracking

3. **Monetization Options**
   - Partner with venues for featured spots
   - Premium features for groups
   - Analytics for venues (how many users visited)
   - Sponsored gathering weeks

---

## Testing Strategy

### Unit Tests
- [ ] Test YelpApiService with mocked WebClient
- [ ] Test PushNotificationService with mocked Firebase
- [ ] Test random selection logic in YelpDataSyncJob
- [ ] Test model serialization/deserialization

### Integration Tests
- [ ] Test Yelp API integration with test API key
- [ ] Test scheduled job execution (with test cron)
- [ ] Test end-to-end notification flow with test topic

### Manual Testing
- [ ] Verify cron schedule triggers correctly
- [ ] Test notification delivery on real devices
- [ ] Validate restaurant data formatting
- [ ] Test error handling with invalid API keys

---

## Error Handling & Monitoring

### Logging
- INFO: Job execution, successful API calls, notifications sent
- WARN: No businesses found, Firebase disabled, empty responses
- ERROR: API failures, notification failures, configuration errors

### Failure Scenarios
1. **Yelp API Failure**
   - Log error with details
   - Do not send notification
   - Retry on next scheduled run
   - Alert administrator if consecutive failures

2. **Firebase Failure**
   - Log error with details
   - Gathering spot still selected successfully
   - Alert administrator immediately
   - Consider backup notification method

3. **No Results from Yelp**
   - Log warning
   - Do not send notification
   - Expand search parameters (increase radius, different categories)
   - Alert administrator to review search configuration

4. **Repeated Location Selection**
   - (Future) Check database history before selecting
   - If selected spot was recent, pick another
   - Track last N selections to ensure variety

### Monitoring Recommendations
- Set up alerts for consecutive job failures
- Monitor Yelp API rate limits
- Track notification delivery success rate
- Monitor Firebase quota usage

---

## Security Considerations

1. **API Keys**
   - Never commit API keys to version control
   - Use environment variables or secret management
   - Rotate keys periodically

2. **Firebase Credentials**
   - Secure service account JSON file
   - Restrict file permissions
   - Do not expose in API responses

3. **Rate Limiting**
   - Yelp API has rate limits (verify your tier)
   - Implement backoff strategy for failures
   - Cache results if needed

4. **Input Validation**
   - Validate location and term parameters in API endpoints
   - Sanitize user inputs
   - Prevent injection attacks

---

## Cost Considerations

### Yelp API
- Free tier: 5000 API calls per day
- Current usage: ~1 call per week (very low)
- Plenty of headroom for additional features

### Firebase
- **Cloud Messaging**: Free tier with unlimited notifications
- **Firestore**:
  - Free tier: 50k reads, 20k writes, 20k deletes per day
  - 1 GB storage free
  - Current usage: ~10 writes/week (well within limits)
  - Projected usage (10 cities): ~100 writes/week + reads
- **Total cost**: $0 on free tier for foreseeable future

### Infrastructure
- Spring Boot application: Small memory footprint
- Can run on minimal VPS or cloud instance
- Consider serverless options for cost optimization

---

## Maintenance & Operations

### Regular Tasks
- [ ] Monitor Yelp API status and updates
- [ ] Check Firebase SDK updates
- [ ] Review logs for errors or warnings
- [ ] Validate notification delivery metrics

### Quarterly Review
- [ ] Assess user engagement with gatherings
- [ ] Review gathering spot selection - any patterns to avoid?
- [ ] Survey community on spot preferences
- [ ] Plan expansion to new cities
- [ ] Evaluate new Yelp API features
- [ ] Review spot history - ensure good variety

### Disaster Recovery
- Document configuration settings
- Backup Firebase credentials securely
- Have rollback plan for deployments
- Test recovery procedures

---

## Development Workflow

### Local Development
1. Clone repository
2. Set environment variables in `.env` file
3. Place Firebase credentials in resources
4. Run with `./gradlew bootRun`
5. Test endpoints with curl/Postman

### Testing the Job Locally
- Temporarily change cron to run more frequently
- Or manually trigger the job method in a test
- Use test Firebase topic to avoid spamming users

### Deployment
1. Build: `./gradlew clean build`
2. Run tests: `./gradlew test`
3. Create deployment artifact
4. Deploy to server/cloud platform
5. Set environment variables in deployment environment
6. Verify health endpoint
7. Monitor first scheduled execution

---

## API Documentation

### Endpoints

#### Health Check
```
GET /api/health
Response: { "status": "UP", "service": "gather-service" }
```

#### Search Businesses
```
GET /api/yelp/search?location=San%20Francisco&term=restaurants
Response: JSON array of businesses from Yelp
```

#### Get Business Details
```
GET /api/yelp/business/{business_id}
Response: JSON object with business details
```

---

## Multi-City Expansion Strategy

### Phase 1: Seattle (Current)
- Launch with Seattle as pilot city
- Gather user feedback
- Refine selection algorithm
- Build community

### Phase 2: Pacific Northwest
- Add Portland, OR
- Add Vancouver, BC (if enough interest)
- Use learnings from Seattle
- City-specific topics and schedules

### Phase 3: West Coast
- San Francisco, CA
- Los Angeles, CA
- San Diego, CA

### Phase 4: Major US Cities
- New York, NY
- Chicago, IL
- Austin, TX
- Denver, CO
- Boston, MA

### Expansion Criteria
- Minimum 50 bars/gathering spots in Yelp results
- Active Yelp user base in the city
- Community interest (pre-registration)
- Local community champion/moderator

## Conclusion

This implementation provides a solid foundation for a community gathering service. The architecture is designed to scale from a single city (Seattle) to multiple cities nationwide.

The modular design separates concerns clearly:
- External API integration (Yelp)
- Push notification delivery (Firebase)
- Scheduled job execution (Spring Scheduler)
- REST API for manual operations and testing

**Core Philosophy**: Remove friction from community gatherings. No planning, no debates - just a random spot each week where everyone shows up.

**Next Steps**:
1. Launch in Seattle with mobile app
2. Build core community (100+ active users)
3. Implement history tracking to avoid repeats
4. Gather feedback and iterate
5. Expand to Portland and San Francisco
6. Continue geographic expansion based on demand

The service prioritizes simplicity and fairness - everyone gets the same random selection, no preferences, no biases. The gathering spot is chosen, and the community decides to show up.
