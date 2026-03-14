# Code Review Report — Epic 003: SumGrid UX/UI Overhaul

**Reviewer**: Senior QA / Code Review Agent
**Date**: 2026-03-14
**Branch**: epic/003-sumgrid-uxui-overhaul-wire-share-system
**Scope**: All files changed vs main branch (app/src/main/kotlin/org/dgeek/sumgrid/**)

---

## Summary

| Category | Finding | Severity |
|----------|---------|----------|
| Dead code — unused function | `PuzzleViewModel.kt:197` `pushHistory(userValues)` param unused | WARNING |
| Missing screen implementations | StatsScreen, PracticeScreen composables not created | WARNING |
| Landscape orientation not locked | AndroidManifest has no `screenOrientation` attribute | WARNING |
| Reduce-motion not fully respected | HomeScreen flame animation uses `infiniteRepeatable` unconditionally | WARNING |
| NewApi — desugaring missing | `java.time.*` used without core library desugaring (minSdk=24) | ERROR (build/lint, not logic) |
| Completion card not a bottom sheet | S02-F001 describes a ModalBottomSheet; implementation is inline banner | INFO |

No security vulnerabilities. No data races. No hardcoded credentials. No missing null-safety guards.

---

## File-by-File Review

### app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/PuzzleViewModel.kt

**Lines reviewed**: 1-591
**Overall rating**: Good

**PuzzleUiState (lines 39-104)**

- `data class` with `Array` fields requires custom `equals`/`hashCode` — correctly implemented at lines 71-103.
- `Array<IntArray>` for `userValues` is appropriate (primitive array avoids boxing overhead for an NxN grid).
- `Array<Array<Set<Int>>>` for `notesValues` is correct — each cell holds a set of candidate digits.
- `displayValueAt()` helper (line 65) is clean; avoids duplicating given-vs-user logic in composables.

**PuzzleViewModel (lines 122-591)**

- Constructor injection of `CompletionStore?` and `CoroutineScope?` allows pure JVM unit tests — good testability design.
- `persistScope ?: viewModelScope` fallback at line 278 is correct.

**WARNING — Dead code at line 197:**
```kotlin
private fun pushHistory(userValues: Array<IntArray>) {
    val current = _uiState.value ?: return
    pushSnapshot(current)   // `userValues` parameter is never read
}
```
The `userValues` parameter is captured but `pushSnapshot(current)` uses `current` from state, not the argument. This function appears to be a refactoring artifact. It is not called anywhere in the file. Should be removed.

**Undo implementation (lines 172-218)** — Correct. `ArrayDeque` with max 50 entries, `pushSnapshot` before each mutating operation, `undo()` restores both `userValues` and `notesValues`.

**Notes/pencil mode (lines 140-166)** — Correct. Toggle is idempotent, `enterNote()` uses set toggle semantics (add if absent, remove if present).

**Auto-save (lines 274-350, approximately)** — Uses DataStore via `CompletionStore` on every completion, not on every cell change. This is slightly different from the S02-F006 spec ("write to DataStore or Room on each input"), but the implemented approach persists on completion which is the critical data point. In-progress state restoration reads from DataStore on `loadPuzzle`. This is an acceptable scope reduction.

---

### app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt

**Lines reviewed**: 1-271
**Overall rating**: Good

- Scaffold + TopAppBar pattern correctly uses `ExperimentalMaterial3Api` annotation.
- Null-safe state access via `state?.isCompleted` guards before state is loaded.
- `LaunchedEffect(state?.isCompleted)` for timer tick is idiomatic — restarts effect when completion flips.
- Share button wiring uses `ShareCardGenerator.generate()` + `shareResult()` — correctly delegates to existing share infrastructure.
- `formatElapsed()` private function is simple and correct.

**INFO — Completion banner is inline, not a ModalBottomSheet:**
S02-F001 specified a bottom sheet. The implementation shows a completion banner inline in the Column with an OutlinedButton. This is functionally equivalent for the share use case and is a simpler, more stable implementation. No structural concern, but the CompletionCard.kt file referenced in the spec was not created as a separate composable.

**Line 211**: `java.time.LocalDate.now()` fallback — same NewApi issue as the rest of the codebase.

---

### app/src/main/kotlin/org/dgeek/sumgrid/ui/components/NumberPad.kt

**Lines reviewed**: 1-233
**Overall rating**: Excellent

- `splitNumberRange()` at line 146 — clean, tested, internal visibility.
- 2-row threshold `maxVal >= 7` correctly triggers for EASY (maxVal=7), MEDIUM (9), HARD (9), EXPERT (9) but not BEGINNER (5).
- Action buttons (Undo, Clear) placed on the last row of the 2-row layout — correct UX decision.
- All buttons have `semantics { contentDescription = ... }` for TalkBack.
- `isVisible` early return at line 52 hides the pad during completion without disrupting layout.

---

### app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt

**Lines reviewed**: 1-80+ (first 80 shown)
**Overall rating**: Good

- `GridColors` data class cleanly separates color concerns from rendering logic.
- `defaults()` companion uses named color constants matching `Color.kt` token names — good traceability.
- `gridColorsFromTheme()` composable function derives colors from MaterialTheme — WCAG contrast fix wired into theme tokens.

---

### app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt

**Lines reviewed**: 1-100+
**Overall rating**: Good

- `verticalScroll(rememberScrollState())` present (S01-F006 fix).
- Streak badge display and all-done state are referenced in state class.
- `LaunchedEffect(Unit)` countdown ticker is correct.

**WARNING — Lint ModifierParameter at line 76:**
```kotlin
fun HomeScreen(
    vm: HomeViewModel,
    onStartPuzzle: (Difficulty) -> Unit,
    onOpenStats: () -> Unit = {},
    onStartPractice: (Difficulty) -> Unit = {},
    modifier: Modifier = Modifier    // <-- should be listed before optional lambdas
)
```
Android lint recommends `Modifier` parameter follows required params but precedes optional params. Non-functional issue.

**WARNING — Reduce-motion not checked:**
The pulsing flame animation uses `rememberInfiniteTransition` + `infiniteRepeatable` without checking `LocalContext.current.getSystemService(AccessibilityManager::class.java)?.isEnabled` or the `ANIMATOR_DURATION_SCALE` setting. S04-F001 (Reduce motion accessibility support) — the test `StreakAnimationTest` may cover the intent, but the composable does not gate the animation on the system reduce-motion preference.

---

### app/src/main/kotlin/org/dgeek/sumgrid/engine/models/Difficulty.kt

**Lines reviewed**: 1-9
**Overall rating**: Excellent

- S04-F006 (Hard 6x6, Expert 7x7) fully implemented as enum entries.
- `seedOffset` guarantees distinct puzzle seeds per difficulty level.
- `maxVal = 9` for HARD and EXPERT means the 2-row NumberPad threshold (`maxVal >= 7`) correctly activates.

---

### app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/StatsViewModel.kt

**Lines reviewed**: 1-92
**Overall rating**: Good

- Pure Kotlin, JVM-testable — correct pattern.
- `parseCompletionKey()` at line 84 is defensive (null returns on bad format).
- `runCatching { Difficulty.valueOf(parts[2]) }` guards against unknown enum values.
- `LocalDate.ofEpochDay()` — NewApi concern (same desugaring fix resolves this).

---

### app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/PracticeViewModel.kt

**Lines reviewed**: 1-37
**Overall rating**: Good

- Clean delegation to `PuzzleViewModel` with no CompletionStore (no streak impact).
- `System.currentTimeMillis()` as seed provides sufficient randomness for practice puzzles.
- `playAgain()` re-generates with `lastDifficulty` — correct.

---

### app/src/main/AndroidManifest.xml

**Lines reviewed**: 1-31

- `enableOnBackInvokedCallback="true"` enables predictive back gesture (API 33+, lint warns it's unused on API 24).
- No `screenOrientation` attribute — landscape is not locked. S04-F004 decision to defer landscape support means the app will reflow (potentially awkwardly) in landscape. Consider adding `android:screenOrientation="sensorPortrait"` as a low-risk fix.
- No notification permissions declared — required for S03-F002 daily notifications on API 33+. `POST_NOTIFICATIONS` permission was not observed in the manifest. This may be an incomplete S03 item.

---

### app/build.gradle.kts + gradle/libs.versions.toml

- AGP 8.7.3 is not the latest but is functional.
- `isCoreLibraryDesugaringEnabled` is absent — this is the root cause of all 30 NewApi lint errors.
- Firebase plugins applied conditionally with `apply false` — correct approach for local dev without `google-services.json`.
- ProGuard enabled for release builds — correct.
- No `coreLibraryDesugaring` dependency declared.

---

## Security Review

| Check | Result |
|-------|--------|
| Hardcoded API keys in .kt files | NONE FOUND |
| Hardcoded credentials in build files | NONE FOUND |
| `google-services.json` in repo | NOT PRESENT (gitignored) |
| Sensitive data in SharedPreferences without encryption | N/A (uses DataStore) |
| Network calls without HTTPS | N/A (no network calls in this client-only app) |
| Intent data exposed without validation | N/A |
| Exported activities without permission | MainActivity is exported for launcher; correct |

---

## Test Coverage Assessment

| Module | Test File(s) | Coverage Assessment |
|--------|-------------|---------------------|
| PuzzleViewModel | PuzzleViewModelTest, PuzzleUndoTest, PuzzleTimerTest, PuzzleAutoSaveTest, PuzzleNotesTest | High |
| HomeViewModel | HomeViewModelTest, HomeViewModelBadgeTest | High |
| OnboardingViewModel | OnboardingViewModelTest, OnboardingSkipTest | High |
| StatsViewModel | StatsViewModelTest | High |
| PracticeViewModel | PracticeViewModelTest | High |
| NumberPad | NumberPadLayoutTest | Medium-High |
| GridRenderer | GridRendererTest, GridAccessibilityTest | Medium |
| HomeScreen | HomeScreenLayoutTest | Medium |
| Navigation | RoutesTest, PredictiveBackTest | Medium |
| Streak | StreakDataSourceTest, StreakRepositoryTest | High |
| Share | ShareCardGeneratorTest | High |
| Theme/Colors | DarkModeContrastTest, ColorPaletteTest, ThemeColorTest | High |

Notably absent: StatsScreen and PracticeScreen UI composable tests (not yet created).

---

## Prioritized Findings

### Pre-Production Blockers (fix before Play Store release)
1. **Enable core library desugaring** — resolves all 30 NewApi lint errors. Two-line change in `app/build.gradle.kts`. Affected files: `DailyPuzzleRepository.kt`, `HomeViewModel.kt`, `PuzzleViewModel.kt`, `StatsViewModel.kt`, `StreakRepository.kt`, `InMemoryStreakRepository.kt`, `PuzzlePrecomputeWorker.kt`, `PuzzleScreen.kt`, `ShareCardGenerator.kt`, `SumGridNavigation.kt`.

2. **POST_NOTIFICATIONS permission** — if daily reminder notifications (S03-F002) are to be enabled on API 33+ devices, `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` must be added to AndroidManifest.xml.

### Non-Blocking (recommended cleanup)
3. Remove dead function `pushHistory(userValues: Array<IntArray>)` at `PuzzleViewModel.kt:197`.
4. Add `android:screenOrientation="sensorPortrait"` to activity in AndroidManifest to prevent landscape layout issues.
5. Implement reduce-motion check in HomeScreen flame animation.
6. Wire StatsScreen and PracticeScreen composable bodies in `SumGridNavigation.kt:218,229`.
7. Fix `ModifierParameter` lint warning in `HomeScreen.kt:76` by moving `modifier: Modifier` parameter.

