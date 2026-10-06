CREATE TABLE processed_events (
    handler      VARCHAR(100) NOT NULL,
    event_key    VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (handler, event_key)
);