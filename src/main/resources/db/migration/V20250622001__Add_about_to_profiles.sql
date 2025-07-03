-- Add about column to profiles table
ALTER TABLE profiles 
ADD COLUMN about TEXT;

-- Update existing records with default value (optional)
UPDATE profiles 
SET about = '' 
WHERE about IS NULL;

-- Add index for search performance (optional)
CREATE INDEX idx_profiles_about ON profiles USING gin(to_tsvector('english', about));