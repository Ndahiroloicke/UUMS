-- ============================================================
-- V7: Reconcile bill notification triggers with application layer
-- Flyway migration: keeps notification trigger functions defined but drops
-- bill-generation and bill-paid notification triggers (handled in Java).
-- Reasserts trg_set_bill_paid trigger that sets status PAID when balance = 0.
-- ============================================================

-- DB routines for SRS compliance.
-- In-app + email notifications are created by BillService and PaymentService (reliable via JPA).
-- DB trigger below enforces bill status = PAID when outstanding_balance reaches zero.

-- Keep notification functions defined for documentation / direct SQL use
CREATE OR REPLACE FUNCTION fn_notify_on_bill_generation()
RETURNS TRIGGER AS $$
DECLARE
    v_customer_name VARCHAR(255);
    v_period_label  TEXT;
    v_message       TEXT;
BEGIN
    SELECT full_names INTO v_customer_name
    FROM customers WHERE id = NEW.customer_id;

    v_period_label := TO_CHAR(NEW.billing_period, 'FMMonth YYYY');

    v_message := 'Dear ' || v_customer_name || ', Your ' || v_period_label ||
                 ' utility bill of ' || NEW.total_amount || ' FRW has been successfully processed.';

    INSERT INTO notifications (customer_id, message, is_read, created_at)
    VALUES (NEW.customer_id, v_message, FALSE, NOW());

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_notify_on_bill_paid()
RETURNS TRIGGER AS $$
DECLARE
    v_customer_name VARCHAR(255);
    v_period_label  TEXT;
    v_message       TEXT;
BEGIN
    IF NEW.status = 'PAID' AND OLD.status IS DISTINCT FROM 'PAID' THEN
        SELECT full_names INTO v_customer_name
        FROM customers WHERE id = NEW.customer_id;

        v_period_label := TO_CHAR(NEW.billing_period, 'FMMonth YYYY');

        v_message := 'Dear ' || v_customer_name || ', Your ' || v_period_label ||
                     ' utility bill of ' || NEW.total_amount ||
                     ' FRW has been fully paid. Thank you!';

        INSERT INTO notifications (customer_id, message, is_read, created_at)
        VALUES (NEW.customer_id, v_message, FALSE, NOW());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Disable notification triggers to avoid duplicates (application layer handles notifications)
DROP TRIGGER IF EXISTS trg_notify_on_bill_generation ON bills;
DROP TRIGGER IF EXISTS trg_notify_on_bill_paid ON bills;

-- Enforce PAID status at database level when balance reaches zero
CREATE OR REPLACE FUNCTION fn_set_bill_paid_on_zero_balance()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.outstanding_balance <= 0 AND OLD.outstanding_balance > 0 THEN
        NEW.status := 'PAID';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_set_bill_paid ON bills;
CREATE TRIGGER trg_set_bill_paid
    BEFORE UPDATE OF outstanding_balance ON bills
    FOR EACH ROW EXECUTE FUNCTION fn_set_bill_paid_on_zero_balance();
