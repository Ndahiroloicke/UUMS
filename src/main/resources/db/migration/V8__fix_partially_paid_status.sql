-- ============================================================
-- V8: Fix bill status for partially paid bills (data correction)
-- Flyway migration: updates bills incorrectly marked APPROVED when
-- amount_paid > 0 but outstanding_balance > 0 to PARTIALLY_PAID.
-- ============================================================

-- Bills with payments but money still owed should be PARTIALLY_PAID, not APPROVED
UPDATE bills
SET status = 'PARTIALLY_PAID'
WHERE status = 'APPROVED'
  AND amount_paid > 0
  AND outstanding_balance > 0;
