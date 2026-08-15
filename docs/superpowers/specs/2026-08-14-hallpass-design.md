# Hallpass — Bathroom Pass Cooldown Timer

**Date:** 2026-08-14
**Status:** Approved design, ready for implementation planning

## Purpose

A wall-mounted Android tablet in a classroom that shows, at a glance, whether a student
may leave for the bathroom, and how long the current student has been gone.

Three states cycle in a fixed order. A student taps once on the way **out** (green → yellow)
and once on the way **back** (yellow → red). Yellow means someone is currently out, and is
the only state that displays a timer — counting **up**, so the teacher can see at a glance
how long they have been gone. Red is a **cooldown between students**: nobody may leave until
it clears.

Both taps are real events the app understands, so "who taps and when" is enforced by the
app rather than left to classroom convention.

The device is an old tablet running **Android 7 or older**, mounted on a wall and kept
plugged in. The original design assumed Android 8; the tablet turned out to be older, and
the app now targets `minSdk 21` (Android 5.0) so the exact version stops mattering.
It is operated by a teacher and surrounded by children, so it must be hard to exit and
hard to tamper with.

## Non-goals

Deliberately excluded to keep the first version small:

- **No countdown of time remaining** in RED. Yellow's counting-*up* elapsed timer is wanted;
  a red countdown is not, because it invites students to stand at the wall watching it.
- No sound or vibration on state change. It is a classroom.
- No logging, history, or per-student tracking. The yellow timer is a live glance, not a record.
- No multiple simultaneous passes. One student out at a time, one shared cooldown.
- No network features of any kind.

## Behavior

### States

Three states, cycling in one direction only.

| State | Meaning | Appearance |
|---|---|---|
| GREEN | Available — someone may go | `#2E7D32` fill, white ✓ centered |
| YELLOW | A student is out | `#F9A825` fill, near-black `#1A1A1A` counting-up timer centered |
| RED | Cooldown — nobody may go | `#C62828` fill, white ✕ centered |

### Transitions

| From | Event | To |
|---|---|---|
| GREEN | Tap anywhere (see touch routing) | YELLOW; elapsed timer starts from zero |
| YELLOW | Tap anywhere, ≥ 2s after entering YELLOW | RED; cooldown starts |
| YELLOW | Tap anywhere, < 2s after entering YELLOW | *Ignored* — see tap guard |
| RED | Tap anywhere | *Ignored* — no restart, no early end |
| RED | Cooldown elapses | GREEN |
| any | "Reset to green" in settings | GREEN; clears both timers |

Touches in RED do not affect the cooldown. A student cannot shorten the wait, and cannot
extend it by tapping. This was chosen over a teacher long-press override for
predictability; the cooldown always runs exactly as long as configured.

**Tap guard.** For the first 2 seconds of YELLOW, taps are ignored. The student who just
tapped to leave still has a finger on the screen, and a double-tap or a lingering press
would otherwise skip yellow entirely and start the cooldown as if they had already come
back. Two seconds is long enough to cover that and far short of any real trip.

**YELLOW has no duration.** It ends only when someone taps to return. If a student never
taps back — sent to the nurse, went somewhere else, simply forgot — yellow keeps counting
up indefinitely and nobody else may go. This is deliberate: a rising number on the wall is
the signal that something needs attention. The teacher clears it with "Reset to green" in
settings.

Rejected alternatives: auto-returning to green after a timeout (silently erases the one
piece of information worth noticing) and escalating the color past a threshold (extra
configuration for a case the rising number already communicates).

### Touch routing

The tap-to-start gesture and the corner long-press share the same screen, so precedence
must be explicit:

| Where | Held | GREEN | YELLOW | RED |
|---|---|---|---|---|
| Bottom-left corner target | ≥ 3s | Open settings; **state unchanged** | Open settings; state unchanged | Open settings; cooldown continues |
| Bottom-left corner target | < 3s | → YELLOW | → RED (after tap guard) | Ignored |
| Anywhere else | any | → YELLOW | → RED (after tap guard) | Ignored |

