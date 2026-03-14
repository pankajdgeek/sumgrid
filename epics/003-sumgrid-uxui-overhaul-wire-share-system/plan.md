# Architecture Plan — Epic 003: SumGrid UX/UI Overhaul

**Epic**: 003-sumgrid-uxui-overhaul-wire-share-system  
**Date**: 2026-03-14  
**Project type**: Android Kotlin + Jetpack Compose + Material3  
**Total scope**: ~141 hours across 4 sprints, 20 features

---

## Executive Summary

This epic is primarily a UI-wiring and screen-building effort on top of an already well-architected foundation. The single most important architectural insight from the codebase audit is that **zero new backend infrastructure is needed for Sprint 1** — `ShareCardGenerator`, `ShareIntentLauncher`, and `StreakBadge` are complete and merely need surface-level UI integration. The remaining sprints introduce two new screens (StatsScreen, PracticeScreen), one new ViewModel (StatsViewModel), one new data layer (NotificationScheduler), and several composable component extensions. No existing contracts are broken.

---

## Architecture

### Pattern: Layered Architecture with MVVM

The existing codebase is a clean MVVM layered architecture:

- **Data layer**: `DailyPuzzleRepository`, `CompletionStore`, `StreakRepository`, `DataStore`
- **Domain/ViewModel layer**: `PuzzleViewModel`, `HomeViewModel`, `OnboardingViewModel`
- **UI layer**: Screen composables + reusable component composables

All new work follows this same layered pattern. No architectural pivot is needed.

### Key Architectural Decisions

- **Decision 1: Share button wiring is pure UI (no ViewModel change required)**  
  `ShareCardGenerator.generate(puzzle, elapsedMillis, date)` and `shareResult(context, text)` are complete. `PuzzleViewModel` already exposes `elapsedMillis` and the puzzle via `uiState`. The only work is adding a `Button` composable that reads `LocalContext.current` and calls these functions. No ViewModel mutation.

- **Decision 2: Completion card replaces the bare text banner in PuzzleScreen**  
  The current `PuzzleScreen.kt` shows `"Puzzle complete! 🎉"` as a plain `Text` composable. Sprint 2 replaces this with a `CompletionCard` composable (new file: `ui/puzzle/CompletionCard.kt`) rendered as a `ModalBottomSheet` (Material3). The card receives `puzzle`, `elapsedMillis`, `onShare`, `onNextPuzzle`, and `onBackHome` lambdas. The card is owned by `PuzzleScreen`; no ViewModel changes are required beyond ensuring `puzzleDate` is accessible for share card generation.

- **Decision 3: Undo uses a move history stack inside PuzzleViewModel (no new ViewModel)**  
  `PuzzleViewModel` currently mutates `userValues` in-place (copy-on-write). Adding undo requires adding a `moveHistory: ArrayDeque<Array<IntArray>>` field to `PuzzleViewModel`. Each call to `enterNumber()` and `clearCell()` pushes the current `userValues` snapshot before mutating. A new `fun undo()` pops the stack and restores state. The `PuzzleUiState` gains a `canUndo: Boolean` field. The `NumberPad` composable gains an optional `onUndoTap` parameter. This is a surgical ViewModel extension — no new classes.

- **Decision 4: StatsScreen requires a new StatsViewModel + CompletionStore query API**  
  There is no existing stats aggregation. The `CompletionStore` interface (key = `"completion_${epochDay}_${difficulty.name}"`) stores per-puzzle results but provides no range-query capability. Two options: (a) extend `CompletionStore` with `getAll(): Map<String, CompletionState>` or (b) build a separate `StatsRepository` that reads all keys from `DataStore`. Decision: **extend `DataStoreCompletionStore` with `getAll()`** to avoid coupling stats to DataStore internals directly. `StatsViewModel` reads from this extended interface and computes aggregates (total solved, avg/best times, calendar dates). New files: `ui/stats/StatsScreen.kt`, `viewmodel/StatsViewModel.kt`.

