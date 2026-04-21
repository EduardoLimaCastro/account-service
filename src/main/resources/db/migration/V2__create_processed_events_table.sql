CREATE TABLE processed_events (
    event_id        VARCHAR(255)    NOT NULL,
    topic           VARCHAR(255)    NOT NULL,
    processed_at    TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_processed_events PRIMARY KEY (event_id, topic)
);
