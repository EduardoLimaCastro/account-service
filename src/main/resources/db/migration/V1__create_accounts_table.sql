CREATE SEQUENCE account_number_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 99999
    NO CYCLE;

CREATE TABLE accounts (
    id              UUID            NOT NULL,
    owner_id        VARCHAR(255)    NOT NULL,
    account_number  CHAR(5)         NOT NULL DEFAULT LPAD(NEXTVAL('account_number_seq')::TEXT, 5, '0'),
    account_digit   CHAR(1)         NOT NULL,
    agency_id       VARCHAR(255)    NOT NULL,
    account_status  VARCHAR(20)     NOT NULL,
    balance         NUMERIC(19, 4)  NOT NULL,
    overdraft_limit NUMERIC(19, 4)  NOT NULL,
    transfer_limit  NUMERIC(19, 4)  NOT NULL,
    account_type    VARCHAR(20)     NOT NULL,
    fraud_blocked   BOOLEAN         NOT NULL DEFAULT FALSE,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL,

    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number)
);