- **Decision 5: PracticeScreen reuses PuzzleGenerator with random seeds**  
  `PuzzleGenerator.generate(seed, difficulty)` is deterministic on seed. For practice mode, seeds are generated from `System.currentTimeMillis()` (non-deterministic). Practice puzzles are NOT saved to `CompletionStore` (no streak impact). New files: `ui/practice/PracticeScreen.kt`, `viewmodel/PracticeViewModel.kt`. `PracticeViewModel` wraps `PuzzleViewModel` logic but skips `CompletionStore.save()`. The simplest implementation is to instantiate `PuzzleViewModel(completionStore = null)` for practice — the null guard in `persistCompletion()` already handles this.

- **Decision 6: Navigation additions use the existing NavHost pattern in SumGridNavigation.kt**  
  Two new routes are added to `Routes` object: `Routes.STATS = "stats"` and `Routes.PRACTICE = "practice/{difficulty}"`. Two new `composable()` blocks are added to the `NavHost`. `HomeScreen` gains two new callbacks: `onOpenStats: () -> Unit` and `onStartPractice: (Difficulty) -> Unit`. The existing animated transitions apply automatically.

- **Decision 7: Notifications use WorkManager (already a Gradle dependency in Android ecosystem)**  
  A `DailyReminderWorker` (`androidx.work.CoroutineWorker`) is scheduled via `WorkManager.enqueueUniquePeriodicWork`. The notification channel is registered in `SumGridApplication.onCreate()`. A user preference (time picker UI in HomeScreen or Settings) is stored in `DataStore`. New files: `notifications/DailyReminderWorker.kt`, `notifications/NotificationScheduler.kt`. No new Gradle dependencies needed if WorkManager is already present; it is standard in all Android projects.

- **Decision 8: Hard (6x6) and Expert (7x7) extend the Difficulty enum**  
  `Difficulty.kt` currently has `BEGINNER`, `EASY`, `MEDIUM`. Adding `HARD(size=6, maxVal=9, emptyCells=22, seedOffset=3)` and `EXPERT(size=7, maxVal=9, emptyCells=30, seedOffset=4)` is a one-line addition. `NumberPad` already handles `difficulty.maxVal` dynamically; the 2-row layout from S02-F003 covers this. `HomeViewModel.puzzleStatuses` uses `Difficulty.entries` — automatically includes new values. `DailyPuzzleRepository` seed formula uses `seedOffset` — no changes needed.

- **Decision 9: Font scaling and dark mode are theme/layout audits, not new components**  
  These are pass-by-pass fixes: replace hardcoded `sp` sizes with scalable equivalents, add `maxLines`/`overflow` where needed, and update dark theme color tokens in `Theme.kt`. The `GridColors.defaults()` hardcoded palette needs a dark-mode override path.

- **Decision 10: Pencil/notes mode adds a parallel notes grid to PuzzleViewModel + GridRenderer**  
  Notes mode stores candidate digits per cell as `Set<Int>`. `PuzzleUiState` gains `notesValues: Array<Array<Set<Int>>>` and `isNotesMode: Boolean`. `GridRenderer` renders up to 4 small superscript digits in cell corners when in notes mode. A pencil toggle button is added to `NumberPad` (or as a floating action button). This is the highest-complexity UI feature in S04.

---

## Component Reuse Analysis

### REUSE: ShareCardGenerator (org.dgeek.sumgrid.share)
- **Status**: Fully implemented, zero UI wiring
- **API**: `ShareCardGenerator.generate(puzzle, elapsedMillis, date): String`
- **Used by**: S01-F001 (share button), S02-F001 (completion card share CTA)
- **What to do**: Call from `PuzzleScreen` button onClick via `LocalContext.current`

