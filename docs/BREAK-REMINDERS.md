# Explorer break reminders

The break interval now persists per explorer in the existing local `play_clock`
preferences. Restarting the app no longer forgets an accrued interval, and changing
explorers no longer carries one explorer's reminder into another's session. The
active explorer's reminder is restored as soon as their profile loads; it does not
wait for the next tick. Changing their configured interval recalculates the state.

The interval uses the same monotonic elapsed time and five-second scheduler-gap cap
as the daily allowance. Parent-area time, background time and paused time are
excluded by resetting the elapsed origin when play resumes. Existing behavior is
preserved: Continue or Back to Arcade acknowledges a pause/reminder and starts a
new break interval. This is a reminder, not an enforced minimum rest duration.
Acknowledgement does not reset the daily allowance, lifetime usage or parent bonus.
A zero-minute break setting disables reminders. A later calendar day resets daily
usage, bonus and the break interval; a backwards calendar change resets none of
these. Bedtime restrictions are unchanged. Deleting an explorer removes their
break interval with their other clock records.

Upgrading preserves existing daily/lifetime/bonus records. Because earlier versions
did not save break intervals, the first upgraded interval starts at zero; it cannot
recover already lost historical time. No Room migration, permission, network call,
new personal-data category, production flag or content change is introduced.

`PlayClockBreakTest` covers recovery, isolation, acknowledgement, limits, disabled
reminders, date boundaries, elapsed-gap handling, upgrades and erasure. Phone and
tablet Compose tests cover immediate recovered reminders, Continue, and active
explorer changes in both offline and Amazon-preview variants. The active-explorer
test changes the synthetic DAO flow; it is not physical profile-switch navigation.
`PlayClockDeviceTest` uses real SharedPreferences in the separate offline Voice QA
installation, generated keys and injected elapsed/date/hour sources. It never opens
profiles or PINs, changes the tablet clock, or waits through a real five-minute
interval. Generated keys are removed in a finally block.

See `release/break-reminder-software-2026-10-24.json` and
`release/break-reminder-device-2026-10-24.json` for actual results and hashes.
Physical full-app switching/restart, audible reminder and final-candidate acceptance
remain pending. These software checks do not approve the production release.

Both variants retain zero lint errors. Two additional warnings per variant concern
the new `SharedPreferences.edit` call and synchronous `commit`; immediate storage
of break acknowledgement is intentional, consistent with existing clock writes.
No lint warning is suppressed.
