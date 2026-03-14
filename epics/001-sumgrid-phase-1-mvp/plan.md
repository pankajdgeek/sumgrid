# SumGrid Phase 1 MVP — Architecture and Implementation Plan

**Epic**: 001-sumgrid-phase-1-mvp
**Generated**: 2026-03-14
**Status**: Active
**Target Launch**: End of May 2026

---

## Architecture Overview

SumGrid Phase 1 is a fully offline, greenfield Android application built with Kotlin + Jetpack Compose. The architecture is a **single-module layered architecture** with three clear layers: Engine (pure JVM Kotlin), Data (DataStore persistence + WorkManager), and UI (Jetpack Compose). There is no network layer for gameplay — the only network-dependent components are Firebase Analytics and Crashlytics, which operate in offline-queue mode.

### Architecture Pattern: Offline-First Layered Architecture

```
┌─────────────────────────────────────────────────────┐
│                    UI LAYER                          │
│  Compose Screens + ViewModels + Navigation           │
│  HomeScreen, PuzzleScreen, OnboardingScreen          │
└──────────────────────┬──────────────────────────────┘
                       │ StateFlow / ViewModel
┌──────────────────────▼──────────────────────────────┐
│                   DATA LAYER                         │
│  Repositories (DataStore) + WorkManager              │
│  DailyPuzzleRepository, StreakRepository,            │
│  OnboardingRepository                                │
└──────────────────────┬──────────────────────────────┘
                       │ pure Kotlin data classes
┌──────────────────────▼──────────────────────────────┐
│                  ENGINE LAYER                        │
│  Pure Kotlin, zero Android deps, JVM-testable        │
│  Xorshift128, PuzzleGenerator, UniqueSolutionValidator│
│  DifficultyCalibrator, ShareCardGenerator            │
└─────────────────────────────────────────────────────┘
```

### Key Architectural Principles

1. **Engine layer has zero Android dependencies**: `engine/` package compiles to a plain JVM module. Unit tests run without an emulator or Robolectric. This is the critical path constraint — the 10,000-puzzle test suite must be runnable on CI with `./gradlew test` in under 60 seconds.

2. **Single source of truth per domain via DataStore**: Each domain (daily puzzle state, streak, onboarding) has exactly one repository backed by one DataStore `Preferences` file. No in-memory caches that can diverge from disk state.

3. **ViewModel mediates engine → UI**: `PuzzleViewModel` holds the live puzzle state as `StateFlow<PuzzleState>`. Engine calls are pure functions invoked in a background dispatcher. No business logic in Composables.

4. **Dependency injection via constructor injection**: No Hilt or Dagger in Phase 1 (adds APK size). Manual DI via `SumGridApplication.kt` creates repository singletons and passes them to ViewModelFactory. This keeps the APK under the 5 MB limit.

