# Sleep inference · v2

I estimate whether someone is awake from phone use, Google Sleep API readings, and their bedtime. The API still returns `AWAKE`, `SLEEPING`, or `UNKNOWN`; confidence is a rule-based score, not measured accuracy.

An `AWAKE` estimate means recent phone use, not that someone is fully awake or ready for a call. Communication availability stays user-controlled.

## Awake comes first

An unlock within **5 minutes** means `AWAKE` (**0.98**), even without a heartbeat. Within **15 minutes**, it also counts if the screen is on or there were two unlocks in the last 30 minutes (**0.90**). Screen-on or motion alone doesn't prove wakefulness, but blocks sleep.

## When I estimate sleep

- **Google:** confidence **85+**, latest reading within **30 minutes**, and no unlock for **45 minutes**. The screen must be off for 45 minutes or its state unknown. I smooth up to three readings from the last hour, at least five minutes apart: median for three, average for two. Duplicate timestamps count once.
- **Their bedtime:** screen off and no unlock for **60 minutes**, with no activity in the last **30 minutes**. MOTION is optional. Base confidence is **0.68**.
- **No bedtime set:** a weaker estimate from **00:00–07:00** in the user's time zone, after **90 minutes** of inactivity (**0.60**). An explicitly disabled schedule disables this fallback too.
- **Outside bedtime:** the stricter fallback needs **120 minutes** of screen-off and known motion inactivity, plus a heartbeat within **60 minutes** (**0.65**).

A missing unlock history doesn't block sleep when other evidence exists. Charging for **30 minutes** adds a little confidence; it never decides sleep on its own. Scheduled estimates lose confidence when heartbeat is older than **60 minutes**. Without any real phone observation in the last **8 hours**, passive estimates stop.

## Avoiding flicker

A low Google estimate blocks new sleep. A single conflicting reading can retain an existing sleep estimate; **two low readings (20 or below)**, recent motion, sustained screen-on, or an unlock clear it. A locked screen lighting up briefly (up to **2 minutes**) can retain previous sleep; screen-off afterwards does not immediately erase that sleep either. Otherwise, previous sleep can survive uncertainty for up to **90 minutes**, with confidence capped at **0.60**. Retention never advances its original calculation time.

New telemetry and the **15-minute** scheduler recalculate the result. Stored awake/unknown expires after **20 minutes**; sleeping expires after **90 minutes**. A fresh `SLEEPING → AWAKE` transition triggers wake notifications.

Events use occurrence time, not arrival time. Future-at-receipt events stay invalid; screen and charging history expires after **12 hours**. Unknown still means insufficient or conflicting evidence. A phone left on a table can resemble sleep, and delayed delivery can delay wake detection.

The approach combines [Google's classification guidance](https://developers.google.com/location-context/sleep) with phone-use gaps, as explored in [iSenseSleep](https://mhealth.jmir.org/2019/5/e11930). These thresholds are project choices, not that study's validated algorithm. The longer heartbeat tolerance accounts for [Android Doze](https://developer.android.com/training/monitoring-device-state/doze-standby).
