# Inference v1 policy and review — 2026-09-30

## Project findings and changes

- Features use one reference instant and one latest-per-type device-event snapshot. Screen/charging state and duration use the same edge. Occurrence order wins over delivery order; opposite edges at identical timestamps become UNKNOWN.
- Queries exclude timestamps later than now or later than server receipt. This also prevents a future event from becoming evidence after time passes. Window counts have an upper bound. Screen/charging edges older than 12 hours become unknown. Invalid timezone gives UNKNOWN schedule; midnight and local-time boundaries remain start-inclusive/end-exclusive.
- Missing/stale heartbeat (>20 whole minutes) means UNKNOWN. Direct unlock within 5 whole minutes means AWAKE; within 15 requires screen ON or repeated unlocks. Motion plus screen ON is insufficient to notify waking.
- Sleep requires screen OFF, no motion within 30 whole minutes and no contradictory recent activity counts. Schedule or fresh Google confidence >=85 can support sleep after 45 minutes since screen-off and unlock. Without either signal, the unscheduled fallback requires at least 120 minutes since screen-off, unlock and known last motion. Missing motion alone is not inactivity; motion older than 12 hours no longer proves current sensor coverage. Fresh Google confidence <=20 vetoes sleep but never proves waking.
- Google must have age 0..20 whole minutes and confidence 0..100. Confidence scores are uncalibrated policy weights; UNKNOWN is always 0. Schedule and charging only support a conclusion.
- All ingestion and recalculation paths serialize on the user's existing PostgreSQL row (FOR NO KEY UPDATE), before reading features. This also protects first creation of user_states. The old enum is captured before updating the managed state. Wake events are published only for a fresh SLEEPING -> AWAKE transition, after updating the state; the listener remains AFTER_COMMIT.
- Stored states expire after 20 minutes for API reads and transition eligibility. Repeated calculations update observation time, but unchanged states do not emit wake events. UNKNOWN -> AWAKE intentionally does not notify.
- All newly inserted device event types and nonempty batches trigger recalculation, including heartbeat and screen edges. A failure for one scheduled user no longer aborts every remaining user.
- Subscription consumption called after commit now uses REQUIRES_NEW so deletion actually commits. Removed its unused self-dependency and the calculation service's unused notification dependency.

## External evidence (not measurements of this project)

- Google SleepClassifyEvent reference, updated 2024-10-31, consulted 2026-09-30: https://developers.google.com/android/reference/com/google/android/gms/location/SleepClassifyEvent . Confidence is a sleep-oriented score 0..100; timestamp is UNIX epoch milliseconds; device motion/light have separate 1..6 scales. Android timestamp conversion is already correct. A classification is not a completed sleep segment. Typical reporting is about every 10 minutes, not a delivery guarantee.
- Ciman and Wac, “Smartphones as Sleep Duration Sensors: Validation of the iSenseSleep Algorithm”, JMIR mHealth and uHealth, 2019-05-21: https://mhealth.jmir.org/2019/5/e11930/ . Phone non-use can differ from actual bedtime/wake time, especially with infrequent use. This supports requiring several agreeing passive signals for the unscheduled fallback. It does not validate our thresholds.

## Deliberate v1 limits

45/120/30/20-minute thresholds, 12-hour state TTL, confidence cutoffs and output scores are explicit engineering policy, not scientific accuracy claims. The 120-minute fallback deliberately requires screen, unlock and motion evidence to agree and scores below scheduled/Google-supported sleep. Whole-minute ages round down, so freshness thresholds have up to one minute of slack.

The current Android client declares MOTION but has no producer, and screen broadcasts are registered in MainActivity. Missing edges, stopped collection, Doze-delayed heartbeat, clock skew and multiple devices per user remain limitations. Heartbeat proves connectivity, not continuous motion-sensor coverage. Missing motion still requires Google support to infer sleep, so a conservative v1 can spend substantial time UNKNOWN. No new mobile collection mechanism or schema is added.

Zero tolerance for future-at-receipt timestamps intentionally discards data from fast client clocks; already-stored invalid events remain stored but excluded from inference. Device events have IDs for ingestion deduplication; Google classification duplicates have no client event ID, but do not independently trigger repeated state-transition notifications.

The PostgreSQL lock prevents duplicate logical transitions among these flows, but the in-process AFTER_COMMIT event is not a durable queue. Process death after commit can lose notifications; subscription deletion commits before external push, so a failed push can lose delivery. Exactly-once external delivery requires a separate durable delivery design and is out of scope. More than one server instance may run the scheduler, but use the same DB lock.

No API/DTO/schema changes. Existing unrelated working-tree edits are preserved.