### REUSE: ShareIntentLauncher / shareResult()
- **Status**: Fully implemented, zero UI wiring
- **API**: `shareResult(context, shareText)`
- **Used by**: S01-F001, S02-F001
- **What to do**: Call from share button onClick lambda

### REUSE: StreakBadge enum (org.dgeek.sumgrid.streak)
- **Status**: Fully defined with 4 values + metadata (requiredDays, displayName, icon emoji)
- **Used by**: S01-F002 (badge display on HomeScreen)
- **What to do**: Read `earnedBadges: Set<StreakBadge>` from `StreakState` (already in HomeViewModel flow); render a `BadgeRow` composable in HomeScreen

### REUSE: StreakState.earnedBadges
- **Status**: `StreakState` already carries `earnedBadges: Set<StreakBadge>`, populated by `StreakRepository.recordCompletion()`
- **Used by**: S01-F002
- **What to do**: HomeViewModel must expose earnedBadges through HomeUiState (minor HomeUiState extension)

### REUSE: PuzzleViewModel.elapsedMillis + PuzzleViewModel.uiState.puzzle
- **Status**: Both properties are already public
- **Used by**: S01-F001, S02-F001 (share card generation needs puzzle + elapsed time + date)
- **What to do**: Pass `puzzleDate` (currently private to PuzzleViewModel) as a new public getter, or pass it down from the NavHost `LaunchedEffect` where it is already available

### REUSE: PuzzleGenerator.generate(seed, difficulty)
- **Status**: Fully implemented, deterministic
- **Used by**: S03-F003 (PracticeScreen — random seeds via currentTimeMillis)
- **What to do**: Instantiate in PracticeViewModel with `completionStore = null`

### REUSE: GridRenderer composable
- **Status**: Fully implemented, accepts `PuzzleUiState`
- **Used by**: S03-F003 (PracticeScreen reuses GridRenderer + NumberPad identical to PuzzleScreen)
- **What to do**: PracticeScreen is structurally identical to PuzzleScreen; extract a shared `PuzzleContent` composable or simply duplicate the screen with practice-specific differences

### REUSE: CelebrationAnimation (CelebrationState, rememberCelebrationState, CelebrationCellWrapper)
- **Status**: Implemented (scale pulse animation), but not yet wired to GridRenderer cells
- **Used by**: S03-F004 (enhanced celebration — extends existing state machine)
- **What to do**: Wire `CelebrationCellWrapper` onto each grid cell in `GridRenderer`; extend `CelebrationState` with confetti particle system

### REUSE: CompletionStore interface
- **Used by**: S03-F001 (StatsScreen — needs historical read access)
- **What to do**: Add `getAll(): Map<String, CompletionState>` to interface and `DataStoreCompletionStore`

### REUSE: DifficultySelector composable
- **Status**: Fully implemented
- **Used by**: S03-F003 (PracticeScreen difficulty selection)
- **What to do**: Import and use as-is

### REUSE: SumGridNavigation animated transitions
- **Status**: Slide+fade transitions defined in NavHost-level enter/exit specs
- **Used by**: S03-F001 (StatsScreen nav), S03-F003 (PracticeScreen nav)
- **What to do**: New composable destinations automatically inherit NavHost-level transitions

**Total identified reuse opportunities: 11**

---

## State Management: Undo (Move History Stack)

Current `PuzzleViewModel` approach: each `enterNumber()` and `clearCell()` creates a new `Array<IntArray>` snapshot (copy-on-write) before assigning to `_uiState`. This makes history cheap to implement.

### Proposed undo implementation inside PuzzleViewModel:

```
private val moveHistory: ArrayDeque<Array<IntArray>> = ArrayDeque()
private val MAX_HISTORY = 50  // cap memory usage

fun enterNumber(number: Int) {
    val current = _uiState.value ?: return
    val (row, col) = current.selectedCell ?: return
    // Push snapshot before mutation
    moveHistory.addLast(current.userValues.deepCopy())
    if (moveHistory.size > MAX_HISTORY) moveHistory.removeFirst()
    // ... existing mutation logic ...
}

fun undo() {
    if (moveHistory.isEmpty()) return
    val previous = moveHistory.removeLast()
    val current = _uiState.value ?: return
    val newState = buildState(current.puzzle, previous, current.selectedCell, current.elapsedSeconds)
    _uiState.value = newState
    handleCompletionChange(newState)
}
```

`PuzzleUiState.canUndo: Boolean` = `moveHistory.isNotEmpty()` (computed each time state is built, or emitted as a separate StateFlow).

The `moveHistory` stack is cleared on `loadPuzzle()` to prevent cross-puzzle undo.

---

## State Management: Auto-Save (S02-F006)

Current behavior: `PuzzleViewModel.persistCompletion()` saves only on *completion* (puzzle fully solved). Mid-puzzle state is lost on process death.

### Proposed auto-save approach:

On every `enterNumber()` and `clearCell()`, write a compact serialized snapshot of `userValues` (flat IntArray) to `DataStore` under key `"in_progress_${epochDay}_${difficulty.name}"`. On `loadPuzzle()`, check for an in-progress key and restore it if present. Clear the in-progress key when `persistCompletion()` is called (puzzle done).

No new interface needed — extend `CompletionStore` with:
```
suspend fun saveInProgress(key: String, values: IntArray)
suspend fun getInProgress(key: String): IntArray?
suspend fun clearInProgress(key: String)
```

Or use a separate `DataStore<Preferences>` instance for in-progress state to avoid polluting completion records.

---

## Navigation Architecture (New Screens)

### Current routes in SumGridNavigation.kt:
- `"onboarding"` → OnboardingScreen
- `"home"` → HomeScreen
- `"puzzle/{difficulty}"` → PuzzleScreen

### New routes to add:
- `"stats"` → StatsScreen (S03-F001)
- `"practice/{difficulty}"` → PracticeScreen (S03-F003)

### Navigation entry points:
- HomeScreen gets `onOpenStats: () -> Unit` callback (icon button in TopAppBar or footer link)
- HomeScreen gets `onStartPractice: (Difficulty) -> Unit` callback (new "Practice" button, visible only when all daily puzzles complete)
- CompletionCard (S02-F001) gets `onNextPuzzle: () -> Unit` and `onBackHome: () -> Unit` callbacks
- PracticeScreen back navigation → Home (standard back stack pop)
- StatsScreen back navigation → Home (standard back stack pop)

### ViewModelFactory changes:
`ViewModelFactory` needs to provide `StatsViewModel` and `PracticeViewModel`. Both require `DailyPuzzleRepository` (already in `AppContainer`). `StatsViewModel` additionally needs the extended `CompletionStore`.

---

## Notification Architecture (S03-F002)

### Components:
1. `NotificationScheduler` — wrapper around `WorkManager` API. Exposes `schedule(hour: Int, minute: Int)` and `cancel()`.
2. `DailyReminderWorker: CoroutineWorker` — posts the notification. Uses `NotificationManagerCompat` to build and show the notification.
3. Notification channel registration — in `SumGridApplication.onCreate()`, register channel `"daily_reminder"` (importance: `IMPORTANCE_DEFAULT`).
4. Permission handling — Android 13+ requires `POST_NOTIFICATIONS` runtime permission. A `NotificationPermissionBanner` composable is shown in HomeScreen if permission not granted.
5. Preferences UI — a time picker dialog (Material3 `TimePickerDialog`) in HomeScreen or a lightweight Settings screen.

### Manifest additions:
- `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` (API 33+)
- `<service android:name=".notifications.DailyReminderWorker" ... />` (WorkManager handles worker registration via metadata)

---

## New Files to Create

