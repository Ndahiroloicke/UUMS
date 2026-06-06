package com.example.uums.enums;

/** Lifecycle states for a utility bill: pending approval through paid or overdue. */
public enum BillStatus {
    PENDING,
    APPROVED,
    PARTIALLY_PAID,
    PAID,
    OVERDUE
}
