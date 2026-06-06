-- ============================================================
-- V3: Default seed data — initial tariffs, tax, service charges
-- Flyway migration: inserts reference billing configuration for first run.
-- Seeds tier-based water/electricity tariffs, fixed service charges,
-- 18% VAT tax, and 5% late-payment penalty (30-day grace), effective 2025-01-01.
-- ============================================================

-- Default Water Tariff (tier-based)
INSERT INTO tariffs (name, meter_type, tariff_type, version, effective_date, is_active, created_at)
VALUES ('Standard Water Tariff v1', 'WATER', 'TIER_BASED', 1, '2025-01-01', TRUE, NOW());

INSERT INTO tariff_tiers (tariff_id, min_consumption, max_consumption, rate, tier_order)
VALUES
    ((SELECT id FROM tariffs WHERE name = 'Standard Water Tariff v1'), 0,    6,    150.00, 1),
    ((SELECT id FROM tariffs WHERE name = 'Standard Water Tariff v1'), 6,    20,   250.00, 2),
    ((SELECT id FROM tariffs WHERE name = 'Standard Water Tariff v1'), 20,   50,   350.00, 3),
    ((SELECT id FROM tariffs WHERE name = 'Standard Water Tariff v1'), 50,   NULL, 500.00, 4);

-- Default Electricity Tariff (tier-based)
INSERT INTO tariffs (name, meter_type, tariff_type, version, effective_date, is_active, created_at)
VALUES ('Standard Electricity Tariff v1', 'ELECTRICITY', 'TIER_BASED', 1, '2025-01-01', TRUE, NOW());

INSERT INTO tariff_tiers (tariff_id, min_consumption, max_consumption, rate, tier_order)
VALUES
    ((SELECT id FROM tariffs WHERE name = 'Standard Electricity Tariff v1'), 0,   15,   100.00, 1),
    ((SELECT id FROM tariffs WHERE name = 'Standard Electricity Tariff v1'), 15,  50,   200.00, 2),
    ((SELECT id FROM tariffs WHERE name = 'Standard Electricity Tariff v1'), 50,  100,  350.00, 3),
    ((SELECT id FROM tariffs WHERE name = 'Standard Electricity Tariff v1'), 100, NULL, 500.00, 4);

-- Default Service Charges
INSERT INTO service_charges (name, meter_type, amount, is_active, effective_date, created_at)
VALUES
    ('Water Fixed Service Charge',       'WATER',       500.00, TRUE, '2025-01-01', NOW()),
    ('Electricity Fixed Service Charge', 'ELECTRICITY', 300.00, TRUE, '2025-01-01', NOW());

-- Default VAT (18%)
INSERT INTO taxes (name, rate, is_active, effective_date, created_at)
VALUES ('VAT', 0.18, TRUE, '2025-01-01', NOW());

-- Default Late Payment Penalty (5%)
INSERT INTO penalties (name, rate, grace_period_days, is_active, effective_date, created_at)
VALUES ('Late Payment Penalty', 0.05, 30, TRUE, '2025-01-01', NOW());
