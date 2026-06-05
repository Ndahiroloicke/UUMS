-- Bills with payments but money still owed should be PARTIALLY_PAID, not APPROVED
UPDATE bills
SET status = 'PARTIALLY_PAID'
WHERE status = 'APPROVED'
  AND amount_paid > 0
  AND outstanding_balance > 0;
