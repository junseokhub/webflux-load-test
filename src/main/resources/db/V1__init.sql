CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS coupons
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    total_stock     INT          NOT NULL,
    remaining_stock INT          NOT NULL,
    created_at      DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS coupon_issues
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT      NOT NULL,
    user_id   BIGINT      NOT NULL,
    issued_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_coupon_user (coupon_id, user_id),
    FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
);