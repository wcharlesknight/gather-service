# Firebase Setup Guide

This guide walks you through setting up Firebase for the Gather Service, including Firestore Database and Cloud Messaging.

## Prerequisites
- Google account
- Access to Firebase Console

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Create a project" (or "Add project")
3. Enter project name: `gather-service` (or your preferred name)
4. Enable/disable Google Analytics (optional, not required for this service)
5. Click "Create Project"
6. Wait for project creation to complete

## Step 2: Enable Firestore Database

1. In the Firebase Console, select your project
2. Click on "Firestore Database" in the left sidebar
3. Click "Create database"
4. Choose production mode or test mode:
   - **Test mode**: Open for 30 days (good for development)
   - **Production mode**: Locked down, requires authentication (recommended for production)
5. Select a Firestore location (choose closest to your users):
   - `us-west1` for Seattle/West Coast
   - `us-central1` for Central US
   - `us-east1` for East Coast
6. Click "Enable"

## Step 3: Set Firestore Security Rules

1. In Firestore Database, click on the "Rules" tab
2. Replace the default rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Cities collection - public read, admin write only
    match /cities/{cityId} {
      allow read: if true;  // Anyone can read cities
      allow write: if false;  // Only service can write (via service account)
    }

    // Gathering spots - public read, service write only
    match /gatheringSpots/{spotId} {
      allow read: if true;  // Anyone can read history
      allow write: if false;  // Only service can write
    }

    // User subscriptions (future)
    match /userSubscriptions/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

3. Click "Publish"

## Step 4: Enable Cloud Messaging

1. In the Firebase Console, click on "Cloud Messaging" in the left sidebar
2. No additional setup required for topics (handled by mobile apps)
3. Note: You'll configure mobile apps separately

## Step 5: Create Service Account

1. In the Firebase Console, click the gear icon (⚙️) next to "Project Overview"
2. Select "Project settings"
3. Click on the "Service accounts" tab
4. Click "Generate new private key"
5. Confirm by clicking "Generate key"
6. A JSON file will download automatically

## Step 6: Configure the Application

1. Rename the downloaded JSON file to `firebase-service-account.json`
2. Move it to your project's `src/main/resources/` directory
3. **IMPORTANT**: Add this file to `.gitignore` to prevent committing secrets:
   ```
   # Add to .gitignore
   src/main/resources/firebase-service-account.json
   ```

## Step 7: Initialize Data (Optional)

### Add Seattle as the First City

You can add Seattle via the REST API after starting the service, or add it directly in Firestore:

**Via Firestore Console**:
1. Go to Firestore Database
2. Click "Start collection"
3. Collection ID: `cities`
4. Document ID: (auto-generate)
5. Add fields:
   ```
   name: "Seattle"
   location: "Seattle, WA"
   topic: "gather-seattle"
   cronSchedule: "0 0 9 * * MON"
   searchTerm: "bars"
   searchLimit: 50
   enabled: true
   createdAt: [current timestamp in milliseconds]
   ```
6. Click "Save"

**Via REST API** (after service is running):
```bash
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

## Step 8: Verify Setup

After starting your service, verify everything works:

### Check Firestore Connection
```bash
# Get all cities
curl http://localhost:8080/api/cities
```

### Monitor Firestore Usage
1. Go to Firebase Console → Firestore Database → Usage tab
2. Monitor reads, writes, and storage
3. Free tier limits:
   - 50,000 reads/day
   - 20,000 writes/day
   - 20,000 deletes/day
   - 1 GB storage

## Firestore Collections Structure

### `cities` Collection
```javascript
{
  id: "auto-generated-doc-id",
  name: "Seattle",
  location: "Seattle, WA",
  topic: "gather-seattle",
  cronSchedule: "0 0 9 * * MON",
  searchTerm: "bars",
  searchLimit: 50,
  enabled: true,
  createdAt: 1234567890000
}
```

### `gatheringSpots` Collection
```javascript
{
  id: "auto-generated-doc-id",
  cityId: "seattle-city-doc-id",
  yelpBusinessId: "pike-brewing-seattle",
  businessName: "Pike Brewing Company",
  address: "1415 1st Ave, Seattle, WA 98101",
  rating: 4.5,
  selectedAt: 1234567890000,
  notificationSent: true,
  notificationSentAt: 1234567890123,
  phoneNumber: "+1-206-555-1234",
  yelpUrl: "https://www.yelp.com/biz/pike-brewing-company-seattle"
}
```

## Composite Indexes

Firestore will automatically create indexes for simple queries. For complex queries, you may need to create composite indexes:

1. When you see an error like "The query requires an index", click the provided link
2. Or manually create in Firebase Console → Firestore Database → Indexes
3. Create index:
   - Collection: `gatheringSpots`
   - Fields: `cityId` (Ascending), `selectedAt` (Descending)
   - Query scope: Collection

## Security Best Practices

1. **Never commit service account JSON to version control**
   - Use `.gitignore` to exclude it
   - Store securely in production (environment variables, secret manager)

2. **Use production mode security rules in production**
   - Lock down write access
   - Only allow reads for public data

3. **Rotate service account keys periodically**
   - Generate new key in Firebase Console
   - Update deployed services
   - Delete old key

4. **Monitor Firestore usage**
   - Set up budget alerts in Google Cloud Console
   - Monitor for unusual activity
   - Review security rules regularly

## Mobile App Setup (Future)

### iOS App
1. Add iOS app in Firebase Console
2. Download `GoogleService-Info.plist`
3. Add to Xcode project
4. Subscribe to topic in app:
   ```swift
   Messaging.messaging().subscribe(toTopic: "gather-seattle")
   ```

### Android App
1. Add Android app in Firebase Console
2. Download `google-services.json`
3. Add to Android project
4. Subscribe to topic in app:
   ```kotlin
   FirebaseMessaging.getInstance().subscribeToTopic("gather-seattle")
   ```

## Troubleshooting

### Service Account Errors
- **Error**: "Failed to initialize Firebase"
  - Check that JSON file is in `src/main/resources/`
  - Verify file name matches `firebase-service-account.json`
  - Check file permissions

### Firestore Permission Errors
- **Error**: "Missing or insufficient permissions"
  - Check security rules allow the operation
  - Verify service account has Firestore access
  - Service accounts bypass rules, so this usually means app initialization failed

### Cloud Messaging Errors
- **Error**: "Requested entity was not found"
  - Topic may not exist yet (created when first app subscribes)
  - Check topic name matches configuration
  - Verify FCM is enabled in Firebase Console

## Cost Monitoring

Free tier quotas:
- **Firestore**: 50k reads, 20k writes, 1 GB storage per day
- **Cloud Messaging**: Unlimited notifications
- **Cloud Functions**: Not used (n/a)

Current expected usage (single city):
- ~7 writes/week (1 gathering spot per week)
- ~70 reads/week (checking last 12 weeks of spots)
- Storage: <1 MB per year

Expected usage (10 cities):
- ~70 writes/week
- ~700 reads/week
- Storage: <10 MB per year

**Result**: Well within free tier for foreseeable future!

## Next Steps

1. ✅ Firebase project created
2. ✅ Firestore enabled
3. ✅ Security rules configured
4. ✅ Service account downloaded and installed
5. ⏳ Add initial city (Seattle)
6. ⏳ Start the service and test
7. ⏳ Configure mobile apps
8. ⏳ Add more cities as needed

## Support Resources

- [Firebase Documentation](https://firebase.google.com/docs)
- [Firestore Documentation](https://firebase.google.com/docs/firestore)
- [Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)
- [Firebase Console](https://console.firebase.google.com)
