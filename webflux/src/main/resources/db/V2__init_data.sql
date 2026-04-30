-- ADMIN 계정 생성 (중복이면 무시)
INSERT IGNORE INTO users (username, password, role, created_at, updated_at)
VALUES ('admin', '$2a$10$8QvYmOmMHxqHfQxO2NkZAOqrP7G1c9RrPXKXJ8Vs5FiN3KqXKP6kO', 'ADMIN', NOW(), NOW());

-- 쿠폰 생성 (중복이면 무시)
INSERT IGNORE INTO coupons (name, type, discount_value, total_stock, remaining_stock, created_at, updated_at)
VALUES ('신규가입 쿠폰', 'FIXED', 3000, 100, 100, NOW(), NOW());