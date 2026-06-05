-- PostgreSQL PROCEDURE cannot be invoked via SELECT from JPA/Hibernate.
-- Convert to FUNCTION so it can be called with: SELECT apply_overdue_penalties()

-- Drop procedure from V2/V5 (handle both possible signatures)
DROP PROCEDURE IF EXISTS apply_overdue_penalties();
DROP PROCEDURE IF EXISTS apply_overdue_penalties(OUT integer);
DROP FUNCTION IF EXISTS apply_overdue_penalties();

CREATE OR REPLACE FUNCTION apply_overdue_penalties() RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_penalty_rate       DECIMAL(5,4);
    v_grace_period_days  INTEGER;
    v_penalty_amount     DECIMAL(12,2);
    v_bill               RECORD;
    v_bills_penalized    INTEGER := 0;
    overdue_bill_cursor  REFCURSOR;
BEGIN
    SELECT rate, grace_period_days
    INTO v_penalty_rate, v_grace_period_days
    FROM penalties
    WHERE is_active = TRUE
    ORDER BY effective_date DESC
    LIMIT 1;

    IF v_penalty_rate IS NULL THEN
        RAISE NOTICE 'No active penalty rate found. Skipping overdue penalty application.';
        RETURN 0;
    END IF;

    IF v_grace_period_days IS NULL THEN
        v_grace_period_days := 0;
    END IF;

    OPEN overdue_bill_cursor FOR
        SELECT id, outstanding_balance, total_amount, customer_id, billing_period
        FROM   bills
        WHERE  status NOT IN ('PAID')
          AND  outstanding_balance > 0
          AND  CURRENT_DATE > (due_date + make_interval(days => v_grace_period_days));

    LOOP
        FETCH overdue_bill_cursor INTO v_bill;
        EXIT WHEN NOT FOUND;

        v_penalty_amount := ROUND(v_bill.outstanding_balance * v_penalty_rate, 2);

        UPDATE bills
        SET    penalty_amount       = penalty_amount + v_penalty_amount,
               total_amount         = total_amount + v_penalty_amount,
               outstanding_balance  = outstanding_balance + v_penalty_amount,
               status               = 'OVERDUE',
               updated_at           = NOW()
        WHERE  id = v_bill.id;

        INSERT INTO notifications (customer_id, message, is_read, created_at)
        VALUES (
            v_bill.customer_id,
            'Dear Customer, your ' || TO_CHAR(v_bill.billing_period, 'FMMonth YYYY') ||
            ' bill is OVERDUE. A late penalty of ' || v_penalty_amount ||
            ' FRW has been applied. Please pay immediately to avoid further charges.',
            FALSE,
            NOW()
        );

        v_bills_penalized := v_bills_penalized + 1;
    END LOOP;

    CLOSE overdue_bill_cursor;

    RAISE NOTICE 'Overdue penalties applied to % bill(s) at %', v_bills_penalized, NOW();
    RETURN v_bills_penalized;
END;
$$;
