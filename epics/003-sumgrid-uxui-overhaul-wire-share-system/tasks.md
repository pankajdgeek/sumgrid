# Tasks — Epic 003: SumGrid UX/UI Overhaul

**Epic**: 003-sumgrid-uxui-overhaul-wire-share-system  
**Date**: 2026-03-14  
**Workflow**: Epic (4 sprints, sequential)  
**Test command**: `./gradlew :app:testDebugUnitTest`  
**Total tasks**: 28  
**TDD cycle**: Every task follows Red → Green → Refactor

---

## Legend

- `[RED]` — Write the failing test first; no implementation exists yet
- `[GREEN]` — Write the minimum implementation to make the test pass
- `[REFACTOR]` — Clean up, extract, and harden without changing behaviour
- `[P]` — High-priority task (must not slip; blocks other work)
- `deps:` — Task IDs that must complete before this task starts
- `parallel:` — Tasks in the same sprint that can run concurrently with this one

---

## Sprint 1 — Quick Wins & Critical Fixes (~15h)

**Goal**: Surface already-built systems (share, badges), fix onboarding friction, add back button,
make HomeScreen scrollable, show all-done state. Zero new architecture required.

**Parallelization map (S01)**:

```
[T001] StreakRepository wiring (BLOCKER — must finish first, ~1h)
  │
  ├─── Track A: PuzzleScreen (T002 → T003)
  ├─── Track B: HomeScreen   (T004 → T005 → T006)
  └─── Track C: OnboardingScreen (T007 → T008)
```

---

### T001 — [P] Fix StreakRepository injection in HomeViewModel

