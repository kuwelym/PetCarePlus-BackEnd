-- Add provider_service_id column
ALTER TABLE bookings 
ADD COLUMN provider_service_id UUID;

-- Create index for performance
CREATE INDEX idx_bookings_provider_service_id ON bookings(provider_service_id);

-- Update existing records
-- Giả sử mỗi booking chỉ có 1 service, lấy provider_service_id từ service_bookings
UPDATE bookings b
SET provider_service_id = (
    SELECT ps.id 
    FROM provider_services ps 
    INNER JOIN service_bookings sb ON ps.service_id = sb.service_id 
    WHERE sb.bookings_id = b.id 
    AND ps.provider_id = b.provider_id
    LIMIT 1
)
WHERE provider_service_id IS NULL;

-- Add foreign key constraint
ALTER TABLE bookings 
ADD CONSTRAINT fk_bookings_provider_service 
FOREIGN KEY (provider_service_id) 
REFERENCES provider_services(id);

-- Make column NOT NULL after data migration
ALTER TABLE bookings 
ALTER COLUMN provider_service_id SET NOT NULL;