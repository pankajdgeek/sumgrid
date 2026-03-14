# SumGrid Phase 1 MVP — Task Breakdown

**Epic**: 001-sumgrid-phase-1-mvp
**Generated**: 2026-03-14
**Total tasks**: 30
**Workflow**: TDD — Red-Green-Refactor per task
**Sprint sequencing**: S01 → S02 (blocked on S01 gate) → S03 (blocked on S01+S02)

---

## How to Read This File

Each task has:
- **ID**: T001–T030 (sequential, but grouped by sprint)
- **TDD phase**: `[RED]` write failing test first | `[GREEN]` make it pass | `[REFACTOR]` improve without breaking
- **Sprint**: S01 / S02 / S03
- **Feature**: which spec feature this implements
- **Priority**: `[P]` = high-value, must not be delayed

---

## Sprint S01 — Core Engine and Grid UI (Tasks T001–T010)

**Exit gate**: `./gradlew test` — BUILD SUCCESSFUL, zero failures, 10,000 puzzles validated unique, duration < 60 seconds.

---

### T001 — Project scaffold and Gradle setup [P]

**Sprint**: S01
**Feature**: Scaffolding (pre-feature)
**TDD phase**: [GREEN] (no test needed — build verification only)

**Description**:
Create the Android project skeleton at package `org.dgeek.sumgrid`. Configure `build.gradle.kts` with min SDK 24, target SDK 35, Kotlin, Jetpack Compose BOM, DataStore, WorkManager, Navigation Compose, Firebase Analytics, Firebase Crashlytics, and Play In-App Review dependencies. Enable R8 full mode in `gradle.properties`. Add a CI GitHub Actions workflow that runs `./gradlew test` and fails if the release APK exceeds 4.5 MB. Create the directory structure: `engine/`, `engine/models/`, `ui/`, `ui/components/`, `ui/screens/`, `ui/theme/`, `viewmodel/`, `daily/`, `share/`, `streak/`, `onboarding/`, `analytics/`, `review/`, `navigation/`.

**Acceptance criteria**:
- `./gradlew assembleDebug` succeeds on a clean checkout
- All declared dependencies resolve without version conflicts
- R8 full mode is enabled in `gradle.properties`
- GitHub Actions workflow file exists at `.github/workflows/ci.yml`
- Package `org.dgeek.sumgrid` is the applicationId

**Test requirements**:
- Not applicable — verify with a clean Gradle build only

**Dependencies**: None

---

### T002 — Engine data models: Puzzle, Cell, Difficulty [P]

**Sprint**: S01
**Feature**: S01-F001 (PRNG), S01-F002 (Generator)
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Define the core data classes in `engine/models/`:
- `Difficulty.kt`: enum with `BEGINNER` (size=3, maxVal=5, emptyCells=4), `EASY` (size=4, maxVal=7, emptyCells=8), `MEDIUM` (size=5, maxVal=9, emptyCells=15). Each entry carries a `seedOffset` (0, 1, 2) for the PRNG.
- `Cell.kt`: data class `Cell(val value: Int, val isGiven: Boolean)`. `value == 0` means empty.
- `Puzzle.kt`: data class holding `size: Int`, `cells: Array<Array<Cell>>`, `rowTargets: IntArray`, `colTargets: IntArray`, `difficulty: Difficulty`, `seed: Long`.

**Acceptance criteria**:
- All three files compile with no Android dependencies (pure Kotlin)
- `Difficulty.BEGINNER.size == 3`, `BEGINNER.emptyCells == 4`, `BEGINNER.seedOffset == 0`
- `Cell(value=0, isGiven=false)` represents an empty user-fillable cell
- `Puzzle` can be instantiated and its `cells` array accessed row-by-row

**Test requirements**:
- `DifficultyTest.kt`: assert size, maxVal, emptyCells, seedOffset for all three difficulties
- Equality and copy tests for `Cell` and `Puzzle`

**Dependencies**: T001

---

### T003 — xorshift128 PRNG [P]

**Sprint**: S01
**Feature**: S01-F001
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `engine/Xorshift128.kt`. Class accepts a `Long` seed and exposes `nextInt(bound: Int): Int` and `nextLong(): Long`. Internal state: two 64-bit words initialized by mixing the seed with splitmix64 (`state0 = seed ^ 0x9e3779b97f4a7c15L`, then mix). The xorshift128+ algorithm: `val t = state0; state0 = state1; state1 = state1 xor (state1 ushr 17) xor t xor (t shl 7); return t + state0`. `nextInt(bound)` maps to `[0, bound)` using `(nextLong() ushr 1) % bound`. No dependency on `kotlin.random.Random` or `java.util.Random`.

**Acceptance criteria**:
- Same seed always produces the same sequence regardless of Android API level
- `nextInt(bound)` returns values in `[0, bound)` exclusively
- Seed 0 does not produce all-zero output (splitmix64 mixing prevents this)
- The sequence for seed `20260101L` is stable across Kotlin versions (captured in a golden test)

**Test requirements**:
- `Xorshift128Test.kt`:
  - Determinism: call `nextInt(100)` 20 times with seed `42L`; assert exact sequence matches golden values
  - Bounds: `nextInt(N)` for N in {1, 2, 5, 9, 10, 100} — all output values in `[0, N)`
  - Seed 0 safety: first output is non-zero
  - Independence: two instances with the same seed produce identical sequences

**Dependencies**: T001, T002

---

### T004 — Constraint propagation unique-solution validator [P]

