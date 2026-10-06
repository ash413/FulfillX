CREATE TABLE outbox_events (
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(64)   NOT NULL,
    aggregate_id   VARCHAR(64)   NOT NULL,
    topic          VARCHAR(128)  NOT NULL,
    payload        VARCHAR(8000) NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    published_at   TIMESTAMP
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (id) WHERE published_at IS NULL;