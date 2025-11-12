# ModMyCar

## Table of Contents

1. [Overview](#Overview)
1. [Product Spec](#Product-Spec)
1. [Wireframes](#Wireframes)

## Overview

### Description

ModMyCar is a mobile app for car enthusiasts of all ages to learn about modding, track build progress, and share projects with the community. Using the phone’s camera, microphone, and sensors, users can capture build photos, record performance stats, and showcase exhaust sounds. The app combines educational tools, cost estimates, and social features to help users plan upgrades, explore new ideas, and stay engaged in their car modding journey.

### App Evaluation

[Evaluation of your app across the following attributes]
- **Category:**
Automotive / Social Networking
- **Mobile:**
Utilizes camera, accelerometer, and microphone to allow users to take photos of their car builds, record performance stats (0-60 times, quarter-mile times, etc.), as well as record exaust sounds.
- **Story:**
This app is very useful for members of the car community who want direction modding their car and a platform where they can track their current modding progress. This app will also serve as a platform for users to share their builds with other members of the community.  
- **Market:**
The car mod market is huge with many members of the car enthusiest community becoming more interested in participating. Amongst these consumers, the age varies from younger people just getting into modding their first cars to older folks who have been doing it for a while. The app appeals to enthusiasts of all ages by offering educational tools for learning about car modding, features to track progress on builds, and a social platform to showcase and share modifications.
- **Habit:**
Users would regularly check it to explore new modifications, monitor prices, or interact with new visualization ideas for their vehicles. Users can create their own car modification plans and can share them with others. The app’s use of push notifications and the regular new posts by other users would encourage users to return often, keeping them engaged in their car’s upgrade journey.
- **Scope:**
The app's implementation is manageable as it will be a social-media like app. Core Features would include car model selection, picture uploads, audio uploads, hierarchical visualization of modifications, and cost estimation. These features will be implemented using the phones sensors such as the camera and the mic alongside existing API's. A stripped down version of the app that only includes photo and audio uploads with cost estimation would still be highly engaging and valuable.

## Product Spec

### 1. User Features (Required and Optional)

#### **Required Features**

- [x] **User Authentication** – Allow users to sign up, log in, and manage their profile.

![Screen Recording 2025-11-11 at 8 28 08 PM](https://github.com/user-attachments/assets/fca943f8-a041-437a-8919-f2ce2e12fe52)

- [ ] **Feed / Home Tab** – Users can view posts (photos, mod updates, or exhaust sound clips) shared by others.  
- [ ] **Create Post** – Upload photos or record audio of their car, add a description, and post to the community feed.  
- [ ] **My Garage** – Users can add their own cars with details (model, year, trim) and view a list of all their registered cars.

    ***Update: Milestone 2***
    - Created supabase tables to store garage data
    - Created storage bucket to store images of users cars
    <img width="338" height="160" alt="car-schema" src="https://github.com/user-attachments/assets/ac811e8e-6f25-4944-b6a7-119e5edb8517" />
    <img width="653" height="46" alt="car-images" src="https://github.com/user-attachments/assets/4745d1a6-f84b-43de-b961-d90e0dd99975" />
- [ ] **Car Detail Screen** – Display car information, photo carousel, and modification overview.
- [ ] **Mod Tree Visualization** – Hierarchical structure (e.g., *Engine → Intake → PRL Short Ram Intake*) to display and organize each car’s modifications.
    **Update: Milestone 2**
    - Created supabase table to store mod tree for each car (linked to car by car_id field)
    - Created storage bucket to store images of mods available for cars
    <img width="263" height="155" alt="mod-schema" src="https://github.com/user-attachments/assets/fe3713ea-8318-48ad-bbf9-f9bbab26326c" />
    <img width="656" height="49" alt="mod-images" src="https://github.com/user-attachments/assets/5cac5e5c-4e17-4222-a64b-3c1fd6f567b3" />
- [ ] **Add Mod Functionality** – Add a new modification under the correct category with photos, cost, and optional audio (e.g., exhaust clip).
    **Update: Milestone 2**
    - Created supabase table to mods completed by individual users on their car
    - Created storage bucket to store images of mods completed by users on their cars
    <img width="263" height="155" alt="mod-schema" src="https://github.com/user-attachments/assets/fe3713ea-8318-48ad-bbf9-f9bbab26326c" />
    <img width="614" height="59" alt="car-mod-media" src="https://github.com/user-attachments/assets/b30814cc-3e2a-445e-9eb6-43097352b831" />
- [ ] **Engagement Features** – Users can like, comment on, and share posts from the community feed.
    **Update: Milestone 2**
    - Working on adding likes and commenting to individual posts.
    - Created supabase tables for likes and comments based on posts and userid's
      <img width="300" height="614" alt="image" src="https://github.com/user-attachments/assets/4d882a3c-8bfc-4284-aa30-20675076309e" />
- [ ] **Explore Tab** – View popular builds and nearby tuning or parts shops on a map.  
- [ ] **Profile / Settings** – Edit personal info, view one’s posts, and log out.
- [ ] **New Articles Stream** - View relevant car-themed news articles.
#### **Optional Features**

- [ ] **Performance Stats Tracking** – Record 0–60 mph and quarter-mile times using phone sensors (accelerometer + GPS).  
- [ ] **Cost Estimation Tool** – Track total mod spending or generate build cost projections.  
- [ ] **Push Notifications** – Notify users when someone likes/comments on their post or when new builds are trending.  
- [ ] **Friend System** – Add friends, view their garages, and follow their updates.  
- [ ] **Educational Mod Guides** – Include curated tutorials or community-submitted how-tos for popular mods.  

---

### 2. Screen Archetypes

- **Login / Sign-Up Screen**  
  - Users create or access their account to save their garage and posts.  

- **Feed (Home Tab)**  
  - Displays a scrollable list of posts with images or audio from other users.  
  - Allows users to like, comment, and share posts.  
  - Floating action button (➕) opens the **Create Post** screen.  

- **Post Detail Screen**  
  - Shows full-size photo/audio and full caption.  
  - Displays all comments and likes.  
  - Option to view poster’s profile.  

- **Create Post Screen**  
  - Upload a photo or record audio (exhaust sound).  
  - Add a description and tag the car from **My Garage**.  
  - Post to the feed.  

- **My Garage Screen**  
  - Displays all cars added by the user with photos.  
  - “Add New Car” button to create a new entry.  
  - Each car opens to **Car Detail**.

### 3. Navigation

**Tab Navigation** (Tab to Screen)
* **Home** – Access the main feed of posts and create new posts.
* **My Garage** – View your cars, add new cars, and manage mods.
* **Explore** – Discover popular builds and nearby shops.
* **Community** – Connect with friends, manage friend requests.
* **Profile / Settings** – Edit profile, view your posts, and logout.

**Flow Navigation** (Screen-to-Screen)

```text
Home
 ├── Feed
 │     ├── Post List
 │     │     └── Post Detail → Like / Comment / Share → View Profile
 │     └── Create Post → Upload Photo/Audio → Add Description → Post
 ├── My Garage
 │     ├── My Cars List
 │     │     └── Car Detail → Mod Tree → Add Mod → Add Photo / Record Exhaust Sound 
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

**Notes:**

* Users can navigate between tabs at any time.
* Within each tab, screens follow the flow outlined above.
* Actions such as liking, commenting, sharing, or adding mods occur within the relevant detail screens.

## Wireframes

[Add picture of your hand sketched wireframes in this section]

<img src="https://github.com/user-attachments/assets/6ca66a18-fd8c-4c66-8171-520b0ed34e8a" width="400">
<img src="https://github.com/user-attachments/assets/6deb6376-0a97-4aa4-b7dc-c3b9d395e1df" width="400">
<img src="https://github.com/user-attachments/assets/e0993d73-461f-4c3f-b59f-26c92ecef03b" width="400">
<img src="https://github.com/user-attachments/assets/99463aef-9ee4-4c9a-8444-21587ec13c69" width="400">

<br>

```