### Sprint 1 (modifications only)
| File | Change type | Notes |
|------|-------------|-------|
| `ui/screens/PuzzleScreen.kt` | Modify | Add share button + TopAppBar |
| `ui/screens/HomeScreen.kt` | Modify | Add badge row, scrollable layout, all-done state |
| `ui/screens/OnboardingScreen.kt` | Modify | Add skip button + rules overlay |

### Sprint 2 (new + modify)
| File | Change type | Notes |
|------|-------------|-------|
| `ui/puzzle/CompletionCard.kt` | New | ModalBottomSheet completion card |
| `ui/screens/PuzzleScreen.kt` | Modify | Replace text banner with CompletionCard |
| `viewmodel/PuzzleViewModel.kt` | Modify | Add move history stack + undo() + canUndo |
| `ui/components/NumberPad.kt` | Modify | Add undo button + 2-row layout for maxVal>=7 |
| `ui/theme/Theme.kt` | Modify | Dark mode contrast fixes |
| `daily/CompletionStore.kt` | Modify | Add in-progress save/restore methods |
| `daily/DataStoreCompletionStore.kt` | Modify | Implement in-progress persistence |

### Sprint 3 (new files)
| File | Change type | Notes |
|------|-------------|-------|
| `ui/stats/StatsScreen.kt` | New | Stats composable |
| `viewmodel/StatsViewModel.kt` | New | Aggregation ViewModel |
| `ui/practice/PracticeScreen.kt` | New | Practice mode screen |
| `viewmodel/PracticeViewModel.kt` | New | Practice mode ViewModel |
| `notifications/DailyReminderWorker.kt` | New | WorkManager worker |
| `notifications/NotificationScheduler.kt` | New | WorkManager wrapper |
| `ui/components/CelebrationAnimation.kt` | Modify | Add confetti particle system |
| `navigation/SumGridNavigation.kt` | Modify | Add stats + practice routes |
| `SumGridApplication.kt` | Modify | Register notification channel |
| `daily/CompletionStore.kt` | Modify | Add getAll() for stats |

### Sprint 4 (modify)
| File | Change type | Notes |
|------|-------------|-------|
| `ui/components/GridRenderer.kt` | Modify | Error shake animation + notes rendering + TalkBack ordering |
| `ui/components/NumberPad.kt` | Modify | Pencil toggle button |
| `viewmodel/PuzzleViewModel.kt` | Modify | Notes mode state (notesValues, isNotesMode) |
| `engine/models/Difficulty.kt` | Modify | Add HARD + EXPERT |
| `AndroidManifest.xml` | Modify | Portrait lock or landscape layout |
| `ui/screens/HomeScreen.kt` | Modify | Reduce motion support |

---

## Data Flow Diagrams

### Share System (S01-F001 → S02-F001)

```
PuzzleScreen composable
  │  reads: uiState.puzzle, vm.elapsedMillis, puzzleDate (from NavHost)
  │
  ├─ [Share Button onClick]
  │    → ShareCardGenerator.generate(puzzle, elapsedMillis, date) : String
  │    → shareResult(LocalContext.current, shareText)
  │         → Android Intent.ACTION_SEND
  │         → System share sheet
  │
  └─ [CompletionCard - S02] 
       ModalBottomSheet(visible = isCompleted)
         → same share flow as above
```

### Badge Display (S01-F002)

```
StreakRepository (DataStore)
  → StreakState.earnedBadges: Set<StreakBadge>
  → HomeViewModel observeStreak()
  → HomeUiState.earnedBadges: Set<StreakBadge>   ← new field
  → HomeScreen BadgeRow composable
       StreakBadge.entries.forEach { badge ->
           BadgeItem(badge, isEarned = badge in earnedBadges)
       }
```

### Statistics Data Flow (S03-F001)