---

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── kotlin/org/dgeek/sumgrid/
│   │   │   ├── SumGridApplication.kt          # Application class, Firebase init, DI root
│   │   │   ├── engine/                        # ENGINE LAYER — zero Android deps
│   │   │   │   ├── Xorshift128.kt             # Custom PRNG (S01-F001)
│   │   │   │   ├── PuzzleGenerator.kt         # Seeded grid generation (S01-F002)
│   │   │   │   ├── UniqueSolutionValidator.kt # Constraint propagation (S01-F003)
│   │   │   │   ├── DifficultyCalibrator.kt    # Step counting (S01-F004)
│   │   │   │   └── models/
│   │   │   │       ├── Puzzle.kt              # Data: grid, sums, difficulty
│   │   │   │       ├── Cell.kt                # Data: value, isGiven, isSelected
│   │   │   │       └── Difficulty.kt          # Enum: BEGINNER, EASY, MEDIUM
│   │   │   ├── ui/                            # UI LAYER — Compose only
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HomeScreen.kt          # Difficulty selector + streak (S02-F011)
│   │   │   │   │   ├── PuzzleScreen.kt        # Grid + number pad + sum indicators
│   │   │   │   │   └── OnboardingScreen.kt    # FTUE puzzle flow (S03-F009)
│   │   │   │   ├── components/
│   │   │   │   │   ├── GridRenderer.kt        # Canvas grid (S01-F006)
│   │   │   │   │   ├── NumberPad.kt           # Bottom number input (S01-F008)
│   │   │   │   │   ├── SumIndicator.kt        # Row/col sum status (S03-F004)
│   │   │   │   │   ├── CelebrationAnimation.kt # Cascade animation (S02-F004)
│   │   │   │   │   ├── DifficultySelector.kt  # Difficulty tile row (S02-F006)
│   │   │   │   │   └── CompletionCard.kt      # Time + share button (S02-F008)
│   │   │   │   └── theme/
│   │   │   │       ├── Theme.kt               # Material3 theme entry point (S03-F001)
│   │   │   │       ├── Color.kt               # Design tokens (S03-F002)
│   │   │   │       ├── Type.kt                # Typography scale
│   │   │   │       └── Shape.kt               # Corner radius tokens
│   │   │   ├── viewmodel/
│   │   │   │   ├── PuzzleViewModel.kt         # Puzzle state machine (S01-F009)
│   │   │   │   ├── HomeViewModel.kt           # Home screen state + countdown
│   │   │   │   └── ViewModelFactory.kt        # Manual DI for ViewModels
│   │   │   ├── daily/                         # DATA LAYER — Daily puzzle domain
│   │   │   │   ├── DailyPuzzleRepository.kt   # Date→Puzzle (S02-F001)
│   │   │   │   └── PuzzlePrecomputeWorker.kt  # WorkManager (S02-F002)
│   │   │   ├── share/
│   │   │   │   └── ShareCardGenerator.kt      # Unicode emoji text (S02-F007)
│   │   │   ├── streak/
│   │   │   │   ├── StreakRepository.kt        # DataStore streak state (S02-F009)
│   │   │   │   └── StreakBadgeProvider.kt     # Milestone badge logic (S02-F010)
│   │   │   ├── onboarding/
│   │   │   │   └── OnboardingRepository.kt    # Launch count + completion (S03-F008)
│   │   │   ├── analytics/
│   │   │   │   ├── AnalyticsTracker.kt        # Interface + Firebase impl (S03-F006)
│   │   │   │   └── CrashlyticsInitializer.kt  # Crashlytics custom keys (S03-F007)
│   │   │   ├── review/
│   │   │   │   └── InAppReviewTrigger.kt      # Play In-App Review (S03-F010)
│   │   │   └── navigation/
│   │   │       └── AppNavigation.kt           # NavHost + FTUE routing (S03-F009)
│   │   └── res/
│   │       └── values/
│   │           └── strings.xml                # Minimal strings (en only)
│   └── test/
│       └── kotlin/org/dgeek/sumgrid/
│           ├── engine/
│           │   ├── Xorshift128Test.kt
│           │   ├── PuzzleGeneratorTest.kt     # 10,000-puzzle test suite (S01-F005)
│           │   ├── UniqueSolutionValidatorTest.kt
│           │   └── DifficultyTest.kt
│           └── share/
│               └── ShareCardGeneratorTest.kt
└── build.gradle.kts                           # R8 full mode, CI size gate, deps
```

---

## Component Design

### ENGINE LAYER

#### Xorshift128.kt

```kotlin
// Interface contract
class Xorshift128(seed: Long) {
    fun nextInt(bound: Int): Int  // [0, bound)
    fun nextLong(): Long
}
```

**Design**: Two 64-bit state words initialized from seed using splitmix64 mixing. Pure arithmetic — no stdlib random. State is mutable but the class is not thread-safe (single-threaded use in generation). Portable: identical output in Kotlin, Java, and TypeScript (verified by test).

**Critical invariant**: `nextInt(bound)` must produce the same sequence for the same seed across all JVM versions and Android API levels 24–35.

#### UniqueSolutionValidator.kt

```kotlin
sealed class SolutionCount { object NONE : SolutionCount(); object UNIQUE : SolutionCount(); object MULTIPLE : SolutionCount() }

