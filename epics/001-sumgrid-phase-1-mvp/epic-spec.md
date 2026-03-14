# Epic Specification: SumGrid Phase 1 MVP

**Epic ID**: 001-sumgrid-phase-1-mvp
**Epic Branch**: epic/001-sumgrid-phase-1-mvp
**Created**: 2026-03-14
**Status**: Active
**Target Launch**: End of May 2026
**Package**: org.dgeek.sumgrid

---

## Objective

Ship SumGrid v1.0 to the Google Play Store: a free, ad-free, offline-first daily number logic puzzle game for Android. Players fill an NxN grid so that every row and column adds up to its target sum. The app delivers the complete core loop — solve a daily puzzle, celebrate completion, share a spoiler-free result card, maintain a streak — entirely client-side with no backend infrastructure. The MVP must be polished enough to earn a 4.5-star store rating and validate the Day 7 retention hypothesis (>15%) within 60 days of launch.

---

## Background

The puzzle game market has a gap: no mobile-native, ad-free daily number logic puzzle with Wordle-style social sharing. Sudoku apps are ad-heavy and complex. Sumplete (the closest mechanic peer) is web-first and lacks native polish, streaks, and offline support. SumGrid fills this gap by combining six differentiators no single competitor matches: daily scarcity + viral sharing + zero monetization pressure + offline-first + 5 MB APK + dgeek ecosystem integration.

The North Star metric for Phase 1 is **Day 7 Retention Rate**. If players come back after a week, the daily puzzle and streak mechanic is working. Phases 2 through 4 are gated on Phase 1 meeting its retention targets.

Three target user personas:
- **"The Commuter"** (Aisha, Cairo) — needs offline, low data, short sessions on a mid-range Android
- **"The Streak Keeper"** (David, Abidjan) — wants a number puzzle to pair with Wordle, values reliability
- **"The Casual Challenger"** (Priya, Addis Ababa) — budget device, limited storage, wants something modern and shareable

---

## Involved Subsystems

- [x] Puzzle Engine (pure Kotlin, JVM-testable, no Android deps)
- [x] Grid UI (Jetpack Compose Canvas)
- [x] Daily Puzzle System (date-seeded, WorkManager precompute)
- [x] Persistence Layer (Jetpack DataStore)
- [x] Analytics and Crash Reporting (Firebase)
- [x] Onboarding Flow (FTUE navigation)
- [x] Play Store Assets and Listing

---

## Sprint Breakdown

The epic is structured as three sequential sprints. S02 cannot start until S01's 10,000-puzzle test suite passes. S03 cannot start until S01 and S02 are both passing.

| Sprint | Name | Est. Days | Dependency | Features |
|--------|------|-----------|------------|---------|
| S01 | Core Engine and Grid UI | 10 | None (critical path) | 9 |
| S02 | Daily Puzzle Loop | 11 | S01 complete | 11 |
| S03 | Polish, Firebase, Onboarding, Play Store | 11 | S01 + S02 complete | 12 |
| **Total** | | **32 dev-days** | | **32 features** |

---

## User Stories

### US-001: Solve the Daily Puzzle

**Given** I open SumGrid on any day after completing onboarding,
**When** I tap a difficulty (Beginner, Easy, or Medium),
**Then** I am shown today's puzzle for that difficulty — the same puzzle that every other player worldwide sees on this day — with empty cells I can fill in.

**Acceptance Criteria:**
- Today's puzzle is determined by `LocalDate.now(ZoneId.systemDefault()).toEpochDay()` cast to Long, used as the xorshift128 seed
- Difficulty seed offsets: Beginner = seed+0, Easy = seed+1, Medium = seed+2
- The same seed and difficulty always produce the same puzzle on any Android device (API 24 through API 35)
- Beginner puzzle: 3x3 grid, numbers 1-5, 5 pre-filled cells, 4 empty cells
- Easy puzzle: 4x4 grid, numbers 1-7, 8 pre-filled cells, 8 empty cells
- Medium puzzle: 5x5 grid, numbers 1-9, 10 pre-filled cells, 15 empty cells
- Every generated puzzle has exactly one valid solution (verified by constraint propagation validator)

### US-002: Fill Cells with the Number Pad

**Given** I am viewing a puzzle with empty cells,
**When** I tap an empty cell and then tap a number on the bottom number pad,
**Then** that number appears in the cell immediately, and sum indicators update in real time.

