-- Add updated_at column to bookings table
ALTER TABLE bookings 
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Update existing records with current timestamp
UPDATE bookings 
SET updated_at = NOW() 
WHERE updated_at IS NULL;

-- Make column NOT NULL after updating existing data
ALTER TABLE bookings 
ALTER COLUMN updated_at SET NOT NULL;

-- Add index for performance (optional)
CREATE INDEX idx_bookings_updated_at ON bookings(updated_at);