```
CompletionStore.getAll() : Map<String, CompletionState>
  → key pattern: "completion_${epochDay}_${difficulty.name}"
  → StatsViewModel.loadStats()
       parse keys → group by difficulty
       compute: totalSolved, avgTimePerDifficulty, bestTimePerDifficulty
       build: calendarDates: Set<LocalDate>
  → StatsUiState
  → StatsScreen
       TotalSolvedCard, TimeCard, CalendarHeatmap
```

### Practice Mode Data Flow (S03-F003)

```
HomeScreen "Practice" button
  → onStartPractice(selectedDifficulty)
  → navController.navigate(Routes.practice(difficulty))
  
PracticeScreen
  → PracticeViewModel.generatePuzzle(difficulty)
       seed = System.currentTimeMillis()
       PuzzleGenerator.generate(seed, difficulty) : Puzzle
       PuzzleViewModel(completionStore = null).loadPuzzle(puzzle)  ← no persistence
  → Same GridRenderer + NumberPad as PuzzleScreen
  → Completion: "Play again" button → regenerate with new seed
              "Back home" button → navController.popBackStack()
```

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| `puzzleDate` not accessible in PuzzleScreen for share card | Low | Medium | Pass date from NavHost LaunchedEffect as a ViewModel parameter (already loaded via `puzzleVm.loadPuzzle(puzzle, today)`) |
| WorkManager not in Gradle dependencies | Low | Medium | Check `app/build.gradle.kts` before S03-F002; add `androidx.work:work-runtime-ktx` if absent |
| CompletionStore `getAll()` implementation complexity with DataStore | Medium | Low | DataStore `data.first()` returns all preferences; filter by key prefix `"completion_"` |
| `StreakRepository` uses DataStore but `HomeViewModel` injects `InMemoryStreakRepository` | High | Medium | BLOCKER: HomeViewModel currently receives `InMemoryStreakRepository`, not `StreakRepository`. `InMemoryStreakRepository` does not persist and does not read from DataStore. Badge display requires the real `StreakRepository`. Must wire `AppContainer.streakRepository` (DataStore-backed) into HomeViewModel. |
| NumberPad 2-row layout breaks existing unit tests | Low | Low | Test `NumberPad` layout with `maxVal=9`; update test assertions for row count |
| CalendarHeatmap composable complexity | Medium | Low | Start with simple Grid-based approach; no external charting library needed |
| Android 13 POST_NOTIFICATIONS permission flow | Medium | Medium | Implement permission rationale dialog before scheduling; graceful degradation if denied |

---

## BLOCKER: HomeViewModel Receives InMemoryStreakRepository

`HomeViewModel` constructor signature:
```kotlin
class HomeViewModel(
    private val puzzleRepository: DailyPuzzleRepository,
    private val streakRepository: InMemoryStreakRepository,  // ← wrong type
    ...
)
```

`InMemoryStreakRepository` does not call `recordCompletion()` with real dates and resets on app restart. This means `StreakState.earnedBadges` is always empty at runtime. Before S01-F002 (badge display) can show real data, `HomeViewModel` must be updated to receive the `StreakRepository` (DataStore-backed) interface or a common supertype.

**Resolution required in S01**: Update `HomeViewModelFactory` in `SumGridNavigation.kt` to inject `container.streakRepository` (the DataStore-backed instance). Update `HomeViewModel` constructor to accept the interface or base type. Verify `StreakRepository.streakState` returns a `Flow<StreakState>` (it does).

---

## Quality Gates

- All new composables must pass Material3 design token compliance (no hardcoded colors).
- All new screens must include `semantics { contentDescription }` for TalkBack.
- `PuzzleViewModel` undo logic requires unit tests covering: undo empty stack (no-op), undo after enterNumber, undo after clearCell, undo does not cross loadPuzzle boundary.
- `StatsViewModel` aggregation logic requires unit tests with mock completion data.
- Font scaling: test all screens at 200% in emulator before Sprint 2 ship.
- Dark mode: run `DarkModeContrastTest` after theme changes in Sprint 2.