**Acceptance Criteria:**
- Tapping an empty cell selects it (shows highlight border); tapping it again deselects it
- Tapping a pre-filled (given) cell does nothing
- The number pad shows digits 1 through 9 and a clear button
- Tapping a number fills the selected cell; tapping the same number a second time clears the cell
- The clear button removes the current value from the selected cell
- Tapping a number when no cell is selected does nothing
- Sum indicators update on every cell value change without debounce
- Row indicator: green when sum equals target, red when sum exceeds target, gray when sum is below target
- Column indicators: same green/red/gray logic
- Sum indicators also show a checkmark icon (complete) or X icon (over-target) for colorblind accessibility
- The number pad is hidden once the puzzle is complete

### US-003: See the Puzzle Complete with Celebration

**Given** I have filled all empty cells so every row and column sum matches its target,
**When** the final cell is filled correctly,
**Then** a celebration animation plays, the timer stops, and the completion card appears showing my time and difficulty.

**Acceptance Criteria:**
- Completion is detected when all empty cells are filled AND every row sum equals its target AND every column sum equals its target
- A cell-by-cell cascade animation plays: cells scale up and turn brighter green in reading order with a 50ms stagger
- Total animation duration is under 1.5 seconds
- Animation maintains at least 30 fps on a device with 2 GB RAM
- Animation is interruptible — the player can tap the share button without waiting for it to finish
- The completion card shows: elapsed time in MM:SS, difficulty label, day number, and a Share button
- The timer started on the player's first cell tap and is hidden during play
- If the player backgrounds the app mid-puzzle and returns, the timer resumes from where it left off

### US-004: Share a Spoiler-Free Result Card

**Given** I have completed today's daily puzzle,
**When** I tap the Share button,
**Then** the Android native share sheet opens with a plain-text result card that shows my performance without revealing any numbers.

**Acceptance Criteria:**
- Share card format:
  ```
  SumGrid #[dayNumber] — [Difficulty] ⏱[MM:SS]
  [NxN grid of Unicode emoji squares]
  Play free -> sumgrid.dgeek.org
  ```
- Day number = days elapsed since January 1 2026 (day 1)
- Cell encoding: given cells = gray square (U+2B1C ⬜), user-solved cells = green square (U+1F7E9 🟩)
- No numbers appear anywhere in the share card
- Grid rows are separated by newline characters
- The share is delivered via `ACTION_SEND` Intent with `type = "text/plain"`
- The Android native share sheet opens (no custom share UI built in MVP)
- The share button is also available from the home screen for a previously completed puzzle

### US-005: Build and Maintain a Streak

**Given** I have completed at least one daily puzzle today,
**When** I complete another daily puzzle the following day,
**Then** my streak counter increments and my current streak is prominently displayed on the home screen.

**Acceptance Criteria:**
- Completing any difficulty on a given calendar day counts as that day's streak completion
- If `lastCompletionDate == yesterday`, streak increments by 1
- If `lastCompletionDate == today`, streak is unchanged (already counted)
- If `lastCompletionDate` is more than one day ago, streak resets to 1
- Streak and `longestStreak` persist across app restarts via Jetpack DataStore
- Milestone badges are awarded and persisted permanently (not lost if streak breaks):
  - 7 days: Weekly Warrior (🔥)
  - 30 days: Monthly Master (⭐)
  - 100 days: Century Solver (💎)
  - 365 days: Year of Logic (👑)
- The highest earned badge always displays on the home screen and completion card

### US-006: View the Home Screen Between Puzzles

**Given** I have opened SumGrid after completing onboarding,
**When** I view the home screen,
**Then** I can see today's puzzle status for each difficulty, my current streak, my highest milestone badge, and the countdown to tomorrow's puzzle.

**Acceptance Criteria:**
- Difficulty selector shows Beginner, Easy, and Medium with their grid sizes (3x3, 4x4, 5x5)
- Each difficulty shows either a "Play" CTA (not yet completed today) or a completed checkmark
- Tapping a completed difficulty shows the completion card, not a fresh puzzle
- Current streak count and highest earned badge are displayed prominently
- A live countdown to midnight (next puzzle) is shown, updating every second
- When all three difficulties are completed for the day, shows "All done! Next puzzle in HH:MM:SS"
- Last selected difficulty is remembered via DataStore

### US-007: Experience Onboarding on First Launch

**Given** I am launching SumGrid for the first time,
**When** the app opens,
**Then** I am shown a simple guided puzzle that teaches the mechanic by doing — no tutorial screens, no text walls.

**Acceptance Criteria:**
- First launch: app opens directly to a 3x3 onboarding puzzle with exactly 1 empty cell
- On completion of the 1-cell puzzle: brief celebration animation plays, then a single subtitle appears: "Come back tomorrow for a new puzzle." A "Got it" button dismisses and navigates to home screen
- Second launch: 3x3 onboarding puzzle with exactly 2 empty cells (no tooltip)
- Third launch: 3x3 onboarding puzzle with exactly 4 empty cells (no tooltip)
- Fourth launch and beyond: onboarding is marked complete, app goes directly to home screen
- Onboarding puzzles are hard-coded constants, not date-seeded
- Onboarding completions do not count toward the streak
- No splash screen, no permissions dialog, no account creation at any point in FTUE