**Feature**: BLOCKER fix (prerequisite to S01-F002)  
**Phase**: [GREEN] then [REFACTOR] (bug fix — no new test needed, existing `HomeViewModelTest` is the RED)  
**Files**:
- `app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/HomeViewModel.kt` — change constructor param from `InMemoryStreakRepository` to `StreakRepository` (the interface or base type)
- `app/src/main/kotlin/org/dgeek/sumgrid/navigation/SumGridNavigation.kt` — inject `container.streakRepository` (DataStore-backed) instead of `InMemoryStreakRepository()`
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/HomeViewModelTest.kt` — verify `streakState.earnedBadges` is non-empty after `recordCompletion()` on the DataStore-backed repo (this is the test that was already failing silently)

**TDD steps**:
1. [RED] Add a test in `HomeViewModelTest` asserting `earnedBadges` contains `StreakBadge.WEEKLY_WARRIOR` after 7 recorded completions on the real `StreakRepository`. Run — test fails because `HomeViewModel` holds `InMemoryStreakRepository` which resets on init.
2. [GREEN] Change `HomeViewModel` constructor to accept `StreakRepository` (interface). Update `ViewModelFactory` and `SumGridNavigation.kt` to inject the DataStore-backed instance. Test passes.
3. [REFACTOR] Ensure `HomeViewModel` parameter name is `streakRepository: StreakRepository` consistently; remove any stale `InMemoryStreakRepository` references from production code paths.

**Acceptance**: `HomeViewModelTest` passes. `InMemoryStreakRepository` is no longer injected at runtime (only in tests).  
**Estimate**: ~1h  
**deps**: none  
**parallel**: none (must complete before T004, T005, T006)

---

### T002 — [P] Wire share button on PuzzleScreen completion state

**Feature**: S01-F001  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/PuzzleViewModelTest.kt` — add share-related state tests
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt` — add Share `Button` composable
- `app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/PuzzleViewModel.kt` — expose `puzzleDate` as public getter if not already accessible

**TDD steps**:
1. [RED] In `PuzzleViewModelTest`, write a test asserting that `uiState.puzzle` and `elapsedMillis` are both non-null after `loadPuzzle()` completes (they are needed to call `ShareCardGenerator.generate()`). Run — passes (they exist) but ensures the contract is pinned.  
   Write a separate test in a new `ShareCardGeneratorTest` (or extend existing) verifying that `ShareCardGenerator.generate(puzzle, elapsedMillis, LocalDate.now())` returns a non-empty string containing `"SumGrid #"`. Run — passes if generator is already implemented.
2. [GREEN] In `PuzzleScreen.kt`, within the `isCompleted` branch that currently shows `"Puzzle complete! 🎉"` text: add an `OutlinedButton(onClick = { shareResult(context, shareText) })` reading `LocalContext.current`, calling `ShareCardGenerator.generate(vm.uiState.value.puzzle, vm.elapsedMillis, puzzleDate)`, then `shareResult(context, shareText)`. Confirm the button is visible in the UI.
3. [REFACTOR] Extract the share-triggering logic into a named lambda `val onShare = { ... }` for readability; ensure `puzzleDate` flow from NavHost `LaunchedEffect` is passed correctly (add as param if currently private).

**Acceptance**: Share button visible on PuzzleScreen when puzzle is complete; tapping it opens Android share sheet.  
**Estimate**: ~2h  
**deps**: none  
**parallel**: T007, T008 (OnboardingScreen track can run concurrently)

---

### T003 — Add TopAppBar with back/exit button to PuzzleScreen

**Feature**: S01-F005  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/GridRendererTest.kt` — reuse pattern; add a new `PuzzleScreenTopBarTest.kt` to assert back navigation callback fires
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt` — add `TopAppBar` with back arrow icon and difficulty label

**TDD steps**:
1. [RED] Create `app/src/test/kotlin/org/dgeek/sumgrid/ui/PuzzleScreenTopBarTest.kt`. Write a test using `composeTestRule` that renders `PuzzleScreen` with a mock `onBack` lambda and asserts that clicking the back arrow invokes `onBack`. Run — fails (no `TopAppBar` exists yet).
2. [GREEN] Wrap `PuzzleScreen` content in a `Scaffold` with a `TopAppBar`. Add a `NavigationIcon` (`Icons.AutoMirrored.Filled.ArrowBack`) that calls `onBack()` lambda. Add the difficulty label as the `title` text. Test passes.
3. [REFACTOR] Ensure the back arrow uses `contentDescription = "Navigate back"` for accessibility. Confirm `Scaffold` padding does not break existing grid layout by running existing `GridRendererTest`.

**Acceptance**: Back arrow visible in PuzzleScreen; tapping it navigates to HomeScreen without breaking grid layout.  
**Estimate**: ~2h  
**deps**: T002 (same file — T002 sets up PuzzleScreen composable structure first)  
**parallel**: T007, T008

---

### T004 — Display earned streak badges on HomeScreen (BadgeRow composable)

**Feature**: S01-F002  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/HomeViewModelTest.kt` — extend to assert `uiState.earnedBadges` populated
- `app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/HomeViewModel.kt` — add `earnedBadges: Set<StreakBadge>` to `HomeUiState`
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt` — add `BadgeRow` composable

**TDD steps**:
1. [RED] In `HomeViewModelTest`, add test: create `HomeViewModel` with a `StreakRepository` that has 7 completions pre-recorded; collect `uiState.earnedBadges`; assert it contains `StreakBadge.WEEKLY_WARRIOR`. Run — fails because `HomeUiState` has no `earnedBadges` field.
2. [GREEN] Add `earnedBadges: Set<StreakBadge> = emptySet()` to `HomeUiState`. In `HomeViewModel`, observe `streakRepository.streakState` and copy `it.earnedBadges` into `HomeUiState`. In `HomeScreen`, add a `BadgeRow` composable below the streak counter that iterates `StreakBadge.entries`: earned badges shown with full opacity/color, unearned shown grayed (0.38f alpha). Test passes.
3. [REFACTOR] Extract `BadgeRow` and `BadgeItem` into separate composables in `HomeScreen.kt` (or a new `ui/components/BadgeRow.kt` file). Add `contentDescription` for each badge: `"${badge.displayName} — ${if (earned) "Earned" else "Locked"}"`.

**Acceptance**: All 4 badges visible on HomeScreen; earned badges colored, locked badges grayed. Requires T001 to be complete.  
**Estimate**: ~3h  
**deps**: T001  
**parallel**: T002, T003, T007, T008 (after T001 unblocks)

---

### T005 — Make HomeScreen scrollable on compact viewports

**Feature**: S01-F006  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/HomeViewModelTest.kt` — no change needed
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt` — wrap Column in `verticalScroll`

**TDD steps**:
1. [RED] In a new `HomeScreenLayoutTest.kt`, render `HomeScreen` inside a `Box` with `height = 360.dp` (compact viewport). Assert that a `ScrollState` or `LazyColumn` is present (via semantics / testTag `"home_scroll_container"`). Run — fails because no scroll exists.
2. [GREEN] Add `testTag("home_scroll_container")` to the root `Column` and wrap it with `Modifier.verticalScroll(rememberScrollState())`. Test passes.
3. [REFACTOR] Ensure the scroll modifier is applied at the outermost content Column (not inside sub-components). Confirm no nested scroll conflict with any `LazyColumn` children (there are none currently).

**Acceptance**: HomeScreen scrolls on 360dp height; no content is clipped below the fold.  
**Estimate**: ~1h  
**deps**: T001 (same file as T006 — do T005 before T006)  
**parallel**: T002, T003, T007, T008

---

### T006 — All-done state for HomeScreen when all 3 difficulties complete

**Feature**: S01-F007  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/HomeViewModelTest.kt` — add `allDailyPuzzlesComplete` state test
- `app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/HomeViewModel.kt` — add `allComplete: Boolean` to `HomeUiState`
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt` — show all-done banner and Practice mode button placeholder

**TDD steps**:
1. [RED] In `HomeViewModelTest`, simulate all 3 difficulty completions stored in `CompletionStore`. Assert `uiState.allComplete == true`. Run — fails because `HomeUiState` has no `allComplete` field.
2. [GREEN] Add `allComplete: Boolean` to `HomeUiState`. In `HomeViewModel`, derive it from `puzzleStatuses`: `allComplete = Difficulty.entries.all { puzzleStatuses[it]?.isComplete == true }`. In `HomeScreen`, when `allComplete == true`, replace or overlay the Play buttons with an "All done for today! 🎉" banner and a grayed-out "Practice" button (fully wired in S03-F003; here it is a visual placeholder).
3. [REFACTOR] Extract the all-done UI into an `AllDoneSection` composable. Ensure the Practice button placeholder has `enabled = false` and `contentDescription = "Practice mode — coming soon"` until S03-F003 wires it.

**Acceptance**: All-done banner and Practice placeholder appear when all 3 daily puzzles are complete. Play buttons change state visually.  
**Estimate**: ~2h  
**deps**: T005 (same file)  
**parallel**: T002, T003, T007, T008

---

### T007 — Add skip option to OnboardingScreen

**Feature**: S01-F003  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/OnboardingViewModelTest.kt` — add skip navigation test
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/OnboardingScreen.kt` — add "Skip tutorial" `TextButton`

**TDD steps**:
1. [RED] In `OnboardingViewModelTest`, add a test asserting a new `fun skipOnboarding()` method sets `onboardingComplete = true` in `OnboardingRepository` and triggers an `onNavigateToHome` callback. Run — fails because `skipOnboarding()` does not exist.
2. [GREEN] Add `fun skipOnboarding()` to `OnboardingViewModel` that calls `onboardingRepository.markComplete()` and invokes the `onNavigateToHome` lambda. In `OnboardingScreen`, add a `TextButton("Skip tutorial")` at the top-right corner that calls `viewModel.skipOnboarding()`. Test passes.
3. [REFACTOR] Use a `Box` overlay to position the Skip button absolutely without disrupting the existing layout flow. Ensure the button has `contentDescription = "Skip tutorial and go to home screen"`.

**Acceptance**: "Skip tutorial" TextButton visible on all onboarding steps; tapping it navigates to HomeScreen and marks onboarding complete.  
**Estimate**: ~2h  
**deps**: none  
**parallel**: T002, T003, T004, T005, T006

---

### T008 — Add rules explanation overlay to OnboardingScreen

**Feature**: S01-F004  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/OnboardingViewModelTest.kt` — add rules overlay visibility test
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/OnboardingScreen.kt` — add rules coach mark / info card

**TDD steps**:
1. [RED] Add a test asserting that on the first onboarding step (`stepIndex == 0`), a composable with `testTag("rules_overlay")` or `contentDescription = "How to play SumGrid"` is visible. Run — fails because no such element exists.
2. [GREEN] On the first onboarding step, render a `Card` (or `AlertDialog` inline) beneath the puzzle title showing: "Fill each empty cell with a number. Every row and column must hit its target sum shown on the edges." Include a "Got it" button that dismisses the card (sets a local `rulesAcknowledged` boolean in `OnboardingScreen`). Test passes.
3. [REFACTOR] If the rules card uses `AnimatedVisibility`, ensure it fades in on step 0 entry. Extract rules text into a string resource. Ensure the "Got it" button tap is announced to TalkBack.

**Acceptance**: Rules card visible on first onboarding step with a dismiss button. Card does not re-appear after dismissal within the same session.  
**Estimate**: ~3h  
**deps**: T007 (same file — T007 establishes Skip button placement to avoid layout conflicts)  
**parallel**: T002, T003, T004, T005, T006

---

### Sprint 1 Integration Gate

Before starting Sprint 2, verify:

- [ ] `./gradlew :app:testDebugUnitTest` passes — all new tests green
- [ ] Share button visible on PuzzleScreen completion branch
- [ ] At least 1 badge row visible on HomeScreen (badges locked/grayed if streak < 7 days)
- [ ] "Skip tutorial" link visible on OnboardingScreen step 0
- [ ] "How to play" rules card visible on OnboardingScreen step 0
- [ ] TopAppBar back arrow visible on PuzzleScreen; back navigation works
- [ ] HomeScreen scrolls on 360dp height viewport (test in emulator with small phone profile)
- [ ] All-done state shows when all 3 difficulties are complete in `CompletionStore`

---

## Sprint 2 — Core Game UX (~36h)

**Goal**: Build the completion card (the #1 retention touchpoint), add undo, fix cramped NumberPad,
audit dark mode contrast, fix font scaling, and persist mid-puzzle state.

**Parallelization map (S02)**:

```
Track A (new UI):    T009 (CompletionCard — 8h)
Track B (ViewModel): T010 (Undo) → T014 (Auto-save) — sequential, same file
Track C (NumberPad): T011 (2-row layout — 4h) — parallel, independent
Track D (Theme):     T012 (Dark mode — 6h) — parallel, independent
Track E (Layout):    T013 (Font scaling — 6h) — parallel, independent
```

---

### T009 — [P] Build CompletionCard ModalBottomSheet

**Feature**: S02-F001  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/CompletionDetectionTest.kt` — extend to pin completion state trigger
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/CompletionCardTest.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/puzzle/CompletionCard.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt` — replace bare "Puzzle complete!" text

**TDD steps**:
1. [RED] Create `CompletionCardTest.kt`. Write tests:
   - `completionCard_showsSolveTime()` — renders `CompletionCard(elapsedMillis = 95_000L, ...)` and asserts `"1:35"` is visible.
   - `completionCard_shareButtonCallsOnShare()` — asserts tapping "Share" invokes `onShare` lambda.
   - `completionCard_nextPuzzleButtonCallsOnNextPuzzle()` — asserts tapping "Next Puzzle" invokes `onNextPuzzle` lambda.
   - `completionCard_backHomeButtonCallsOnBackHome()` — asserts tapping "Back to Home" invokes `onBackHome` lambda.
   All fail — `CompletionCard.kt` does not exist.
2. [GREEN] Create `CompletionCard.kt` with a `@Composable fun CompletionCard(elapsedMillis: Long, difficulty: Difficulty, onShare: () -> Unit, onNextPuzzle: () -> Unit, onBackHome: () -> Unit, modifier: Modifier = Modifier)`. Renders in a `ModalBottomSheet`: formatted solve time (M:SS), difficulty chip/badge, Share `OutlinedButton` (calls `onShare`), "Next Puzzle" `Button`, "Back to Home" `TextButton`. In `PuzzleScreen.kt`, replace the `if (isCompleted) Text("Puzzle complete! 🎉")` branch with `ModalBottomSheet(onDismissRequest = {})` wrapping `CompletionCard(...)`. The share lambda reuses the `onShare` logic from T002. All 4 tests pass.
3. [REFACTOR] Extract M:SS time formatter into a private `fun formatElapsed(millis: Long): String` function in `CompletionCard.kt`. Confirm all text uses Material3 theme typography tokens (no hardcoded `sp`). Add `contentDescription = "Puzzle complete. Solved in ${formatElapsed(elapsedMillis)}"` to the sheet.

**Acceptance**: ModalBottomSheet appears on puzzle completion showing solve time, difficulty badge, and all 3 CTAs. Share CTA opens Android share sheet with correct share card text.  
**Estimate**: ~8h  
**deps**: T002 (share logic proven; reused here), Sprint 1 integration gate  
**parallel**: T010, T011, T012, T013

---

### T010 — [P] Add undo functionality to PuzzleViewModel

**Feature**: S02-F002  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/PuzzleViewModelTest.kt` — add undo test suite
- `app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/PuzzleViewModel.kt` — add `moveHistory` stack + `undo()` + `canUndo`
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/NumberPad.kt` — add undo button parameter

**TDD steps**:
1. [RED] In `PuzzleViewModelTest`, add:
   - `undo_onEmptyHistory_isNoOp()` — call `undo()` before any moves; assert state unchanged.
   - `undo_afterEnterNumber_restoresPreviousValue()` — enter a number; undo; assert cell returns to 0.
   - `undo_afterClearCell_restoresPreviousValue()` — enter a number; clear cell; undo; assert number restored.
   - `undo_doesNotCrossPuzzleBoundary()` — load puzzle A; enter number; load puzzle B; undo; assert puzzle B state unchanged (history cleared on `loadPuzzle`).
   - `canUndo_isFalseInitially()` — assert `uiState.value.canUndo == false` before any moves.
   - `canUndo_isTrueAfterMove()` — enter number; assert `uiState.value.canUndo == true`.
   All fail — `undo()`, `canUndo`, and `moveHistory` do not exist.
2. [GREEN] Add `private val moveHistory: ArrayDeque<Array<IntArray>> = ArrayDeque()` and `private const val MAX_HISTORY = 50` to `PuzzleViewModel`. In `enterNumber()` and `clearCell()`, push `current.userValues.deepCopy()` before mutation. Cap to `MAX_HISTORY` via `removeFirst()`. Add `fun undo()` that pops from `moveHistory` and restores state. Add `canUndo: Boolean` to `PuzzleUiState` (computed as `moveHistory.isNotEmpty()`). Clear `moveHistory` in `loadPuzzle()`. In `NumberPad.kt`, add `onUndo: (() -> Unit)? = null` parameter; render an undo `IconButton` (Undo icon) when `onUndo != null` and `canUndo == true`. All 6 tests pass.
3. [REFACTOR] Extract `Array<IntArray>.deepCopy()` as a private extension function. Ensure `canUndo` is derived in the state-building function (not duplicated). Add `contentDescription = "Undo last move"` to the undo `IconButton`.

**Acceptance**: Undo button visible on NumberPad; pressing it reverts the last cell change. `canUndo` is false when history is empty. History clears on new puzzle load.  
**Estimate**: ~6h  
**deps**: Sprint 1 integration gate  
**parallel**: T009, T011, T012, T013

---

### T011 — 2-row NumberPad layout for medium/large difficulties

**Feature**: S02-F003  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/SumIndicatorTest.kt` — reference pattern for UI tests
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/NumberPadLayoutTest.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/NumberPad.kt` — add 2-row layout branch

**TDD steps**:
1. [RED] Create `NumberPadLayoutTest.kt`. Write:
   - `numberPad_singleRow_whenMaxValSix()` — renders `NumberPad(maxVal = 6, ...)` and asserts exactly 1 `Row` container with test tag `"number_pad_row_0"`.
   - `numberPad_twoRows_whenMaxValSeven()` — renders `NumberPad(maxVal = 7, ...)` and asserts `"number_pad_row_0"` and `"number_pad_row_1"` both present.
   - `numberPad_twoRows_whenMaxValNine()` — same for `maxVal = 9`.
   Both two-row tests fail — only a single row exists today.
2. [GREEN] In `NumberPad.kt`, add logic: if `maxVal >= 7`, split button list into two rows (first row: buttons 1..5, second row: 6..`maxVal` + Clear). Apply `testTag("number_pad_row_0")` and `testTag("number_pad_row_1")` to the respective `Row` composables. Tests pass.
3. [REFACTOR] Extract row-split logic into `fun splitNumberRange(maxVal: Int): List<List<Int>>` pure function. Write unit test for this function with inputs `6, 7, 9`. Remove testTag from production if only needed for tests (use a parameter or keep for debuggability — team decision).

**Acceptance**: Medium (5x5, maxVal=9) shows a 2-row NumberPad. Beginner (3x3, maxVal=6) retains single row.  
**Estimate**: ~4h  
**deps**: Sprint 1 integration gate  
**parallel**: T009, T010, T012, T013

---

### T012 — Dark mode contrast audit and theme fixes

**Feature**: S02-F004  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/theme/DarkModeContrastTest.kt` — extend existing tests
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Color.kt` — adjust token values
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Theme.kt` — update dark color scheme

