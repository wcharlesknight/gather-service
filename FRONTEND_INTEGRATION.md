# Frontend Integration Guide

## Overview
This guide describes how the LoopIn mobile app (frontend) integrates with the Gather Service (backend).

**Frontend Repository**: `/Users/charlieknight/Projects/LoopIn`

## Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────┐
│  LoopIn Mobile  │◄───────►│  Gather Service  │◄───────►│   Firebase  │
│      App        │  REST    │   (Backend)      │         │  Firestore  │
│  (React Native) │  API     │  (Spring Boot)   │         │     FCM     │
└─────────────────┘         └──────────────────┘         └─────────────┘
        │                             │                           │
        │                             │                           │
        └─────────────────────────────┴───────────────────────────┘
                      Firebase Cloud Messaging
                   (Push Notifications)
```

## User Flow

### 1. Sign Up / Onboarding
**Mobile App**:
1. User opens app for the first time
2. Sign up with email/phone (Firebase Auth)
3. Select their city (e.g., Seattle)
4. Subscribe to city-specific FCM topic

**Backend Integration**:
- User data stored in Firebase Auth
- App subscribes to FCM topic: `gather-seattle`
- Future: Store user preferences in Firestore `userSubscriptions` collection

### 2. Weekly Notification (Thursday)
**Backend Process** (Thursday 9:00 AM):
1. Gather Service job runs
2. Selects random gathering spot from Yelp
3. Saves to Firestore `gatheringSpots` collection
4. Sends FCM notification to `gather-seattle` topic

**Mobile App Receives**:
```javascript
// Notification payload
{
  notification: {
    title: "📍 This Week's Gather Spot!",
    body: "Meet Friday at Pike Brewing Company - 1415 1st Ave, Seattle, WA"
  },
  data: {
    businessId: "pike-brewing-seattle",
    businessName: "Pike Brewing Company",
    rating: "4.5",
    address: "1415 1st Ave, Seattle, WA 98101",
    url: "https://www.yelp.com/biz/pike-brewing-company-seattle",
    latitude: "47.6097",
    longitude: "-122.3401"
  }
}
```

**Mobile App Actions**:
1. Display notification to user
2. When tapped, open app to spot details screen
3. Show map with location pin
4. Show Yelp rating, address, phone
5. "Add to Calendar" button (Friday evening)
6. "Open in Maps" button

### 3. Viewing History
**Mobile App** calls:
```
GET /api/gathering-spots/city/{cityId}/recent?limit=4
```

**Response**:
```json
[
  {
    "id": "spot-123",
    "cityId": "seattle-id",
    "yelpBusinessId": "pike-brewing-seattle",
    "businessName": "Pike Brewing Company",
    "address": "1415 1st Ave, Seattle, WA 98101",
    "rating": 4.5,
    "selectedAt": 1234567890000,
    "notificationSent": true,
    "phoneNumber": "+1-206-555-1234",
    "yelpUrl": "https://www.yelp.com/biz/..."
  },
  // ... 3 more spots
]
```

**Mobile App Displays**:
- List of last 4 gathering spots
- Show date, name, address
- Tap to see details and map
- Show who attended (future feature)

## API Endpoints Used by Mobile App

### Get Available Cities
```
GET /api/cities
```
Used during onboarding to show city selection.

### Get Recent Gathering Spots
```
GET /api/gathering-spots/city/{cityId}/recent?limit=4
```
Show history of last 4 meetups.

### Health Check
```
GET /api/health
```
Verify service is running.

### Manual Search (Optional)
```
GET /api/yelp/search?location=Seattle&term=bars
```
For admin/testing purposes, or future "suggest a spot" feature.

## Firebase Setup for Mobile App

### iOS (React Native)

1. **Install Firebase SDK**:
```bash
npm install @react-native-firebase/app
npm install @react-native-firebase/messaging
npm install @react-native-firebase/auth
npm install @react-native-firebase/firestore
cd ios && pod install
```

2. **Add GoogleService-Info.plist** to Xcode project

3. **Subscribe to Topic**:
```javascript
import messaging from '@react-native-firebase/messaging';

// Subscribe to city-specific topic
await messaging().subscribeToTopic('gather-seattle');
```

4. **Handle Notifications**:
```javascript
// Foreground notifications
messaging().onMessage(async remoteMessage => {
  console.log('Notification received:', remoteMessage);
  // Display in-app notification
  showInAppNotification(remoteMessage);
});

// Background/Quit state - notification tapped
messaging().onNotificationOpenedApp(remoteMessage => {
  console.log('Notification opened:', remoteMessage);
  // Navigate to spot details
  navigation.navigate('SpotDetails', {
    businessId: remoteMessage.data.businessId
  });
});

// App opened from quit state via notification
messaging()
  .getInitialNotification()
  .then(remoteMessage => {
    if (remoteMessage) {
      console.log('Notification caused app to open:', remoteMessage);
      // Navigate to spot details
    }
  });