### US-008: Use the App Offline

**Given** I have no network connectivity,
**When** I open SumGrid and play today's puzzle,
**Then** every feature of the core game works identically — puzzle generation, play, completion, streak, share card.

**Acceptance Criteria:**
- Puzzle generation is entirely client-side from the date seed; no network call is made for gameplay
- Streak state is read from and written to local DataStore only
- Share card generation requires no network
- Firebase Analytics events are queued locally when offline and sent when connectivity returns
- There is no error state, loading spinner, or degraded experience when offline
- The app runs fully on Android 7.0 (API 24) with no minimum device spec beyond the Android version

### US-009: Use the App in Dark Mode

**Given** my Android system is set to dark mode,
**When** I open SumGrid,
**Then** the app automatically renders in a dark theme that is easy on the eyes.

**Acceptance Criteria:**
- Dark mode is determined by `isSystemInDarkTheme()` — the app follows the system setting
- There is no in-app dark mode toggle in Phase 1
- All screens render correctly in both light and dark mode: grid, number pad, home screen, completion card
- No hardcoded color hex values anywhere in the codebase — all colors reference Material3 theme tokens
- The app switches between modes without requiring an app restart

### US-010: Receive an In-App Review Prompt

**Given** I have completed my third daily puzzle (lifetime total),
**When** the completion celebration finishes,
**Then** the Google Play In-App Review flow appears asking me to rate SumGrid.

**Acceptance Criteria:**
- Review prompt is triggered exactly once, after the third lifetime daily puzzle completion
- The trigger uses Google Play In-App Review API (`com.google.android.play:review`)
- If Play Store is not available (sideloaded APK), the exception is caught silently and the prompt is not shown
- The app does not show its own rating dialog or manipulate the review count
- A DataStore flag prevents the prompt from appearing more than once

---

## Functional Requirements

### FR-001: xorshift128 Pseudo-Random Number Generator
Custom xorshift128 PRNG implemented in pure Kotlin. Accepts a `Long` seed. Exposes `nextInt(bound: Int)`. Must produce byte-for-byte identical output on all Android versions (API 24 through API 35). Must not use `kotlin.random.Random` or `java.util.Random` — both have non-deterministic cross-version behavior. Must be portable to TypeScript in a future web version.

### FR-002: Puzzle Generator
`PuzzleGenerator` uses xorshift128 to fill an NxN grid (N = 3, 4, or 5) with numbers in `[1..maxVal]` where `maxVal` is 5 for 3x3, 7 for 4x4, 9 for 5x5. Calculates row and column target sums from the completed grid. Removes cells one at a time, calling `UniqueSolutionValidator` after each removal, until the target number of empty cells for the difficulty is reached. Retries with a seed offset if the generated puzzle does not match the target difficulty calibration.

### FR-003: Constraint Propagation Unique-Solution Validator
`UniqueSolutionValidator` implements constraint propagation with backtracking. Given a partially-filled grid and row/column targets, returns one of `NONE | UNIQUE | MULTIPLE`. Must handle 3x3, 4x4, and 5x5. Must complete in under 50 ms per call for a 5x5 grid (called after every cell removal during generation). Is the quality gate: no puzzle is shipped that has zero or multiple solutions.

### FR-004: Difficulty Calibrator
`DifficultyCalibrator` counts constraint propagation steps needed to solve a puzzle without backtracking. Maps step counts to difficulty tiers:
- Beginner: 3x3 grid, 4 empty cells
- Easy: 4x4 grid, 8 empty cells
- Medium: 5x5 grid, 15 empty cells

### FR-005: 10,000-Puzzle Test Suite
Parameterized JVM unit test generating 10,000 puzzles across all three difficulties (approximately 3,333 each). Runs `UniqueSolutionValidator` on every generated puzzle and asserts `UNIQUE` for all. Also runs determinism test: same seed + difficulty must produce the same puzzle. Must complete in under 60 seconds on CI. **No S02 work may begin until this test suite passes cleanly.**

### FR-006: Compose Canvas Grid Renderer
`GridRenderer` is a `@Composable` using `Canvas`. Draws the NxN grid filling available screen width. Cells visually distinguish: empty, pre-filled (given), user-filled, and selected (highlight border). Row sum targets appear to the right of each row; column sum targets appear below each column. Must render without clipping at 360dp and 420dp screen widths.