**TDD steps**:
1. [RED] Open existing `DarkModeContrastTest.kt`. Add contrast ratio assertions using WCAG luminance formula:
   - `darkMode_givenCell_onSurface_contrastAtLeast4_5()` — asserts contrast ratio between `darkColorScheme.primaryContainer` (given cell fill) and `darkColorScheme.surface` ≥ 4.5:1.
   - `darkMode_userCell_onSurface_contrastAtLeast4_5()` — same for `secondaryContainer` vs `surface`.
   - `darkMode_sumIndicatorText_contrastAtLeast4_5()` — asserts sum indicator text on its background ≥ 4.5:1.
   Run — one or more assertions fail if current token values are under threshold.
2. [GREEN] Adjust `darkColorScheme` tokens in `Color.kt`/`Theme.kt` to bring failing contrast pairs above 4.5:1. For OLED dark (`OledBlack = #090909`), ensure `primaryContainer` is light enough (e.g., shift towards `md_theme_dark_primaryContainer` standard Material tones). Run `DarkModeContrastTest` — all pass.
3. [REFACTOR] Extract the WCAG contrast ratio calculation into a test utility function `fun contrastRatio(color1: Color, color2: Color): Double` in a shared `TestColorUtils.kt` test file so other contrast tests can reuse it. Verify `ThemeColorTest.kt` and `ColorPaletteTest.kt` still pass.

**Acceptance**: `DarkModeContrastTest` passes with ≥ 4.5:1 contrast for given cells, user cells, and sum indicators in dark mode.  
**Estimate**: ~6h  
**deps**: Sprint 1 integration gate  
**parallel**: T009, T010, T011, T013

---

### T013 — Font scaling audit and fixes at 200% scale

**Feature**: S02-F005  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/theme/TypographyTest.kt` — extend with scaled assertions
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt` — fix text overflow
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/NumberPad.kt` — fix button label overflow
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Type.kt` — ensure scalable type sizes

**TDD steps**:
1. [RED] In `TypographyTest.kt`, add assertions with a `fontScale = 2.0f` density override:
   - `homeScreen_streakLabel_noOverflow_at200PercentScale()` — renders `HomeScreen` at 2x font scale; asserts streak label has `overflow = TextOverflow.Ellipsis` or `maxLines` set (no unbounded text).
   - `numberPad_digits_noClip_at200PercentScale()` — renders `NumberPad(maxVal = 9, ...)` at 2x scale; asserts no digit button has a zero-size bounding box.
   Run — one or more fail if layout uses fixed `sp` values that overflow at 2x.
2. [GREEN] In `HomeScreen.kt`, add `maxLines = 1, overflow = TextOverflow.Ellipsis` to all streak/label `Text` composables. In `NumberPad.kt`, ensure button height is `height = IntrinsicSize.Min` or has a minimum touch target. In `Type.kt`, verify no font size is specified in fixed `dp` (use `sp` throughout). Tests pass.
3. [REFACTOR] Review remaining screens (`OnboardingScreen`, `PuzzleScreen`) with the same 200% font scale. Fix any remaining overflows identified. Consolidate scalable text style definitions in `Type.kt`.

**Acceptance**: HomeScreen and NumberPad render correctly at 200% font scale with no overflow or clipping.  
**Estimate**: ~6h  
**deps**: Sprint 1 integration gate  
**parallel**: T009, T010, T011, T012

---

### T014 — [P] State persistence and auto-save on every cell change

