-- Add location column to profiles table
ALTER TABLE profiles 
ADD COLUMN location VARCHAR(255);

-- Update existing records with default value (optional)
UPDATE profiles 
SET location = '' 
WHERE location IS NULL;

-- Add index for location search (optional)
CREATE INDEX idx_profiles_location ON profiles(location);