### FR-007: Tap-to-Select Cell Interaction
Tapping a user-fillable cell selects it (ViewModel-backed `StateFlow`). Tapping the selected cell again deselects it. Tapping a pre-filled cell does nothing. Selection state drives both the visual highlight and the number pad behavior.

### FR-008: Bottom Number Pad
`NumberPad` `@Composable` with buttons 1 through 9 and a clear button. Positioned in the bottom half of the screen within comfortable thumb reach on 360dp+ phones. Fills the selected cell on number tap; clears on same-number re-tap or clear button. No effect when no cell is selected. Hidden when the puzzle is complete.

### FR-009: Real-Time Sum Validation
Row and column sum indicators recalculate on every cell value change (no debounce). Color: green = sum equals target, red = sum exceeds target, gray = sum below target. Shape: checkmark icon when complete, X icon when over-target (colorblind support). Computed as a pure function in `PuzzleViewModel`.

### FR-010: Daily Puzzle Repository
`DailyPuzzleRepository` returns the deterministic puzzle for a given `LocalDate` and difficulty. Seed = `date.toEpochDay().toLong()` + difficulty offset (0/1/2). Persists today's completed state in DataStore. Handles timezone via `ZoneId.systemDefault()` — "today" means the device's local calendar day. Exposes `today()` and `tomorrow()`.

### FR-011: Background Puzzle Precompute
`WorkManager` `OneTimeWorkRequest` fires on app start if today's puzzle is not yet cached. Pre-generates today's and tomorrow's puzzles for all three difficulties and stores them in DataStore. Worker is idempotent. Ensures first puzzle tap is instant.

### FR-012: Puzzle Completion Detection
`PuzzleViewModel` exposes `isComplete: StateFlow<Boolean>`. Completion condition: all empty cells filled AND every row sum equals its target AND every column sum equals its target. On `false → true` transition, records completion timestamp and elapsed time in DataStore. Does not re-trigger if state is later modified.

### FR-013: Celebration Animation
On completion, a cell-by-cell cascade animation plays. Cells animate in reading order with a 50ms stagger. Each cell briefly scales up and turns brighter green. Total duration under 1.5 seconds. Built with Compose animation APIs only (no Lottie, no GIF). Runs at 30+ fps on 2 GB RAM devices. Interruptible — share button works during animation.

### FR-014: Elapsed Timer
Timer starts on the player's first cell tap. Hidden during play. Stops on completion. Displays as MM:SS on the completion card. Persisted in DataStore — backgrounding and returning resumes the timer.

### FR-015: Difficulty Selector UI
Home screen component showing Beginner (3x3), Easy (4x4), and Medium (5x5). Each tile shows today's completion status (checkmark if done, "Play" CTA if not). Tapping a completed difficulty shows the completion card. Last selected difficulty remembered via DataStore.

### FR-016: Share Card Generator
`ShareCardGenerator` produces a plain-text string:
```
SumGrid #[dayNumber] — [Difficulty] ⏱[MM:SS]
[NxN emoji grid — ⬜ for given cells, 🟩 for user-solved cells]
Play free -> sumgrid.dgeek.org
```
Day number is days since January 1 2026 (day 1). No numbers in the output.

### FR-017: One-Tap Share via Android Intent
Share button on the completion card and home screen (for previously completed puzzles) triggers `ACTION_SEND` Intent with `type = "text/plain"` and the share card text. Opens Android native share sheet.

### FR-018: Streak Repository
`StreakRepository` using `DataStore<Preferences>`. Tracks `currentStreak`, `lastCompletionDate` (as `LocalDate`), and `longestStreak`. Increment logic: yesterday → +1, today → no-op, older → reset to 1. Updates `longestStreak` if exceeded. Completing any difficulty counts for the day.

### FR-019: Streak Milestone Badges
`StreakBadgeProvider` maps streak milestones to badges (7/30/100/365 days). Badges are awarded permanently once earned and persisted in DataStore — breaking a streak does not remove a badge. Highest earned badge displays.

### FR-020: Home Screen
`HomeScreen` `@Composable` showing: app title, difficulty selector (FR-015), current streak with highest badge, countdown to midnight (updating every second). "All done! Next puzzle in HH:MM:SS" when all difficulties are complete for the day. Navigation to puzzle screen on difficulty tap.

### FR-021: Firebase Analytics
`AnalyticsTracker` interface wrapping Firebase Analytics. Initialized in `Application.onCreate()`. Events tracked from Day 1:
- `puzzle_started` (difficulty, date)
- `puzzle_completed` (difficulty, date, elapsed_seconds)
- `puzzle_abandoned` (difficulty, date, cells_filled)
- `share_tapped` (difficulty, date)
- `streak_milestone` (streak_days)
- `onboarding_completed` (attempt_number)
- `app_open` (daily)

