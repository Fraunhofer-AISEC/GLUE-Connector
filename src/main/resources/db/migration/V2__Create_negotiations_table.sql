CREATE TABLE negotiations (
    id UUID PRIMARY KEY,
    role VARCHAR NOT NULL,
    peer VARCHAR,
    peer_pid VARCHAR NOT NULL,
    start_timestamp TIMESTAMP NOT NULL,
    state VARCHAR NOT NULL,
    callback_url VARCHAR NOT NULL,
    peer_url VARCHAR NOT NULL,
    contract JSON,
    failure_reason VARCHAR,
    last_received_message VARCHAR,
    next_sent_message INT,
);