Two consequences worth stating plainly:

- **The corner long-press works in all three states.** Settings must be reachable at any
  time; otherwise the teacher would have to wait out a cooldown to change its length, and
  a stuck yellow could never be reset — which is the entire escape hatch.
- **A completed long-press never changes state.** The gesture is consumed. Otherwise every
  trip to settings would send a phantom student out of the room, or lock out the next one.

A short tap in the corner is treated as an ordinary tap, so there is no dead zone on the
screen and no behavior a student could discover by poking at the corner.

### Appearance

- Colors are saturated enough to read from the back of a room, but avoid pure `#FF0000`,
  which is unpleasant as a full-screen fixture running all day. Defined as three constants
  and easy to change.
- The ✓ and ✕ are drawn as strokes on a `Canvas`, not as text glyphs, so rendering does
  not depend on the device font having those characters.
- Symbol size: 40% of the screen's shorter dimension, centered. White on green and red;
  the yellow timer is dark-on-amber instead (see below).
- The symbols exist for **colorblindness**. Red/green is the worst possible pair — roughly
  1 in 12 boys has some red-green deficiency, so a class of 25 likely contains a student
  who cannot reliably distinguish the states by color alone. Amber and green are also a
  poor pair for the same viewers. Every state is therefore distinguishable by shape alone:
  ✓, digits, ✕. Yellow is the only state showing numbers, which is unmistakable at a glance
  regardless of color perception.

### The yellow timer

Counts **up** from the moment the student tapped to leave. Format `M:SS`, rolling past an
hour without wrapping (`72:15` rather than `12:15`), because a wrapped number would be
worse than useless in exactly the situation it matters.

Rendered in near-black `#1A1A1A` on amber. White on amber is a genuinely poor contrast pair
and would be hard to read across a classroom; dark-on-amber is the readable direction.

Digit height matches the ✓/✕ symbol size — 40% of the screen's shorter dimension — so all
three states carry the same visual weight from the back of the room. The display refreshes
once per second, and only while in YELLOW.

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

Contains exactly five things:

- **"Reset to green"** button, placed first and prominently — it is the one item needed
  urgently, mid-class, to clear a stuck yellow
- Cooldown duration in minutes
- Change PIN
- "Pin app to screen" button (invokes Android screen pinning)
- "Auto-pin on launch" toggle

Reset sits behind the long-press and PIN like everything else. That is deliberate — it
would be the single most attractive button for a student to press — but it does mean
clearing a stuck yellow takes a few seconds of gesture and PIN entry. If that proves too
slow in practice, a dedicated teacher gesture can be added later.

### Both settings screens close themselves

The PIN screen and the settings screen each finish on `onStop`, and after 60 seconds of no
interaction.

This is not tidiness. Without it, a teacher interrupted mid-change leaves an editable
"Change PIN" field on the wall at child height for the rest of the period. A student who
sets a PIN nobody knows locks her out permanently — recovery means exiting screen pinning
with the device lock PIN, then uninstalling and reinstalling, losing all settings.

`onStop` rather than `onPause`, specifically: the screen-pinning confirmation is a system
dialog that pauses but does not stop the activity beneath it. Using `onPause` would dismiss
the settings screen out from under its own confirmation prompt.

Both screens also hold `FLAG_KEEP_SCREEN_ON`. The main display holds it too, but without it
here the tablet's normal timeout blanks the wall while settings are open.

### Changing the PIN

Both the new-PIN and confirm-PIN fields are masked, and the change is only saved when the
two match and the format is valid.

Masking matters more here than on the entry screen. The stated reason for having a PIN at
all is that a student who watches the teacher perform the corner gesture still cannot get
in — and an unmasked field would display the new PIN, in large type, to the whole room at
the exact moment it is chosen. The confirmation field exists because a single mistyped digit
would otherwise silently set a PIN the teacher does not know, with the same lockout
consequence as above.

### Always-on display

The app holds `FLAG_KEEP_SCREEN_ON` and runs in immersive full-screen mode, hiding the
status and navigation bars.

