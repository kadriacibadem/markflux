CREATE TABLE outbox_event
(
    id              UUID PRIMARY KEY,
    sequence_number BIGINT GENERATED ALWAYS AS IDENTITY,
    topic           VARCHAR(255) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB        NOT NULL,
    trace_context   JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,
    attempts        INTEGER      NOT NULL DEFAULT 0,
    last_error      TEXT
);

CREATE INDEX idx_outbox_event_unpublished ON outbox_event (sequence_number) WHERE published_at IS NULL;

CREATE TABLE inbox_message
(
    consumer     VARCHAR(150) NOT NULL,
    message_id   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, message_id)
);
