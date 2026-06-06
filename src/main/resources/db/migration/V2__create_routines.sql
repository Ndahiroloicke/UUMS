-- ============================================================
-- V2: WASAC/REG — Database Routines (Triggers, Procedures, Cursor)
-- Flyway migration: adds PostgreSQL triggers and stored procedures.
-- Triggers: notify on new bill, auto-set PAID when balance hits zero,
-- notify on full payment. Procedure apply_overdue_penalties() finds
-- overdue unpaid bills and applies active penalty rates.
-- ============================================================

-- ----------------------------------------------------------------
-- TRIGGER 1: AFTER INSERT on bills
-- Inserts a notification record whenever a new bill is generated.
-- ----------------------------------------------------------------
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

CREATE TRIGGER trg_notify_on_bill_generation
AFTER INSERT ON bills
FOR EACH ROW EXECUTE FUNCTION fn_notify_on_bill_generation();


-- ----------------------------------------------------------------
-- TRIGGER 2: BEFORE UPDATE on bills (outstanding_balance)
-- Automatically sets status = 'PAID' when the balance reaches 0.
-- ----------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_set_bill_paid_on_zero_balance()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.outstanding_balance <= 0 AND OLD.outstanding_balance > 0 THEN
        NEW.status := 'PAID';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_bill_paid
BEFORE UPDATE OF outstanding_balance ON bills
FOR EACH ROW EXECUTE FUNCTION fn_set_bill_paid_on_zero_balance();


-- ----------------------------------------------------------------
-- TRIGGER 3: AFTER UPDATE on bills (status)
-- Inserts a payment-confirmed notification when status changes to PAID.
-- ----------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_notify_on_bill_paid()
RETURNS TRIGGER AS $$
DECLARE
    v_customer_name VARCHAR(255);
    v_period_label  TEXT;
    v_message       TEXT;
BEGIN
    IF NEW.status = 'PAID' AND OLD.status <> 'PAID' THEN
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

CREATE TRIGGER trg_notify_on_bill_paid
AFTER UPDATE OF status ON bills
FOR EACH ROW EXECUTE FUNCTION fn_notify_on_bill_paid();


-- ----------------------------------------------------------------
-- STORED PROCEDURE WITH CURSOR: apply_overdue_penalties()
-- Uses a cursor to iterate over all unpaid overdue bills and
-- applies the currently active late-payment penalty rate.
-- Called daily by the Spring scheduler (@Scheduled) or manually.
-- ----------------------------------------------------------------
CREATE OR REPLACE PROCEDURE apply_overdue_penalties()
LANGUAGE plpgsql
AS $$
DECLARE
    v_penalty_rate    DECIMAL(5,4);
    v_penalty_amount  DECIMAL(12,2);
    v_bill            RECORD;

    -- CURSOR: selects all non-paid bills past their due date
    overdue_bill_cursor CURSOR FOR
        SELECT id, outstanding_balance, total_amount, customer_id, billing_period
        FROM   bills
        WHERE  status NOT IN ('PAID')
          AND  due_date < CURRENT_DATE
          AND  outstanding_balance > 0;
BEGIN
    -- Fetch the latest active penalty rate
    SELECT rate INTO v_penalty_rate
    FROM   penalties
    WHERE  is_active = TRUE
    ORDER BY effective_date DESC
    LIMIT  1;

    IF v_penalty_rate IS NULL THEN
        RAISE NOTICE 'No active penalty rate found. Skipping overdue penalty application.';
        RETURN;
    END IF;

    OPEN overdue_bill_cursor;

    LOOP
        FETCH overdue_bill_cursor INTO v_bill;
        EXIT WHEN NOT FOUND;

        -- Calculate penalty on the outstanding balance
        v_penalty_amount := ROUND(v_bill.outstanding_balance * v_penalty_rate, 2);

        -- Apply penalty: update bill amounts and mark as OVERDUE
        UPDATE bills
        SET    penalty_amount       = penalty_amount + v_penalty_amount,
               total_amount        = total_amount + v_penalty_amount,
               outstanding_balance = outstanding_balance + v_penalty_amount,
               status              = 'OVERDUE',
               updated_at          = NOW()
        WHERE  id = v_bill.id;

        -- Insert an overdue notification
        INSERT INTO notifications (customer_id, message, is_read, created_at)
        VALUES (
            v_bill.customer_id,
            'Dear Customer, your ' || TO_CHAR(v_bill.billing_period, 'FMMonth YYYY') ||
            ' bill is OVERDUE. A late penalty of ' || v_penalty_amount ||
            ' FRW has been applied. Please pay immediately to avoid further charges.',
            FALSE,
            NOW()
        );

    END LOOP;

    CLOSE overdue_bill_cursor;

    RAISE NOTICE 'Overdue penalties applied successfully at %', NOW();
END;
$$;