**This assumes the tablet is permanently plugged in.** An old device with the screen
never sleeping will exhaust its battery in a few hours.

### Lockdown

Android **screen pinning** (lock task mode), which is available without device-owner
privileges since API 21. Exiting requires holding Back + Overview together, then the device
lock PIN.

The app calls `startLockTask()` itself, so re-pinning after a reboot is one tap rather than
a trip through system settings. With "auto-pin on launch" enabled, the app requests pinning
on start and the user only confirms the system dialog.

**Auto-pin defaults to on.** With it off, a fresh install runs unpinned: a swipe reveals the
navigation bar and Home exits the app, so the classroom loses its display until an adult
notices. Shipping locked-down by default is the right posture for this device; it remains
switchable in settings.

Pinning reports its outcome. Every other control on the settings screen shows a toast, so a
silent failure would read as success — the teacher taps "Pin app to screen", sees nothing,
and walks away from an unpinned tablet. Since pinning is the only thing standing between the
class and the rest of the device, both success and failure are announced.

Rejected alternative: registering the app as the device's Home launcher. Stronger (survives
reboot with no interaction) but takes over the tablet entirely. Not wanted.

### Surviving restarts

Two timestamps are persisted, and all state is derived from them:

- `outStartMillis` — when the current student left (set on GREEN → YELLOW)
- `cooldownEndMillis` — when the cooldown expires (set on YELLOW → RED)

State is a pure function of those two values and the current time:

```
if (cooldownEnd != null && now < cooldownEnd)  RED
else if (outStart != null)                     YELLOW
else                                           GREEN
```

Entering RED clears `outStart`, so an expiring cooldown falls through to GREEN rather than
back into YELLOW. Reset clears both.

Storing an end timestamp rather than a counting-down value means a reboot two minutes into a
five-minute cooldown resumes with three minutes remaining, rather than resetting and handing
the next student a free pass. The same applies to yellow: a tablet that reboots while a
student is out comes back showing the correct elapsed time, not zero.

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
| `PassState.kt` | `stateAt(now, outStart, cooldownEnd)` → GREEN/YELLOW/RED, plus `elapsedIn(now, outStart)`. The whole state machine. | none |
| `ElapsedFormat.kt` | `format(millis)` → `"M:SS"`, non-wrapping past an hour | none |
| `CornerTarget.kt` | Given screen size and a touch point, is this in the settings corner? | none |
| `TouchRouter.kt` | `route(inCorner, heldMillis, state, millisInState)` → GO_OUT / RETURN / OPEN_SETTINGS / IGNORE | none |
| `Settings.kt` | SharedPreferences wrapper: duration (clamped), PIN, auto-pin flag, the two timestamps | thin |
| `PassView.kt` | Custom View: fills with the color, draws the ✓/✕ or the timer digits | yes |
| `MainActivity.kt` | Lifecycle, immersive mode, keep-screen-on, touch routing, expiry scheduling, lock task | yes |
| `PinActivity.kt` | 4-digit PIN entry | yes |
| `SettingsActivity.kt` | The settings form | yes |

The split exists so that the four files containing actual decision logic — `PassState`,
`ElapsedFormat`, `CornerTarget`, and `TouchRouter` — are plain Kotlin with no Android imports
and run as fast JVM unit tests. Everything below them is plumbing verified by hand on the
device.

`MainActivity` measures how long a touch was held and where; it does not decide what that
means. That keeps the gesture rules — the part with genuine edge cases — out of the
lifecycle code and under test.

### Configuration

- `minSdk` 21, `targetSdk` 35. 21 is the floor because every activity uses `Theme.Material`.
  Going this low costs one API-23 guard around `lockTaskModeState` and a non-adaptive
  launcher icon in `res/mipmap/` for devices below API 26.
- Application ID: `io.github.leemmcc.hallpass`
- Kotlin, Gradle, Android Gradle Plugin

## Testing

### Automated (JVM unit tests, run in CI)

- `stateAt` for all three states, including the boundary: at exactly `cooldownEnd` the state
  is GREEN, not RED.
