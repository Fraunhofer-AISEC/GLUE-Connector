CREATE TABLE contracts(
    id VARCHAR PRIMARY KEY,
    contract JSON,
);

CREATE TABLE lookup(
    target VARCHAR,
    action VARCHAR,
    contract VARCHAR,
    PRIMARY KEY (target, action, contract),
    FOREIGN KEY (contract) REFERENCES contracts(id),
);
