-- V20250526001__Refactor_payment_for_multi_gateway.sql

-- Add new columns
ALTER TABLE payments 
ADD COLUMN order_code VARCHAR(255),
ADD COLUMN gateway_data JSONB;

-- Create indexes for better performance
CREATE INDEX idx_payments_order_code ON payments(order_code);
CREATE INDEX idx_payments_gateway_data ON payments USING GIN(gateway_data);

-- Migrate existing VNPay data to new structure
UPDATE payments 
SET gateway_data = jsonb_build_object(
    'transaction_code', transaction_code,
    'bank_code', bank_code,
    'card_type', card_type
),
order_code = transaction_code
WHERE payment_method = 'VNPAY' AND transaction_code IS NOT NULL;

-- Set order_code for existing payments without transaction_code
UPDATE payments 
SET order_code = CONCAT('ORDER_', id::text)
WHERE order_code IS NULL;

-- Add constraint (after data migration)
ALTER TABLE payments 
ADD CONSTRAINT chk_order_code_required CHECK (order_code IS NOT NULL);