**Sprint**: S01
**Feature**: S01-F003
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `engine/UniqueSolutionValidator.kt`. Sealed class `SolutionCount { NONE, UNIQUE, MULTIPLE }`. Public method: `fun validate(grid: Array<IntArray>, rowTargets: IntArray, colTargets: IntArray, maxVal: Int): SolutionCount`. Algorithm: (1) constraint propagation — for each empty cell, compute the domain of valid values (values that don't exceed any row/column target when added to the current partial sum). (2) Backtracking search — fill cells in domain-size order (MRV heuristic). Immediately return `MULTIPLE` when a second solution is found. Return `NONE` if no solution exists. Pure Kotlin, no Android dependencies.

**Acceptance criteria**:
- Returns `UNIQUE` for any correctly generated puzzle
- Returns `MULTIPLE` for a grid with more than one solution
- Returns `NONE` for an over-constrained grid
- Handles 3x3, 4x4, and 5x5 grids
- Completes in < 50 ms for a 5x5 grid on CI hardware (measured in test)

**Test requirements**:
- `UniqueSolutionValidatorTest.kt`:
  - Known unique 3x3 puzzle → `UNIQUE`
  - Known multiple-solution 3x3 partial grid → `MULTIPLE`
  - Over-constrained 3x3 grid (sum target impossible) → `NONE`
  - 4x4 unique case → `UNIQUE`
  - 5x5 unique case → `UNIQUE`
  - Performance: 100 calls on a 5x5 grid complete in < 5,000 ms total

**Dependencies**: T001, T002

---

### T005 — Puzzle generator [P]

**Sprint**: S01
**Feature**: S01-F002
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `engine/PuzzleGenerator.kt`. Public API: `fun generate(seed: Long, difficulty: Difficulty): Puzzle`. Algorithm: (1) Seed `Xorshift128` with `seed + difficulty.seedOffset`. (2) Fill NxN grid with values in `[1..maxVal]` drawn from the PRNG. (3) Compute `rowTargets` and `colTargets` from the complete grid. (4) Build a removal order by shuffling cell indices with the PRNG. (5) Remove cells one at a time; after each removal call `UniqueSolutionValidator.validate()`. Keep removing until `difficulty.emptyCells` empty cells remain. (6) If unique-solution constraint fails within 100 removal attempts, increment the seed offset by 10 and retry from step 1. Return a `Puzzle` with a `givenMask` marking pre-filled cells.

**Acceptance criteria**:
- Same seed + difficulty always produces the same puzzle (deterministic)
- Generated puzzle always has exactly `difficulty.emptyCells` empty cells
- `UniqueSolutionValidator.validate()` returns `UNIQUE` for every generated puzzle
- Generator handles all three difficulties: BEGINNER, EASY, MEDIUM

**Test requirements**:
- `PuzzleGeneratorTest.kt`:
  - Determinism: generate puzzle with seed `1000L` + BEGINNER twice; assert grids are identical
  - Empty cell count: generated EASY puzzle has exactly 8 empty cells
  - Uniqueness: generate one puzzle per difficulty; validate each → `UNIQUE`
  - No given cell is 0 (given cells always have a value)
  - No user cell is non-zero in the initial puzzle (user cells start empty)

**Dependencies**: T003, T004

---

### T006 — Difficulty calibrator [P]

**Sprint**: S01
**Feature**: S01-F004
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `engine/DifficultyCalibrator.kt`. Method: `fun countSolveSteps(puzzle: Puzzle): Int` — counts the number of constraint-propagation deduction steps (cell fills that follow necessarily without backtracking) needed to solve a puzzle. Method: `fun calibrate(puzzle: Puzzle): Difficulty` — maps step count to difficulty tier. The calibrator is invoked by `PuzzleGenerator` to verify the puzzle's actual difficulty matches the target; if it does not, the generator retries with a seed offset.

**Acceptance criteria**:
- `countSolveSteps` returns a non-negative integer for any valid puzzle
- A 3x3 puzzle with 4 empty cells calibrates as BEGINNER
- A 4x4 puzzle with 8 empty cells calibrates as EASY
- A 5x5 puzzle with 15 empty cells calibrates as MEDIUM
- Pure Kotlin, no Android dependencies

**Test requirements**:
- `DifficultyCalibrationTest.kt`:
  - Generate one puzzle per difficulty; assert `calibrate(puzzle) == difficulty`
  - `countSolveSteps` returns >= 0 for all difficulties

**Dependencies**: T004, T005

---

### T007 — 10,000-puzzle automated test suite (S01 quality gate) [P]

**Sprint**: S01
**Feature**: S01-F005
**TDD phase**: [RED] → [GREEN]

**Description**:
Create `engine/PuzzleCorpusTest.kt`. This is a JVM parameterized test (not an Android instrumented test). Generates 10,000 puzzles: approximately 3,333 per difficulty using seeds `1L` through `10000L` (cycling through difficulties). For each generated puzzle: (a) calls `UniqueSolutionValidator.validate()` and asserts `UNIQUE`; (b) generates the same puzzle a second time with the same seed and asserts byte-identical output (determinism check). The test must complete in < 60 seconds on CI. This test is the **hard gate** — S02 work must not begin until this test passes with zero failures.

**Acceptance criteria**:
- Generates exactly 10,000 puzzles (0 skipped)
- Every puzzle validates as `UNIQUE` — zero failures
- Every seed produces the same puzzle on two consecutive calls (determinism)
- Total wall-clock time < 60 seconds on standard CI hardware
- `./gradlew test --tests "*.PuzzleCorpusTest"` passes

**Test requirements**:
- The test IS the deliverable for this task
- Uses `@ParameterizedTest` with seed range or a loop-based approach
- Reports progress every 1,000 puzzles to aid debugging if failures occur

**Dependencies**: T005, T006

---

### T008 — Compose Canvas grid renderer [P]

**Sprint**: S01
**Feature**: S01-F006
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `ui/components/GridRenderer.kt` as a `@Composable` using `Canvas`. The composable accepts a `Puzzle`, a `userGrid: Array<IntArray>` (player-entered values), a `selectedCell: Pair<Int,Int>?`, `rowSumStatus: Array<SumStatus>`, and `colSumStatus: Array<SumStatus>`. Cell size = `(availableWidth - padding) / gridSize` using `BoxWithConstraints`. Draw cells with distinct visual states: given (filled, surfaceVariant background), user-filled (surface background), empty (surface background, no text), selected (3dp `primary` border). Draw row sum targets to the right; column sum targets below. All colors reference `MaterialTheme.colorScheme` — zero hardcoded hex values. Include `Modifier.semantics { contentDescription = "..." }` for every logical cell.

**Acceptance criteria**:
- Renders 3x3, 4x4, and 5x5 grids without clipping at 360dp and 420dp width
- Given cells, user-filled cells, empty cells, and selected cells are visually distinct
- Row and column sum targets are displayed in correct positions
- All colors come from `MaterialTheme.colorScheme` only
- Each cell has a contentDescription for TalkBack

**Test requirements**:
- `GridRendererTest.kt` (instrumented):
  - At 360dp width, grid does not overflow its bounding box (assert maxX <= screenWidth)
  - At 420dp width, same constraint
  - Selected cell shows primary-colored border
  - Given cell shows different background from user-filled cell

**Dependencies**: T002 (for Puzzle model)

---

### T009 — Cell tap-to-select and number pad input [P]

**Sprint**: S01
**Feature**: S01-F007, S01-F008
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Part A — Cell selection in `PuzzleViewModel.kt`: Add `selectedCell: StateFlow<Pair<Int,Int>?>`. Method `onCellTap(row: Int, col: Int)`: if `puzzle.cells[row][col].isGiven` → no-op; if `selectedCell == (row, col)` → set to null (deselect); else → set to `(row, col)`. Wire `Modifier.clickable` on each cell in `GridRenderer` to call `onCellTap`.

Part B — NumberPad in `ui/components/NumberPad.kt`: `@Composable` showing buttons 1–9 and a Clear button in a responsive grid layout. Positioned at the bottom of `PuzzleScreen`. When a cell is selected, `onNumberTap(n)`: if `userGrid[selectedCell] == n` → set to 0 (clear); else → set to n. `onClearTap()` → set to 0. Hidden (`isVisible = false`) when puzzle is complete. All buttons have minimum 48dp touch targets with contentDescriptions.

**Acceptance criteria**:
- Tapping a given cell does nothing; selectedCell remains unchanged
- Tapping an empty cell selects it; tapping it again deselects it
- Tapping number N when cell is selected fills it; tapping N again clears it
- Clear button removes the current value from the selected cell
- Tapping a number when no cell is selected does nothing
- NumberPad is hidden after puzzle completion

**Test requirements**:
- `CellSelectionTest.kt` (instrumented): tap a non-given cell → it becomes selected; tap again → deselected
- `NumberPadTest.kt` (instrumented): tap cell, tap number → cell shows that number; tap same number → cell is cleared

**Dependencies**: T008

---

### T010 — Real-time row/column sum validation [P]

**Sprint**: S01
**Feature**: S01-F009
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Extend `PuzzleViewModel.kt` to expose `rowSumStatus: StateFlow<Array<SumStatus>>` and `colSumStatus: StateFlow<Array<SumStatus>>`. `SumStatus` enum: `UNDER`, `EXACT`, `OVER`. Computed as a pure function `computeSumStatus(userGrid, puzzle)` called on every cell value change (no debounce). Pass `rowSumStatus` and `colSumStatus` to `GridRenderer` so sum target labels render in green (EXACT), red (OVER), or gray (UNDER). This is pure ViewModel logic — no Android I/O.

**Acceptance criteria**:
- rowSumStatus and colSumStatus update immediately on every `onNumberTap` call
- EXACT when sum equals target; OVER when sum exceeds target; UNDER otherwise
- GridRenderer renders sum labels in correct color for each status
- Logic is a pure function — testable with no mocking

**Test requirements**:
- `SumValidationTest.kt` (JVM unit test):
  - Fill all cells of a row to exactly match target → `EXACT`
  - Fill a cell that causes a row sum to exceed target → `OVER`
  - Partially filled row → `UNDER`
  - Multiple rows/columns in one puzzle — all statuses computed correctly

**Dependencies**: T009

---

## Sprint S02 — Daily Puzzle Loop (Tasks T011–T022)

**Prerequisite**: T007 (10,000-puzzle test suite) must pass before any S02 task begins.
**Exit gate**: Full daily puzzle loop playable end-to-end on an API 24 emulator.

---

### T011 — SumGridApplication and manual DI root [P]

**Sprint**: S02
**Feature**: S02-F001 (scaffolding)
**TDD phase**: [GREEN]

**Description**:
Create `SumGridApplication.kt` (subclass of `Application`). In `onCreate()`: create DataStore instances (one per domain: puzzle, streak, onboarding), instantiate `PuzzleGenerator`, `DailyPuzzleRepository`, `StreakRepository`, `OnboardingRepository`. Store these as `companion object` properties so ViewModelFactory can access them. This is the manual DI root — no Hilt. Register `SumGridApplication` in `AndroidManifest.xml`. Also create `ViewModelFactory.kt` that provides `PuzzleViewModel` and `HomeViewModel` with injected dependencies.

**Acceptance criteria**:
- Application class registered in AndroidManifest.xml
- DataStore instances created once at app start and reused
- `PuzzleGenerator`, `DailyPuzzleRepository`, `StreakRepository` accessible via Application singleton
- No NullPointerException on cold start

**Test requirements**:
- No unit test required; verified by instrumented startup smoke test
- Manual verification: app launches without crash on API 24 emulator

**Dependencies**: T010

---

### T012 — Daily puzzle repository (date-seeded, DataStore-backed) [P]

**Sprint**: S02
**Feature**: S02-F001
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `daily/DailyPuzzleRepository.kt`. Key method: `suspend fun getPuzzleForDate(date: LocalDate, difficulty: Difficulty): Puzzle` — computes seed as `date.toEpochDay() + difficulty.seedOffset`, calls `PuzzleGenerator.generate(seed, difficulty)`. `suspend fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())`. Persist completion state in DataStore with keys namespaced as `puzzle_{epochDay}_{difficulty.name}_{field}` (fields: `completed`, `elapsedMs`, `completionTimestamp`, `userGrid`). Methods: `suspend fun getCompletionState(date: LocalDate, difficulty: Difficulty): CompletionState?` and `suspend fun saveCompletionState(state: CompletionState)`.

**Acceptance criteria**:
- Same date + difficulty → same puzzle on any call (determinism via PRNG)
- `today()` uses `ZoneId.systemDefault()` — local calendar day, not UTC
- Completion state survives app restart (DataStore persistence)
- DataStore keys are unique per date+difficulty combination

**Test requirements**:
- `DailyPuzzleRepositoryTest.kt` (JVM unit test with FakeDataStore):
  - `getPuzzleForDate(date, BEGINNER)` twice returns identical puzzles
  - Different dates produce different puzzles
  - Saving and loading completion state round-trips without data loss
  - `today()` returns correct local date (mocked `ZoneId` for determinism)

**Dependencies**: T005, T011

---

### T013 — Background puzzle precompute worker [P]

**Sprint**: S02
**Feature**: S02-F002
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `daily/PuzzlePrecomputeWorker.kt` as a `CoroutineWorker`. Worker generates today's and tomorrow's puzzles for all three difficulties and stores them in DataStore. Uses `Dispatchers.Default` for generation. Idempotency: check if puzzles already exist in DataStore before generating; skip if cached. Register as `OneTimeWorkRequest` in `SumGridApplication.onCreate()` with `ExistingWorkPolicy.KEEP` (prevents duplicate runs). Worker is triggered on every app start but only does real work when the cache is empty or stale (new day).

**Acceptance criteria**:
- After worker runs, DataStore contains today's and tomorrow's puzzles for all three difficulties (6 puzzles)
- Worker is idempotent — running it twice produces the same DataStore content
- Worker uses `ExistingWorkPolicy.KEEP` to prevent concurrent duplicate runs
- Worker runs on a background thread — never blocks the main thread

**Test requirements**:
- `PuzzlePrecomputeWorkerTest.kt` (JVM unit test):
  - Worker called once → DataStore has 6 puzzle entries
  - Worker called twice → DataStore still has 6 entries (no duplication)
  - Worker skips generation when puzzles already cached

**Dependencies**: T012

---

### T014 — Puzzle completion detection [P]

**Sprint**: S02
**Feature**: S02-F003
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Extend `PuzzleViewModel.kt` to expose `isComplete: StateFlow<Boolean>`. Completion condition (pure function `checkComplete(userGrid, puzzle)`): all cells that are not given have a non-zero value AND every row's sum of `userGrid` equals its `rowTarget` AND every column's sum equals its `colTarget`. On the first `false → true` transition: stop the timer, persist the `CompletionState` (elapsed time, timestamp) to `DailyPuzzleRepository`, call `StreakRepository.recordCompletion(today)`, fire the analytics event `puzzle_completed`. Guard against re-triggering: once `isComplete == true`, ignore further cell changes.

**Acceptance criteria**:
- `isComplete` becomes true only when all cells are correctly filled
- `isComplete` does not trigger on a partially filled board
- Completion triggers exactly once per puzzle session (no double-fire)
- Persisted elapsed time matches the timer value at completion

**Test requirements**:
- `CompletionDetectionTest.kt` (JVM unit test):
  - Fill all cells incorrectly (wrong sums) → `isComplete == false`
  - Fill all cells correctly → `isComplete == true`
  - After completion, changing a cell does not re-trigger
  - `checkComplete` is called after every `onNumberTap`

**Dependencies**: T010, T012

---

### T015 — Elapsed timer (hidden during play, shown on completion) [P]

**Sprint**: S02
**Feature**: S02-F005
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Add timer logic to `PuzzleViewModel.kt`. State: `timerStarted: Boolean` (false until first cell tap), `elapsedMillis: Long`. `onFirstCellTap()` starts the timer. `PuzzleScreen` drives the timer with a `LaunchedEffect` that calls `viewModel.tickTimer()` every 1 second when `timerStarted && !isComplete`. `tickTimer()` increments `elapsedMillis` by 1,000. Timer value is persisted to DataStore so backgrounding and returning resumes from the saved value. On completion, timer stops. On the completion card, format `elapsedMillis` as `MM:SS`.

**Acceptance criteria**:
- Timer does not start until the player taps their first cell
- Timer is not displayed during play (hidden in PuzzleScreen UI)
- Timer stops exactly when `isComplete` becomes true
- Backgrounding and returning resumes the timer from the persisted value
- `formatTime(90_000L)` returns "01:30"

**Test requirements**:
- `PuzzleTimerTest.kt` (JVM unit test):
  - `tickTimer()` 3 times → `elapsedMillis == 3000L`
  - Timer not started until first cell tap
  - Timer does not increment after completion
  - `formatTime` edge cases: 0ms → "00:00", 3661000ms → "61:01"

**Dependencies**: T014

---

### T016 — Share card generator [P]

**Sprint**: S02
**Feature**: S02-F007
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `share/ShareCardGenerator.kt` as a singleton `object`. Pure function: `fun generate(puzzle: Puzzle, userGrid: Array<IntArray>, elapsedMillis: Long, difficulty: Difficulty): String`. Output format:
```
SumGrid #[dayNumber] — [Difficulty] ⏱[MM:SS]
[NxN emoji grid]
Play free -> sumgrid.dgeek.org
```
`dayNumber = LocalDate.now().toEpochDay() - LocalDate.of(2026, 1, 1).toEpochDay() + 1`. Cell encoding: `puzzle.cells[r][c].isGiven` → `⬜` (U+2B1C); user-solved → `🟩` (U+1F7E9). Grid rows separated by `\n`. No numbers anywhere in the output. Pure Kotlin, no Android dependencies.

**Acceptance criteria**:
- Output contains no digit characters (0–9) other than in the day number and time
- Wait — time is in MM:SS format which contains digits; spec says "no numbers" meaning no grid solution numbers. The emoji grid cells must contain zero digit characters.
- `⬜` is used for given cells, `🟩` for user-solved cells
- Day number for 2026-01-01 is 1; for 2026-01-02 is 2
- Grid rows are newline-separated
- Output ends with "Play free -> sumgrid.dgeek.org"

**Test requirements**:
- `ShareCardGeneratorTest.kt` (JVM unit test):
  - A completed BEGINNER puzzle → output matches expected template exactly
  - Given cells produce `⬜`; user-solved cells produce `🟩`
  - `dayNumber` is correct for a known date (2026-03-14 → day 73)
  - Timer format: 90,000ms → "01:30" in header
  - No digit appears in the emoji grid rows

**Dependencies**: T014

---

### T017 — Streak repository and milestone badges [P]

**Sprint**: S02
**Feature**: S02-F009, S02-F010
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Part A — `streak/StreakRepository.kt`: DataStore-backed repository. Exposes `val streakState: Flow<StreakState>` where `StreakState(currentStreak: Int, longestStreak: Int, lastCompletionDate: LocalDate?, earnedBadges: Set<StreakBadge>)`. Method: `suspend fun recordCompletion(date: LocalDate)`. Increment logic: `lastCompletionDate == null || > 1 day ago` → streak = 1; `lastCompletionDate == yesterday` → streak++; `lastCompletionDate == today` → no-op. Always update `longestStreak = max(longestStreak, currentStreak)`.

Part B — `streak/StreakBadgeProvider.kt`: `fun getEarnedBadges(currentStreak: Int, existingBadges: Set<StreakBadge>): Set<StreakBadge>`. Badges: `WEEKLY_WARRIOR` (7 days), `MONTHLY_MASTER` (30), `CENTURY_SOLVER` (100), `YEAR_OF_LOGIC` (365). Once earned, a badge is never removed. `StreakBadgeProvider` is called by `StreakRepository.recordCompletion`.

**Acceptance criteria**:
- Completing on day 1 → streak = 1; day 2 → streak = 2
- Skipping a day → streak resets to 1 on next completion
- Completing twice on the same day → streak unchanged (no-op on second call)
- At streak = 7, WEEKLY_WARRIOR badge is added and persisted permanently
- Breaking streak after 10 days does not remove WEEKLY_WARRIOR badge
- `longestStreak` captures the historical maximum

**Test requirements**:
- `StreakRepositoryTest.kt` (JVM unit test with FakeDataStore):
  - Day 1 completion → currentStreak == 1
  - Day 1 then day 2 → currentStreak == 2
  - Day 1 then day 3 (skip day 2) → currentStreak == 1
  - Two completions same day → no-op on second call
- `StreakBadgeTest.kt` (JVM unit test):
  - Streak reaches 7 → WEEKLY_WARRIOR in earnedBadges
  - Streak drops to 3 → WEEKLY_WARRIOR still in earnedBadges
  - Streak reaches 30 → both WEEKLY_WARRIOR and MONTHLY_MASTER present

**Dependencies**: T014

---

### T018 — Celebration animation (cascade, 30fps on 2 GB RAM) [P]

**Sprint**: S02
**Feature**: S02-F004
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `ui/components/CelebrationAnimation.kt`. On `isComplete` transitioning to true, animate each cell in reading order (row 0 left-to-right, then row 1, etc.) with a 50ms stagger. Each cell: scale 1.0f → 1.2f → 1.0f with color briefly shifting to a brighter green (`MaterialTheme.colorScheme.primary`). Use `LaunchedEffect(isComplete)` with `Animatable` per cell. Total duration ≤ 1,500 ms (gridSize * 50ms stagger + 200ms per cell animation). Animation state tracked in ViewModel as `isAnimating: StateFlow<Boolean>`. The share button must remain clickable during the animation (animation does not block interaction).

**Acceptance criteria**:
- Cascade plays in reading order with visible 50ms stagger
- All cells have completed their animation within 1,500ms
- Share button is tappable during animation (no touch-blocking overlay)
- Animation uses only Compose `Animatable`/`animate*AsState` APIs — no Lottie, no GIF
- No frame drops (maintain >= 30fps) on a 2 GB RAM API 24 emulator

**Test requirements**:
- `CelebrationAnimationTest.kt` (instrumented):
  - After puzzle completion, assert animation state `isAnimating == true` briefly
  - After 1.5 seconds, assert animation state `isAnimating == false`
  - Share button node is tappable during animation (Compose test rule can perform click)

**Dependencies**: T008, T014

---

### T019 — Completion card and one-tap share [P]

**Sprint**: S02
**Feature**: S02-F008
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `ui/components/CompletionCard.kt`. `@Composable` shown in `PuzzleScreen` when `isComplete == true`. Shows: elapsed time (MM:SS), difficulty label, day number, highest earned streak badge. Share button calls `ShareCardGenerator.generate(...)` and fires an Android `ACTION_SEND` Intent with `type = "text/plain"` using `Context.startActivity(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareCard) })`. Also triggers `AnalyticsTracker.shareTapped(...)`. Share button also appears on `HomeScreen` for previously completed puzzles.

**Acceptance criteria**:
- Completion card shows correct elapsed time, difficulty, and day number
- Share button fires `ACTION_SEND` Intent with correct `EXTRA_TEXT`
- Share card text matches `ShareCardGenerator` output
- Share button is accessible from both completion card and home screen (for previous completions)
- Share button has a `contentDescription` for TalkBack

**Test requirements**:
- `ShareIntentTest.kt` (instrumented):
  - Mock `ShareCardGenerator`; tap share button → assert `ACTION_SEND` Intent was fired
  - Assert `EXTRA_TEXT` is non-empty and contains "SumGrid"
- CompletionCard renders elapsed time in "MM:SS" format

**Dependencies**: T015, T016, T018

---

### T020 — Difficulty selector UI [P]

**Sprint**: S02
**Feature**: S02-F006
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `ui/components/DifficultySelector.kt`. `@Composable` showing three difficulty tiles: BEGINNER (3x3), EASY (4x4), MEDIUM (5x5). Each tile reads today's completion status from `HomeViewModel` and shows either "Play" CTA (not completed) or a checkmark (completed). Tapping a "Play" tile navigates to `PuzzleScreen(difficulty)`. Tapping a completed tile shows the `CompletionCard` for that difficulty. Last selected difficulty is persisted via DataStore in `HomeViewModel`. All tiles have minimum 48dp touch targets and contentDescriptions.

**Acceptance criteria**:
- Three tiles display with correct labels and grid size indicators
- Completed tile shows checkmark; incomplete tile shows "Play" CTA
- Tapping an incomplete tile navigates to the puzzle screen
- Tapping a completed tile shows the completion card (not a new puzzle)
- Last selected difficulty is remembered after app restart

**Test requirements**:
- `DifficultySelectorTest.kt` (instrumented):
  - All three difficulties rendered with correct labels
  - Completed tile shows checkmark icon
  - Tapping an incomplete tile triggers navigation (assert NavController route change)

**Dependencies**: T012, T019

---

### T021 — Home screen with streak display and midnight countdown [P]

**Sprint**: S02
**Feature**: S02-F011
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `ui/screens/HomeScreen.kt`. Backed by `HomeViewModel.kt`. Shows: app title "SumGrid", `DifficultySelector` component, current streak count (from `StreakRepository.streakState`), highest earned badge emoji, live countdown to midnight (updates every second via `ticker(1.second)`). When all three difficulties are completed for the day, replace countdown with "All done! Next puzzle in HH:MM:SS". `HomeViewModel` collects `StreakState` as a `StateFlow` and exposes a ticker-driven `timeToMidnight: StateFlow<String>`.

**Acceptance criteria**:
- App title, difficulty selector, streak count, highest badge, and countdown all visible
- Streak count and badge reflect the latest `StreakState`
- Countdown updates every second (use `LaunchedEffect` with delay loop)
- "All done!" message shows when all three difficulties are complete for today
- Home screen does not re-compose on every second tick for components that haven't changed

**Test requirements**:
- `HomeScreenTest.kt` (instrumented):
  - Streak count node shows the correct streak value from ViewModel
  - When all difficulties complete, "All done!" text is present in semantic tree
  - Difficulty selector is visible and shows three tiles

**Dependencies**: T017, T020

---

### T022 — PuzzleScreen wiring (full daily play loop) [P]

**Sprint**: S02
**Feature**: S02-F001 through S02-F011 (integration)
**TDD phase**: [GREEN] → [REFACTOR]

**Description**:
Create `ui/screens/PuzzleScreen.kt` as the full integration screen that wires together: `GridRenderer`, `NumberPad`, `SumIndicator` labels, `CelebrationAnimation`, and `CompletionCard`. Accepts `difficulty: Difficulty` as a nav argument. On enter: loads today's puzzle via `PuzzleViewModel.loadPuzzle(date, difficulty)`. If already completed today, shows `CompletionCard` immediately (skip to share mode). `LaunchedEffect` drives the per-second timer tick. Number pad is hidden on completion. Back navigation returns to HomeScreen. `PuzzleViewModel` is shared between `PuzzleScreen` and the completion card.

**Acceptance criteria**:
- Navigating to PuzzleScreen(EASY) loads today's Easy puzzle
- Timer starts on first cell tap
- Completing the puzzle shows celebration then completion card
- Back press from PuzzleScreen returns to HomeScreen (not re-opens puzzle)
- If puzzle already completed today, completion card shown immediately

**Test requirements**:
- Integration-level manual test: play a puzzle from start to completion, verify the full flow on API 24 emulator
- `PuzzleViewModelTest.kt` (JVM): `loadPuzzle` called with a date and difficulty → `puzzleState.puzzle` is non-null and has correct size

**Dependencies**: T018, T019, T020, T021

---

## Sprint S03 — Polish, Firebase, Onboarding, and Play Store (Tasks T023–T030)

**Prerequisite**: All S02 tasks complete and daily loop playable end-to-end.
**Exit gate**: APK < 4.5 MB, Firebase events verified in DebugView, internal testing track submitted.

---

### T023 — Material3 theme and dark mode [P]

**Sprint**: S03
**Feature**: S03-F001, S03-F002
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `ui/theme/Color.kt`, `ui/theme/Type.kt`, `ui/theme/Shape.kt`, and `ui/theme/Theme.kt`. `Color.kt` defines named color constants using `Color(0xFF...)` literals — these are the ONLY place hex values appear in the codebase. All other files reference `MaterialTheme.colorScheme.*`. `Theme.kt` defines `LightColorScheme` (primary: deep indigo `0xFF3730A3`, secondary/accent: amber `0xFFF59E0B`) and `DarkColorScheme`. `SumGridTheme` composable selects the scheme via `isSystemInDarkTheme()`. Wrap `MainActivity` content with `SumGridTheme`. Verify all existing composables render without error after theme application.

**Acceptance criteria**:
- Deep indigo primary and amber accent render in light mode
- Dark mode activates automatically when system setting is dark
- App switches between modes without restart
- Zero hardcoded hex literals in any composable outside `Color.kt`
- All screens (puzzle, home, completion card, number pad) render correctly in both modes

**Test requirements**:
- `ThemeTest.kt` (instrumented):
  - Assert `MaterialTheme.colorScheme.primary` is non-null in light mode
  - Assert `MaterialTheme.colorScheme.primary` is non-null in dark mode
- `DarkModeTest.kt` (instrumented):
  - Force dark mode configuration; assert primary surface color changes vs light mode

**Dependencies**: T022 (all composables exist to test against)

---

### T024 — Firebase Analytics and Crashlytics [P]

**Sprint**: S03
**Feature**: S03-F006, S03-F007
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Part A — `analytics/AnalyticsTracker.kt`: Define `interface AnalyticsTracker` with methods `puzzleStarted(difficulty, date)`, `puzzleCompleted(difficulty, date, elapsedSeconds)`, `puzzleAbandoned(difficulty, date, cellsFilled)`, `shareTapped(difficulty, date)`, `streakMilestone(streakDays)`, `onboardingCompleted(attemptNumber)`, `appOpen()`. Implement `FirebaseAnalyticsTracker` using `FirebaseAnalytics.logEvent(...)`. Implement `NoOpAnalyticsTracker` for tests. Initialize in `SumGridApplication.onCreate()`.

Part B — `analytics/CrashlyticsInitializer.kt`: Initialize `FirebaseCrashlytics` in `Application.onCreate()`. Set custom keys: `current_difficulty`, `puzzle_day_number`, `app_version`. In debug builds only: add a long-press (5 taps) gesture on the app logo in `HomeScreen` that triggers `throw RuntimeException("Test crash")`.

**Acceptance criteria**:
- `AnalyticsTracker` interface is used everywhere — no direct `FirebaseAnalytics` calls outside the `FirebaseAnalyticsTracker` impl
- All 7 events are instrumented at the correct call sites in ViewModels
- Crashlytics initialized before any puzzle is loaded
- Custom keys are set on every crash report
- Debug crash button is not visible in release builds (`BuildConfig.DEBUG` guard)

**Test requirements**:
- `AnalyticsTrackerTest.kt` (JVM unit test):
  - Mock `AnalyticsTracker`; complete a puzzle in ViewModel → assert `puzzleCompleted` was called once
  - Call `shareTapped` → assert method was invoked with correct arguments
- `CrashlyticsTest.kt` (JVM unit test):
  - `CrashlyticsInitializer` does not throw on init (smoke test with no-op Firebase)

**Dependencies**: T022

---

### T025 — Onboarding puzzle sequence (1→2→4 cells) [P]

**Sprint**: S03
**Feature**: S03-F008
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `onboarding/OnboardingRepository.kt`. Hard-code three verified 3x3 puzzles as `companion object val` constants:
- `PUZZLE_1`: 3x3 with exactly 1 empty cell (trivially forced answer)
- `PUZZLE_2`: 3x3 with exactly 2 empty cells
- `PUZZLE_3`: 3x3 with exactly 4 empty cells
All three must validate as `UNIQUE` via `UniqueSolutionValidator`. DataStore-backed: tracks `launchCount: Int` and `isComplete: Boolean`. Methods: `suspend fun incrementLaunchCount()`, `suspend fun markComplete()`, `fun getPuzzleForLaunch(launchCount: Int): Puzzle?` (returns null if launchCount > 3 or isComplete). Onboarding completions do NOT call `StreakRepository.recordCompletion`.

**Acceptance criteria**:
- Launch 1 returns PUZZLE_1 (1 empty cell)
- Launch 2 returns PUZZLE_2 (2 empty cells)
- Launch 3 returns PUZZLE_3 (4 empty cells)
- Launch 4+ returns null (onboarding complete)
- Completing onboarding puzzle does not increment streak
- `UniqueSolutionValidator.validate(PUZZLE_1)` == `UNIQUE`

**Test requirements**:
- `OnboardingRepositoryTest.kt` (JVM unit test):
  - `getPuzzleForLaunch(1)` returns puzzle with 1 empty cell
  - `getPuzzleForLaunch(4)` returns null
  - `markComplete()` followed by `getPuzzleForLaunch(1)` returns null
  - All three hard-coded puzzles validate as UNIQUE

**Dependencies**: T004, T011

---

### T026 — FTUE navigation and onboarding screen [P]

**Sprint**: S03
**Feature**: S03-F009
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `navigation/AppNavigation.kt` and `ui/screens/OnboardingScreen.kt`. `AppNavigation` is a `NavHost` with destinations: `onboarding`, `home`, `puzzle/{difficulty}`. Start destination is determined at boot by `OnboardingRepository`: if `!isComplete` → start at `onboarding`; if `isComplete` → start at `home`. `OnboardingScreen`: shows the current onboarding puzzle using `PuzzleScreen`'s grid + number pad. On completion: play celebration animation, then show the subtitle "Come back tomorrow for a new puzzle." + "Got it" button (launch 1 only). On "Got it": call `OnboardingRepository.markComplete()` (after launch 3) and navigate to `home`. Launches 2 and 3: navigate to home on completion without tooltip. Launch 4+: `AppNavigation` routes directly to home.

**Acceptance criteria**:
- Fresh install opens directly to onboarding puzzle (no splash, no login)
- Launch 1 completion shows "Come back tomorrow" tooltip with "Got it" button
- Launch 2 and 3 completions navigate directly to home without tooltip
- Launch 4+ opens home screen immediately
- Back press from onboarding puzzle does nothing (onboarding is the root destination)

**Test requirements**:
- `FTUETest.kt` (instrumented):
  - `launchCount == 1` → start destination is `onboarding`
  - `isComplete == true` → start destination is `home`
  - Completing launch 1 onboarding puzzle → "Come back tomorrow" node exists in semantic tree
  - `launchCount == 4` → home screen shown immediately

**Dependencies**: T018, T025

---

### T027 — Responsive layout and accessibility audit

**Sprint**: S03
**Feature**: S03-F003, S03-F004, S03-F005
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Audit all screens for 360dp compatibility and accessibility compliance.

Part A — Responsive layout: test `PuzzleScreen` and `HomeScreen` at 360dp width using `ComposeTestRule` with custom display metrics. Fix any clipping or overflow. Number pad must be fully visible at the bottom of a 360dp x 640dp viewport.

Part B — Colorblind indicators: extend `ui/components/SumIndicator.kt` (or update `GridRenderer`) to show a checkmark icon (✓) when `SumStatus == EXACT` and an X icon (✗) when `SumStatus == OVER`. Icons are in addition to color, not instead of it. Use `Material Icons` (already in Compose BOM).

Part C — Dynamic text: audit all `TextStyle` definitions to use `sp` units. Test at font scale 1.3x using `LocalDensity` override in tests. Fix any overflow or clipped text at larger font scales.

**Acceptance criteria**:
- All screens render without clipping at 360dp width
- Sum indicators show checkmark or X icon alongside color
- No text is clipped at 1.3x font scale
- All interactive elements have `contentDescription` for TalkBack
- All touch targets are >= 48dp

**Test requirements**:
- `ResponsiveLayoutTest.kt` (instrumented): at 360dp, assert no node bounds exceed screen width
- `ColorblindIndicatorTest.kt` (instrumented): when row sum is EXACT, checkmark icon node exists
- `FontScaleTest.kt` (instrumented): at 1.3x scale, all text nodes are visible (not clipped)

**Dependencies**: T023

---

### T028 — In-app review prompt (after 3rd puzzle completion)

**Sprint**: S03
**Feature**: S03-F010
**TDD phase**: [RED] → [GREEN] → [REFACTOR]

**Description**:
Implement `review/InAppReviewTrigger.kt`. Method: `suspend fun requestReviewIfEligible(activity: Activity)`. Logic: check DataStore flag `review_requested`; if true, return immediately (no-op). If false and `lifetimePuzzleCompletions >= 3`: create `ReviewManager` via `ReviewManagerFactory.create(context)`, call `manager.requestReviewFlow()`, launch the flow with `manager.launchReviewFlow(activity, reviewInfo)`. Set `review_requested = true` in DataStore after launching. Wrap everything in a try-catch — if Play Store unavailable (sideloaded APK), catch silently and set the flag so it's never retried. `InAppReviewTrigger` is called from `PuzzleViewModel` on the `false → true` completion transition.

**Acceptance criteria**:
- Review prompt is requested exactly once (DataStore flag prevents re-trigger)
- Prompt is only requested after the 3rd lifetime daily puzzle completion
- If Play Store unavailable, exception is caught silently — no error shown to user
- App does not show its own rating dialog
- Lifetime completion count is tracked in DataStore (separate from streak)

**Test requirements**:
- `InAppReviewTriggerTest.kt` (JVM unit test with FakeDataStore):
  - 2 completions → `requestReviewIfEligible` does nothing
  - 3rd completion → review is triggered once
  - 4th completion → `review_requested` flag is true → no trigger
  - Play Store unavailable → exception caught, flag set, no crash

**Dependencies**: T014

---

### T029 — Play Store screenshots (5 required)

**Sprint**: S03
**Feature**: S03-F011
**TDD phase**: [GREEN] (artifact capture, not code)

**Description**:
Capture the 5 required Play Store screenshots using an Android emulator at Pixel 5 profile (1080x2340, 440dpi). Screenshots are taken in the finalized S03 build with the deep indigo/amber theme and real puzzle content. Required screenshots:
1. Medium puzzle in-progress (mid-solve, several cells filled, sum indicators visible)
2. Completion card showing elapsed time and share button
3. Home screen with a 7-day streak and Weekly Warrior badge visible
4. Difficulty selector showing all three options with at least one completed today
5. Onboarding puzzle (launch 1, 1-cell puzzle with "Come back tomorrow" tooltip visible)

Save screenshots to `play-store/screenshots/` as `screenshot-01.png` through `screenshot-05.png` at 1080x1920px (portrait). Feature graphic (1024x500px) showing a stylized 4x4 grid on deep indigo background saved to `play-store/feature-graphic.png`.

**Acceptance criteria**:
- 5 screenshot PNG files exist in `play-store/screenshots/` at correct dimensions
- Feature graphic PNG exists in `play-store/` at 1024x500px
- Screenshots show the finalized visual design (not placeholder UI)
- No personal data or developer-identifying information visible in screenshots

**Test requirements**:
- File existence check: `ls play-store/screenshots/*.png | wc -l == 5`
- Image dimension check: each screenshot is 1080x1920px

**Dependencies**: T023, T026

---

### T030 — Play Store listing (ASO copy and internal testing submission)

**Sprint**: S03
**Feature**: S03-F012
**TDD phase**: [GREEN] (content creation and submission)

**Description**:
Write the Play Store listing copy and submit to internal testing track.

Short description (80 chars max): "Free daily number puzzle. Fill the grid, match the sums. No ads, no accounts."

Long description: ASO-optimized, 3,000 chars max. Include: mechanic explanation (fill NxN grid so every row and column sums to its target), daily habit hook (new puzzle every day, same puzzle for every player worldwide), differentiators (free, no ads, offline, 5 MB), keywords woven naturally: number puzzle, daily challenge, math game, logic puzzle, brain teaser, sudoku alternative, number grid, offline puzzle, free puzzle, daily math.

App name: "SumGrid - Daily Number Puzzle". Package: `org.dgeek.sumgrid`. Content rating: E (Everyone). Category: Puzzle. Pricing: Free.

Save final copy to `play-store/listing/short-description.txt`, `play-store/listing/long-description.txt`. Submit the signed release AAB to Play Console → Internal Testing track. Verify the internal testing link is accessible.

**Acceptance criteria**:
- Short description is <= 80 characters
- Long description is <= 3,000 characters and contains at least 8 of the target keywords
- App name is exactly "SumGrid - Daily Number Puzzle"
- Release AAB is signed with the upload key and submitted to internal testing
- Internal testing track URL is accessible (share with QA testers)

**Test requirements**:
- Character count check: `wc -c play-store/listing/short-description.txt` <= 80
- Keyword audit: long description contains "daily", "offline", "no ads", "number puzzle", "logic puzzle"

**Dependencies**: T029

---

## Task Summary

| ID | Task | Sprint | Feature(s) | TDD Phase | Priority |
|----|------|--------|-----------|-----------|----------|
| T001 | Project scaffold and Gradle setup | S01 | Scaffold | [GREEN] | [P] |
| T002 | Engine data models: Puzzle, Cell, Difficulty | S01 | S01-F001/F002 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T003 | xorshift128 PRNG | S01 | S01-F001 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T004 | Constraint propagation unique-solution validator | S01 | S01-F003 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T005 | Puzzle generator | S01 | S01-F002 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T006 | Difficulty calibrator | S01 | S01-F004 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T007 | 10,000-puzzle automated test suite (S01 gate) | S01 | S01-F005 | [RED]→[GREEN] | [P] |
| T008 | Compose Canvas grid renderer | S01 | S01-F006 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T009 | Cell tap-to-select and number pad input | S01 | S01-F007/F008 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T010 | Real-time row/column sum validation | S01 | S01-F009 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T011 | SumGridApplication and manual DI root | S02 | S02-F001 scaffold | [GREEN] | [P] |
| T012 | Daily puzzle repository (date-seeded) | S02 | S02-F001 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T013 | Background puzzle precompute worker | S02 | S02-F002 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T014 | Puzzle completion detection | S02 | S02-F003 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T015 | Elapsed timer (hidden during play) | S02 | S02-F005 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T016 | Share card generator | S02 | S02-F007 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T017 | Streak repository and milestone badges | S02 | S02-F009/F010 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T018 | Celebration animation (cascade) | S02 | S02-F004 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T019 | Completion card and one-tap share | S02 | S02-F008 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T020 | Difficulty selector UI | S02 | S02-F006 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T021 | Home screen with streak and countdown | S02 | S02-F011 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T022 | PuzzleScreen wiring (full daily play loop) | S02 | S02 integration | [GREEN]→[REFACTOR] | [P] |
| T023 | Material3 theme and dark mode | S03 | S03-F001/F002 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T024 | Firebase Analytics and Crashlytics | S03 | S03-F006/F007 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T025 | Onboarding puzzle sequence (1→2→4 cells) | S03 | S03-F008 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T026 | FTUE navigation and onboarding screen | S03 | S03-F009 | [RED]→[GREEN]→[REFACTOR] | [P] |
| T027 | Responsive layout and accessibility audit | S03 | S03-F003/F004/F005 | [RED]→[GREEN]→[REFACTOR] | — |
| T028 | In-app review prompt (3rd completion) | S03 | S03-F010 | [RED]→[GREEN]→[REFACTOR] | — |
| T029 | Play Store screenshots (5 required) | S03 | S03-F011 | [GREEN] | — |
| T030 | Play Store listing and internal testing submission | S03 | S03-F012 | [GREEN] | — |

**Totals**: 30 tasks — 10 backend/engine, 12 frontend/UI, 2 backend data, 4 backend service, 2 store assets

**Priority tasks [P]**: 26 out of 30 (all tasks T001–T026 are high-priority)

**TDD breakdown**:
- Tasks with [RED] phase: 24 (write failing test first)
- Tasks with [GREEN] phase: 30 (all tasks — make the test pass)
- Tasks with [REFACTOR] phase: 24 (improve without breaking)
- Pure [GREEN] tasks (no test required): 3 (T001, T011, T029/T030 are content/verification)

---

## Critical Path

```
T001 (scaffold)
  → T002 (data models)
    → T003 (PRNG) → T004 (validator) → T005 (generator) → T006 (calibrator) → T007 (10k suite)
    → T008 (grid renderer) → T009 (cell + number pad) → T010 (sum validation)
                                                           ↓
[S01 gate: T007 must pass before S02 begins]
                                                           ↓
T011 (Application DI) → T012 (daily repo) → T013 (precompute worker)
                       → T014 (completion detection) → T015 (timer) → T016 (share card)
                                                      → T017 (streak + badges)
                                                      → T018 (celebration) → T019 (completion card)
                                                      → T020 (difficulty selector) → T021 (home screen) → T022 (PuzzleScreen)
[S02 gate: T022 playable]
                                                           ↓
T023 (theme + dark mode) → T024 (Firebase) → T025 (onboarding repo) → T026 (FTUE nav)
                         → T027 (responsive + accessibility)
                         → T028 (in-app review)
                         → T029 (screenshots) → T030 (Play Store listing)
[S03 gate: APK < 4.5 MB, Firebase verified, internal testing submitted]
```

Any delay in T004 (validator) delays the entire epic. It must be prioritized first within S01.
