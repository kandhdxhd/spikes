CREATE TABLE FOO
(
    id    BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    key   VARCHAR NOT NULL UNIQUE,
    value BIGINT  NOT NULL
);