class UniqueSolutionValidator {
    fun validate(grid: Array<IntArray>, rowTargets: IntArray, colTargets: IntArray, maxVal: Int): SolutionCount
}
```

**Algorithm**: Constraint propagation first (domain reduction), then backtracking with early termination. Once a second solution is found, returns `MULTIPLE` immediately without exhausting the search. Performance target: < 50 ms for 5x5 on mid-range hardware.

**Build first**: This is the hardest component and the quality gate. S01-F003 must be implemented and passing before S01-F002 (generator) can be completed.

#### PuzzleGenerator.kt

```kotlin
class PuzzleGenerator {
    fun generate(seed: Long, difficulty: Difficulty): Puzzle
}
```

**Algorithm**:
1. Seed Xorshift128 with `seed + difficulty.offset`
2. Fill NxN grid with values in `[1..maxVal]` using PRNG
3. Compute row and column target sums from the full grid
4. Remove cells one at a time in random order; call `UniqueSolutionValidator` after each removal
5. Stop when `targetEmptyCells` is reached
6. If a valid unique-solution puzzle cannot be formed within 100 attempts, increment seed offset by 10 and retry

**Data model**:
```kotlin
data class Puzzle(
    val size: Int,            // 3, 4, or 5
    val cells: Array<IntArray>, // solution values (all cells)
    val givenMask: Array<BooleanArray>, // true = pre-filled for player
    val rowTargets: IntArray,
    val colTargets: IntArray,
    val difficulty: Difficulty,
    val seed: Long
)
```

#### DifficultyCalibrator.kt

Counts constraint propagation steps during validation (steps without backtracking needed). Maps count to difficulty tier. Called by `PuzzleGenerator` to verify the generated puzzle meets the target difficulty before returning it.

#### ShareCardGenerator.kt

```kotlin
object ShareCardGenerator {
    fun generate(puzzle: Puzzle, userGrid: Array<IntArray>, elapsedMillis: Long, difficulty: Difficulty): String
}
```

Pure function. No Android dependencies. Given cells → ⬜ (U+2B1C), user-solved cells → 🟩 (U+1F7E9). Day number = `LocalDate.now().toEpochDay() - LocalDate.of(2026, 1, 1).toEpochDay() + 1`.

---

### DATA LAYER

#### DailyPuzzleRepository.kt

```kotlin
class DailyPuzzleRepository(
    private val dataStore: DataStore<Preferences>,
    private val generator: PuzzleGenerator
) {
    suspend fun getPuzzleForDate(date: LocalDate, difficulty: Difficulty): Puzzle
    suspend fun getCompletionState(date: LocalDate, difficulty: Difficulty): CompletionState?
    suspend fun saveCompletionState(state: CompletionState)
    suspend fun saveUserProgress(date: LocalDate, difficulty: Difficulty, userGrid: Array<IntArray>)
}
```

**DataStore key strategy**: Keys namespaced as `puzzle_{epochDay}_{difficulty}_{field}`. Stores: user grid cells (serialized as comma-separated ints), completion flag, elapsed time in milliseconds, completion timestamp.

#### StreakRepository.kt

```kotlin
class StreakRepository(private val dataStore: DataStore<Preferences>) {
    val streakState: Flow<StreakState>
    suspend fun recordCompletion(date: LocalDate)
}