**Feature**: S02-F006  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/PuzzleViewModelTest.kt` — add persistence tests
- `app/src/main/kotlin/org/dgeek/sumgrid/daily/CompletionStore.kt` — add in-progress interface methods
- `app/src/main/kotlin/org/dgeek/sumgrid/daily/DataStoreCompletionStore.kt` — implement new methods
- `app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/PuzzleViewModel.kt` — call save on each `enterNumber`/`clearCell`; restore on `loadPuzzle`

**TDD steps**:
1. [RED] In `PuzzleViewModelTest`, add:
   - `autoSave_writesInProgressState_onEnterNumber()` — enter a number; assert `completionStore.getInProgress(key)` returns a non-null `IntArray` matching the cell entry.
   - `autoSave_restoresMidPuzzleState_onLoadPuzzle()` — pre-populate `completionStore.saveInProgress(key, values)`; call `loadPuzzle()`; assert `uiState.userValues` matches the saved values.
   - `autoSave_clearsInProgressState_onCompletion()` — complete a puzzle; assert `completionStore.getInProgress(key) == null`.
   All fail — `getInProgress`/`saveInProgress`/`clearInProgress` do not exist.
2. [GREEN] Add to `CompletionStore` interface: `suspend fun saveInProgress(key: String, values: IntArray)`, `suspend fun getInProgress(key: String): IntArray?`, `suspend fun clearInProgress(key: String)`. Implement in `DataStoreCompletionStore` using key prefix `"in_progress_"`. In `PuzzleViewModel.enterNumber()` and `clearCell()`, call `viewModelScope.launch { completionStore?.saveInProgress(key, flattenedValues) }`. In `loadPuzzle()`, check `completionStore?.getInProgress(key)` and restore if non-null. In `persistCompletion()`, call `clearInProgress(key)`. Tests pass.
3. [REFACTOR] Debounce the save call (use `debounce(300)` on a `SharedFlow<IntArray>`) to avoid hammering DataStore on every keystroke. Ensure `InMemoryCompletionStore` (used in tests) also implements the new methods. Add the new methods to the test `InMemoryCompletionStore` in `app/src/test/kotlin/...`.

**Acceptance**: Mid-puzzle state survives process kill (verify with "Don't keep activities" developer option). State clears on puzzle completion.  
**Estimate**: ~6h  
**deps**: T010 (both touch `PuzzleViewModel` — T010 undo first, then T014 persistence)  
**parallel**: T009, T011, T012, T013 (T014 is sequential after T010, not before)

---

### Sprint 2 Integration Gate

Before starting Sprint 3, verify:

- [ ] `./gradlew :app:testDebugUnitTest` passes — all new tests green
- [ ] `CompletionCard` appears as `ModalBottomSheet` on puzzle completion with solve time, difficulty, share CTA, and next-puzzle / back-home buttons
- [ ] Undo button visible on `NumberPad`; `canUndo` correctly reflects history state
- [ ] Medium (5x5) difficulty shows 2-row NumberPad
- [ ] `DarkModeContrastTest` — contrast ≥ 4.5:1 for given/user cells in dark mode
- [ ] HomeScreen and NumberPad render without overflow at 200% font scale
- [ ] Mid-puzzle state restored after `adb shell am kill org.dgeek.sumgrid` (or "Don't keep activities" test)

---

## Sprint 3 — Retention & Engagement (~42h)

**Goal**: Drive repeat usage with a stats screen, daily push notifications, unlimited practice puzzles,
and an enhanced completion celebration.

**Parallelization map (S03)**:

```
[T015] Navigation routes prereq (~1h — shared file, do first)
  │
  ├─── Track A: T016 (StatsScreen + StatsViewModel — 12h)
  ├─── Track B: T017 + T018 (Notifications — 8h, two tasks)
  ├─── Track C: T019 + T020 (PracticeScreen + PracticeViewModel — 16h, two tasks)
  └─── Track D: T021 (Enhanced celebration — 6h, after above tracks underway)
```

---

### T015 — [P] Add Stats and Practice navigation routes

**Feature**: Prerequisite for S03-F001 and S03-F003  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/navigation/AnimatedNavTest.kt` — extend with stats/practice route assertions
- `app/src/main/kotlin/org/dgeek/sumgrid/navigation/SumGridNavigation.kt` — add `Routes.STATS` and `Routes.PRACTICE`

**TDD steps**:
1. [RED] In `AnimatedNavTest.kt`, add:
   - `navGraph_containsStatsRoute()` — verifies the `NavHost` has a destination with route `"stats"`.
   - `navGraph_containsPracticeRoute()` — verifies a destination with route matching `"practice/{difficulty}"`.
   - `homeScreen_statsButtonInvokesOnOpenStats()` — assert `HomeScreen` exposes an `onOpenStats` callback parameter (compile-time contract check via reflection or by calling `HomeScreen(onOpenStats = { ... })`).
   All fail — routes and callbacks do not exist.
2. [GREEN] In `SumGridNavigation.kt`, add `Routes.STATS = "stats"` and `Routes.PRACTICE = "practice/{difficulty}"` + a helper `Routes.practice(difficulty) = "practice/${difficulty.name}"`. Add stub `composable("stats")` and `composable("practice/{difficulty}")` destinations (screens TBD in T016 and T019). Add `onOpenStats: () -> Unit` and `onStartPractice: (Difficulty) -> Unit` parameters to `HomeScreen`; wire them to `navController.navigate(...)` calls. Tests pass.
3. [REFACTOR] Ensure `Routes` object is the single source of truth — no raw string literals in `NavHost` blocks. Add inline KDoc explaining each route.

**Acceptance**: Nav graph compiles and includes both new routes. `HomeScreen` accepts the new callbacks without breaking existing callers.  
**Estimate**: ~1h  
**deps**: Sprint 2 integration gate  
**parallel**: Immediately precedes T016, T017, T018, T019, T020, T021

---

### T016 — [P] Build StatsScreen and StatsViewModel

**Feature**: S03-F001  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/StatsViewModelTest.kt` — new file
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/StatsScreenTest.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/StatsViewModel.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/stats/StatsScreen.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/daily/CompletionStore.kt` — add `getAll()` method
- `app/src/main/kotlin/org/dgeek/sumgrid/daily/DataStoreCompletionStore.kt` — implement `getAll()`

**TDD steps**:
1. [RED] Create `StatsViewModelTest.kt`. Write:
   - `stats_totalSolvedCounts_allDifficulties()` — seed `CompletionStore` with 10 completions (3 BEGINNER, 4 EASY, 3 MEDIUM); assert `uiState.totalSolved == 10` and per-difficulty counts match.
   - `stats_averageTime_perDifficulty()` — seed with known `elapsedMillis` values; assert `avgTimeMillis[Difficulty.BEGINNER]` equals the arithmetic mean.
   - `stats_bestTime_perDifficulty()` — same seeds; assert `bestTimeMillis[Difficulty.BEGINNER]` equals the minimum.
   - `stats_completionDates_asSet()` — assert `calendarDates` contains the `LocalDate` values derived from epoch day keys.
   All fail — `StatsViewModel`, `StatsUiState`, and `CompletionStore.getAll()` do not exist.
2. [GREEN] Add `suspend fun getAll(): Map<String, CompletionState>` to `CompletionStore` interface. Implement in `DataStoreCompletionStore` by reading `data.first().asMap()` and filtering keys starting with `"completion_"`. Create `StatsUiState(totalSolved: Int, countByDifficulty: Map<Difficulty, Int>, avgTimeMillis: Map<Difficulty, Long>, bestTimeMillis: Map<Difficulty, Long>, calendarDates: Set<LocalDate>)`. Create `StatsViewModel` with a `loadStats()` fun that reads `completionStore.getAll()`, parses keys, and computes aggregates. Create `StatsScreen.kt` with: total solved `Card`, avg/best time per difficulty `Card`, and a calendar `LazyVerticalGrid` heatmap (30 days, colored cells for completed dates). Wire `StatsScreen` into the `"stats"` NavHost destination. All tests pass.
3. [REFACTOR] Extract key parsing logic into `fun parseCompletionKey(key: String): Pair<LocalDate, Difficulty>?` private function in `StatsViewModel`. Ensure calendar heatmap uses `MaterialTheme.colorScheme.primary` for completed days (no hardcoded color). Add `semantics { heading() }` to section titles in `StatsScreen`.

**Acceptance**: Stats screen accessible via HomeScreen stats button; shows total solved count, avg/best times per difficulty, and a 30-day calendar heatmap with completed days highlighted.  
**Estimate**: ~12h  
**deps**: T015, T014 (reliable completion data from auto-save)  
**parallel**: T017, T018, T019, T020, T021

---

### T017 — Build NotificationScheduler and DailyReminderWorker

**Feature**: S03-F002 (part 1 — backend)  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/notifications/NotificationSchedulerTest.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/notifications/DailyReminderWorker.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/notifications/NotificationScheduler.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/SumGridApplication.kt` — register notification channel

**TDD steps**:
1. [RED] Create `NotificationSchedulerTest.kt`. Write:
   - `schedule_enqueuesUniquePeriodicWork()` — using `WorkManagerTestInitHelper`, call `NotificationScheduler.schedule(hour=9, minute=0)`; assert `WorkManager.getInstance(context).getWorkInfosByTag("daily_reminder")` returns a non-empty list with `ENQUEUED` state.
   - `cancel_removesScheduledWork()` — schedule then cancel; assert work info list is empty.
   Both fail — `NotificationScheduler` does not exist.
2. [GREEN] Create `NotificationScheduler.kt`: `object NotificationScheduler { fun schedule(context: Context, hour: Int, minute: Int) { ... } fun cancel(context: Context) { ... } }` using `WorkManager.enqueueUniquePeriodicWork("daily_reminder", KEEP, periodicWorkRequest)`. Create `DailyReminderWorker: CoroutineWorker` that builds and posts a `NotificationCompat` notification with channel `"daily_reminder"`, title `"Your daily SumGrid is ready!"`, and a deep-link `PendingIntent` to `MainActivity`. In `SumGridApplication.onCreate()`, create notification channel `"daily_reminder"` with `IMPORTANCE_DEFAULT`. Tests pass.
3. [REFACTOR] Ensure `DailyReminderWorker` is registered in `AndroidManifest.xml` as a `<provider>` via WorkManager's `androidx.startup` (or explicitly). Add a guard: do not post notification if today's puzzle is already complete (check `CompletionStore`).

**Acceptance**: `NotificationSchedulerTest` passes. WorkManager enqueues/cancels correctly in test environment.  
**Estimate**: ~5h  
**deps**: T015  
**parallel**: T016, T019, T020, T021

---

### T018 — Notification preferences UI (time picker + permission flow)

**Feature**: S03-F002 (part 2 — UI)  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/NotificationPermissionBannerTest.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt` — add permission banner + notification settings entry

