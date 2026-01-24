# Changelog

## Schedule Update - Thursday Notifications (Latest Update)

### Summary
Updated the scheduled job to run on **Thursdays at 9:00 AM** (instead of Mondays) to align with the actual app concept: notification sent Thursday, community gathers Friday.

### Changes

#### Scheduling
- **YelpDataSyncJob.java**: Changed cron from `MON` to `THU`
- **application.yml**: Updated default cron to `0 0 9 * * THU`
- **Documentation**: Updated all references from Monday to Thursday

#### Notification Message
- **PushNotificationService.java**: Updated notification body to include "Meet Friday at [Spot Name]"
- Clarifies that gathering happens Friday, not same day as notification

#### Documentation Updates
- **README.md**:
  - Clarified Thursday notification → Friday gathering timeline
  - Added Frontend Integration section
  - Added note about last 4 meetups display
- **IMPLEMENTATION_PLAN.md**:
  - Added weekly schedule timeline
  - Added frontend repository location
  - Updated mobile app integration details
  - Emphasized in-app chat as priority feature
- **FRONTEND_INTEGRATION.md** (NEW): Complete guide for mobile app integration
  - User flow from signup to gathering
  - API endpoints for mobile app
  - FCM notification handling
  - Mobile app screens design
  - Future features (RSVP, chat, check-in)

### Weekly Timeline
```
Thursday 9:00 AM  →  Backend selects spot & sends notification
Thursday-Friday   →  Users see notification and plan to attend
Friday evening    →  Community gathers at selected spot
```

### Frontend Integration
- Mobile app repository: `/Users/charlieknight/Projects/LoopIn`
- Display last 4 meetups via API
- Firebase Auth for user management
- React Native with Firebase SDK

---

## Firebase Firestore Integration (Previous Update)

### Summary
Integrated Firebase Firestore as the primary database for all data persistence, replacing the initially planned PostgreSQL. The service is now fully multi-city ready with history tracking and REST APIs for management.

### New Features

#### Database Integration
- ✅ **Firebase Firestore** integrated as primary database
- ✅ **City Management** - Store and manage multiple cities in Firestore
- ✅ **Gathering Spot History** - Track all selected spots per city
- ✅ **Avoid Repeats** - Automatically filter out spots selected in last 12 weeks
- ✅ **REST APIs** - Full CRUD operations for cities and spot history queries

#### New Models
- `City.java` - Firestore entity for city configuration
- `GatheringSpot.java` - Firestore entity for historical selections

#### New Repositories
- `CityRepository.java` - Firestore data access for cities
  - `findAllEnabled()` - Get all active cities
  - `findById(id)` - Get specific city
  - `findByName(name)` - Get city by name
  - `save(city)` - Create/update city
  - `delete(id)` - Remove city

- `GatheringSpotRepository.java` - Firestore data access for gathering spots
  - `save(spot)` - Save new selection
  - `findRecentByCityId(cityId, limit)` - Get recent spots
  - `findRecentYelpIds(cityId, weeksBack)` - Get IDs to avoid repeats
  - `findAllByCityId(cityId)` - Get full history
  - `markNotificationSent(spotId)` - Update notification status

#### New Controllers
- `CityController.java` - REST API for city management
  - `GET /api/cities` - List all enabled cities
  - `GET /api/cities/{id}` - Get specific city
  - `POST /api/cities` - Create new city
  - `PUT /api/cities/{id}` - Update city
  - `DELETE /api/cities/{id}` - Delete city

- `GatheringSpotController.java` - REST API for spot history
  - `GET /api/gathering-spots/city/{cityId}` - All spots for city
  - `GET /api/gathering-spots/city/{cityId}/recent?limit=10` - Recent spots
  - `GET /api/gathering-spots/city/{cityId}/recent-ids?weeks=12` - Recent Yelp IDs

#### Configuration Updates
- **FirebaseConfig.java** - Added Firestore initialization
- **application.yml** - Added `avoid-repeat-weeks: 12` configuration
- **build.gradle** - Added Google Cloud Firestore dependency

#### Job Enhancements
- **YelpDataSyncJob.java** updated to:
  - Query Firestore for enabled cities
  - Process multiple cities automatically
  - Check history to avoid repeating spots
  - Save selections to Firestore
  - Mark notifications as sent
  - Fallback to default config if no cities in Firestore

