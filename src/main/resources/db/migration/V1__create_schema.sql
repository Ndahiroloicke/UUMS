-- ============================================================
-- V1: WASAC/REG Utility Billing System — Initial Schema
-- Flyway migration: creates all core PostgreSQL tables for UUMS.
-- Tables: users, customers, meters, meter_readings, tariffs/tiers,
-- service_charges, taxes, penalties, bills, payments, notifications.
-- Also adds indexes and constraints (e.g. one reading per meter per month).
-- ============================================================

-- Users table (system login accounts)
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL      PRIMARY KEY,
    full_names    VARCHAR(255)   NOT NULL,
    email         VARCHAR(255)   NOT NULL UNIQUE,
    phone_number  VARCHAR(20)    NOT NULL,
    password      VARCHAR(255)   NOT NULL,
    role          VARCHAR(50)    NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Customers table
CREATE TABLE IF NOT EXISTS customers (
    id            BIGSERIAL      PRIMARY KEY,
    full_names    VARCHAR(255)   NOT NULL,
    national_id   VARCHAR(50)    NOT NULL UNIQUE,
    email         VARCHAR(255)   NOT NULL UNIQUE,
    phone_number  VARCHAR(20)    NOT NULL,
    address       TEXT,
    status        VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    user_id       BIGINT         UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Meters table
CREATE TABLE IF NOT EXISTS meters (
    id                 BIGSERIAL    PRIMARY KEY,
    meter_number       VARCHAR(100) NOT NULL UNIQUE,
    meter_type         VARCHAR(20)  NOT NULL,
    installation_date  DATE         NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    customer_id        BIGINT       NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Meter Readings table
CREATE TABLE IF NOT EXISTS meter_readings (
    id               BIGSERIAL      PRIMARY KEY,
    meter_id         BIGINT         NOT NULL REFERENCES meters(id) ON DELETE RESTRICT,
    previous_reading DECIMAL(12,2)  NOT NULL,
    current_reading  DECIMAL(12,2)  NOT NULL,
    consumption      DECIMAL(12,2)  NOT NULL,
    reading_date     DATE           NOT NULL,
    captured_by_id   BIGINT         REFERENCES users(id),
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reading_positive CHECK (current_reading > previous_reading)
);

-- Unique index: one reading per meter per calendar month
CREATE UNIQUE INDEX idx_meter_reading_period
    ON meter_readings (meter_id, date_part('year', reading_date), date_part('month', reading_date));

-- Tariffs table (versioned)
CREATE TABLE IF NOT EXISTS tariffs (
    id             BIGSERIAL      PRIMARY KEY,
    name           VARCHAR(100)   NOT NULL,
    meter_type     VARCHAR(20)    NOT NULL,
    tariff_type    VARCHAR(20)    NOT NULL,
    flat_rate      DECIMAL(10,4),
    version        INTEGER        NOT NULL DEFAULT 1,
    effective_date DATE           NOT NULL,
    end_date       DATE,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_by_id  BIGINT         REFERENCES users(id),
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tariff Tiers table
CREATE TABLE IF NOT EXISTS tariff_tiers (
    id              BIGSERIAL      PRIMARY KEY,
    tariff_id       BIGINT         NOT NULL REFERENCES tariffs(id) ON DELETE CASCADE,
    min_consumption DECIMAL(12,2)  NOT NULL,
    max_consumption DECIMAL(12,2),
    rate            DECIMAL(10,4)  NOT NULL,
    tier_order      INTEGER        NOT NULL
);

-- Service Charges table
CREATE TABLE IF NOT EXISTS service_charges (
    id             BIGSERIAL      PRIMARY KEY,
    name           VARCHAR(100)   NOT NULL,
    meter_type     VARCHAR(20)    NOT NULL,
    amount         DECIMAL(10,2)  NOT NULL,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    effective_date DATE           NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Taxes table (e.g. VAT)
CREATE TABLE IF NOT EXISTS taxes (
    id             BIGSERIAL      PRIMARY KEY,
    name           VARCHAR(100)   NOT NULL,
    rate           DECIMAL(5,4)   NOT NULL,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    effective_date DATE           NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Penalties table (late payment)
CREATE TABLE IF NOT EXISTS penalties (
    id                BIGSERIAL      PRIMARY KEY,
    name              VARCHAR(100)   NOT NULL,
    rate              DECIMAL(5,4)   NOT NULL,
    grace_period_days INTEGER        NOT NULL DEFAULT 30,
    is_active         BOOLEAN        NOT NULL DEFAULT TRUE,
    effective_date    DATE           NOT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Bills table
CREATE TABLE IF NOT EXISTS bills (
    id                     BIGSERIAL      PRIMARY KEY,
    bill_reference         VARCHAR(100)   NOT NULL UNIQUE,
    customer_id            BIGINT         NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    meter_id               BIGINT         NOT NULL REFERENCES meters(id) ON DELETE RESTRICT,
    meter_reading_id       BIGINT         REFERENCES meter_readings(id),
    billing_period         DATE           NOT NULL,
    consumption            DECIMAL(12,2)  NOT NULL,
    consumption_amount     DECIMAL(12,2)  NOT NULL,
    service_charge_amount  DECIMAL(12,2)  NOT NULL DEFAULT 0,
    tax_amount             DECIMAL(12,2)  NOT NULL DEFAULT 0,
    penalty_amount         DECIMAL(12,2)  NOT NULL DEFAULT 0,
    total_amount           DECIMAL(12,2)  NOT NULL,
    amount_paid            DECIMAL(12,2)  NOT NULL DEFAULT 0,
    outstanding_balance    DECIMAL(12,2)  NOT NULL,
    status                 VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    due_date               DATE           NOT NULL,
    approved_by_id         BIGINT         REFERENCES users(id),
    approved_at            TIMESTAMP,
    created_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id                 BIGSERIAL      PRIMARY KEY,
    payment_reference  VARCHAR(100)   NOT NULL UNIQUE,
    bill_id            BIGINT         NOT NULL REFERENCES bills(id) ON DELETE RESTRICT,
    amount_paid        DECIMAL(12,2)  NOT NULL,
    payment_method     VARCHAR(50)    NOT NULL,
    payment_date       TIMESTAMP      NOT NULL,
    processed_by_id    BIGINT         REFERENCES users(id),
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id          BIGSERIAL   PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    message     TEXT        NOT NULL,
    is_read     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Useful indexes for performance
CREATE INDEX idx_bills_customer       ON bills (customer_id);
CREATE INDEX idx_bills_meter          ON bills (meter_id);
CREATE INDEX idx_bills_status         ON bills (status);
CREATE INDEX idx_payments_bill        ON payments (bill_id);
CREATE INDEX idx_notifications_cust   ON notifications (customer_id);
CREATE INDEX idx_meter_readings_meter ON meter_readings (meter_id);
