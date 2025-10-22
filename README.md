# cs388groupproject

## tech stack:
frontend:
- language: kotlin
- ui: android
- maps/route display/tracking: google maps sdk
- use map + location updates to mark start/finish, measure lap
- location tracking: FusedLocationProviderClient

Backend / Database
- supabase: auth, storage, rest api

wireframe:
home
 ├── routes
 │     ├── route list
 │     │     └── Route Detail → Start Lap → Tracking → Lap Results → Friends/Global Leaderboard
 │     ├── My Routes (user-created)
 │     │     └── Route Detail → Start Lap → Tracking → Lap Results → Friends/Global Leaderboard
 │     └── Create Route
 │           └── Route Creation Flow
 ├── Leaderboard
 │     ├── Friends Leaderboard (points, not time)
 │     └── Global Leaderboard (points, not time)
 ├── Friends
 │     ├── Friend List
 │     └── Add Friend / Friend Requests
 └── Profile/Settings
       └── Edit Profile / Logout