```

### Android (React Native)

1. **Install Firebase SDK** (same as iOS)

2. **Add google-services.json** to `android/app/`

3. **Configure AndroidManifest.xml**:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

4. **Subscribe and Handle** (same code as iOS)

## Mobile App Screens

### 1. Home Screen
**Displays**:
- Current week's gathering spot (if selected)
- Big map view with location pin
- Spot name, address, rating
- "Add to Calendar" button
- "Get Directions" button
- Countdown to Friday gathering

**API Calls**:
- `GET /api/gathering-spots/city/{cityId}/recent?limit=1` (get latest)

### 2. History Screen
**Displays**:
- List of last 4 gathering spots
- Date selected
- Spot name and address
- Tap to see full details

**API Calls**:
- `GET /api/gathering-spots/city/{cityId}/recent?limit=4`

### 3. Profile/Settings Screen
**Displays**:
- User info (from Firebase Auth)
- Current city selection
- Notification preferences
- "Change City" button

**API Calls**:
- `GET /api/cities` (for city selection)

### 4. Onboarding Screen
**Displays**:
- Welcome message
- City selection dropdown
- Sign up with email/phone
- Permission requests (notifications)

**API Calls**:
- `GET /api/cities` (list available cities)

## Data Flow Example

### Thursday Morning (Notification Sent)
```
1. Backend Job Runs (9:00 AM Thursday)
   └─> Selects "Pike Brewing Company"
   └─> Saves to Firestore
   └─> Sends FCM to topic "gather-seattle"

2. User's Phone Receives Notification
   └─> Shows banner: "This Week's Gather Spot!"
   └─> User taps notification

3. App Opens
   └─> Fetches latest spot details
   └─> Shows map with Pike Brewing pin
   └─> Shows "Add to Calendar" button

4. User Taps "Add to Calendar"
   └─> Creates event: "Gather at Pike Brewing Company"
   └─> Date: This Friday, 7:00 PM
   └─> Location: 1415 1st Ave, Seattle, WA
```

### Friday Morning (User Opens App)
```
1. User Opens App
   └─> Home screen shows this week's spot
   └─> Map with location
   └─> "Get Directions" button

2. User Taps "Get Directions"
   └─> Opens Apple Maps / Google Maps
   └─> Navigation to Pike Brewing

3. User Arrives at Spot
   └─> (Future) Check-in feature
   └─> (Future) See who else is there
   └─> (Future) In-app chat
```

## Mobile App State Management

### Using React Context or Redux

```javascript
// GatherContext.js
export const GatherContext = createContext();

export const GatherProvider = ({ children }) => {
  const [currentCity, setCurrentCity] = useState(null);
  const [currentSpot, setCurrentSpot] = useState(null);
  const [recentSpots, setRecentSpots] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchCurrentSpot = async () => {
    setLoading(true);
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/gathering-spots/city/${currentCity.id}/recent?limit=1`
      );
      const data = await response.json();
      setCurrentSpot(data[0]);
    } catch (error) {
      console.error('Error fetching current spot:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchRecentSpots = async () => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/gathering-spots/city/${currentCity.id}/recent?limit=4`
      );
      const data = await response.json();
      setRecentSpots(data);
    } catch (error) {
      console.error('Error fetching recent spots:', error);
    }
  };

  return (
    <GatherContext.Provider
      value={{
        currentCity,
        setCurrentCity,
        currentSpot,
        recentSpots,
        fetchCurrentSpot,
        fetchRecentSpots,
        loading
      }}
    >
      {children}
    </GatherContext.Provider>
  );
};
```

## Environment Configuration

### Mobile App (.env)
```bash
API_BASE_URL=https://your-backend-domain.com
# Or for local development:
# API_BASE_URL=http://localhost:8080

FIREBASE_API_KEY=your-firebase-api-key
FIREBASE_PROJECT_ID=gather-service
```

## Testing Integration

### Local Development
1. Start backend: `./gradlew bootRun`
2. Backend runs on `http://localhost:8080`
3. Use ngrok or similar to expose to mobile device:
   ```bash
   ngrok http 8080
   ```
4. Update mobile app `API_BASE_URL` to ngrok URL
5. Test API calls and notifications

### Testing Notifications
1. Use Firebase Console to send test notification:
   - Go to Cloud Messaging
   - Send test message to topic: `gather-seattle`
   - Verify app receives it

2. Or trigger job manually (for testing):
   - Modify cron to run every minute
   - Or create test endpoint to trigger job

## Security Considerations

### Mobile App
1. **API Key Storage**: Use secure storage (Keychain/Keystore)
2. **HTTPS Only**: Always use HTTPS in production
3. **Token Refresh**: Handle FCM token refresh
4. **Auth Required**: Require Firebase Auth for user-specific features

### Backend
1. **API Rate Limiting**: Implement rate limiting
2. **CORS Configuration**: Configure allowed origins
3. **Input Validation**: Validate all API inputs

## Future Features

### Phase 2 - RSVP
**Mobile App**:
- "I'm Going" button on spot details
- Show count of attendees
- Avatar list of who's going

**Backend**:
- New endpoint: `POST /api/gathering-spots/{spotId}/rsvp`
- Store RSVPs in Firestore
- Update notification count

### Phase 3 - In-App Chat
**Mobile App**:
- Chat screen for each gathering
- Real-time messaging

**Backend**:
- Use Firestore real-time listeners
- Collection: `gatheringChats`
- Messages stored per gathering spot

### Phase 4 - Check-In
**Mobile App**:
- "Check In" button when near location
- Uses GPS to verify

**Backend**:
- New endpoint: `POST /api/gathering-spots/{spotId}/checkin`
- Track who actually showed up
- Award points/badges (gamification)

## Troubleshooting

### Notifications Not Received
1. Check FCM token is valid
2. Verify subscribed to correct topic
3. Check notification permissions enabled
4. Test with Firebase Console direct send

### API Calls Failing
1. Check backend is running
2. Verify API_BASE_URL is correct
3. Check network connectivity
4. Review backend logs

### Map Not Loading
1. Enable location permissions
2. Check Google Maps API key (if using)
3. Verify coordinates are valid

## Resources

- [React Native Firebase Docs](https://rnfirebase.io/)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
- [Gather Service API Docs](./README.md)
- [Backend Setup Guide](./FIREBASE_SETUP.md)
