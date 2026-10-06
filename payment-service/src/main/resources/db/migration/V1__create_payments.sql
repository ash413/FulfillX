CREATE TABLE payments (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT NOT NULL UNIQUE,
    status         VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(255),
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);