data class StreakState(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastCompletionDate: LocalDate?,
    val earnedBadges: Set<StreakBadge>
)
```

**Streak update logic** (invoked in a coroutine from ViewModel):
- `lastCompletionDate == null` → streak = 1
- `lastCompletionDate == today` → no-op
- `lastCompletionDate == yesterday` → streak++
- else → streak = 1
Always update `longestStreak = max(longestStreak, streak)`.

#### OnboardingRepository.kt

```kotlin
class OnboardingRepository(private val dataStore: DataStore<Preferences>) {
    val launchCount: Flow<Int>
    val isComplete: Flow<Boolean>
    suspend fun incrementLaunchCount()
    suspend fun markComplete()
}
```

Hard-coded onboarding puzzles are `val` constants in a companion object within `OnboardingRepository`. Not generated — verified once at development time.

#### PuzzlePrecomputeWorker.kt

`CoroutineWorker` subclass. Triggered by `OneTimeWorkRequest` in `SumGridApplication.onCreate()` with `ExistingWorkPolicy.KEEP` (idempotent). Generates today's and tomorrow's puzzles for all three difficulties and stores results in DataStore. Uses a dispatchers.Default coroutine.

---

### UI LAYER

#### PuzzleViewModel.kt

**State machine**:
```kotlin
data class PuzzleState(
    val puzzle: Puzzle,
    val userGrid: Array<IntArray>,      // user-entered values
    val selectedCell: Pair<Int,Int>?,   // null = no selection
    val rowSumStatus: Array<SumStatus>, // UNDER, EXACT, OVER
    val colSumStatus: Array<SumStatus>,
    val isComplete: Boolean,
    val elapsedMillis: Long,
    val timerStarted: Boolean
)
```

**Key interactions**:
- `onCellTap(row, col)`: toggle selection, ignore if given cell
- `onNumberTap(n)`: fill selected cell; same number = clear; update sum status
- `onClearTap()`: clear selected cell
- Timer: `tickTimer()` called from a `LaunchedEffect` coroutine in PuzzleScreen every 1 second when `timerStarted && !isComplete`

**Completion detection**: Pure function `checkComplete(userGrid, puzzle)` called on every cell value change. On `false → true` transition, ViewModel persists completion state, updates streak, and triggers analytics event.

#### GridRenderer.kt

`@Composable` using `Canvas`. Layout:

```
┌─────────────────────────┐
│     C O L U M N S       │
│  ┌───┬───┬───┐  rowSum  │
│  │   │   │   │   →  12  │
│  ├───┼───┼───┤  →  9   │
│  │   │   │   │  →  15  │
│  └───┴───┴───┘          │
│   ↓   ↓   ↓             │
│   10  8   18            │
└─────────────────────────┘
```

Cell size = `(availableWidth - padding) / gridSize`. All drawing in `dp` units converted to `px` using `LocalDensity`. No hardcoded pixel values.

Cell visual states:
- **Given**: `MaterialTheme.colorScheme.surfaceVariant` fill, `MaterialTheme.colorScheme.onSurfaceVariant` text
- **User-filled**: `MaterialTheme.colorScheme.surface` fill, `MaterialTheme.colorScheme.onSurface` text
- **Selected**: border in `MaterialTheme.colorScheme.primary`, 3dp stroke
- **Empty**: `MaterialTheme.colorScheme.surface` fill, no text

#### CelebrationAnimation.kt

Reading-order cascade using `LaunchedEffect`. Each cell's scale and color animated via `Animatable`. Stagger: `delay(index * 50L)`. Scale keyframes: 1.0f → 1.2f → 1.0f. Color transition: normal fill → `MaterialTheme.colorScheme.primary`. Total duration ≤ 1500ms. Animation state stored in ViewModel as a `StateFlow<Boolean>` so the share button remains interactive during playback.

#### AppNavigation.kt

```
startDestination = determined at boot by OnboardingRepository.isComplete

ONBOARDING_COMPLETE = false:
  NavGraph: Onboarding → Home
  
ONBOARDING_COMPLETE = true:
  NavGraph: Home → Puzzle(difficulty)
```

`NavController` with three destinations: `onboarding`, `home`, `puzzle/{difficulty}`. Back stack is managed so back-press from `puzzle` returns to `home` (not re-opens puzzle).

---

## Theme and Design Tokens

Material3 dynamic color is NOT used (adds complexity and requires Android 12+). A static color scheme is defined:

```kotlin
// Color.kt
val DeepIndigoPrimary = Color(0xFF3730A3)    // deep indigo
val AmberAccent = Color(0xFFF59E0B)          // amber
val SumGreen = Color(0xFF22C55E)             // sum correct
val SumRed = Color(0xFFEF4444)               // sum over-target
val SumGray = Color(0xFF9CA3AF)              // sum under-target

// Theme.kt — one LightColorScheme, one DarkColorScheme
// Selected by isSystemInDarkTheme()
// All color references in Composables go through MaterialTheme.colorScheme.*
```

No hex literals in any `@Composable` or `Canvas` drawing code — all colors reference the `colorScheme`.

---

## Dependency Graph

### Sprint-Level

```
S01 (Core Engine + Grid UI)
  └── S02 (Daily Puzzle Loop)
        └── S03 (Polish + Firebase + Play Store)
