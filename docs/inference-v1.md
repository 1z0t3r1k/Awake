# Sleep inference · v1

I estimate sleep from phone activity, the user's schedule, and Google Sleep API data. The result is `AWAKE`, `SLEEPING`, or `UNKNOWN`. When the signals don't agree, I leave the state unknown.

## Start with fresh data

A heartbeat must be no older than **20 minutes**. Without it, the result is `UNKNOWN`, even if other activity exists.

Events are ordered by when they happened, not when they arrived. Future timestamps are ignored. Screen and charging states expire after **12 hours**; schedules use the user's time zone.

## Awake

- An unlock within **5 minutes** gives `AWAKE` with confidence **0.98**.
- An unlock within **15 minutes** also counts if the screen is on or there were at least two unlocks in the last **30 minutes**. Confidence is **0.90**.

Screen-on or motion alone doesn't prove the person is awake.

## Sleeping

The screen must have been off for at least **45 minutes**, with no unlock in that time and no motion in the last **30 minutes**. Then I look for one of two supporting signals:

- Google Sleep API reports confidence **85 or higher**, from the last **20 minutes**.
- The user is inside their sleep schedule, and the last known motion was more than **30 minutes** ago but no older than **12 hours**.

Without either, sleep needs a longer quiet period: at least **120 minutes** since screen-off, the last unlock, and the last known motion. That motion must still be within **12 hours**.

A fresh Google score of **20 or lower** blocks a sleep estimate. Charging for **30 minutes** slightly raises confidence, but never decides the state on its own.

## Updates and limits

New telemetry triggers recalculation; the scheduler also runs every **15 minutes**. A stored result older than **20 minutes** is returned as `UNKNOWN`. Wake notifications require a fresh `SLEEPING → AWAKE` transition.

These thresholds are practical choices. Confidence is a rule-based score, not measured accuracy, and leaving a phone untouched doesn't necessarily mean sleeping.

The Android client doesn't currently send `MOTION`, so schedule-only and long-inactivity sleep rules can't work yet; strong Google data can still support sleep. Delayed background work, missing events, or incorrect device time can also leave the state unknown.