**TDD steps**:
1. [RED] Create `NotificationPermissionBannerTest.kt`. Write:
   - `permissionBanner_visibleWhen_permissionNotGranted()` — renders `HomeScreen` with `notificationsPermissionGranted = false`; asserts element with `testTag("notification_permission_banner")` is visible.
   - `permissionBanner_hiddenWhen_permissionGranted()` — same with `true`; asserts banner is not visible.
   Both fail — no banner exists yet.
2. [GREEN] Add a `NotificationPermissionBanner` composable to `HomeScreen.kt` that shows a dismissible `Card` at the top: "Enable daily reminders?" with "Turn on" and "Not now" actions. Show it when `notificationsPermissionGranted == false` (read from ViewModel state). "Turn on" calls `ActivityCompat.requestPermissions(activity, POST_NOTIFICATIONS, ...)`. Add a "Reminder time" entry in a settings-style row below the streak section — tapping opens `TimePickerDialog` (Material3) and calls `NotificationScheduler.schedule(hour, minute)` on confirm. Tests pass.
3. [REFACTOR] Store user's chosen reminder time in `DataStore` (key `"notif_hour"`, `"notif_minute"`). Restore it on HomeScreen entry. Ensure the `POST_NOTIFICATIONS` `<uses-permission>` is declared in `AndroidManifest.xml`. Graceful degradation: if permission is denied, the banner offers an "Open settings" deep-link instead.

**Acceptance**: Permission banner shown on devices with `POST_NOTIFICATIONS` not granted. Time picker appears on tap. Notification scheduled after permission granted.  
**Estimate**: ~3h  
**deps**: T017 (NotificationScheduler must exist before UI can call it)  
**parallel**: T016, T019, T020, T021

---

### T019 — [P] Build PracticeViewModel

**Feature**: S03-F003 (part 1 — ViewModel)  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/PracticeViewModelTest.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/PracticeViewModel.kt` — new file

**TDD steps**:
1. [RED] Create `PracticeViewModelTest.kt`. Write:
   - `practiceVm_generatePuzzle_producesValidPuzzle()` — call `vm.generatePuzzle(Difficulty.MEDIUM)`; assert `uiState.puzzle` is non-null and `puzzle.size == 5`.
   - `practiceVm_generatePuzzle_doesNotSaveToCompletionStore()` — complete the puzzle; assert `completionStore.getAll()` is empty (practice does not affect streak).
   - `practiceVm_playAgain_generatesNewPuzzle()` — call `generatePuzzle` twice; assert the two `puzzle.seed` values differ (non-deterministic seeds).
   All fail — `PracticeViewModel` does not exist.
2. [GREEN] Create `PracticeViewModel(puzzleGenerator: PuzzleGenerator, completionStore: CompletionStore? = null)`. Add `fun generatePuzzle(difficulty: Difficulty)` that sets seed from `System.currentTimeMillis()`, calls `puzzleGenerator.generate(seed, difficulty)`, and sets `_uiState.value` accordingly. Deliberately pass `completionStore = null` so completion is never persisted. Add `fun playAgain()` that calls `generatePuzzle` with the last chosen difficulty. Tests pass.
3. [REFACTOR] Ensure `PracticeViewModel` reuses `PuzzleViewModel` state types (`PuzzleUiState`) for consistency. Add `PracticeViewModel` to `ViewModelFactory`. Confirm that `StreakRepository.recordCompletion()` is never called from `PracticeViewModel`.

**Acceptance**: `PracticeViewModelTest` passes. Practice puzzles never appear in `CompletionStore` or affect streak.  
**Estimate**: ~4h  
**deps**: T015  
**parallel**: T016, T017, T018, T021

---

### T020 — Build PracticeScreen composable

**Feature**: S03-F003 (part 2 — UI)  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/PracticeScreenTest.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/practice/PracticeScreen.kt` — new file
- `app/src/main/kotlin/org/dgeek/sumgrid/navigation/SumGridNavigation.kt` — wire PracticeScreen into nav destination
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt` — enable Practice button (replaces placeholder from T006)

**TDD steps**:
1. [RED] Create `PracticeScreenTest.kt`. Write:
   - `practiceScreen_showsGridAndNumberPad()` — renders `PracticeScreen` with a mock `PracticeViewModel`; asserts `testTag("puzzle_grid")` and `testTag("number_pad")` visible.
   - `practiceScreen_completion_showsPlayAgainButton()` — simulate puzzle completion in mock VM; assert button with text "Play again" is visible.
   - `practiceScreen_backHome_invokesOnBack()` — assert tapping "Back to Home" invokes `onBack` lambda.
   All fail — `PracticeScreen` does not exist.
2. [GREEN] Create `PracticeScreen.kt` composable. Structure: `TopAppBar` (back arrow → `onBack`), `GridRenderer(uiState)`, `NumberPad(...)`, completion overlay showing "Play again" `Button` (calls `vm.playAgain()`) and "Back to Home" `TextButton` (calls `onBack()`). Reuse `GridRenderer` and `NumberPad` composables directly. Wire `composable(Routes.PRACTICE) { PracticeScreen(vm = ...) }` in `SumGridNavigation.kt`. Enable the Practice `Button` in `HomeScreen.kt` (replace the disabled placeholder from T006). Tests pass.
3. [REFACTOR] Consider extracting a shared `PuzzleContent(uiState, onNumberTap, onUndo, onClearCell)` composable shared between `PuzzleScreen` and `PracticeScreen` to eliminate duplication. If extraction is done, run all existing `PuzzleViewModelTest` and `CompletionDetectionTest` to confirm no regressions.

**Acceptance**: Practice mode reachable from HomeScreen all-done state. Generates random puzzles per tap. "Play again" regenerates. Completion does not increment streak.  
**Estimate**: ~12h  
**deps**: T019 (PracticeViewModel must exist first), T015 (nav route)  
**parallel**: T016, T017, T018, T021

---

### T021 — Enhanced celebration animation (confetti)

**Feature**: S03-F004  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/CelebrationWiringTest.kt` — extend existing test
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/CelebrationAnimation.kt` — add confetti particle system

**TDD steps**:
1. [RED] In `CelebrationWiringTest.kt`, add:
   - `celebrationState_confettiParticles_emittedOnComplete()` — set `celebrationState.trigger()`; assert `celebrationState.particles.isNotEmpty()` (new `particles` field on `CelebrationState`).
   - `celebrationState_particles_clearAfterDuration()` — advance clock by 3000ms; assert `particles.isEmpty()` (particles expire).
   Both fail — `particles` field does not exist on `CelebrationState`.
2. [GREEN] Add `particles: List<ConfettiParticle>` to `CelebrationState` (or a new `data class ConfettiParticle(x: Float, y: Float, color: Color, rotation: Float, velocity: Offset)`). In `rememberCelebrationState()`, generate 60 random particles when triggered (randomized positions, 6 brand colors). Use `LaunchedEffect` with `delay(2500)` to clear particles. In `CelebrationCellWrapper` (existing), overlay the confetti particles using `Canvas` drawCircle/drawOval. Wire `CelebrationCellWrapper` onto each grid cell in `GridRenderer` (it already exists but is not yet wired — this is the "free win" noted in the audit). Tests pass.
3. [REFACTOR] Animate particles with `animateFloatAsState` for alpha fade-out over 500ms after 2000ms hold. Ensure the animation respects the reduce-motion flag (T028 in S04 will add the system check; here simply expose a `reduceMotion: Boolean = false` parameter and skip particle generation when true). Use `MaterialTheme.colorScheme.primary/secondary/tertiary` as confetti colors (no hardcoded hex).

**Acceptance**: Confetti particles appear on puzzle completion for ~2.5 seconds then fade. `CelebrationCellWrapper` is wired to `GridRenderer` cells.  
**Estimate**: ~6h  
**deps**: T009 (CompletionCard must exist — celebration coordinates with the completion UI layer)  
**parallel**: T016, T017, T018, T019, T020

---

### Sprint 3 Integration Gate

Before starting Sprint 4, verify:

- [ ] `./gradlew :app:testDebugUnitTest` passes — all new tests green
- [ ] Stats screen accessible from HomeScreen; shows total solved, avg/best times per difficulty, calendar heatmap
- [ ] "Practice" button active on HomeScreen all-done state; generates new random puzzles on each "Play again" tap
- [ ] Practice puzzles do not affect streak counter (verify `CompletionStore.getAll()` is empty after practice session)
- [ ] Notification opt-in banner shown on Android 13+ devices; time picker schedules `DailyReminderWorker`
- [ ] Confetti celebration fires on puzzle completion (grid cells show confetti overlay)
- [ ] All tests pass including `CelebrationWiringTest`, `StatsViewModelTest`, `PracticeViewModelTest`, `NotificationSchedulerTest`

---

## Sprint 4 — Polish & Accessibility (~48h)

**Goal**: Close all accessibility gaps, add power-user features (pencil/notes mode, Hard and Expert
difficulties), and ensure correct landscape behaviour.

**Parallelization map (S04)**:

```
Track A: T022 (Reduce motion — 3h) + T023 (Error shake — 3h) — fast, parallel
Track B: T024 (Landscape/portrait lock — 6h) — independent
Track C: T025 (TalkBack focus — 4h) — independent
Track D: T026 + T027 (Pencil mode — 16h, two tasks) — longest path
Track E: T028 + T029 (Hard/Expert — 16h, two tasks) — parallel to Track D
```

---

### T022 — Reduce motion accessibility support

**Feature**: S04-F001  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/StreakAnimationTest.kt` — extend with reduce-motion assertions
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt` — check reduce-motion preference
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/CelebrationAnimation.kt` — respect `reduceMotion` param (already prepared in T021)