```

### Within S01 (internal ordering)

```
S01-F001 (Xorshift128)
    ├── S01-F003 (Validator)          ← build in parallel
    │     └── S01-F002 (Generator)   ← needs Validator
    │           └── S01-F004 (Calibrator)
    │                 └── S01-F005 (10k Test Suite)  ← S01 gate
    └── S01-F006 (GridRenderer)      ← build in parallel with engine
          └── S01-F007 (Tap-to-Select)
                └── S01-F008 (NumberPad)
                      └── S01-F009 (Sum Validation)  ← S01 complete
```

### Within S02

```
S02-F001 (DailyPuzzleRepository)    ← needs S01 Puzzle model
    └── S02-F002 (PrecomputeWorker)
S02-F003 (CompletionDetection)      ← needs S01-F009
    ├── S02-F004 (CelebrationAnim)
    ├── S02-F005 (ElapsedTimer)
    ├── S02-F007 (ShareCardGen)
    │     └── S02-F008 (ShareIntent)
    └── S02-F009 (StreakRepository)
          └── S02-F010 (BadgeProvider)
S02-F006 (DifficultySelector)       ← needs S02-F001
S02-F011 (HomeScreen)               ← needs S02-F006, F009, F010
```

### Within S03

```
S03-F001 (Material3 Theme)          ← foundational, build first
    └── S03-F002 (Dark Mode)
S03-F003 (Responsive Layout)        ← needs S01-F006, F008, S02-F011
S03-F004 (Colorblind Indicators)    ← needs S01-F009, S03-F001
S03-F005 (Dynamic Text)             ← needs S03-F003
S03-F006 (Firebase Analytics)       ← standalone
    └── S03-F007 (Crashlytics)
S03-F008 (OnboardingRepository)     ← needs S01-F006-F009
    └── S03-F009 (FTUE Navigation)  ← needs S03-F008, S02-F004
S03-F010 (InAppReview)              ← needs S02-F003
S03-F011 (Screenshots)              ← needs S03-F001, S03-F002, S02-F008, S02-F011
    └── S03-F012 (ASO Listing)
