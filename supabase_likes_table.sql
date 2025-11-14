-- SQL script to create the likes table in Supabase
-- Run this in your Supabase SQL Editor

-- Create likes table
CREATE TABLE IF NOT EXISTS likes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    -- Ensure a user can only like a post once
    UNIQUE(post_id, user_id)
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_likes_post_id ON likes(post_id);
CREATE INDEX IF NOT EXISTS idx_likes_user_id ON likes(user_id);
CREATE INDEX IF NOT EXISTS idx_likes_created_at ON likes(created_at);

-- Enable Row Level Security (RLS)
ALTER TABLE likes ENABLE ROW LEVEL SECURITY;

-- Policy: Anyone can read likes
CREATE POLICY "Likes are viewable by everyone"
    ON likes FOR SELECT
    USING (true);

-- Drop existing insert policy if it exists
DROP POLICY IF EXISTS "Logged in can insert likes" ON public.likes;
DROP POLICY IF EXISTS "Authenticated users can insert likes" ON public.likes;

-- Policy: Authenticated users can insert likes
-- This ensures the user_id matches the authenticated user's ID for security
CREATE POLICY "Logged in can insert likes"
    ON public.likes
    FOR INSERT
    TO authenticated
    WITH CHECK (
        auth.uid() = user_id AND
        auth.uid() IS NOT NULL
    );

-- Policy: Users can delete their own likes
CREATE POLICY "Users can delete their own likes"
    ON likes FOR DELETE
    USING (auth.uid() = user_id);

-- Note: No UPDATE policy needed since likes are either created or deleted (toggle behavior)


