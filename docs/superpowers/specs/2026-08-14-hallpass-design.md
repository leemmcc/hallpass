# Hallpass — Bathroom Pass Cooldown Timer

**Date:** 2026-08-14
**Status:** Approved design, ready for implementation planning

## Purpose

A wall-mounted Android tablet in a classroom that shows, at a glance, whether a student
may leave for the bathroom. Green means someone may go. Red means wait.

Red is a **cooldown between students**, not an "occupied" indicator. A student touches
the screen on returning; nobody else may leave until it turns green again. Whether the
tap happens on the way out or on the way back is a classroom convention, not an app
behavior — the app only implements "touch → red for N minutes → green".

The device is an old tablet running **Android 8**, mounted on a wall and kept plugged in.
It is operated by a teacher and surrounded by children, so it must be hard to exit and
hard to tamper with.

## Non-goals

Deliberately excluded to keep the first version small:

- No visible countdown or remaining-time display. Explicitly not wanted.
- No sound or vibration on state change. It is a classroom.
- No logging, history, or per-student tracking.
- No multiple simultaneous passes. One shared cooldown.
- No network features of any kind.

## Behavior

### States

Two states, and only two.

| State | Meaning | Appearance |
|---|---|---|
| GREEN | Idle — someone may go | `#2E7D32` fill, white ✓ centered |
| RED | Cooldown — nobody may go | `#C62828` fill, white ✕ centered |

### Transitions

| From | Event | To |
|---|---|---|
| GREEN | Tap anywhere (see touch routing) | RED; cooldown starts |
| RED | Tap anywhere | *Ignored* — no restart, no early end |
| RED | Cooldown elapses | GREEN |

Touches in RED do not affect the cooldown. A student cannot shorten the wait, and cannot
extend it by tapping. This was chosen over a teacher long-press override for
predictability; the cooldown always runs exactly as long as configured.

### Touch routing

The tap-to-start gesture and the corner long-press share the same screen, so precedence
must be explicit:

| Where | Held | GREEN | RED |
|---|---|---|---|
| Bottom-left corner target | ≥ 3s | Open settings; **cooldown does not start** | Open settings; cooldown continues |
| Bottom-left corner target | < 3s | Start cooldown | Ignored |
| Anywhere else | any | Start cooldown | Ignored |

Two consequences worth stating plainly:

- **The corner long-press works in both states.** Settings must be reachable during a
  cooldown; otherwise the teacher would have to wait out the timer to change the timer.
- **A completed long-press never starts a cooldown.** The gesture is consumed. Otherwise
  every trip to settings would silently lock out the next student.

A short tap in the corner is treated as an ordinary tap, so there is no dead zone on the
screen and no behavior a student could discover by poking at the corner.

### Appearance

- Colors are saturated enough to read from the back of a room, but avoid pure `#FF0000`,
  which is unpleasant as a full-screen fixture running all day. Defined as two constants
  and easy to change.
- The ✓ and ✕ are drawn as strokes on a `Canvas`, not as text glyphs, so rendering does
  not depend on the device font having those characters.
- Symbol size: 40% of the screen's shorter dimension, centered, white.
- The symbols exist for **colorblindness**. Red/green is the worst possible pair — roughly
  1 in 12 boys has some red-green deficiency, so a class of 25 likely contains a student
  who cannot reliably distinguish the two states by color alone. The shape carries the
  meaning independently of the color.

### Cooldown duration

Default **5 minutes**. Configurable **1–60** minutes in whole-minute steps. Values outside
the range are clamped rather than rejected.

### Settings access

Press and hold the **bottom-left corner** for **3 seconds**, then enter a **4-digit PIN**.
The target is invisible and occupies 15% of the screen's shorter dimension in each
direction from the corner.

Default PIN is `1234`, changeable in settings. The PIN exists so that a student who
watches the teacher perform the gesture still cannot get in.

### Settings screen

Contains exactly four things:

- Cooldown duration in minutes
- Change PIN
- "Pin app to screen" button (invokes Android screen pinning)
- "Auto-pin on launch" toggle

### Always-on display

The app holds `FLAG_KEEP_SCREEN_ON` and runs in immersive full-screen mode, hiding the
status and navigation bars.

**This assumes the tablet is permanently plugged in.** An Android 8 device with the screen
never sleeping will exhaust its battery in a few hours.

### Lockdown

Android **screen pinning** (lock task mode), which is available without device-owner
privileges on API 26. Exiting requires holding Back + Overview together, then the device
lock PIN.

The app calls `startLockTask()` itself, so re-pinning after a reboot is one tap rather than
a trip through system settings. With "auto-pin on launch" enabled, the app requests pinning
on start and the user only confirms the system dialog.

Rejected alternative: registering the app as the device's Home launcher. Stronger (survives
reboot with no interaction) but takes over the tablet entirely. Not wanted.

### Surviving restarts

The cooldown is persisted as an **end timestamp**, not as a counting-down value. On launch
the app compares the current time against the stored timestamp. A reboot two minutes into a
five-minute cooldown resumes with three minutes remaining, rather than resetting and handing
the next student a free pass.

**Accepted trade-off:** this reads the wall clock, so a clock jump (timezone change, NTP
correction) could end an in-flight cooldown early or late. For a five-minute timer in a
classroom, reboot resilience is worth more than immunity to clock changes.