```

---

## Key Technical Decisions

### Decision 1: Custom xorshift128, not `kotlin.random.Random(seed)`

**Rationale**: The Kotlin stdlib `Random(seed)` implementation explicitly warns the generated sequence may change between Kotlin/JVM versions. A daily puzzle game where every player worldwide must receive the same puzzle requires absolute determinism. xorshift128 is ~20 lines of pure arithmetic with no standard library dependency — identical output is guaranteed on API 24 through API 35 and portable to TypeScript for the future web version.

**Trade-off**: We own the PRNG implementation. If there is a bug, we fix it. The test suite verifies byte-for-byte identical output across seeds.

### Decision 2: Single-module, no Hilt DI

**Rationale**: Hilt adds approximately 100–200 KB to the APK and requires annotation processing, which slows builds. With a small number of singletons (three repositories, one WorkManager worker, Firebase services), manual constructor injection via `SumGridApplication` is sufficient. All dependencies are created once at app startup.

**Trade-off**: Adding features in Phase 2 that require scoped injection (e.g., per-screen ViewModels with different lifecycles) will require either adding Hilt at that point or careful manual scoping.

### Decision 3: Compose Canvas for grid, not `LazyGrid`

**Rationale**: The puzzle grid requires pixel-level control: custom cell borders, per-cell color states, sum indicator positioning, responsive sizing to fill exactly the available width. `LazyGrid` cannot achieve this without heavy decoration hacks. `Canvas` gives direct control and predictable performance.

**Trade-off**: More boilerplate for accessibility — each logical cell must have explicit `contentDescription` wired through `Modifier.semantics`. This is a required investment for NFR-007.

### Decision 4: Compose animations, not Lottie

**Rationale**: Lottie adds approximately 800 KB to the APK. The celebration cascade animation can be fully implemented using `Animatable` and `LaunchedEffect` with a 50ms stagger loop. APK size is a first-class constraint at 5 MB.

**Trade-off**: The animation implementation requires custom code and careful performance testing on 2 GB RAM devices. The simplicity of the animation (scale up, color shift, scale down) makes this a reasonable build.

### Decision 5: Text-based share card, not bitmap

**Rationale**: Generating a bitmap requires `Canvas.drawBitmap`, temporary file creation via `FileProvider`, and additional boilerplate for API 24 compatibility. A Unicode emoji text card is zero-cost to generate, renders correctly in every Android messaging app (WhatsApp, Telegram, SMS), and contains no library dependencies. Phase 2 can add image generation if analytics show text share rate is low.

**Trade-off**: Text cards may not render as visually distinctively as bitmap cards on some platforms. Monitored by `share_tapped` Firebase event.

### Decision 6: WorkManager precompute, not on-demand generation

**Rationale**: 5x5 Medium puzzle generation (including validator calls after each cell removal) can take 200–500 ms on low-end hardware. Blocking the first puzzle tap with a loading spinner violates the "instant" performance requirement (NFR-005). WorkManager runs on a background thread, stores results in DataStore, and ensures puzzle data is available before the player ever taps "Play."

**Trade-off**: If the device restarts between WorkManager completion and first play, the worker re-runs idempotently. The `ExistingWorkPolicy.KEEP` guard prevents double-generation.

### Decision 7: Hard-coded onboarding puzzles

**Rationale**: The onboarding progression (1 empty cell, 2 empty cells, 4 empty cells) requires guarantees of exact difficulty that the generator's calibration system cannot reliably provide at 3x3 scale. Hard-coded puzzles are verified correct at development time and remain stable across app updates.

**Trade-off**: Three small arrays of ints in the source. No maintenance burden.

---

## Risk Mitigations

| Risk | Mitigation |
|------|-----------|
| Validator too slow for CI (>60s for 10k puzzles) | Implement early-termination in backtracking — return MULTIPLE as soon as second solution found. Profile on CI machine before S01 gate. Cache domain reductions across validator calls in the same generation pass. |
| Puzzle has 0 or multiple solutions reaches production | `UniqueSolutionValidator` is mandatory — `PuzzleGenerator` is structurally incapable of returning a puzzle without calling it. 10,000-puzzle test suite is a hard CI gate before S02 begins. |
| APK exceeds 4.5 MB CI gate | Monitor with `./gradlew assembleRelease` size check in CI from Sprint 1. R8 full mode in `build.gradle.kts` from day 1. No Lottie, no bitmap assets, no sound in Phase 1. Firebase SDK pair (~1.2 MB) is the largest external dependency. |
| PRNG produces different output on different Android API levels | Test suite includes a determinism test: same seed on API 24, 28, 33, 35 emulators must produce byte-identical puzzle output. This runs in CI as part of the S01 gate. |
| Celebration animation drops below 30 fps on 2 GB RAM | Cap total animation duration at 1,500 ms. Use `animateFloatAsState` with `spring()` spec (hardware-accelerated). Test on API 24 emulator with 2 GB RAM configuration before S02 review. |
| Play Store review delay | Submit to internal testing track by end of Sprint 3, Week 1. Expect 1–2 week review lag. Build buffer into launch timeline. |
| Dark mode renders incorrectly | All colors reference `MaterialTheme.colorScheme.*` — never hardcoded. Screenshot tests in both light and dark mode as part of S03 quality gate. |

---

## Gradle and Build Configuration

```kotlin
// app/build.gradle.kts (key settings)
android {
    defaultConfig {
        minSdk = 24
        targetSdk = 35
        applicationId = "org.dgeek.sumgrid"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

// R8 full mode in gradle.properties
android.enableR8.fullMode=true

// CI size gate (GitHub Actions step)
// assert release APK size < 4.5 MB
```

**Dependencies (estimated APK impact)**:
| Library | Version | Size Impact |
|---------|---------|-------------|
| Jetpack Compose BOM | Latest stable | ~1.5 MB |
| Firebase Analytics | 21.x | ~800 KB |
| Firebase Crashlytics | 18.x | ~400 KB |
| Jetpack DataStore | 1.x | ~100 KB |
| WorkManager | 2.x | ~150 KB |
| Play In-App Review | 2.x | ~50 KB |
| Navigation Compose | 2.x | ~200 KB |
| **Total estimated** | | **~3.2-3.8 MB** |

---

## Data Flow Diagrams

### Daily Puzzle Load Flow

```
App Start
  │
  ├─► WorkManager (background)
  │     └─► PuzzleGenerator.generate(today's seed, difficulty)
  │           └─► Stores Puzzle in DataStore
  │
  └─► HomeScreen loads
        └─► DailyPuzzleRepository.getCompletionState(today)
              ├─► If complete: show CompletionCard (share/view only)
              └─► If not complete: show "Play" CTA
                    └─► User taps → PuzzleScreen
                          └─► PuzzleViewModel.loadPuzzle()
                                └─► DataStore (instant, precomputed)
```

### Cell Interaction Flow

```
User taps cell (row, col)
  └─► PuzzleViewModel.onCellTap(row, col)
        ├─► If given cell: no-op
        ├─► If selected == (row, col): selectedCell = null
        └─► Else: selectedCell = (row, col)

User taps number N
  └─► PuzzleViewModel.onNumberTap(N)
        ├─► If selectedCell == null: no-op
        ├─► If userGrid[row][col] == N: clear cell (= 0)
        └─► Else: userGrid[row][col] = N
              └─► Recompute rowSumStatus, colSumStatus
                    └─► If checkComplete(): trigger completion flow
                          ├─► Stop timer
                          ├─► Save CompletionState to DataStore
                          ├─► StreakRepository.recordCompletion(today)
                          ├─► AnalyticsTracker.puzzleCompleted(...)
                          └─► If 3rd lifetime completion: InAppReviewTrigger.request()
```

### Share Flow

```
User taps Share
  └─► ShareCardGenerator.generate(puzzle, userGrid, elapsedMillis, difficulty)
        └─► Returns String (Unicode emoji grid)
              └─► Context.startActivity(Intent(ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(EXTRA_TEXT, shareCard)
                  })
                    └─► Android native share sheet opens
                          └─► AnalyticsTracker.shareTapped(...)
```

---

## Testing Strategy

### Unit Tests (JVM, no emulator required)

| Test Class | Coverage |
|-----------|---------|
| `Xorshift128Test` | Determinism, sequence correctness, edge cases (seed 0, Long.MAX_VALUE) |
| `UniqueSolutionValidatorTest` | UNIQUE/NONE/MULTIPLE cases for 3x3/4x4/5x5, performance < 50ms |
| `PuzzleGeneratorTest` | 10,000 puzzles (all difficulties), determinism, UNIQUE assertion on all |
| `DifficultyTest` | Calibration correctness, step count ranges |
| `ShareCardGeneratorTest` | Format, day number, emoji encoding, no numbers in output |
| `StreakLogicTest` | All increment/no-op/reset cases, badge award logic |

### Android Instrumented Tests (emulator)

| Test | Coverage |
|-----|---------|
| `GridRendererTest` | Renders at 360dp and 420dp without clipping |
| `PuzzleViewModelTest` | State transitions, completion detection |
| `DataStoreTest` | Streak persistence across process restart |

### Manual QA Checklist (before Play Store submission)

- App runs on API 24, 28, 33, and 35 emulators
- Dark mode and light mode on all screens
- Offline mode: airplane mode, complete a puzzle, check share works
- Font scale 0.85x, 1.0x, 1.3x: layout does not break
- Screen widths 360dp, 390dp, 420dp: grid renders correctly
- Firebase DebugView: all 7 events received during smoke test
- APK size: `./gradlew assembleRelease && ls -la app/build/outputs/apk/release/`

---

## Quality Gates

| Gate | Criteria | Blocks |
|------|---------|--------|
| S01 Engine Gate | 10,000-puzzle test suite passes, zero failures, < 60s on CI | S02 start |
| S02 Completion Gate | All S02 features implemented, PuzzleViewModel tests pass | S03 start |
| S03 Pre-Submission Gate | APK < 4.5 MB, all screens pass dark mode check, Firebase events verified | Play Store submission |
| Play Store Gate | 5 screenshots uploaded, ASO listing complete, internal testing track approved | Production launch |