Interface is no-op'd in unit tests.

### FR-022: Firebase Crashlytics
Crashlytics initialized in `Application.onCreate()`. Custom keys on each crash report: `current_difficulty`, `puzzle_day_number`, `app_version`. Debug-only crash test button (long-press app logo 5 times) to verify Crashlytics is receiving reports.

### FR-023: Onboarding Puzzle Sequence
`OnboardingRepository` (DataStore backed) tracks launch count and onboarding completion. Three curated hard-coded 3x3 puzzles (not date-seeded):
- Launch 1: 1 empty cell
- Launch 2: 2 empty cells
- Launch 3: 4 empty cells

From launch 4 onward: onboarding complete, daily system takes over. Onboarding completions do not count toward streak.

### FR-024: First-Time User Experience Navigation
On first launch: app opens directly to onboarding puzzle (no splash, no permissions, no account). On first completion: celebration + tooltip "Come back tomorrow for a new puzzle" + "Got it" button → home screen. Launches 2 and 3: direct to onboarding puzzle, no tooltip. Launch 4+: direct to home screen. Implemented in `AppNavigation.kt`.

### FR-025: In-App Review Prompt
After third lifetime daily puzzle completion, trigger Google Play In-App Review API once. Persist trigger flag in DataStore. Silent catch if Play Store unavailable (e.g., sideloaded APK). No custom rating dialog.

### FR-026: Play Store Listing and Assets
Five screenshots at 1080x1920px captured from Pixel 5 emulator. Feature graphic at 1024x500px. ASO-optimized short description (80 chars max). Long description with keywords: number puzzle, daily challenge, math game, logic puzzle, brain teaser, sudoku alternative, free, no ads, offline. App name: "SumGrid - Daily Number Puzzle". Package: `org.dgeek.sumgrid`. Submit to internal testing track.

---

## Non-Functional Requirements

### NFR-001: APK Size
Release APK must be under 5 MB with R8 full-mode shrinking enabled. CI must fail if the release APK exceeds 4.5 MB. Estimated target: 3.5–4.0 MB. No Lottie, no heavy image assets. Sound effects and ambient music are deferred to Phase 2.

### NFR-002: Offline-First
The entire core gameplay loop — puzzle generation, play, completion, share card, streak — works with no network connection. Firebase events queue locally when offline and sync when connectivity returns. No error state is shown to the user for network absence.

### NFR-003: Puzzle Correctness
Every puzzle delivered to a player must have exactly one valid solution. The `UniqueSolutionValidator` must be called after every cell removal during generation. The 10,000-puzzle test suite must pass with zero failures before any S02 work begins. An incorrect puzzle is a critical product failure.

### NFR-004: Determinism Across Devices
Given the same date and difficulty, every player worldwide must receive the same puzzle on the same day. The xorshift128 PRNG must produce identical output on all Android versions from API 24 to API 35. This must be verified with a cross-device determinism test as part of the test suite.

### NFR-005: Performance
- Validator per call: < 50 ms for 5x5 grid
- 10,000-puzzle test suite: < 60 seconds on CI
- First puzzle load: instant (WorkManager precompute ensures cache exists)
- Celebration animation: 30+ fps on 2 GB RAM device
- App startup to home screen: < 2 seconds cold start on a mid-range 2022 Android device

### NFR-006: Minimum Android Support
Min SDK: API 24 (Android 7.0) — covers 95%+ of active Android devices. Target SDK: API 35 (latest). All features must function correctly on API 24 without using APIs unavailable below API 24.

### NFR-007: Accessibility
- All interactive elements have `contentDescription` for TalkBack/screen reader support
- All interactive elements meet the 48dp minimum touch target size
- Sum indicators communicate status through both color AND shape/icon (colorblind support)
- All text uses `sp` units and respects system font scale at 0.85x, 1.0x, and 1.3x
- Layout must not break at 360dp screen width (minimum supported)
- One-handed play: number pad in bottom half of screen, reachable without repositioning grip

### NFR-008: No Monetization
Free. No ads. No in-app purchases. No paywall of any kind in Phase 1. This is a product principle, not a feature flag.

### NFR-009: No Backend
No server infrastructure for gameplay. All puzzle generation is client-side. All persistence is local DataStore. Firebase Analytics and Crashlytics are the only network-dependent components, and both operate in offline-queue mode.

