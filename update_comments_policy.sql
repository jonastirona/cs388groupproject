-- Option 1: Simple policy (as you requested)
-- This allows any authenticated user to insert comments with any user_id
-- WARNING: This is less secure as users could potentially insert comments with other users' IDs

DROP POLICY IF EXISTS "Logged in can insert comments" ON public.comments;
DROP POLICY IF EXISTS "Authenticated users can insert comments" ON public.comments;

CREATE POLICY "Logged in can insert comments"
    ON public.comments
    FOR INSERT
    TO authenticated
    WITH CHECK (true);

-- Option 2: Secure policy (RECOMMENDED)
-- This ensures users can only insert comments with their own user_id
-- Uncomment the lines below and comment out Option 1 if you want to use this instead:

-- DROP POLICY IF EXISTS "Logged in can insert comments" ON public.comments;
-- DROP POLICY IF EXISTS "Authenticated users can insert comments" ON public.comments;
-- 
-- CREATE POLICY "Logged in can insert comments"
--     ON public.comments
--     FOR INSERT
--     TO authenticated
--     WITH CHECK (
--         auth.uid() = user_id AND
--         auth.uid() IS NOT NULL
--     );


