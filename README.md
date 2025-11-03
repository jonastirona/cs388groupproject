# cs388groupproject

## App Overview
ModMyCar is a mobile app for car enthusiasts of all ages to learn about modding, track build progress, and share projects with the community. Using the phone’s camera, microphone, and sensors, users can capture build photos, record performance stats, and showcase exhaust sounds. The app combines educational tools, cost estimates, and social features to help users plan upgrades, explore new ideas, and stay engaged in their car modding journey.

## App Spec

### User Features


### Navigation/Flows
```text
Home
 ├── Feed
 │     ├── Post List
 │     │     └── Post Detail → Like / Comment / Share → View Profile
 │     └── Create Post → Upload Photo/Audio → Add Description → Post
 ├── My Garage
 │     ├── My Cars List
 │     │     └── Car Detail → Mod Tree → Add Mod → Add Photo/Record Exhaust Sound 
 │     └── Add New Car
 ├── Explore
 │     ├── Popular Builds
 │     └── Nearby Shops (Map)
 ├── Community
 │     ├── Friends List
 │     └── Add Friend / Friend Requests
 └── Profile / Settings
       └── Edit Profile / My Posts / Logout
```

## Tech Stack
frontend:
- language: kotlin
- ui: android
- maps/route display/tracking: google maps sdk
- use map + location updates to mark start/finish, measure lap
- location tracking: FusedLocationProviderClient

Backend / Database
- supabase: auth, storage, rest api