### NFR-010: Code Architecture
- All engine logic (PRNG, generator, validator, calibrator) must be pure Kotlin with no Android dependencies, enabling JVM unit testing without an emulator
- Use Jetpack DataStore (not SharedPreferences) for all local persistence
- Use Compose animation APIs (not Lottie) for all animations
- Use Material3 theming with design tokens — no hardcoded hex color values anywhere
- Firebase initialized eagerly in `Application.onCreate()` — not lazily

---

## Technical Architecture

### Tech Stack

| Layer | Technology | Rationale |
|-------|-----------|-----------|
| Language | Kotlin | Android-first, concise, modern |
| UI Framework | Jetpack Compose | Declarative, Canvas API for grid, first-class Compose test support |
| Persistence | Jetpack DataStore | Async, type-safe, coroutine-native, replaces SharedPreferences |
| PRNG | Custom xorshift128 | ~20 lines, deterministic across all Android versions, portable |
| Puzzle Engine | Custom constraint propagation | Core IP, ~400-600 lines, no suitable library |
| Background Work | WorkManager | Puzzle precompute, idempotent, no foreground service |
| Analytics | Firebase Analytics | Already in dgeek project, free tier, offline queue |
| Crash Reporting | Firebase Crashlytics | Bundled with Analytics, already configured |
| Build | Gradle + R8 full mode | APK shrinking, CI size check |
| CI/CD | GitHub Actions | Free tier, APK size gate |
| Min SDK | API 24 (Android 7.0) | 95%+ device coverage |
| Target SDK | API 35 | Latest |

### Project Structure

```
app/src/main/kotlin/org/dgeek/sumgrid/
  engine/
    Xorshift128.kt              # S01-F001
    PuzzleGenerator.kt          # S01-F002
    UniqueSolutionValidator.kt  # S01-F003
    DifficultyCalibrator.kt     # S01-F004
  ui/
    GridRenderer.kt             # S01-F006
    GridInteraction.kt          # S01-F007
    NumberPad.kt                # S01-F008
    SumIndicator.kt             # S03-F004
    CelebrationAnimation.kt     # S02-F004
    DifficultySelector.kt       # S02-F006
    CompletionCard.kt           # S02-F008
    HomeScreen.kt               # S02-F011
    theme/
      Theme.kt                  # S03-F001, S03-F002
      Color.kt
      Type.kt
      Shape.kt
  viewmodel/
    PuzzleViewModel.kt          # S01-F009, S02-F003, S02-F005
  daily/
    DailyPuzzleRepository.kt    # S02-F001
    PuzzlePrecomputeWorker.kt   # S02-F002
  share/
    ShareCardGenerator.kt       # S02-F007
  streak/
    StreakRepository.kt         # S02-F009
    StreakBadgeProvider.kt      # S02-F010
  analytics/
    AnalyticsTracker.kt         # S03-F006
    CrashlyticsInitializer.kt   # S03-F007
  onboarding/
    OnboardingRepository.kt     # S03-F008
  review/
    InAppReviewTrigger.kt       # S03-F010
  navigation/
    AppNavigation.kt            # S03-F009
```

### Key Design Decisions

1. **Custom xorshift128, not `kotlin.random.Random(seed)`**: The Kotlin stdlib explicitly warns that the sequence generated by a seeded `Random` may change between versions. For a game where every player worldwide must receive the same puzzle, cross-version determinism is non-negotiable. xorshift128 is ~20 lines of Kotlin and provides complete control.

2. **DataStore, not SharedPreferences**: DataStore is the Jetpack-recommended replacement for SharedPreferences. It is async, type-safe, and coroutine-native. All persistence (streaks, completion state, onboarding progress, settings) uses DataStore exclusively.

3. **Compose Canvas for grid, not LazyGrid**: The puzzle grid requires custom visual behavior (cell highlighting, sum indicators, responsive sizing) that cannot be easily achieved with a standard grid composable. Canvas provides direct control over rendering.

4. **Compose animations, not Lottie**: Lottie adds approximately 800 KB to the APK. The celebration animation can be implemented with `animateFloatAsState` and `updateTransition`. APK size is a first-class constraint.

5. **Text-based share card, not image generation**: Generating a bitmap share card requires additional libraries and memory. A Unicode emoji text card is zero-cost, instantly shareable to any platform, and renders correctly in every messaging app.

6. **WorkManager for precompute, not coroutine on main**: Puzzle generation for 5x5 Medium can take several hundred milliseconds. Blocking the main thread is unacceptable. WorkManager handles this in a background thread, and the result is cached in DataStore before the player ever taps "Play."

7. **Hard-coded onboarding puzzles, not generated**: The onboarding progression (1 → 2 → 4 empty cells) requires guarantees of exact difficulty that the generator's calibration system cannot provide reliably at such small scales. Hard-coded puzzles are verified to be correct and remain stable across app updates.

---

