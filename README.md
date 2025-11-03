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
