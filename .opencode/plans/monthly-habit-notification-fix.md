# Monthly Habit Notification Fix

## Changes needed in 3 files:

### 1. Habit.kt — add `nextOccurrenceFrom()`
Insert after `shouldShowToday()`:

```kotlin
fun Habit.nextOccurrenceFrom(today: LocalDate): LocalDate {
    var d = today
    while (true) {
        if (shouldShowToday(d)) return d
        d = LocalDate.fromEpochDays(d.toEpochDays() + 1)
    }
}
```

### 2. AlarmSchedulerImpl.kt — replace daily setRepeating with one-shot
Rewrite `schedule(habit)` to:
- Calculate next occurrence via `habit.nextOccurrenceFrom(LocalDate.now())`
- Combine target date + reminder time into exact trigger
- If trigger time is in the past, find next occurrence from tomorrow
- Call `alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pending)` (one-shot)
- Keep `schedule(task)` and `cancel()` methods unchanged

### 3. BootReceiver.kt — re-arm after notification
After `showNotification()`, get `AlarmScheduler` from Koin and call `scheduler.schedule(habit)` to schedule the next occurrence.

## Verification
- Run `./gradlew test` to verify existing tests pass
- Manually verify: create monthly habit on 17th at 7 AM, confirm notification fires on 17th only