## Feature-to-Sprint Matrix

| Feature ID | Feature Name | Sprint | Domain | Dependencies |
|------------|-------------|--------|--------|-------------|
| S01-F001 | xorshift128 PRNG | S01 | Engine | — |
| S01-F002 | Puzzle generator (seeded, deterministic) | S01 | Engine | S01-F001, S01-F003 |
| S01-F003 | Constraint propagation unique-solution validator | S01 | Engine | — |
| S01-F004 | Difficulty calibration (Beginner/Easy/Medium) | S01 | Engine | S01-F003 |
| S01-F005 | 10,000-puzzle automated test suite | S01 | Engine | S01-F001-F003 |
| S01-F006 | Compose Canvas grid renderer | S01 | UI | — |
| S01-F007 | Tap-to-select cell interaction | S01 | UI | S01-F006 |
| S01-F008 | Bottom number pad input | S01 | UI | S01-F007 |
| S01-F009 | Real-time row/column sum validation | S01 | UI | S01-F008 |
| S02-F001 | Daily puzzle system (date seed, midnight reset) | S02 | Backend | S01-F001, F002 |
| S02-F002 | Background precompute (WorkManager) | S02 | Backend | S02-F001 |
| S02-F003 | Puzzle completion detection | S02 | Backend | S01-F009 |
| S02-F004 | Celebration animation | S02 | UI | S02-F003, S01-F006 |
| S02-F005 | Elapsed timer | S02 | Backend | S02-F003 |
| S02-F006 | Difficulty selector UI | S02 | UI | S02-F001 |
| S02-F007 | Share card generator | S02 | Backend | S02-F003 |
| S02-F008 | One-tap share via Android Intent | S02 | UI | S02-F007, S02-F004 |
| S02-F009 | Streak counter (DataStore backed) | S02 | Backend | S02-F003 |
| S02-F010 | Streak milestone badges (7/30/100/365) | S02 | Backend | S02-F009 |
| S02-F011 | Home screen | S02 | UI | S02-F006, F009, F010 |
| S03-F001 | Deep indigo/amber Material3 theme | S03 | UI | — |
| S03-F002 | Dark mode (system-following) | S03 | UI | S03-F001 |
| S03-F003 | Responsive layout (360dp minimum) | S03 | UI | S01-F006, F008, S02-F011 |
| S03-F004 | Colorblind-friendly sum indicators | S03 | UI | S01-F009, S03-F001 |
| S03-F005 | Dynamic text sizing (system font scale) | S03 | UI | S03-F003 |
| S03-F006 | Firebase Analytics + event tracking | S03 | Backend | — |
| S03-F007 | Firebase Crashlytics integration | S03 | Backend | S03-F006 |
| S03-F008 | Onboarding puzzle sequence (1→2→4 cells) | S03 | Backend | S01-F006-F009 |
| S03-F009 | First-time user experience navigation | S03 | UI | S03-F008, S02-F004 |
| S03-F010 | In-app review prompt (after 3rd completion) | S03 | Backend | S02-F003 |
| S03-F011 | Play Store screenshots (5 required) | S03 | General | S03-F001, F002, S02-F008, F011 |
| S03-F012 | Play Store feature graphic + ASO listing | S03 | General | S03-F011 |

---

## Success Metrics

### Launch Gate (Day 60)
| Metric | Target | Kill Threshold |
|--------|--------|----------------|
| Total installs | 500 | < 200 at 30 days → pause Phase 2 |
| Day 1 retention | > 25% | — |
| Day 7 retention | > 15% | < 10% → mechanic may not work |
| Average session time | > 3 min | — |
| DAU at 60 days | > 50 | < 20 at 60 days → ghost town |
| Store rating | > 4.5 stars | < 4.0 any time → stop features, fix quality |
| Share rate | > 5% of completions | < 2% → do not build Phase 3 social |

### Phase 1 Quality Gate (Pre-submission)
- 10,000-puzzle test suite passes with zero failures
- Release APK size < 5 MB (CI gate at 4.5 MB)
- Firebase Analytics receives events in DebugView during smoke test
- App runs fully offline with no errors
- No crashes on API 24, 28, 33, and 35 emulators
- Accessibility: all elements have `contentDescription`, all touch targets ≥ 48dp
- Dark mode and light mode both render correctly on all screens

---

## Constraints