**TDD steps**:
1. [RED] In `StreakAnimationTest.kt`, add:
   - `streakFlame_pulseDisabled_whenReduceMotionEnabled()` — render `HomeScreen` with a `LocalDensity` that has `fontScale = 1f` but a custom `AccessibilityManager` with `isEnabled && isTouchExplorationEnabled` (simulate reduce-motion); assert the pulsing flame `InfiniteTransition` is NOT running (check animation spec via testTag or a `produceState` wrapper).
   This is challenging to unit test directly — instead assert that the `FlameAnimation` composable receives `animationEnabled = false` when reduce-motion is on.
   Fail — no reduce-motion check exists.
2. [GREEN] In `HomeScreen.kt`, read `LocalContext.current.getSystemService(AccessibilityManager::class.java)` and check `isEnabled && isTouchExplorationEnabled` (proxy for reduce-motion on older APIs). On Android 12+, use `Settings.Global.TRANSITION_ANIMATION_SCALE == "0"` check. Pass `reduceMotion: Boolean` into the flame/pulse animation composable. When `reduceMotion == true`, replace infinite pulse with a static display. Pass `reduceMotion` to `CelebrationAnimation` (already accepts the param from T021). Test passes.
3. [REFACTOR] Extract reduce-motion detection into a `@Composable fun rememberReduceMotion(): Boolean` helper in a new `ui/theme/MotionPreference.kt` file. Use this in both `HomeScreen` and `CelebrationAnimation` for consistency.

**Acceptance**: Pulsing flame animation is static when Android "Remove animations" is enabled. Confetti does not appear when reduce-motion is on.  
**Estimate**: ~3h  
**deps**: Sprint 3 integration gate  
**parallel**: T023, T024, T025, T026, T027, T028, T029

---

### T023 — Error indicator shake/flash animation on GridRenderer

**Feature**: S04-F002  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/SumIndicatorTest.kt` — extend with error animation test
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt` — add shake animation to sum indicator

**TDD steps**:
1. [RED] In `SumIndicatorTest.kt`, add:
   - `sumIndicator_triggersShakeAnimation_onTransitionToError()` — render `SumIndicator` with `isError = false`; recompose with `isError = true`; assert a `shakeOffset` state variable transitions from `0f` to non-zero (or check for a `testTag("sum_indicator_shake_trigger")` state). Fail — no shake animation exists.
