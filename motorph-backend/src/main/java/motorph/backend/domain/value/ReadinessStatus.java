package motorph.backend.domain.value;

/**
 * Represents an employee's attendance readiness for payroll generation
 * within a payroll period.
 */
public enum ReadinessStatus {

    /** Has at least one record in the period and every recorded day is complete. */
    READY,

    /** Has attendance records but at least one is missing time-in or time-out. */
    INCOMPLETE,

    /** Has no attendance records in the period. */
    NO_ATTENDANCE
}