- Kotlin + Jetpack Compose only — no Flutter
- No backend infrastructure — all puzzle generation is client-side from date seed
- APK < 5 MB; CI must fail if release APK exceeds 4.5 MB
- Min SDK: API 24 (Android 7.0); Target SDK: API 35
- Custom xorshift128 PRNG — do NOT use `kotlin.random.Random(seed)` as sequence may change between Kotlin versions
- Jetpack DataStore for all persistence — NOT SharedPreferences
- Compose animation APIs only — NOT Lottie, NOT GIF
- Material3 theming — NOT hardcoded hex color values anywhere
- Firebase initialized eagerly in `Application.onCreate()` — NOT lazily
- Hard/Expert difficulties (5x5 Hard, 6x6 Expert) are deferred to v1.1
- Sound effects and haptic feedback are deferred to Phase 2
- No user accounts, no cloud sync, no server calls for gameplay

---

## Out of Scope (Phase 1)

- Hard difficulty (5x5, 7 pre-filled cells) — deferred to v1.1
- Expert difficulty (6x6) — deferred to v1.1
- Sound effects (click, chime, cascade) — deferred to Phase 2
- Haptic feedback — deferred to Phase 2
- Pencil/notes mode — deferred to Phase 2
- Undo button — deferred to Phase 2
- Hint system — deferred to Phase 2
- Statistics dashboard — deferred to Phase 2
- Practice mode (unlimited non-daily puzzles) — deferred to Phase 2
- Push notifications — deferred to Phase 2
- Theme system (5 unlockable themes) — deferred to Phase 2
- Home screen widget — deferred to Phase 3
- Global leaderboards — deferred to Phase 3
- Challenge-a-Friend feature — deferred to Phase 3
- Achievement system (20 badges) — deferred to Phase 3
- Store localization (Spanish, Portuguese, etc.) — deferred to Phase 3
- Web/PWA version — deferred to Phase 4
- Multiplayer race mode — deferred to Phase 4
- Wear OS companion — deferred to Phase 4
- dgeek unified account — deferred to Phase 4

---

## Dependencies

### Internal
- dgeek Firebase project (google-services.json already exists and is configured)
- dgeek Play Store developer account (submit as new app under org.dgeek.sumgrid)
- dgeek cross-app promotion (1,300 existing users across pacmaze, Brain Puzzle, Beyond)

### External
- Google Play In-App Review API (`com.google.android.play:review`) — requires Play Store to be installed
- Firebase Analytics SDK (~800 KB)
- Firebase Crashlytics SDK (~400 KB)
- WorkManager (Jetpack, no external dependency)
- Jetpack DataStore (Jetpack, no external dependency)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Puzzle algorithm generates duplicate-solution puzzles | Critical | Low | Rigorous unique-solution validator called after every cell removal. 10,000-puzzle test suite is a hard gate before S02. |
| xorshift128 produces different output on different Android versions | Critical | Very Low | PRNG is pure arithmetic — no stdlib dependency. Cross-device determinism verified in test suite. |
| APK exceeds 5 MB size limit | High | Low | CI gate at 4.5 MB catches this before release. No Lottie, no sound assets in Phase 1. R8 full mode enabled. |
| Puzzle mechanic doesn't engage players (depth ceiling) | High | Medium | D7 retention is the kill criterion. Phase 2 (practice mode, stats, harder difficulties) is designed to address this if Phase 1 shows the mechanic works. |
| Low initial discovery (zero marketing budget) | High | High | Leverage 1,300 existing dgeek users from Day 1. Reddit, Product Hunt, puzzle micro-influencers. ASO-optimized listing. "Free, no ads" story angle. |
| Wordle-style share card doesn't drive installs | Medium | Medium | Share-to-install conversion tracked from Day 1. If < 2% at 60 days, Phase 3 social features are cut. |
| Play Store review delay for new developer account | Medium | Medium | Submit to internal testing track early (Week 5). Expect 1–2 week review. Plan buffer in timeline. |
| Performance issues on low-end devices | Medium | Low | Test on 2 GB RAM emulator throughout development. Celebration animation capped at 1.5 seconds. WorkManager handles background compute. |

---

## Notes

- **Critical dev order** (must not be parallelized): S01 (engine + grid) → S02 (daily loop) → S03 (polish + launch). S01 F001-F005 (engine) should be built before F006-F009 (grid UI) within S01 since grid UI needs engine data models.
- **The 10,000-puzzle test suite is a hard gate**: no implementation work in S02 should begin until this passes cleanly on CI.
- **Launch target**: End of May 2026 (32 dev-days at 4 productive days/week = 8 calendar weeks from April 1).
- **dgeek philosophy applies**: Free. No ads. No dark patterns. Every user who installs SumGrid and finds a respectful, high-quality experience becomes someone who trusts dgeek apps.
- **Phase 2 is gated on Phase 1**: Do not plan or begin Phase 2 (Deepen Engagement) until Phase 1 meets its Day 60 success metrics (installs > 300, D7 retention > 12%, DAU > 30).
