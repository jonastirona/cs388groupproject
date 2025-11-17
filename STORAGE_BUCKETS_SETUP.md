# Storage Buckets Setup Guide

This guide explains how to set up the required storage buckets in Supabase for the ModMyCar application.

## Required Storage Buckets

1. **car-images** - Stores reference images for car models
2. **mod-images** - Stores reference images for mods
3. **garage-media** - Stores user-uploaded media for their garage cars
4. **garage-mod-media** - Stores user-uploaded media for completed mods

## Setup Instructions

### Step 1: Access Supabase Storage

1. Log in to your Supabase dashboard
2. Navigate to **Storage** in the left sidebar
3. Click **New bucket** to create each bucket

### Step 2: Create Storage Buckets

Create each bucket with the following settings:

#### 1. car-images Bucket

- **Name**: `car-images`
- **Public bucket**: ✅ **Yes** (checked)
- **File size limit**: 5 MB (or your preferred limit)
- **Allowed MIME types**: `image/*` (or specific: `image/jpeg, image/png, image/webp`)

**Folder Structure:**
```
car-images/
  └── car_<car_id>/
        └── civic-si.png
```

#### 2. mod-images Bucket

- **Name**: `mod-images`
- **Public bucket**: ✅ **Yes** (checked)
- **File size limit**: 5 MB (or your preferred limit)
- **Allowed MIME types**: `image/*` (or specific: `image/jpeg, image/png, image/webp`)

**Folder Structure:**
```
mod-images/
  └── car_<car_id>/
        └── mod_<mod_id>/
              └── turbo.png
```

#### 3. garage-media Bucket

- **Name**: `garage-media`
- **Public bucket**: ❌ **No** (unchecked) - Private bucket for user data
- **File size limit**: 10 MB (or your preferred limit)
- **Allowed MIME types**: `image/*, video/*` (or specific types)

**Folder Structure:**
```
garage-media/
  └── user_<user_id>/
        └── car_<garage_car_id>/
              ├── front.jpg
              ├── rear.jpg
              └── interior.png
```

#### 4. garage-mod-media Bucket

- **Name**: `garage-mod-media`
- **Public bucket**: ❌ **No** (unchecked) - Private bucket for user data
- **File size limit**: 10 MB (or your preferred limit)
- **Allowed MIME types**: `image/*, audio/*, video/*` (or specific types)

**Folder Structure:**
```
garage-mod-media/
  └── user_<user_id>/
        └── mod_<garage_mod_id>/
              ├── exhaust.mp3
              └── turbo.png
```

### Step 3: Configure Storage Policies

After creating the buckets, you need to set up Row Level Security (RLS) policies for access control.

#### For Public Buckets (car-images, mod-images)

These buckets are public, so you can use the default public access policy, or create explicit policies:

**Policy for car-images:**
```sql
-- Allow public read access
CREATE POLICY "Public Access"
ON storage.objects FOR SELECT
USING (bucket_id = 'car-images');

-- Only authenticated users with service role can upload
-- (This should be done via service role in your backend)
```

**Policy for mod-images:**
```sql
-- Allow public read access
CREATE POLICY "Public Access"
ON storage.objects FOR SELECT
USING (bucket_id = 'mod-images');
```

#### For Private Buckets (garage-media, garage-mod-media)

These buckets require user-specific access control:

**Policy for garage-media:**
```sql
-- Users can view their own files
CREATE POLICY "Users can view their own garage media"
ON storage.objects FOR SELECT
USING (
    bucket_id = 'garage-media' AND
    (storage.foldername(name))[1] = 'user_' || auth.uid()::text
);

-- Users can upload to their own folder
CREATE POLICY "Users can upload their own garage media"
ON storage.objects FOR INSERT
WITH CHECK (
    bucket_id = 'garage-media' AND
    (storage.foldername(name))[1] = 'user_' || auth.uid()::text
);

-- Users can update their own files
CREATE POLICY "Users can update their own garage media"
ON storage.objects FOR UPDATE
USING (
    bucket_id = 'garage-media' AND
    (storage.foldername(name))[1] = 'user_' || auth.uid()::text
);

-- Users can delete their own files
CREATE POLICY "Users can delete their own garage media"
ON storage.objects FOR DELETE
USING (
    bucket_id = 'garage-media' AND
    (storage.foldername(name))[1] = 'user_' || auth.uid()::text
);
```

**Policy for garage-mod-media:**
```sql
-- Users can view their own files
CREATE POLICY "Users can view their own garage mod media"
ON storage.objects FOR SELECT
USING (
    bucket_id = 'garage-mod-media' AND
    (storage.foldername(name))[1] = 'user_' || auth.uid()::text
);

-- Users can upload to their own folder
CREATE POLICY "Users can upload their own garage mod media"
ON storage.objects FOR INSERT
WITH CHECK (
    bucket_id = 'garage-mod-media' AND
    (storage.foldername(name))[1] = 'user_' || auth.uid()::text
);

-- Users can update their own files
CREATE POLICY "Users can update their own garage mod media"
ON storage.objects FOR UPDATE
USING (
    bucket_id = 'garage-mod-media' AND
    (storage.foldername(name))[1] = 'user_' || auth.uid()::text
);

-- Users can delete their own files
CREATE POLICY "Users can delete their own garage mod media"
ON storage.objects FOR DELETE
USING (
    bucket_id = 'garage-mod-media' AND
    (storage.foldername(name))[1] = 'user_' || auth.uid()::text
);
```

### Step 4: Apply Policies via SQL Editor

1. Go to **SQL Editor** in your Supabase dashboard
2. Create a new query
3. Copy and paste the policy SQL statements above
4. Run the query to apply the policies

### Step 5: Verify Setup

1. Test uploading a file to each bucket using the Supabase Storage UI
2. Verify that:
   - Public buckets allow anonymous read access
   - Private buckets require authentication
   - Users can only access their own files in private buckets

## Notes

- **Public vs Private**: Public buckets allow anyone to read files (good for reference images). Private buckets require authentication and RLS policies (good for user-uploaded content).
- **File Size Limits**: Adjust based on your needs. Images typically need 1-5 MB, videos may need 10-50 MB.
- **MIME Types**: Restrict to specific types for security (e.g., only allow `image/jpeg, image/png` instead of `image/*`).
- **Folder Structure**: The folder structure is enforced by your application code, not by Supabase. Make sure your upload code follows the specified structure.

## Troubleshooting

- **403 Forbidden**: Check that RLS policies are correctly set up and the user is authenticated
- **File upload fails**: Verify file size and MIME type restrictions
- **Cannot access files**: For private buckets, ensure the user is authenticated and the path matches the policy conditions