2. [GREEN] In `GridRenderer.kt`, for each row/column sum indicator, use `animateFloatAsState` to drive a horizontal offset: when `isError` transitions from `false → true`, animate a shake sequence `0f → 8f → -8f → 4f → -4f → 0f` over 300ms using a `spring` or keyframe animation spec. Apply as `Modifier.offset(x = shakeOffset.dp)`. Use `LaunchedEffect(isError)` to trigger only on transition to the error state. Test passes.
3. [REFACTOR] Extract the shake animation into a `@Composable fun rememberShakeOffset(trigger: Boolean): State<Float>` composable helper. Ensure shake animation is suppressed when `reduceMotion == true` (pass the flag from T022's helper). Verify `GridRendererTest` still passes.

**Acceptance**: Sum indicators visibly shake/flash when a row or column sum exceeds its target. Animation does not repeat — fires once per error transition.  
**Estimate**: ~3h  
**deps**: Sprint 3 integration gate  
**parallel**: T022, T024, T025, T026, T027, T028, T029

---

### T024 — Landscape layout handling or portrait lock

**Feature**: S04-F004  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/main/AndroidManifest.xml` — add `screenOrientation` or Window size class logic
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt` — optional adaptive layout
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/GridRendererTest.kt` — add landscape viewport test

**TDD steps**:
1. [RED] In `GridRendererTest.kt`, add:
   - `puzzleScreen_rendersCorrectly_inLandscapeViewport()` — render `PuzzleScreen` inside a `Box(Modifier.size(640.dp, 360.dp))`; assert neither `testTag("puzzle_grid")` nor `testTag("number_pad")` has a zero-size bounding box.
   Fail if current layout stretches to zero height for one of the components in landscape.
2. [GREEN] **Decision**: Portrait lock is the lowest-risk option. Add `android:screenOrientation="portrait"` to all `<activity>` declarations in `AndroidManifest.xml`. This immediately fixes landscape distortion without layout changes. The test now passes (portrait-only, landscape test is moot — adjust test to reflect locked portrait). Alternatively, if adaptive layout is chosen: use `WindowSizeClass` to switch to a `Row { GridRenderer(); NumberPad() }` layout in compact-wide windows.
3. [REFACTOR] If portrait lock is chosen, document the decision in `AndroidManifest.xml` comments and in `NOTES.md`. If adaptive layout is chosen, extract the width-class branching into a `@Composable fun PuzzleLayout(windowSizeClass: WindowSizeClass, ...)` function.

**Acceptance**: App does not distort in landscape. Either locked to portrait (manifest) or shows proper side-by-side layout (adaptive).  
**Estimate**: ~6h  
**deps**: Sprint 3 integration gate  
**parallel**: T022, T023, T025, T026, T027, T028, T029

---

### T025 — TalkBack focus ordering for PuzzleScreen

**Feature**: S04-F005  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/GridAccessibilityTest.kt` — extend existing test
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt` — add traversal groups + accessibility announcement

**TDD steps**:
1. [RED] In `GridAccessibilityTest.kt`, add:
   - `puzzleScreen_talkBackOrder_timerFirst()` — using `composeTestRule.onNode(hasTestTag("puzzle_timer")).fetchSemanticsNode().config[SemanticsProperties.TraversalIndex]`; assert timer traversal index < grid traversal index.
   - `puzzleScreen_completion_accessibilityAnnouncementFired()` — simulate completion; assert a node with `LiveRegion.Polite` semantics emits `"Puzzle complete"`.
   Both fail — no explicit traversal ordering or live region exists.
2. [GREEN] In `PuzzleScreen.kt`, apply `Modifier.semantics { traversalIndex = 0f }` to the timer, `traversalIndex = 1f` to the `GridRenderer` container, `traversalIndex = 2f` to the `NumberPad` container. Use `isTraversalGroup = true` on each container. Add a hidden `Text` with `Modifier.semantics { liveRegion = LiveRegion.Polite }` that shows `"Puzzle complete, solved in ${formatTime(elapsed)}"` when `isCompleted == true`. Tests pass.
3. [REFACTOR] Audit `OnboardingScreen` and `HomeScreen` for TalkBack ordering in a single pass — fix any issues found. Add `contentDescription` to the timer text: `"Elapsed time: ${formatTime(elapsed)}"`.

**Acceptance**: TalkBack traversal order: timer → grid cells → number pad. "Puzzle complete" announcement fires when puzzle is solved.  
**Estimate**: ~4h  
**deps**: Sprint 3 integration gate  
**parallel**: T022, T023, T024, T026, T027, T028, T029

---

### T026 — [P] Pencil/notes mode state in PuzzleViewModel

**Feature**: S04-F003 (part 1 — ViewModel)  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/PuzzleViewModelTest.kt` — add notes mode test suite
- `app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/PuzzleViewModel.kt` — add `notesValues`, `isNotesMode`, `toggleNotesMode()`, `enterNote()`

**TDD steps**:
1. [RED] In `PuzzleViewModelTest.kt`, add:
   - `notesMode_toggledOn_by_toggleNotesMode()` — call `toggleNotesMode()`; assert `uiState.isNotesMode == true`.
   - `notesMode_enterNote_addsToCell()` — enable notes mode; select a cell; call `enterNote(3)`; assert `uiState.notesValues[row][col].contains(3)`.
   - `notesMode_enterNote_sameDigitTwice_removesIt()` — toggle same digit; assert it is removed (toggle behaviour).
   - `notesMode_enterNumber_clearsNotesForCell()` — enter a real answer in a cell; assert `notesValues[row][col].isEmpty()`.
   - `notesMode_notesCleared_onLoadPuzzle()` — load a new puzzle; assert all notes are empty.
   All fail — `isNotesMode`, `notesValues`, `toggleNotesMode()` do not exist.
2. [GREEN] Add `notesValues: Array<Array<Set<Int>>>` and `isNotesMode: Boolean` to `PuzzleUiState`. Add `fun toggleNotesMode()` to `PuzzleViewModel` (flips `isNotesMode`). Add `fun enterNote(digit: Int)` that, when `isNotesMode`, adds/removes from `notesValues[row][col]`. Modify `enterNumber()` to clear `notesValues[row][col]` when a real answer is entered. Clear notes in `loadPuzzle()`. Tests pass.
3. [REFACTOR] Ensure `notesValues` deep copy is part of the `moveHistory` snapshot (undo reverts notes too). Extract `Array<Array<Set<Int>>>.deepCopy()` as a private extension. Update `undo()` to restore `notesValues` from the snapshot.

**Acceptance**: `PuzzleViewModelTest` passes. Notes state is independent from answer state. Undo reverts notes changes.  
**Estimate**: ~6h  
**deps**: T010 (undo pattern established — notes must be part of undo history)  
**parallel**: T022, T023, T024, T025, T028, T029

---

### T027 — Pencil/notes mode rendering in GridRenderer and NumberPad

**Feature**: S04-F003 (part 2 — UI)  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/GridRendererTest.kt` — add notes rendering test
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt` — render superscript notes in cell corners
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/NumberPad.kt` — add pencil toggle button

**TDD steps**:
1. [RED] In `GridRendererTest.kt`, add:
   - `gridRenderer_showsNoteDigits_whenNotesPresent()` — render `GridRenderer` with `uiState.notesValues[0][0] = setOf(1, 3, 5)`; assert 3 elements with `contentDescription` matching `"Note: 1"`, `"Note: 3"`, `"Note: 5"` are visible within the cell.
   - `numberPad_pencilToggle_visibleAndCallsToggleNotesMode()` — render `NumberPad(isNotesMode = false, onToggleNotes = {})`; assert pencil icon button visible.
   Both fail — notes rendering and pencil toggle do not exist.
2. [GREEN] In `GridRenderer.kt`, within the cell rendering logic, if `notesValues[row][col].isNotEmpty()` and `userValues[row][col] == 0`: render a `Box` with a 2x3 sub-grid of small `Text` composables (up to 6 notes) at 40% of normal cell text size. Each shown note digit has `contentDescription = "Note: $digit"`. In `NumberPad.kt`, add a pencil `IconButton` (`Icons.Filled.Edit` or similar) with `contentDescription = "Toggle pencil mode"` that calls `onToggleNotes()`. When `isNotesMode == true`, the icon button shows a tinted/filled state. Tests pass.
3. [REFACTOR] Ensure note digits use `MaterialTheme.colorScheme.tertiary` for colour (distinguishable from answer digits). Apply `Modifier.clearAndSetSemantics { }` to the notes sub-grid container to avoid TalkBack reading each tiny digit individually — only the cell-level description is needed. Run `GridAccessibilityTest` to confirm no regression.

**Acceptance**: Notes mode toggle visible on NumberPad. Tapping a number in notes mode adds/removes a small superscript digit in the cell corner. Entering a real answer clears the notes for that cell.  
**Estimate**: ~10h  
**deps**: T026 (ViewModel notes state must exist), T025 (TalkBack ordering established — must not break)  
**parallel**: T028, T029

---

### T028 — [P] Add Hard (6x6) and Expert (7x7) difficulty levels — engine

**Feature**: S04-F006 (part 1 — engine/data)  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/engine/DifficultyTest.kt` — extend with HARD/EXPERT cases
- `app/src/test/kotlin/org/dgeek/sumgrid/engine/PuzzleGeneratorTest.kt` — extend with 6x6/7x7 generation tests
- `app/src/main/kotlin/org/dgeek/sumgrid/engine/models/Difficulty.kt` — add HARD and EXPERT entries

**TDD steps**:
1. [RED] In `DifficultyTest.kt`, add:
   - `difficulty_hard_hasSizeOf6()` — `assert(Difficulty.HARD.size == 6)`.
   - `difficulty_expert_hasSizeOf7()` — `assert(Difficulty.EXPERT.size == 7)`.
   In `PuzzleGeneratorTest.kt`, add:
   - `puzzleGenerator_generates6x6Puzzle_forHard()` — `PuzzleGenerator.generate(seed=42L, Difficulty.HARD)` returns a puzzle with `grid.size == 6`.
   - `puzzleGenerator_generates7x7Puzzle_forExpert()` — same for `EXPERT`.
   - `puzzleGenerator_hard_puzzleHasUniqueSolution()` — `UniqueSolutionValidator.hasUniqueSolution(puzzle)` returns true for a generated HARD puzzle.
   All fail — `Difficulty.HARD` and `Difficulty.EXPERT` do not exist.
2. [GREEN] In `Difficulty.kt`, add `HARD(size = 6, maxVal = 9, emptyCells = 22, seedOffset = 3)` and `EXPERT(size = 7, maxVal = 9, emptyCells = 30, seedOffset = 4)`. Verify `PuzzleGenerator.generate` uses `difficulty.size` and `difficulty.maxVal` dynamically (it should already). Run `UniqueSolutionValidator` on a HARD puzzle — adjust `emptyCells` if solver times out (reduce if necessary). Tests pass.
3. [REFACTOR] Run `PuzzlePrecomputeWorker` logic against HARD and EXPERT to confirm pre-computation does not time out. Add HARD and EXPERT to `DifficultyCalibrator` if it uses a difficulty list. Ensure `DifficultySelector` composable dynamically renders all `Difficulty.entries` (it should already).

**Acceptance**: `DifficultyTest` and `PuzzleGeneratorTest` pass. HARD and EXPERT difficulties generate valid, uniquely-solvable puzzles.  
**Estimate**: ~6h  
**deps**: T011 (2-row NumberPad confirmed — HARD/EXPERT need it for maxVal=9)  
**parallel**: T022, T023, T024, T025, T026, T027

---

### T029 — Hard and Expert difficulty UI integration

**Feature**: S04-F006 (part 2 — UI integration)  
**Phase**: [RED] → [GREEN] → [REFACTOR]  
**Files**:
- `app/src/test/kotlin/org/dgeek/sumgrid/viewmodel/HomeViewModelTest.kt` — extend for 5-difficulty puzzle status
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt` — show HARD/EXPERT in DifficultySelector
- `app/src/main/kotlin/org/dgeek/sumgrid/viewmodel/HomeViewModel.kt` — handle 5 difficulties in `puzzleStatuses`

**TDD steps**:
1. [RED] In `HomeViewModelTest.kt`, add:
   - `homeVm_puzzleStatuses_includesHardAndExpert()` — assert `uiState.puzzleStatuses.containsKey(Difficulty.HARD)` and `containsKey(Difficulty.EXPERT)`.
   - `homeVm_allComplete_requiresAllFiveDifficulties()` — mark only 3 difficulties complete; assert `allComplete == false`.
   Both fail — `puzzleStatuses` currently only tracks 3 difficulties.
2. [GREEN] `HomeViewModel.puzzleStatuses` already uses `Difficulty.entries` — adding `HARD` and `EXPERT` to the enum automatically includes them. Verify the tests pass with no code change in `HomeViewModel`. In `HomeScreen.kt`, the `DifficultySelector` composable already iterates `Difficulty.entries` — HARD and EXPERT appear automatically. Run a manual test to verify the number pad shows the 2-row layout for 6x6/7x7 puzzles. All tests pass.
3. [REFACTOR] Update `allComplete` logic in `HomeViewModel` to `Difficulty.entries.all { ... }` (it likely already does). Ensure `HomeScreen` layout accommodates 5 difficulty cards without overflow (add horizontal scroll or a compact card size if needed). Run the full test suite.

**Acceptance**: HomeScreen shows HARD (6x6) and EXPERT (7x7) difficulty options. Both are playable. All-done state requires all 5 difficulties complete.  
**Estimate**: ~10h  
**deps**: T028 (HARD/EXPERT must exist in enum first)  
**parallel**: T026, T027

---

### Sprint 4 Integration Gate (Epic Complete)

All items must pass before the epic is considered complete:

- [ ] `./gradlew :app:testDebugUnitTest` passes — full test suite green
- [ ] Pulsing flame animation disabled when Android accessibility "Remove animations" is on
- [ ] Sum indicator shakes/flashes on first error transition (not on every update)
- [ ] App does not distort in landscape (portrait lock or adaptive layout implemented)
- [ ] TalkBack traversal order: timer → grid → number pad (verified with TalkBack enabled on device)
- [ ] "Puzzle complete" accessibility announcement fires on completion
- [ ] Pencil mode toggle visible on NumberPad; candidate numbers render in cell corners for Medium+
- [ ] Entering a real answer clears notes for that cell; undo reverts notes
- [ ] Hard (6x6) puzzle is selectable, generates a valid puzzle, and is playable
- [ ] Expert (7x7) puzzle is selectable, generates a valid puzzle, and is playable
- [ ] All 31 UX review findings addressed (cross-reference against `docs/sumgrid-ux-review.md`)

---

## Task Summary

### Complete task list with TDD phase markers

| ID | Sprint | Feature | TDD Phase | Priority | Est. |
|----|--------|---------|-----------|----------|------|
| T001 | S01 | BLOCKER: StreakRepository injection fix | [GREEN]+[REFACTOR] | [P] | 1h |
| T002 | S01 | S01-F001: Wire share button | [RED]→[GREEN]→[REFACTOR] | [P] | 2h |
| T003 | S01 | S01-F005: TopAppBar with back button | [RED]→[GREEN]→[REFACTOR] | — | 2h |
| T004 | S01 | S01-F002: Streak badges BadgeRow | [RED]→[GREEN]→[REFACTOR] | [P] | 3h |
| T005 | S01 | S01-F006: Scrollable HomeScreen | [RED]→[GREEN]→[REFACTOR] | — | 1h |
| T006 | S01 | S01-F007: All-done state HomeScreen | [RED]→[GREEN]→[REFACTOR] | — | 2h |
| T007 | S01 | S01-F003: Skip onboarding option | [RED]→[GREEN]→[REFACTOR] | — | 2h |
| T008 | S01 | S01-F004: Rules overlay onboarding | [RED]→[GREEN]→[REFACTOR] | — | 3h |
| T009 | S02 | S02-F001: CompletionCard BottomSheet | [RED]→[GREEN]→[REFACTOR] | [P] | 8h |
| T010 | S02 | S02-F002: Undo in PuzzleViewModel | [RED]→[GREEN]→[REFACTOR] | [P] | 6h |
| T011 | S02 | S02-F003: 2-row NumberPad layout | [RED]→[GREEN]→[REFACTOR] | — | 4h |
| T012 | S02 | S02-F004: Dark mode contrast fixes | [RED]→[GREEN]→[REFACTOR] | — | 6h |
| T013 | S02 | S02-F005: Font scaling fixes | [RED]→[GREEN]→[REFACTOR] | — | 6h |
| T014 | S02 | S02-F006: Auto-save in PuzzleViewModel | [RED]→[GREEN]→[REFACTOR] | [P] | 6h |
| T015 | S03 | Nav routes prereq (stats + practice) | [RED]→[GREEN]→[REFACTOR] | [P] | 1h |
| T016 | S03 | S03-F001: StatsScreen + StatsViewModel | [RED]→[GREEN]→[REFACTOR] | [P] | 12h |
| T017 | S03 | S03-F002: NotificationScheduler + Worker | [RED]→[GREEN]→[REFACTOR] | — | 5h |
| T018 | S03 | S03-F002: Notification preferences UI | [RED]→[GREEN]→[REFACTOR] | — | 3h |
| T019 | S03 | S03-F003: PracticeViewModel | [RED]→[GREEN]→[REFACTOR] | [P] | 4h |
| T020 | S03 | S03-F003: PracticeScreen composable | [RED]→[GREEN]→[REFACTOR] | [P] | 12h |
| T021 | S03 | S03-F004: Enhanced celebration confetti | [RED]→[GREEN]→[REFACTOR] | — | 6h |
| T022 | S04 | S04-F001: Reduce motion support | [RED]→[GREEN]→[REFACTOR] | — | 3h |
| T023 | S04 | S04-F002: Error shake animation | [RED]→[GREEN]→[REFACTOR] | — | 3h |
| T024 | S04 | S04-F004: Landscape layout / portrait lock | [RED]→[GREEN]→[REFACTOR] | — | 6h |
| T025 | S04 | S04-F005: TalkBack focus ordering | [RED]→[GREEN]→[REFACTOR] | — | 4h |
| T026 | S04 | S04-F003: Pencil mode — ViewModel | [RED]→[GREEN]→[REFACTOR] | [P] | 6h |
| T027 | S04 | S04-F003: Pencil mode — UI rendering | [RED]→[GREEN]→[REFACTOR] | — | 10h |
| T028 | S04 | S04-F006: Hard/Expert difficulty — engine | [RED]→[GREEN]→[REFACTOR] | [P] | 6h |
| T029 | S04 | S04-F006: Hard/Expert difficulty — UI | [RED]→[GREEN]→[REFACTOR] | — | 10h |

**Total tasks**: 29  
**High-priority [P] tasks**: 12  
**TDD RED tasks**: 27 (all except T001 which is a bug fix with pre-existing RED)  
**Total estimated hours**: ~141h

---

## Dependency Graph (full epic)

```
T001 (BLOCKER)
  └── T004 (badges)

T002 (share button)
  └── T003 (TopAppBar) [same file]
  └── T009 (CompletionCard — reuses share logic)

T005 (scrollable home)
  └── T006 (all-done state) [same file]

T007 (skip onboarding)
  └── T008 (rules overlay) [same file]

Sprint 1 gate
  └── T009 (CompletionCard)
  └── T010 (undo)
  └── T011 (2-row pad)
  └── T012 (dark mode)
  └── T013 (font scaling)
  T010 → T014 (auto-save, same ViewModel — sequential)

T011 (2-row pad)
  └── T028 (Hard/Expert engine)
      └── T029 (Hard/Expert UI)

Sprint 2 gate
  └── T015 (nav routes prereq)
      └── T016 (StatsScreen)
      └── T017 (notification backend)
          └── T018 (notification UI)
      └── T019 (PracticeViewModel)
          └── T020 (PracticeScreen)
  T009 (CompletionCard) → T021 (celebration)
  T014 (auto-save) → T016 (stats reads persistent data)

Sprint 3 gate
  └── T022 (reduce motion)
  └── T023 (error shake)
  └── T024 (landscape)
  └── T025 (TalkBack)
  T010 (undo) → T026 (pencil mode VM)
      └── T027 (pencil mode UI)
```

---

## New Test Files to Create

| Test file | Sprint | Covers |
|-----------|--------|--------|
| `ui/PuzzleScreenTopBarTest.kt` | S01 | T003 back button |
| `ui/HomeScreenLayoutTest.kt` | S01 | T005 scroll |
| `ui/NumberPadLayoutTest.kt` | S02 | T011 2-row layout |
| `ui/CompletionCardTest.kt` | S02 | T009 completion card |
| `viewmodel/StatsViewModelTest.kt` | S03 | T016 stats aggregation |
| `ui/StatsScreenTest.kt` | S03 | T016 stats screen |
| `notifications/NotificationSchedulerTest.kt` | S03 | T017 WorkManager |
| `ui/NotificationPermissionBannerTest.kt` | S03 | T018 permission banner |
| `viewmodel/PracticeViewModelTest.kt` | S03 | T019 practice VM |
| `ui/PracticeScreenTest.kt` | S03 | T020 practice screen |

---

## New Implementation Files to Create

| File | Sprint | Task |
|------|--------|------|
| `ui/puzzle/CompletionCard.kt` | S02 | T009 |
| `viewmodel/StatsViewModel.kt` | S03 | T016 |
| `ui/stats/StatsScreen.kt` | S03 | T016 |
| `viewmodel/PracticeViewModel.kt` | S03 | T019 |
| `ui/practice/PracticeScreen.kt` | S03 | T020 |
| `notifications/DailyReminderWorker.kt` | S03 | T017 |
| `notifications/NotificationScheduler.kt` | S03 | T017 |
| `ui/theme/MotionPreference.kt` | S04 | T022 |
