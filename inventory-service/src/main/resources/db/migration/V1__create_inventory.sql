CREATE TABLE inventory_items (
    product_id         BIGINT PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    available_quantity INT NOT NULL CHECK (available_quantity >= 0),
    version            BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE reservations (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT NOT NULL,
    product_id BIGINT NOT NULL REFERENCES inventory_items(product_id),
    quantity   INT NOT NULL CHECK (quantity > 0),
    status     VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (order_id, product_id)
);