-- ============================================================
-- V4: Add payment outstanding balance snapshot column
-- Flyway migration: adds outstanding_balance_after_payment to payments table
-- so each payment record stores the remaining bill balance at payment time.
-- Backfills existing rows via window function, then enforces NOT NULL.
-- ============================================================

-- Store outstanding balance snapshot at the time each payment was recorded
ALTER TABLE payments
    ADD COLUMN outstanding_balance_after_payment DECIMAL(12, 2);

-- Backfill existing payments: balance remaining after each payment (chronological per bill)
WITH payment_running AS (
    SELECT
        p.id,
        b.total_amount - SUM(p.amount_paid) OVER (
            PARTITION BY p.bill_id
            ORDER BY p.payment_date, p.id
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS balance_after_payment
    FROM payments p
    JOIN bills b ON b.id = p.bill_id
)
UPDATE payments p
SET outstanding_balance_after_payment = pr.balance_after_payment
FROM payment_running pr
WHERE p.id = pr.id;

ALTER TABLE payments
    ALTER COLUMN outstanding_balance_after_payment SET NOT NULL;
