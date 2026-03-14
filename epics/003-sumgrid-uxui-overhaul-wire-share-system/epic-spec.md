# Epic Specification: SumGrid UX/UI Overhaul — Wire Share System, Badges, and Full Product Polish

**Epic ID**: 003  
**Epic Branch**: epic/003-sumgrid-uxui-overhaul-wire-share-system  
**Created**: 2026-03-14  
**Status**: Approved  
**Source Audit**: docs/sumgrid-ux-review.md (31 findings)  
**Total Scope**: ~141 hours across 4 sprints, 20 features  
**Workflow Type**: Epic (4 sprints, sequential with independent shippability per sprint)

---

## Objective

SumGrid has a well-architected foundation with a critical gap: the **share system and badge rewards are fully implemented in code but never surfaced in the UI** — these are free wins. The puzzle completion moment (the #1 retention touchpoint) is a single line of text. Onboarding forces a multi-day commitment with no skip option and no rules. This epic executes a comprehensive UX/UI pass across all 3 screens, 7 components, and the theme system based on a 31-finding source-code audit, transforming SumGrid from a functional prototype into a polished, retention-optimised daily puzzle game.

---

## Architecture Notes: Existing Code Ready to Reuse

Before describing features, the following existing implementations must be wired rather than rebuilt:

### Share System (Fully Implemented — Needs UI Only)
- **`ShareCardGenerator`** (`org.dgeek.sumgrid.share`): Generates a spoiler-free plain-text share card. Accepts `puzzle: Puzzle`, `elapsedMillis: Long`, `date: LocalDate`. Returns a string in the format `SumGrid #N — Difficulty ⏱ M:SS \n [emoji grid] \n Play free → sumgrid.dgeek.org`. White squares (⬜) for given cells, green squares (🟩) for user-solved cells.
- **`shareResult(context, shareText)`** (`org.dgeek.sumgrid.share.ShareIntentLauncher`): Top-level function that creates an `Intent.ACTION_SEND` chooser and calls `context.startActivity()`. Ready to call directly from a Compose `onClick` via `LocalContext.current`.
- **Wire point**: `PuzzleScreen.kt` already has `state.puzzle`, `state.elapsedSeconds`, and the completion flag. Add a Share button calling `shareResult(context, ShareCardGenerator.generate(puzzle, elapsedMillis, date))`.

### Badge System (Fully Implemented — Needs UI Only)
- **`StreakBadge` enum** (`org.dgeek.sumgrid.streak`): Defines 4 milestone badges — `WEEKLY_WARRIOR(7, "Weekly Warrior", "🔥")`, `MONTHLY_MASTER(30, "Monthly Master", "⭐")`, `CENTURY_SOLVER(100, "Century Solver", "💎")`, `YEAR_OF_LOGIC(365, "Year of Logic", "👑")`.
- **`StreakState`** (`org.dgeek.sumgrid.streak`): Contains `earnedBadges: Set<StreakBadge>`, `currentStreak: Int`, `longestStreak: Int`, `lastCompletionDate: LocalDate?`. Already persisted via `StreakRepository` (DataStore-backed).
- **`StreakRepository`** (`org.dgeek.sumgrid.streak`): DataStore-backed; exposes `streakState: Flow<StreakState>`. Already collected by `HomeViewModel` and exposed as `state.currentStreak`. **`state.earnedBadges` is not yet in `HomeUiState`** — add it.
- **Wire point**: Add `earnedBadges: Set<StreakBadge>` to `HomeUiState` in `HomeViewModel`. Observe from `StreakRepository.streakState`. Render in `HomeScreen`.

### Celebration Animation (Implemented — Needs Enhancement)
- **`CelebrationState`** and **`rememberCelebrationState`** (`org.dgeek.sumgrid.ui.components.CelebrationAnimation`): Per-cell scale animation (1.0 → 1.2 → 1.0, 50ms stagger, 300ms duration). Color progress animatable from surface color to `colorScheme.tertiary`. The overlay `Box` in `PuzzleScreen` has a `testTag("celebration_overlay")` placeholder comment explicitly calling out S03-F003.
- **Enhancement point**: Add confetti particle overlay or full-screen animated burst on top of existing `rememberCelebrationState`. The existing `CelebrationCellWrapper` composable can remain; supplement it with a full-screen overlay composable.

### State Persistence (Partial — CompletionStore Wired, Mid-Puzzle Not)
- **`PuzzleViewModel`**: Persists completion via `CompletionStore` on the `false → true` transition. Does NOT persist mid-puzzle `userValues` on each cell change. `timerStarted` and `completionPersisted` are in-memory only — lost on process kill.
- **`StreakRepository`**: DataStore-backed; streak persists across kills. `earnedBadges` accumulates and never resets.
- **Gap**: Add DataStore/Room persistence for `userValues` on every `enterNumber()` / `clearCell()` call in `PuzzleViewModel`.

### Grid & Theme (Complete — Audit Required)
- **`GridRenderer`**: Canvas-based with `gridColorsFromTheme()` that maps `colorScheme.primaryContainer` to `givenCellBg` and `colorScheme.surface` to `userCellBg`. On OLED dark theme (`OledBlack = #090909`), `primaryContainer` is `Indigo90 (#DDE1FF)` — contrast ratio needs audit.
- **`NumberPad`**: Single `Row` at `height(56.dp)` with `weight(1f)` per button. Currently renders all numbers 1..`difficulty.maxVal` in one row. For `MEDIUM` (5×5, maxVal=9), this is 10 buttons at ~32dp each on a 360dp phone — below the 44dp minimum touch target.

---

## Sprint Structure

| Sprint | Name | Hours | Features | Dependency |
|--------|------|-------|----------|------------|
| S01 | Quick Wins & Critical Fixes | ~15h | 7 | None |
| S02 | Core Game UX | ~36h | 6 | S01 |
| S03 | Retention & Engagement | ~42h | 4 | S02 |
| S04 | Polish & Accessibility | ~48h | 6 | S03 |

---

## Sprint 1: Quick Wins & Critical Fixes (~15h)

**Objective**: Wire the already-built systems (share, badges), fix the most painful UX friction points (onboarding, no exit), and harden the HomeScreen for all device sizes. All 7 features are Small effort — no new architecture needed.

**Sprint Goal**: After S01, the app has its first user-facing share button, earned badges are visible, new users can skip onboarding, and the HomeScreen works on every phone.

---

### S01-F001: Wire Share Button to Puzzle Completion Screen

**Feature ID**: S01-F001  
**Review Task**: #1  
**Effort**: ~2h  
**Files**: `PuzzleScreen.kt`, `ShareCardGenerator.kt` (read only), `ShareIntentLauncher.kt` (read only)

#### User Story
As a player who just solved a puzzle, I want a Share button on the completion screen so that I can share my result with friends without having to dig through menus.

#### Acceptance Criteria

**Given** a player has just completed a puzzle (all cells filled, all sum indicators green),  
**When** the completion banner is shown,  
**Then** a "Share" button is visible below the "Puzzle complete! 🎉" text.

**Given** a player taps the Share button on the completion screen,  
**When** the Android system share sheet opens,  
**Then** it contains the spoiler-free share card generated by `ShareCardGenerator.generate(puzzle, elapsedMillis, date)` with the correct day number, difficulty label, solve time, emoji grid, and `sumgrid.dgeek.org` link.

**Given** a player taps the Share button,  
**When** `shareResult(context, text)` is called,  
**Then** the system chooser displays with `type = "text/plain"` and the share card as `EXTRA_TEXT`.

**Given** the completion screen is visible,  
**When** the share button is rendered,  
**Then** it uses `MaterialTheme.colorScheme.tertiary` (warm amber) as its container color to visually distinguish it as a CTA.

#### Technical Notes
- Call `val context = LocalContext.current` in `PuzzleScreen`.
- `elapsedMillis` is available as `currentState.elapsedSeconds * 1000L`.
- The puzzle `date` must be passed into `PuzzleScreen` or resolved from `PuzzleViewModel`. Add a `puzzleDate: LocalDate` parameter to `PuzzleScreen` composable (already used internally by `PuzzleViewModel`).
- No new classes needed — both `ShareCardGenerator` and `shareResult` are ready.

---

### S01-F002: Display Earned Streak Badges on HomeScreen

**Feature ID**: S01-F002  
**Review Task**: #2  
**Effort**: ~3h  
**Files**: `HomeViewModel.kt`, `HomeScreen.kt`, `StreakBadge.kt` (read only), `StreakState.kt` (read only)

#### User Story
As a player who has maintained a long streak, I want to see my earned badges on the home screen so that I feel rewarded for my commitment and am motivated to continue.

As a new player, I want to see locked badges displayed on the home screen so that I understand what milestones I am working toward.

#### Acceptance Criteria

**Given** a player has earned one or more `StreakBadge` entries,  
**When** the HomeScreen loads,  
**Then** earned badges are displayed with their `icon` emoji, `displayName` label, and a visually distinct "earned" style (e.g., `tertiaryContainer` background, full opacity).

**Given** a player has not yet earned a particular `StreakBadge`,  
**When** the badges section is rendered,  
**Then** the locked badge is shown at reduced opacity (0.35–0.4f) with a grayed-out appearance, indicating a future target.

**Given** a player has earned no badges (streak < 7),  
**When** the HomeScreen badges section renders,  
**Then** all 4 badges are shown in locked state — none are hidden.

**Given** a player earns a new badge after completing a puzzle,  
**When** they return to the HomeScreen,  
**Then** the newly earned badge transitions to the earned style without requiring an app restart (reactive via `StateFlow`).

**Given** the badges section is rendered,  
**When** any badge is tapped,  
**Then** nothing happens (badges are display-only in this sprint; no navigation).

#### Technical Notes
- Add `earnedBadges: Set<StreakBadge> = emptySet()` to `HomeUiState` in `HomeViewModel.kt`.
- In `HomeViewModel.observeStreak()`, update to also copy `earnedBadges` from `StreakState`.
- `InMemoryStreakRepository` (used by `HomeViewModel`) wraps `StreakRepository`; confirm `streakState` flow includes `earnedBadges` — it does (see `StreakRepository.streakState`).
- Add a `BadgesRow` private composable in `HomeScreen.kt` below `StreakDisplay`. Use `LazyRow` or a standard `Row` with `Arrangement.spacedBy(8.dp)`.
- Each badge item: a `Surface` with `shape = MaterialTheme.shapes.small`, 8dp padding, badge icon in `headlineSmall`, `displayName` in `labelSmall`. Earned: `tertiaryContainer`. Locked: `surfaceVariant` at alpha 0.4f.

---

### S01-F003: Add Skip Option to Onboarding

**Feature ID**: S01-F003  
**Review Task**: #3  
**Effort**: ~2h  
**Files**: `OnboardingScreen.kt`, `OnboardingViewModel.kt`

#### User Story
As an experienced puzzle player installing SumGrid for the first time, I want a "Skip" option on the onboarding screen so that I can go directly to the home screen without being forced through a 3-day tutorial sequence.

#### Acceptance Criteria

**Given** the onboarding screen is showing,  
**When** it renders for the first time,  
**Then** a "Skip tutorial" text button or link is visible at the top-right of the `Scaffold` (e.g., in a `TopAppBar` action slot or as a standalone `TextButton` row).

**Given** a user taps "Skip tutorial",  
**When** the tap is handled,  
**Then** `onboardingViewModel.markComplete()` is called and `onOnboardingComplete()` navigates to the home screen immediately.

**Given** a user taps "Skip tutorial",  
**When** they arrive at the home screen,  
**Then** the onboarding screen does not appear again on subsequent app launches (completion persisted via `OnboardingViewModel.markComplete()`).

**Given** a user is on launch 2 or launch 3 of onboarding (mid-sequence),  
**When** they tap "Skip tutorial",  
**Then** onboarding is marked complete and they are taken directly to the home screen (same behaviour as launch 1 skip).

#### Technical Notes
- `OnboardingViewModel.markComplete()` already exists and persists the completion flag.
- Add a `TextButton("Skip tutorial")` in a `Row` below the title text, aligned end, before the `GridRenderer`.
- The `onOnboardingComplete` callback is already wired at the call site — reuse it.

---

### S01-F004: Add Rules Explanation to Onboarding

**Feature ID**: S01-F004  
**Review Task**: #4  
**Effort**: ~3h  
**Files**: `OnboardingScreen.kt`

#### User Story
As a new player seeing SumGrid for the first time, I want a brief explanation of the rules before I attempt the first puzzle so that I know what the row and column numbers mean and what I am trying to accomplish.

#### Acceptance Criteria

**Given** a player is on launch 1 of onboarding (`launchCount == 1`),  
**When** the onboarding screen renders,  
**Then** a rules explanation panel is shown above the grid containing the text: "Fill each empty cell with a number. Every row and column must add up to the target shown beside it."

**Given** a player is on launch 2 or launch 3 of onboarding,  
**When** the onboarding screen renders,  
**Then** the rules panel is not shown (they have seen it once; subsequent puzzles reinforce without repeating).

**Given** the rules panel is shown,  
**When** it renders,  
**Then** it uses a `Surface` with `secondaryContainer` background, `bodyMedium` typography, rounded corners (`MaterialTheme.shapes.medium`), and 12dp padding — visually distinct from the grid.

**Given** the rules panel is shown,  
**When** the screen is rendered at 200% font scale,  
**Then** the panel text wraps correctly and does not clip or overflow the screen.

#### Technical Notes
- Show panel when `isFirstLaunch || launchCount == 1` — the same condition already used for the "Got it" button.
- Position the panel between the title text and the `GridRenderer`, with an `8.dp` spacer above and below.

---

### S01-F005: Add Back/Exit Button to PuzzleScreen

**Feature ID**: S01-F005  
**Review Task**: #5  
**Effort**: ~2h  
**Files**: `PuzzleScreen.kt`

#### User Story
As a player who started a puzzle but wants to exit, I want a visible back button on the puzzle screen so that I can navigate back to the home screen without relying on the Android system back gesture.

#### Acceptance Criteria

**Given** a player is on the puzzle screen,  
**When** the screen renders,  
**Then** a `TopAppBar` is visible at the top with a back arrow icon (`Icons.AutoMirrored.Filled.ArrowBack`) on the left and the current difficulty label on the right or centre.

**Given** a player taps the back arrow,  
**When** the tap is handled,  
**Then** the `onNavigateBack: () -> Unit` callback is invoked, returning the player to the home screen.

**Given** a player taps the back arrow while a puzzle is in progress (not completed),  
**When** they arrive back at the home screen,  
**Then** no completion is recorded (partial progress behaviour is unchanged from current system — state persistence is addressed in S02-F006).

**Given** the `TopAppBar` is rendered,  
**When** using TalkBack,  
**Then** the back button has `contentDescription = "Back to home"` and the difficulty label is a separate semantics node.

#### Technical Notes
- Add `onNavigateBack: () -> Unit` parameter to `PuzzleScreen` composable.
- Replace the current `Scaffold` with one that includes a `topBar` slot containing `TopAppBar(title = { Text(difficulty.displayName) }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, ...) } })`.
- `currentState.puzzle.difficulty.name` gives the difficulty string; lowercase + capitalise for display.
- The existing `Scaffold(modifier = modifier.fillMaxSize())` becomes `Scaffold(topBar = { ... }, modifier = modifier.fillMaxSize())`.

---

### S01-F006: Make HomeScreen Scrollable

**Feature ID**: S01-F006  
**Review Task**: #10  
**Effort**: ~1h  
**Files**: `HomeScreen.kt`

#### User Story
As a player on a small phone (360dp width, compact height), I want the home screen to scroll so that I can reach the Play button and footer even if content extends below the visible area.

#### Acceptance Criteria

**Given** a player is on a device with compact screen height (e.g., Pixel 4a),  
**When** the HomeScreen renders,  
**Then** all content including the Play button and "More by dgeek" footer is reachable by scrolling vertically.

**Given** the HomeScreen is wrapped in a scroll container,  
**When** the screen is rendered on a tall device (standard phone),  
**Then** the content layout is visually identical to the current design (no undesirable whitespace or stretching).

**Given** the HomeScreen is scrollable,  
**When** using TalkBack linear navigation,  
**Then** all elements remain reachable in logical top-to-bottom order.

#### Technical Notes
- Add `rememberScrollState()` and `.verticalScroll(scrollState)` to the root `Column` modifier.
- Remove the `Spacer(modifier = Modifier.weight(1f))` before the footer or replace it with a fixed `Spacer(modifier = Modifier.height(24.dp))` — `weight()` is not compatible with `verticalScroll`.

---

### S01-F007: All-Done State for HomeScreen

**Feature ID**: S01-F007  
**Review Task**: #13  
**Effort**: ~2h  
**Files**: `HomeScreen.kt`, `HomeViewModel.kt`

#### User Story
As a player who has completed all three daily puzzles, I want the home screen to acknowledge my accomplishment so that I know there is nothing left to do today and feel rewarded for completing everything.

#### Acceptance Criteria

**Given** all 3 daily puzzles are completed (`puzzleStatuses.all { it.isCompleted }`),  
**When** the HomeScreen renders,  
**Then** the Play button is replaced by an "All done for today! 🎉" message in `titleMedium` text with `tertiaryContainer` surface background.

**Given** all 3 daily puzzles are completed,  
**When** the all-done state renders,  
**Then** a secondary "Replay" `OutlinedButton` is shown below the all-done message, allowing replay with a clearly different visual weight than the primary Play button.

**Given** the all-done state renders,  
**When** a player taps "Replay",  
**Then** the `onStartPuzzle(selectedDifficulty)` callback is invoked with the selected difficulty — identical to the current Play button behaviour.

**Given** not all 3 puzzles are completed,  
**When** the HomeScreen renders,  
**Then** the standard Play button is shown (no regression).

#### Technical Notes
- Add computed property `val allDone = state.puzzleStatuses.all { it.isCompleted }` in the composable.
- Conditionally render the all-done Surface or the Play Button in the same layout position.
- The `CountdownDisplay` and `DifficultySelector` remain visible in both states.

---

## Sprint 2: Core Game UX (~36h)

**Objective**: Build the completion card (the #1 retention lever), add undo, fix the cramped number pad, audit dark mode contrast, fix font scaling, and ensure puzzle state persists across process kills.

**Sprint Goal**: After S02, the puzzle completion moment is a rich, shareable experience. Players can undo moves. The number pad is ergonomic on all phones. The app passes WCAG AA contrast in dark mode. Puzzle state survives app kills.

**Dependency**: Requires S01 (specifically S01-F001 for share button reuse in completion card).

---

### S02-F001: Build Completion Card / Bottom Sheet

**Feature ID**: S02-F001  
**Review Task**: #6  
**Effort**: ~8h  
**Files**: New `CompletionCard.kt` (composable), `PuzzleScreen.kt`  
**Depends on**: S01-F001 (share button wiring)

#### User Story
As a player who just solved a puzzle, I want a rich completion card to appear so that I can see my solve time, share my result, proceed to the next puzzle, or go back home — all from one clear moment of celebration.

#### Acceptance Criteria

**Given** a player completes a puzzle,  
**When** `isCompleted` transitions to `true`,  
**Then** a `ModalBottomSheet` (or full-width card) slides up from the bottom, replacing the current "Puzzle complete! 🎉" text banner.

**Given** the completion card is shown,  
**When** rendered,  
**Then** it contains: (1) a celebration headline "Puzzle Complete! 🎉" in `headlineMedium`, (2) the solve time formatted as `MM:SS` in `displayLarge` monospace style, (3) a difficulty badge chip showing the difficulty name, (4) a prominent "Share" button in `tertiary` colors, (5) a "Next Puzzle" filled `Button`, and (6) a "Back to Home" `OutlinedButton`.

**Given** the completion card Share button is tapped,  
**When** the Android share sheet opens,  
**Then** the share card text is identical to that produced by `ShareCardGenerator.generate(puzzle, elapsedMillis, date)` — reusing S01-F001's implementation.

**Given** the completion card is shown,  
**When** the player taps "Next Puzzle",  
**Then** the `onNextPuzzle: () -> Unit` callback is invoked (navigates to the next uncompleted difficulty, if any, otherwise equivalent to Back to Home).

**Given** the completion card is shown,  
**When** the player taps "Back to Home",  
**Then** `onNavigateBack()` is invoked and the player returns to the HomeScreen.

**Given** the completion card is shown on a compact device at 200% font scale,  
**When** rendered,  
**Then** all content is visible and the card scrolls internally if needed (use `Column` with `verticalScroll` inside the bottom sheet).

**Given** the completion card is shown,  
**When** the player uses TalkBack,  
**Then** the accessibility announcement "Puzzle complete. Solved in MM:SS." is emitted via `LocalAccessibilityManager` on card display.

#### Technical Notes
- Extract to a new composable `CompletionCard(state: PuzzleUiState, date: LocalDate, onShare: () -> Unit, onNextPuzzle: () -> Unit, onNavigateBack: () -> Unit)` in a new file `CompletionCard.kt`.
- Use `ModalBottomSheet` from `material3` (requires `androidx.compose.material3:material3` — already a dependency).
- The `CelebrationAnimation` (`rememberCelebrationState`) overlay in `PuzzleScreen` already fires when `isCompleted` — the completion card should display after the animation completes (~1.5s for 5×5 grid). Use a `LaunchedEffect` delay of `1500ms` before showing the card.
- Remove the current plain `Text("Puzzle complete! 🎉")` banner once the completion card is wired.

---

### S02-F002: Add Undo Functionality

**Feature ID**: S02-F002  
**Review Task**: #7  
**Effort**: ~6h  
**Files**: `PuzzleViewModel.kt`, `NumberPad.kt`

#### User Story
As a player who entered a wrong number, I want an undo button on the number pad so that I can revert my last move without manually clearing the cell.

#### Acceptance Criteria

**Given** a player has made at least one cell entry,  
**When** the number pad is visible,  
**Then** an "Undo" button (↩ icon or "Undo" label) is shown alongside the existing "✕" clear button.

**Given** a player taps the Undo button,  
**When** there is at least one move in the history stack,  
**Then** the last cell entry is reverted: the cell returns to its previous value (0 if it was empty before the entry).

**Given** a player taps Undo multiple times,  
**When** there are multiple moves in history,  
**Then** each tap reverts one move, in last-in-first-out order.

**Given** a player has made no moves yet (puzzle just loaded),  
**When** the Undo button is shown,  
**Then** the Undo button is visually disabled (same disabled style as the number buttons when no cell is selected).

**Given** a player undoes all moves back to the initial state,  
**When** they tap Undo again,  
**Then** nothing happens (no crash, button remains disabled).

**Given** the undo history exists,  
**When** a player completes the puzzle,  
**Then** the undo history is cleared (no undo after completion, as the completion card takes over).

#### Technical Notes
- Add `private val moveHistory: ArrayDeque<Pair<Pair<Int,Int>, Int>> = ArrayDeque()` to `PuzzleViewModel` — each entry is `(row, col) to previousValue`.
- In `enterNumber()`, before updating `userValues`, push `(selectedCell to existingValue)` onto the history.
- In `clearCell()`, push `(selectedCell to existingValue)` before clearing.
- Add `fun undo()` to `PuzzleViewModel`: pop from history, restore the cell value, call `buildState()`, update `_uiState`.
- Add `val canUndo: StateFlow<Boolean>` derived from history size, exposed to the UI.
- In `loadPuzzle()`, clear `moveHistory`.
- In `NumberPad.kt`, add `onUndoTap: () -> Unit` and `canUndo: Boolean` parameters. Add `UndoButton` composable (similar to `ClearButton`). Position undo between number buttons and clear button, or as a second clear-row button.

---

### S02-F003: Two-Row Number Pad for Medium Difficulty

**Feature ID**: S02-F003  
**Review Task**: #8  
**Effort**: ~4h  
**Files**: `NumberPad.kt`

#### User Story
As a player on a narrow phone solving a Medium (5×5) puzzle, I want the number pad to use two rows so that each button is large enough to tap accurately without accidentally hitting adjacent numbers.

#### Acceptance Criteria

**Given** the active puzzle has `difficulty.maxVal >= 7` (Medium, Hard, Expert),  
**When** the `NumberPad` renders,  
**Then** the buttons are arranged in two rows: row 1 shows numbers 1–5, row 2 shows numbers 6–maxVal plus the Clear button. The Undo button (from S02-F002) is also placed on row 2.

**Given** the active puzzle has `difficulty.maxVal < 7` (Beginner 3×3, Easy 4×4),  
**When** the `NumberPad` renders,  
**Then** the single-row layout is preserved (no regression for simpler difficulties).

**Given** the 2-row layout is used on a 360dp phone,  
**When** rendered,  
**Then** each button is at least 44dp in height and 44dp in effective touch target size (minimum per accessibility guidelines).

**Given** the 2-row layout is used,  
**When** rendered at 200% font scale,  
**Then** button labels remain visible and do not overflow their bounds.

#### Technical Notes
- Check `difficulty.maxVal >= 7` inside `NumberPad` composable.
- For 2-row layout: use a `Column` containing two `Row` composables. First row: `(1..5).toList()`. Second row: `(6..difficulty.maxVal).toList() + Clear + Undo`.
- Set each row `height(60.dp)` in the 2-row layout (vs current `height(56.dp)` single row).
- The `Difficulty` enum already has `maxVal`: Beginner=4, Easy=6, Medium=9. Verify by checking `Difficulty.kt`.

---

### S02-F004: Dark Mode Contrast Audit and Fix

**Feature ID**: S02-F004  
**Review Task**: #15  
**Effort**: ~6h  
**Files**: `Theme.kt`, `Color.kt`, `GridRenderer.kt`

#### User Story
As a player who uses dark mode, I want given cells and user cells to be clearly distinguishable from each other and from the background so that I can read the grid without strain.

#### Acceptance Criteria

**Given** the app is in dark mode (`isSystemInDarkTheme() == true`),  
**When** the puzzle grid is rendered,  
**Then** the contrast ratio between `givenCellBg` and `userCellBg` text is at least 4.5:1 (WCAG AA).

**Given** the app is in dark mode with OLED theme (`OledBlack = #090909`),  
**When** `gridColorsFromTheme()` maps `primaryContainer` to `givenCellBg`,  
**Then** the resulting color has a contrast ratio of at least 4.5:1 against `givenText` (which uses `colorScheme.primary`).

**Given** the dark mode theme is updated,  
**When** any theme token is changed,  
**Then** the change is reflected via `gridColorsFromTheme()` without modifying `GridRenderer.kt` directly (all colors flow through the theme mapping).

**Given** the light mode theme is not modified,  
**When** the app runs in light mode,  
**Then** no visual regressions occur (contrast audit applies to dark mode only).

#### Technical Notes
- Audit target: OLED dark scheme `primaryContainer = Indigo90 (#DDE1FF)` against `surface = OledSurface (#121212)`. Contrast of `#DDE1FF` on `#090909` background = ~15:1 (passes). Verify `givenText (primary = Indigo80 #BBC2FF)` on `givenCellBg (Indigo90 #DDE1FF)` = ~1.4:1 — this **fails** WCAG AA. Fix: use `Indigo20 (#0001AC)` for `givenText` in dark mode, or use `onPrimaryContainer` token instead.
- Check `Theme.kt` dark color scheme mapping for `onPrimaryContainer`. Update `gridColorsFromTheme()` to use `cs.onPrimaryContainer` for `givenText` instead of `cs.primary`.
- Also audit `userCellBg (OledSurface #121212)` and `cellText (Neutral10 #1B1B1F)` in dark mode — these will be near-invisible. Fix: `cellText` in dark mode should use `cs.onSurface` which maps to a light color.
- Run contrast checks with the Android Studio accessibility scanner or use the WCAG formula directly.

---

### S02-F005: Font Scaling Test and Fix (200% Scale)

**Feature ID**: S02-F005  
**Review Task**: #23  
**Effort**: ~6h  
**Files**: `HomeScreen.kt`, `NumberPad.kt`, `PuzzleScreen.kt`, `OnboardingScreen.kt`

#### User Story
As a player who uses large text accessibility settings on Android, I want the app UI to adapt so that all text remains readable and no content is clipped or hidden.

#### Acceptance Criteria

**Given** the device font scale is set to 200% in Android accessibility settings,  
**When** the HomeScreen renders,  
**Then** the streak display, puzzle status chips, countdown, difficulty selector, and Play button all remain visible and legible with no overflow or clipping.

**Given** the device font scale is set to 200%,  
**When** the NumberPad renders in 1-row layout,  
**Then** button labels remain readable and buttons remain tappable (touch targets not collapsed).

**Given** the device font scale is set to 200%,  
**When** the PuzzleScreen renders,  
**Then** the timer text and completion banner do not overflow the screen width.

**Given** the device font scale is set to 200%,  
**When** the OnboardingScreen renders with the rules panel (S01-F004),  
**Then** the rules text wraps correctly without overflow.

#### Technical Notes
- The primary risk areas are fixed-height containers. `NumberPad` uses `.height(56.dp)` — this must switch to `wrapContentHeight()` or `heightIn(min = 56.dp)` so large text does not clip.
- `PuzzleStatusChip` in HomeScreen has fixed `padding(vertical = 8.dp, horizontal = 4.dp)` — at 200% scale, a `labelSmall` chip label may still fit, but test with an emulator.
- For timer text in `PuzzleScreen`: currently `style = MaterialTheme.typography.labelLarge` — safe for scaling since `sp` units scale with accessibility. Ensure the `Column` does not have a fixed height that clips the timer.
- Use `LocalDensity` + `fontScale` to programmatically detect scale if conditional layout changes are needed, but prefer flexible layouts (avoid hardcoded heights on text-containing containers).

---

### S02-F006: State Persistence and Auto-Save

**Feature ID**: S02-F006  
**Review Task**: #31  
**Effort**: ~6h  
**Files**: `PuzzleViewModel.kt`, new `PuzzleProgressStore.kt`

#### User Story
As a player who is interrupted mid-puzzle (call, app kill, screen off), I want my progress to be automatically saved so that when I return to the puzzle, my filled cells are exactly as I left them.

#### Acceptance Criteria

**Given** a player has entered values in several cells,  
**When** the system kills the app process (not just background, but full process death),  
**Then** on relaunch and navigation to the same puzzle, all previously entered cell values are restored exactly.

**Given** the puzzle state is persisted,  
**When** a player enters a number in any cell,  
**Then** the state is written to persistent storage (DataStore) within the same coroutine frame (no 5-second delay or debounce — every cell change triggers a save).

**Given** a player completes the puzzle,  
**When** the completion is persisted (existing `CompletionStore.save`),  
**Then** the mid-puzzle progress state is cleared from the progress store (no stale progress after completion).

**Given** a player starts a new day and a fresh puzzle is loaded,  
**When** `loadPuzzle()` is called with a new `date`,  
**Then** any saved progress for the previous date's puzzle is not loaded (each puzzle is keyed by `date + difficulty`).

**Given** the auto-save is working,  
**When** the UI renders,  
**Then** a subtle auto-save indicator (e.g., a `Text("Saved", style = labelSmall)` or a small `Icon` that briefly appears after each save) confirms the save occurred.

#### Technical Notes
- Create `PuzzleProgressStore.kt` with a DataStore-backed store. Key: `"progress_${date.toEpochDay()}_${difficulty.name}"`. Value: serialized `userValues` (store as comma-separated flat array, e.g., `"0,1,0,2,3,0,0,1,0"` for a 3×3 grid).
- In `PuzzleViewModel.enterNumber()` and `clearCell()`, after calling `_uiState.value = newState`, call `persistScope?.launch { progressStore?.save(key, userValues) }` — same pattern as `persistCompletion`.
- In `loadPuzzle()`, attempt to load saved progress for the given `date + difficulty` key. Restore `userValues` from the store if found.
- Add `val autoSaveIndicator: StateFlow<Boolean>` to `PuzzleViewModel` — set to `true` after a save, reset to `false` after 1 second (use a coroutine with `delay(1000)`).

---

## Sprint 3: Retention & Engagement (~42h)

**Objective**: Add the features that drive players back daily — statistics, push notifications, practice mode, and an enhanced celebration animation.

**Sprint Goal**: After S03, players have a reason to open the app even after completing daily puzzles (practice mode), can see their performance history (stats), receive daily reminders (notifications), and feel rewarded at completion (enhanced animation).

**Dependency**: Requires S02 (particularly S02-F006 for the statistics data source).

---

### S03-F001: Statistics Screen

**Feature ID**: S03-F001  
**Review Task**: #9  
**Effort**: ~12h  
**Files**: New `StatsScreen.kt`, new `StatsViewModel.kt`, navigation  
**Depends on**: S02-F006 (puzzle history written to DataStore)

#### User Story
As a regular player, I want to see a statistics screen showing my total puzzles solved, average and best solve times per difficulty, and a calendar heatmap of past completions so that I can track my improvement over time.

#### Acceptance Criteria

**Given** a player navigates to the Stats screen (via a new "Stats" icon on the HomeScreen top bar or bottom navigation),  
**When** the screen loads,  
**Then** the following sections are displayed: (1) Total puzzles solved (sum across all difficulties), (2) Per-difficulty stats table with average time and best time for Beginner, Easy, and Medium, (3) Current streak and longest streak, (4) Earned badges (reusing the `BadgesRow` composable from S01-F002), (5) A monthly calendar heatmap of completion dates.

**Given** a player has solved puzzles across multiple days,  
**When** the calendar heatmap renders,  
**Then** dates with at least one puzzle solved are highlighted in `tertiaryContainer` color; dates with no completions are shown in `surfaceVariant`; today is outlined with a `primary` border.

**Given** a player has solved no puzzles yet,  
**When** the Stats screen loads,  
**Then** all stats show zero values and the calendar is empty — no crash and no placeholder data.

**Given** the Stats screen is displaying data,  
**When** a player pulls down to refresh,  
**Then** the data reloads from the persistence layer and the displayed values update.

**Given** the Stats screen renders at 200% font scale,  
**When** displayed,  
**Then** all text and the calendar heatmap remain legible with no overflow.

#### Technical Notes
- `StatsViewModel` reads from `CompletionStore` (keyed by `"completion_${date.toEpochDay()}_${difficulty.name}"`) and `StreakRepository.streakState`.
- For the heatmap: iterate over the past 365 days (or calendar month view), check for completion keys. Render as a `LazyVerticalGrid` of 7-column rows (one per week), each cell a small colored `Box`.
- Route to `StatsScreen` via a stats icon (`Icons.Filled.BarChart`) in the `HomeScreen` `TopAppBar` actions slot.
- Best time = minimum `elapsedMillis` across all `CompletionState` entries for that difficulty. Average = mean.

---

### S03-F002: Daily Reminder Push Notification

**Feature ID**: S03-F002  
**Review Task**: #11  
**Effort**: ~8h  
**Files**: New `DailyReminderManager.kt`, new `NotificationPreferencesScreen.kt` (or bottom sheet), `AndroidManifest.xml`

#### User Story
As a player who often forgets to check in daily, I want an opt-in push notification at a time I choose so that I am reminded to play my daily SumGrid puzzle.

#### Acceptance Criteria

**Given** a player has not yet configured notifications,  
**When** they complete their first puzzle of the day,  
**Then** a one-time notification opt-in prompt is shown: "Get a daily reminder to play? [Enable] [Not now]".

**Given** a player taps "Enable" on the opt-in prompt,  
**When** Android 13+ POST_NOTIFICATIONS permission is requested,  
**Then** the system permission dialog appears. If granted, the notification is scheduled.

**Given** a player enables daily reminders,  
**When** the scheduled time arrives each day,  
**Then** a notification is posted with title "SumGrid" and body "Your daily puzzle is ready! 🧩", with a deep link that opens the app directly to the HomeScreen.

**Given** a player has enabled daily reminders and has already completed all 3 puzzles today,  
**When** the notification fires,  
**Then** the notification body reads "You're all done for today! Come back tomorrow. 🔥" (checks `CompletionStore` at notification time).

**Given** a player wants to change the notification time or disable reminders,  
**When** they access app settings (via a gear icon on HomeScreen `TopAppBar`),  
**Then** a settings bottom sheet shows a time picker and a toggle to enable/disable reminders.

#### Technical Notes
- Use `WorkManager` with a `PeriodicWorkRequest` (24-hour interval) to schedule the notification. This survives app kills.
- Create a `NotificationChannel` with `IMPORTANCE_DEFAULT` in `Application.onCreate()`.
- For Android 13+ `POST_NOTIFICATIONS`: request with `ActivityResultContracts.RequestPermission()` from the opt-in prompt composable.
- Store notification enabled state and time preference in DataStore (`notifications_enabled`, `notifications_hour`, `notifications_minute`).
- At notification fire time: the `Worker` reads `CompletionStore` to determine whether the all-done message should be used.

---

### S03-F003: Practice Mode with Unlimited Random Puzzles

**Feature ID**: S03-F003  
**Review Task**: #12  
**Effort**: ~16h  
**Files**: New `PracticeScreen.kt`, new `PracticeViewModel.kt`, navigation

#### User Story
As a player who has completed all daily puzzles, I want a Practice mode that generates unlimited random puzzles so that I can keep playing and improving without waiting until the next day.

As a competitive player, I want Practice mode puzzles to be clearly separated from daily puzzles so that my streak and stats are not affected by practice play.

#### Acceptance Criteria

**Given** a player has completed all 3 daily puzzles (all-done state on HomeScreen),  
**When** the all-done state renders,  
**Then** a "Practice" `OutlinedButton` is shown alongside the "Replay" button, providing a path to Practice mode.

**Given** a player taps "Practice" from the HomeScreen (or from a persistent "Practice" menu item),  
**When** the PracticeScreen loads,  
**Then** a randomly generated puzzle of the currently selected difficulty is shown, using the same `GridRenderer` and `NumberPad` components as `PuzzleScreen`.

**Given** a player completes a practice puzzle,  
**When** the completion card appears,  
**Then** it shows solve time and a "New Puzzle" button instead of "Next Puzzle". There is no streak impact and the completion is not written to `CompletionStore`.

**Given** a player taps "New Puzzle" on the practice completion card,  
**When** a new puzzle is requested,  
**Then** a new randomly generated puzzle of the same difficulty is immediately loaded.

**Given** a player taps "Back to Home" from the practice completion card,  
**When** navigation occurs,  
**Then** the player returns to HomeScreen and the all-done state is still shown (daily puzzle status unchanged).

**Given** the practice puzzle is generating,  
**When** the puzzle engine generates a random puzzle,  
**Then** the puzzle is solvable (the engine guarantees a unique solution — same constraint as daily puzzles).

#### Technical Notes
- `PracticeViewModel` generates puzzles using the existing puzzle engine with a random seed (`Random.nextLong()` or `System.currentTimeMillis()`).
- No date, no `CompletionStore` writes, no streak updates — practice completions are ephemeral.
- `PracticeScreen` can reuse `PuzzleScreen`'s layout wholesale; the key difference is the ViewModel (`PracticeViewModel` vs `PuzzleViewModel`) and the completion card "New Puzzle" variant.
- Navigate to Practice from HomeScreen "Practice" button. Add a "Practice" menu item in the HomeScreen `TopAppBar` overflow menu for persistent access (not just all-done state).
- `PracticeViewModel` exposes the same `uiState: StateFlow<PuzzleUiState?>` shape as `PuzzleViewModel` so `GridRenderer` and `NumberPad` can be reused without changes.

---

### S03-F004: Enhanced Celebration Animation

**Feature ID**: S03-F004  
**Review Task**: #14  
**Effort**: ~6h  
**Files**: `CelebrationAnimation.kt`, `PuzzleScreen.kt`  
**Depends on**: S02-F001 (completion card timing)

#### User Story
As a player who just solved a puzzle, I want a visually exciting celebration animation so that completing the puzzle feels like a genuine achievement rather than a quiet transition.

#### Acceptance Criteria

**Given** a player completes a puzzle,  
**When** `isCompleted` transitions to `true`,  
**Then** a confetti particle animation plays over the full grid area for approximately 2 seconds before the completion card appears.

**Given** the confetti animation is playing,  
**When** rendered,  
**Then** colored particles (using `colorScheme.primary`, `secondary`, and `tertiary` palette) fall from the top of the grid area downward, with randomised speed, size (4–12dp), and horizontal drift.

**Given** a player has enabled "Reduce motion" in Android accessibility settings,  
**When** the completion animation triggers,  
**Then** the confetti particle animation is skipped (replaced with a simple color flash on the grid cells). The `LocalConfiguration` `reduceMotion` check from S04-F001 is leveraged here.

**Given** the animation plays,  
**When** it completes (~2 seconds),  
**Then** the completion card (from S02-F001) appears and the confetti stops.

**Given** the confetti animation is implemented,  
**When** it runs on a low-end device (< 3GB RAM),  
**Then** the frame rate does not drop below 30fps (limit particle count to ≤ 60 simultaneous particles for performance).

#### Technical Notes
- Implement a new `ConfettiOverlay` composable in `CelebrationAnimation.kt`. Use `Canvas` with a custom `DrawModifier` or `Animatable<Float>` list for particle positions.
- Each particle: random `startX` across full width, `startY = -20dp`, velocity `(dy = 300..600dp/s, dx = -50..50dp/s)`, lifetime `1500..2500ms`.
- Drive animation with `LaunchedEffect(isComplete)` using `withFrameMillis` loop for smooth physics.
- The existing `rememberCelebrationState` cell-scale animation can continue to run alongside the confetti.
- Extend `rememberCelebrationState` or add a new `rememberConfettiState` — prefer additive approach to avoid breaking existing animation.
- `CompletionCard` delay (S02-F001: 1500ms) is extended to 2000ms to accommodate confetti.

---

## Sprint 4: Polish & Accessibility (~48h)

**Objective**: Respect system accessibility preferences, add error feedback animations, implement pencil/notes mode, handle landscape layout, improve screen reader navigation, and introduce Hard and Expert difficulties.

**Sprint Goal**: After S04, SumGrid passes all accessibility audits, respects device preferences, supports power users with notes mode, and offers two new challenge levels for players who have mastered Medium.

**Dependency**: Requires S03.

---

### S04-F001: Reduce Motion Accessibility Support

**Feature ID**: S04-F001  
**Review Task**: #16  
**Effort**: ~3h  
**Files**: `HomeScreen.kt`, `GridRenderer.kt`, `CelebrationAnimation.kt`

#### User Story
As a player with vestibular sensitivity, I want the app to respect my Android "Remove animations" / "Reduce motion" system setting so that pulsing and continuous animations do not cause discomfort.

#### Acceptance Criteria

**Given** the player has enabled "Remove animations" in Android developer options or "Reduce motion" in accessibility settings,  
**When** the HomeScreen renders,  
**Then** the flame emoji pulse animation (infinite `animateFloat` from alpha 1.0→0.6) does not play. The flame renders at static full opacity.

**Given** reduce motion is enabled,  
**When** a puzzle completes,  
**Then** the confetti animation (S03-F004) and cell-scale animation (`rememberCelebrationState`) are skipped. The grid cells apply a simple background color change to `tertiary` without scale or particle animation.

**Given** reduce motion is enabled,  
**When** the grid selection spring animation (`selectionScale` in `GridRenderer`) plays,  
**Then** the animation is disabled: `selectionScale` stays at `1.0f` (no spring pulse on cell selection).

**Given** reduce motion is disabled (default),  
**When** all animations play,  
**Then** behaviour is identical to pre-S04 (no regression).

#### Technical Notes
- Detect reduce motion via `LocalContext.current.resources.configuration` or `LocalView.current.isAccessibilityEnabled` — on Android, use `Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f` for animation scale.
- Prefer `@Composable fun isReduceMotionEnabled(): Boolean` helper using `LocalContext`.
- In `StreakDisplay`, wrap the `infiniteRepeatable` tween in `if (!reduceMotion)` — use `targetValue = 1f` (static) when reduce motion is on.
- In `rememberCelebrationState`, skip the `animateTo` calls when `reduceMotion` is true.

---

### S04-F002: Error Indicator Shake/Flash Animation

**Feature ID**: S04-F002  
**Review Task**: #17  
**Effort**: ~3h  
**Files**: `GridRenderer.kt`, `PuzzleViewModel.kt`

#### User Story
As a player who makes an error (column or row sum exceeds target), I want the sum indicator to visually react with a shake or flash animation so that I notice the error immediately without staring at the color changes.

#### Acceptance Criteria

**Given** a player enters a number that causes any row or column sum to exceed its target,  
**When** the affected sum indicator transitions from GRAY/GREEN to RED,  
**Then** the sum indicator label (rendered at the right of a row or below a column) plays a brief horizontal shake animation (3–4 oscillations, total duration ~400ms).

**Given** the sum indicator is already RED and a player enters another wrong number,  
**When** the indicator transitions RED → RED (value changes but still over target),  
**Then** the shake animation replays.

**Given** reduce motion is enabled (S04-F001),  
**When** a sum exceeds its target,  
**Then** the indicator flashes between `error` color and `errorContainer` (two color alternations over 200ms) instead of shaking — a less motion-intensive feedback alternative.

**Given** the shake animation fires,  
**When** rendered on a low-end device,  
**Then** the animation completes within its 400ms budget without dropping frames (the Canvas `drawGrid` is already on the render thread).

#### Technical Notes
- `GridRenderer` is a Canvas-based composable. Sum labels are drawn via `drawCenteredText`. To animate them, replace the static Canvas draw of RED indicators with a composable-driven offset.
- Approach: expose `rowErrorTrigger: List<Int>` and `colErrorTrigger: List<Int>` from `PuzzleUiState` — incrementing integers that trigger a `LaunchedEffect(trigger)` in the composable.
- In `PuzzleViewModel.buildState()`, track previous `rowSumIndicators` and increment a counter when a GRAY/GREEN → RED transition occurs. Add `rowShakeTriggers: List<Int>` and `colShakeTriggers: List<Int>` to `PuzzleUiState`.
- In `GridRenderer`, use `animateFloatAsState` driven by an `Offset` state per sum indicator. When `trigger` changes, run a `keyframes` spec: `0dp at 0ms → 8dp at 100ms → -8dp at 200ms → 4dp at 300ms → 0dp at 400ms`.
- Apply the `Offset` by adjusting `labelX` in `drawCenteredText` calls for RED indicators.

---

### S04-F003: Pencil/Notes Mode for Candidate Numbers

**Feature ID**: S04-F003  
**Review Task**: #19  
**Effort**: ~16h  
**Files**: `GridRenderer.kt`, `NumberPad.kt`, `PuzzleViewModel.kt`, `PuzzleUiState`  
**Depends on**: S02-F002 (undo should also undo pencil marks)

#### User Story
As a player solving a Medium (5×5) puzzle, I want a pencil mode where tapping number buttons marks candidate values in cell corners so that I can track possibilities without committing to an answer.

#### Acceptance Criteria

**Given** the pencil mode toggle button is shown on the `NumberPad`,  
**When** a player taps it,  
**Then** the pad enters pencil mode: the pencil icon is highlighted and the number buttons are visually distinct (e.g., `secondaryContainer` color instead of `primaryContainer`).

**Given** pencil mode is active and a cell is selected,  
**When** a player taps a number,  
**Then** that number is added as a candidate mark in the cell corner (small superscript rendering). Tapping the same number again removes the mark (toggle).

**Given** pencil mode is active and a cell has candidate marks,  
**When** a player switches to normal mode and enters a number in that cell,  
**Then** the entered number replaces the display in the main cell body AND all candidate marks for that cell are cleared.

**Given** pencil marks exist in a cell,  
**When** the player taps "Clear" (✕),  
**Then** all candidate marks in the selected cell are cleared (not just one).

**Given** undo is tapped (S02-F002) after adding a pencil mark,  
**When** the undo processes,  
**Then** the last pencil mark change is reverted.

**Given** the grid is rendered with pencil marks,  
**When** displayed at 200% font scale,  
**Then** the superscript candidate digits are still distinguishable from main cell values (size constraint: candidate digits should be ~30% of cell font size, using Canvas `drawText` at smaller font).

#### Technical Notes
- Add `notesValues: Array<Array<Set<Int>>>` to `PuzzleUiState` — one `Set<Int>` per cell for candidate digits.
- Add `pencilModeActive: Boolean` to `PuzzleUiState`.
- In `PuzzleViewModel.enterNumber()`: if `pencilModeActive`, toggle the number in `notesValues[row][col]` instead of setting `userValues[row][col]`. If not active and a cell has notes, clear notes for that cell when a definitive value is set.
- In `GridRenderer.drawGrid()`: after drawing the main cell text, if a cell has notes, draw each candidate digit in the corner quadrants (top-left 2×2 arrangement for up to 4 digits; overflow to 2×3 if needed). Use `fontSize = (cellSize * 0.15f).coerceIn(8f, 12f)`.
- Add `PencilToggleButton` in `NumberPad.kt`, positioned to the left of the number buttons (or as a row above the number row in 2-row mode).

---

### S04-F004: Landscape Layout or Portrait Lock

**Feature ID**: S04-F004  
**Review Task**: #24  
**Effort**: ~6h  
**Files**: `AndroidManifest.xml`, optionally `PuzzleScreen.kt`, `HomeScreen.kt`

#### User Story
As a player rotating my phone to landscape, I want the app to either display a proper landscape layout or gracefully lock to portrait so that the UI does not stretch or become unusable.

#### Acceptance Criteria

**Given** the decision is made to lock to portrait (simplest approach),  
**When** `android:screenOrientation="portrait"` is set in `AndroidManifest.xml` for all `Activity` entries,  
**Then** the app does not rotate when the device is in landscape orientation, and no layout issues occur.

**Given** the decision is made to implement a landscape layout (full approach),  
**When** the device is in landscape orientation,  
**Then** `PuzzleScreen` renders in a side-by-side layout: grid on the left 60% of the screen, number pad on the right 40%, with the timer in the top-right above the pad.

**Given** landscape layout is implemented,  
**When** `WindowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded`,  
**Then** the landscape layout activates. For `Compact` and `Medium` width, portrait layout is used.

**Given** the chosen approach (lock or adaptive) is implemented,  
**When** the app is tested on an 8-inch tablet in landscape,  
**Then** the UI is usable without text overflow or cramped grid cells.

#### Technical Notes
- **Recommended default**: Add `android:screenOrientation="sensorPortrait"` to `<activity>` in `AndroidManifest.xml`. This is a 1-line change, zero risk, resolves all landscape issues for the current release.
- If full landscape support is desired: add `implementation("androidx.compose.material3:material3-window-size-class")`. Use `calculateWindowSizeClass(activity)` and branch the layout.
- For the tablet case: a `Row` containing `GridRenderer(modifier = Modifier.weight(0.6f))` and a `Column(modifier = Modifier.weight(0.4f))` with timer + number pad. This requires extracting the column layout from `PuzzleScreen` into named composables.

---

### S04-F005: Screen Reader Focus Ordering for PuzzleScreen

**Feature ID**: S04-F005  
**Review Task**: #28  
**Effort**: ~4h  
**Files**: `PuzzleScreen.kt`, `GridRenderer.kt`, `NumberPad.kt`

#### User Story
As a blind or low-vision player using TalkBack, I want the focus to move in the order: timer → grid cells (row by row) → number pad buttons so that I can navigate the puzzle systematically without losing orientation.

#### Acceptance Criteria

**Given** TalkBack is active and the player is on the PuzzleScreen,  
**When** the player swipes right with TalkBack linear navigation,  
**Then** focus moves: timer text → row 1 col 1 → row 1 col 2 → ... → row N col N → Undo button → number buttons 1..maxVal → Clear button.

**Given** a puzzle cell is focused by TalkBack,  
**When** the player performs the TalkBack "double-tap to activate" gesture,  
**Then** `vm.selectCell(row, col)` is called (same as a visual tap).

**Given** the puzzle is completed,  
**When** the completion card (S02-F001) appears,  
**Then** TalkBack announces "Puzzle complete. Solved in MM:SS." via `AccessibilityEvent.TYPE_ANNOUNCEMENT`.

**Given** a sum indicator turns RED,  
**When** the error animation fires (S04-F002),  
**Then** TalkBack announces "Row N sum exceeded" via a live region announcement.

#### Technical Notes
- The `GridRenderer` accessibility overlay `Box` nodes already have `contentDescription` set via `cellDescription()`. Ensure `traversalIndex` semantic modifier is applied: timer = 0f, grid cells = row * n + col + 1f, number pad = n*n + 2f onwards.
- Use `Modifier.semantics { isTraversalGroup = true }` on the `Column` containing the timer to ensure it is visited first.
- In `NumberPad.kt`, add `Modifier.semantics { isTraversalGroup = true }` on the root `Row` / `Column`.
- For the completion announcement: use `view.announceForAccessibility("Puzzle complete. Solved in $time")` in a `LaunchedEffect(isCompleted)` in `PuzzleScreen`.
- For the sum error announcement: add a `LaunchedEffect(rowShakeTriggers, colShakeTriggers)` that announces the affected row/col when a trigger increments.

---

### S04-F006: Hard (6×6) and Expert (7×7) Difficulty Levels

**Feature ID**: S04-F006  
**Review Task**: #30  
**Effort**: ~16h  
**Files**: `Difficulty.kt`, puzzle engine files, `NumberPad.kt` (3-row layout), `HomeScreen.kt` (4 difficulty chips), `DifficultySelector.kt`  
**Depends on**: S02-F003 (2-row pad already exists; 3-row pad is an extension)

#### User Story
As a player who has mastered Medium (5×5) puzzles, I want Hard (6×6) and Expert (7×7) difficulty levels so that I have a continued challenge as my skills improve.

#### Acceptance Criteria

**Given** Hard difficulty is added to the `Difficulty` enum,  
**When** the HomeScreen loads,  
**Then** a 4th puzzle status chip "Hard" is shown in the daily puzzle row alongside Beginner, Easy, Medium.

**Given** Expert difficulty is added,  
**When** the HomeScreen loads,  
**Then** a 5th puzzle status chip "Expert" is shown.

**Given** a player selects Hard difficulty and taps Play,  
**When** the puzzle screen loads,  
**Then** a 6×6 grid is rendered with `maxVal = 12` (values 1–12 in a 6×6 grid can sum to target row/column totals).

**Given** a player selects Expert difficulty and taps Play,  
**When** the puzzle screen loads,  
**Then** a 7×7 grid is rendered with `maxVal = 14` (values 1–14).

**Given** Hard or Expert difficulty is active,  
**When** the `NumberPad` renders,  
**Then** a 3-row layout is used: row 1 = 1–5, row 2 = 6–10, row 3 = 11–maxVal + Clear + Undo. Minimum touch target per button: 44dp.

**Given** the puzzle engine generates a Hard or Expert puzzle,  
**When** generation completes,  
**Then** the puzzle has a unique solution (same guarantee as current Beginner/Easy/Medium puzzles).

#### Technical Notes
- In `Difficulty.kt`: add `HARD(gridSize = 6, maxVal = 12)` and `EXPERT(gridSize = 7, maxVal = 14)`. Verify enum field names match existing `maxVal` usage in `NumberPad` and engine.
- The puzzle generation engine takes `gridSize` and `maxVal` as parameters — extending to 6×6 and 7×7 should be additive. Validate uniqueness constraint still holds at larger sizes (may need to increase solver iteration limits).
- `NumberPad` 3-row: extend the `difficulty.maxVal >= 7` check to also add a third row when `difficulty.maxVal >= 11`. Row 3: `(11..difficulty.maxVal).toList() + Undo + Clear`.
- `HomeScreen` puzzle status chips row: currently a `Row` with `weight(1f)` per chip. With 5 chips on 360dp, each chip is 72dp — tight but workable. Consider switching to a `LazyRow` with fixed chip widths (60–68dp) if overflow occurs.
- `DifficultySelector` composable: add Hard and Expert entries.

---

## Cross-Cutting Concerns

### Design Token Compliance
All new UI components must use Material 3 design tokens from `MaterialTheme.colorScheme`. Never hardcode hex values. Specifically:
- CTAs and share button: `colorScheme.tertiary` / `colorScheme.onTertiary`
- Earned badges: `colorScheme.tertiaryContainer` / `colorScheme.onTertiaryContainer`
- All-done state: `colorScheme.tertiaryContainer`
- Error states: `colorScheme.error` / `colorScheme.errorContainer`
- Locked/disabled states: `colorScheme.surfaceVariant` at reduced alpha

### No Backend Changes
All features are client-side only. DataStore is used for local persistence. No remote API calls are introduced in this epic.

### Per-Sprint Shippability
Each sprint produces an independently shippable app update:
- S01: Share button visible, badges shown, onboarding fixed — ready for beta
- S02: Completion card, undo, improved pad, dark mode passes WCAG — ready for store update
- S03: Stats, notifications, practice — ready for feature store update
- S04: Full accessibility pass, new difficulties — ready for v2.0 release

### Existing Tests
Do not break existing tests. The `PuzzleViewModel` unit tests verify `buildState()`, `enterNumber()`, `clearCell()`, `tickTimer()` — any new parameters must have defaults. The `ShareCardGenerator` tests verify share card format — do not modify the generator's output format.

---

## User Story Summary

| Sprint | Feature | Stories |
|--------|---------|---------|
| S01 | Wire share button | 1 |
| S01 | Display earned badges | 2 |
| S01 | Add skip to onboarding | 1 |
| S01 | Add rules explanation | 1 |
| S01 | Add back button | 1 |
| S01 | Make HomeScreen scrollable | 1 |
| S01 | All-done state | 1 |
| S02 | Completion card | 2 |
| S02 | Add undo | 1 |
| S02 | 2-row number pad | 1 |
| S02 | Dark mode contrast | 1 |
| S02 | Font scaling fix | 1 |
| S02 | State persistence | 1 |
| S03 | Statistics screen | 2 |
| S03 | Daily notifications | 2 |
| S03 | Practice mode | 2 |
| S03 | Enhanced celebration | 1 |
| S04 | Reduce motion | 1 |
| S04 | Error shake animation | 1 |
| S04 | Pencil/notes mode | 2 |
| S04 | Landscape/portrait | 1 |
| S04 | Screen reader order | 1 |
| S04 | Hard & Expert levels | 1 |
| **Total** | **20 features** | **28 user stories** |

**Total acceptance criteria**: 87 (including Given/When/Then scenarios across all 20 features)

---

*Specification date: 2026-03-14*  
*Source: docs/sumgrid-ux-review.md — 31 findings mapped to 20 features across 4 sprints*