### Documentation Updates

#### New Files
- `FIREBASE_SETUP.md` - Complete Firebase setup guide
  - Project creation
  - Firestore enablement
  - Security rules
  - Service account setup
  - Data initialization
  - Troubleshooting

#### Updated Files
- `README.md` - Updated with Firestore references and new API endpoints
- `IMPLEMENTATION_PLAN.md` - Replaced PostgreSQL references with Firestore
- `MULTI_CITY_EXPANSION.md` - Updated to show Firestore implementation complete
- `.gitignore` - Added Firebase credentials exclusion

### Breaking Changes
None - Service was not yet deployed, this is initial setup.

### Migration Notes
If you were planning to use PostgreSQL, no migration is needed. The service now uses Firebase Firestore from the start.

### Configuration Required

1. **Firebase Project Setup**:
   - Create Firebase project
   - Enable Firestore Database
   - Enable Cloud Messaging
   - Download service account JSON

2. **Environment Variables**:
   ```bash
   YELP_API_KEY=your-yelp-api-key
   FIREBASE_CREDENTIALS_PATH=classpath:firebase-service-account.json
   FIREBASE_ENABLED=true
   ```

3. **Initialize Data**:
   ```bash
   # Add Seattle via REST API after starting service
   curl -X POST http://localhost:8080/api/cities \
     -H "Content-Type: application/json" \
     -d '{
       "name": "Seattle",
       "location": "Seattle, WA",
       "topic": "gather-seattle",
       "cronSchedule": "0 0 9 * * MON",
       "searchTerm": "bars",
       "searchLimit": 50,
       "enabled": true
     }'
   ```

### Technical Details

#### Firestore Collections

**cities**:
```javascript
{
  id: "auto-generated",
  name: "Seattle",
  location: "Seattle, WA",
  topic: "gather-seattle",
  cronSchedule: "0 0 9 * * MON",
  searchTerm: "bars",
  searchLimit: 50,
  enabled: true,
  createdAt: timestamp
}
```

**gatheringSpots**:
```javascript
{
  id: "auto-generated",
  cityId: "city-doc-id",
  yelpBusinessId: "yelp-id",
  businessName: "Bar Name",
  address: "123 Main St, Seattle, WA",
  rating: 4.5,
  selectedAt: timestamp,
  notificationSent: true,
  notificationSentAt: timestamp,
  phoneNumber: "+1-206-xxx-xxxx",
  yelpUrl: "https://yelp.com/biz/..."
}
```

#### Dependencies Added
- `com.google.cloud:google-cloud-firestore:3.14.5`

### Benefits of Firestore

1. **No Infrastructure Management** - Fully managed NoSQL database
2. **Real-time Sync** - Can add real-time features in future
3. **Offline Support** - Mobile apps can work offline
4. **Automatic Scaling** - Scales automatically with usage
5. **Free Tier** - Generous free tier covers expected usage
6. **Integrated with Firebase** - Same credentials as FCM
7. **Global CDN** - Low latency worldwide

### Cost Impact
- **Current**: $0/month (well within free tier)
- **10 Cities**: $0/month (still within free tier)
- **50 Cities**: $0/month (still within free tier)
- **Free Tier**: 50k reads, 20k writes, 1 GB storage per day

### Testing

Test the integration:
```bash
# Check cities
curl http://localhost:8080/api/cities

# Add a city
curl -X POST http://localhost:8080/api/cities \
  -H "Content-Type: application/json" \
  -d '{"name":"Portland","location":"Portland, OR","topic":"gather-portland","cronSchedule":"0 0 9 * * MON","searchTerm":"bars","searchLimit":50,"enabled":true}'

# View gathering spot history
curl http://localhost:8080/api/gathering-spots/city/{cityId}/recent?limit=10
```

### Next Steps

1. Complete Firebase setup (see FIREBASE_SETUP.md)
2. Add Seattle as first city
3. Test weekly job (can trigger manually for testing)
4. Build mobile app with city selection
5. Add Portland and San Francisco
6. Implement per-city cron scheduling

---

## Previous Updates

### Initial Setup
- Spring Boot 3.2.1 application
- Yelp API integration
- Firebase Cloud Messaging for push notifications
- Weekly scheduled job (Monday 9 AM)
- Seattle as default city
- Random bar selection
- Basic REST APIs for Yelp queries