- Both timestamps null → GREEN. `outStart` set, `cooldownEnd` null → YELLOW.
- **RED outranks YELLOW** while the cooldown is live, and an expiring cooldown falls through
  to GREEN rather than reverting to YELLOW. This is the transition most likely to regress.
- `elapsedIn` returns time since `outStart`, and zero rather than a negative number if the
  clock has moved backwards.
- `ElapsedFormat`: zero, single-digit seconds pad (`0:07`), the minute rollover (`0:59` →
  `1:00`), and past an hour without wrapping (`72:15`).
- Duration clamping: `0 → 1`, `999 → 60`, in-range values unchanged.
- PIN validation: correct, incorrect, wrong length, non-numeric.
- Corner hit-testing across several screen sizes and both orientations, including points
  just inside and just outside the target.
- Touch routing, every cell of the table above: corner long-press in each of the three
  states opens settings and changes nothing else; a short corner tap behaves as an ordinary
  tap; taps in RED are inert.
- The tap guard: a tap 1s into YELLOW is ignored, a tap 3s in returns to RED.

### Manual (on the device)

- Immersive mode actually hides the system bars
- Screen pinning engages and resists exit
- Screen stays awake indefinitely while plugged in
- ✓, ✕, and the yellow timer digits are all legible from the back of a classroom
- **The three states carry equal visual weight.** The digits are fitted by measuring rendered
  glyph bounds, not by setting a font size — font `textSize` is the em size, so setting it to
  40% of the screen would render digits roughly 40% smaller than the ✓/✕. Check by eye that
  they match.
- **`72:15` in portrait does not clip or touch the screen edges.** The fitted size is clamped
  to 90% of the width, but the clamp is unverifiable without the device.
- The yellow timer ticks once per second and does not visibly stutter or drift
- Dark-on-amber contrast holds up under real classroom lighting, including glare
- Settings and the PIN screen both close themselves after 60 seconds untouched, and on leaving
- Changing the PIN requires a matching confirmation, and neither field shows the digits
- "Pin app to screen" reports success or failure rather than doing nothing visible
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

## Known limitations

Found by review, deliberately left unfixed. Recorded so they are not rediscovered as new.

- **The settings idle-timeout may not reset while typing.** The 60-second timer resets on
  `onUserInteraction()`, which many soft keyboards do not trigger per keystroke — they commit
  text without dispatching key events the activity sees. The fields are a 4-digit PIN and a
  1–2 digit number, so entry takes seconds against a 60-second timeout, and a genuine stall
  mid-entry closing the screen is the safe outcome.
- **A malformed release tag produces a bad version code.** `versionCodeFrom` yields `0` for a
  tag whose leading component is non-numeric (`v-nightly`), which is lower than the untagged
  default of `1`. Only reachable by hand-pushing a malformed tag past the `v*` filter. Worth a
  guard if tagging is ever automated.
- **The `-1L` "absent" sentinel** would collide with a real timestamp only on a device whose
  clock is set before 1970. Not reachable in practice.
- **Release signing is gated on one env var,** not all three. Fails loud rather than dangerous:
  with no signing config, AGP emits `app-release-unsigned.apk`, which does not match the
  workflow's upload glob, so a release would land with no asset rather than an unsigned one.
- **The signing key exists only as a GitHub secret** and cannot be read back. If the repo is
  deleted, the key is unrecoverable and future versions could only be installed by uninstalling
  first, losing settings.
- **A child tapping twice, two seconds apart, burns a full cooldown** with nobody having left
  the room. This follows directly from the tap guard's size and is expected behavior, not a
  malfunction — worth knowing before it gets reported as a bug.

## Deferred ideas

Not in scope now; recorded so they are not rediscovered as novel:

- Teacher long-press override to end a cooldown early
- A dedicated reset gesture, if going through settings proves too slow mid-class
- Visual escalation of yellow past a threshold (deeper orange, pulse)
- Configurable colors
- Separate durations for different times of day
- Any indication of time remaining in RED