## Architecture

Plain Android Views and Kotlin. One activity for the display, two small ones for settings.
No Jetpack Compose, no ViewModel, no DataStore, no third-party dependencies beyond AndroidX
core.

The rationale: the entire visual requirement is filling the screen with one of two colors
and a symbol. Compose would add roughly 2.5 MB of runtime and a measurable cold-start cost
on eight-year-old hardware — the exact device class being targeted — to render a rectangle.
If the app later grows real screens, migrating is a contained rewrite of one file.

Also rejected: a foreground service owning the timer. It solves a problem that does not
exist here — the app is screen-pinned, plugged in, and holding the screen awake, so it is
never backgrounded. Persisting an end timestamp already survives process death *and* reboot,
which a service does not.

### Files

| File | Responsibility | Android deps |
|---|---|---|
| `PassState.kt` | `stateAt(now, cooldownEnd)` → GREEN/RED. The whole state machine. | none |
| `CornerTarget.kt` | Given screen size and a touch point, is this in the settings corner? | none |
| `TouchRouter.kt` | `route(inCorner, heldMillis, state)` → START_COOLDOWN / OPEN_SETTINGS / IGNORE | none |
| `Settings.kt` | SharedPreferences wrapper: duration (clamped), PIN, auto-pin flag, cooldown end timestamp | thin |
| `PassView.kt` | Custom View: fills with the color, draws the ✓/✕ | yes |
| `MainActivity.kt` | Lifecycle, immersive mode, keep-screen-on, touch routing, expiry scheduling, lock task | yes |
| `PinActivity.kt` | 4-digit PIN entry | yes |
| `SettingsActivity.kt` | The settings form | yes |

The split exists so that the three files containing actual decision logic — `PassState`,
`CornerTarget`, and `TouchRouter` — are plain Kotlin with no Android imports and run as fast
JVM unit tests. Everything below them is plumbing verified by hand on the device.

`MainActivity` measures how long a touch was held and where; it does not decide what that
means. That keeps the gesture rules — the part with genuine edge cases — out of the
lifecycle code and under test.

### Configuration

- `minSdk` 26, `targetSdk` 35
- Application ID: `io.github.leemmcc.hallpass`
- Kotlin, Gradle, Android Gradle Plugin

## Testing

### Automated (JVM unit tests, run in CI)

- `stateAt` before, exactly at, and after the end timestamp. The boundary matters: at
  exactly the end time the state is GREEN.
- A missing or already-past timestamp reads as GREEN.
- Duration clamping: `0 → 1`, `999 → 60`, in-range values unchanged.
- PIN validation: correct, incorrect, wrong length, non-numeric.
- Corner hit-testing across several screen sizes and both orientations, including points
  just inside and just outside the target.
- Touch routing, every cell of the table above: corner long-press in GREEN opens settings
  *without* starting a cooldown; corner long-press in RED opens settings without disturbing
  the running cooldown; short corner tap in GREEN starts a cooldown; any tap in RED is inert.

### Manual (on the device)

- Immersive mode actually hides the system bars
- Screen pinning engages and resists exit
- Screen stays awake indefinitely while plugged in
- ✓ and ✕ are legible from the back of a classroom
- Colors are distinguishable in real classroom lighting

No emulator and no instrumented tests. Not worth the setup at this size.

## Build and distribution

No local Android toolchain. All builds happen in GitHub Actions.

### Signing

A release keystore stored in GitHub repository secrets. Stable signing matters because
Android only installs an update over an existing app when both are signed with the same
key; a changed key means uninstall/reinstall and losing settings.

### Keystore bootstrap (one time)

1. Create the repo **private**.
2. A throwaway `workflow_dispatch` workflow runs `keytool` on the runner, base64-encodes
   the keystore, and uploads it as an artifact.
3. Fetch it with `gh run download`; store it via `gh secret set`.
4. Delete the artifact and the bootstrap workflow, then make the repo public.

The private-first ordering is deliberate: on a public repo, workflow artifacts are
downloadable by any authenticated GitHub user, so generating the keystore in the open
would expose it for as long as the artifact exists.

### Release flow

Push a tag (`v0.1.0`) → CI runs unit tests → builds and signs a release APK → attaches it
to a GitHub Release.

The tablet installs by opening the releases page and tapping the APK. This requires the
repo to be **public**, because Release assets on a private repo need an authenticated
GitHub session, which defeats the point. Optionally, the Obtainium app can watch the repo
and turn updates into a single tap.

## Accepted constraints

- **No local test execution.** With no Android SDK on the development machine, unit tests
  can only be run by pushing to CI and reading the result. This is a slower and less certain
  loop than running them locally, and it means correctness claims rest on CI output rather
  than direct local verification. Accepted deliberately to keep the machine clean; revisit
  if it becomes painful.
- **Repo is public**, so a bathroom-timer project is visible on the internet. This is the
  price of frictionless installs on the tablet.
- **Tablet must stay plugged in.**
- **Screen pinning must be re-engaged after a reboot**, reduced to one confirmation tap by
  the auto-pin setting.

## Deferred ideas

Not in scope now; recorded so they are not rediscovered as novel:

- Teacher long-press override to end a cooldown early
- Configurable colors
- Separate durations for different times of day
- Any indication of time remaining
