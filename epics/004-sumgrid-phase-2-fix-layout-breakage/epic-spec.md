# Epic 004 — SumGrid Phase 2: Fix Layout Breakage

**Status**: Specification  
**Created**: 2026-03-14  
**Device target**: Oppo (narrow Android phone, dark mode primary)  
**Build context**: Post-Phase-1 — Epic 003 shipped share, badges, undo, 2-row numpad, Hard/Expert difficulties  
**Total estimate**: ~48 hours across 3 sprints  

---

## Overview

Epic 003 added significant content to the SumGrid app: 5 difficulty levels (up from 3), streak badges, a 2-row numpad, undo, share, and Hard/Expert grid sizes. The screens were not designed to hold that much — they were built for 3 difficulties, no badges, and a single-row numpad.

Phase 2 exists to fix the layout breakage this caused. The single most critical issue is difficulty card text truncating mid-word ("Begin ner", "Mediu m") on every device every session. A cascade of secondary issues — badge label truncation, invisible grid cells, dead space on the puzzle screen, zero-streak nonsense — follow in priority order.

This epic is structured around three sprints:

- **Sprint 2A — Critical Layout Fixes** (~12h): Stop the bleeding. Fix the things that are visually broken right now.
- **Sprint 2B — Layout and Spacing Polish** (~16h): Eliminate wasted space, make screens comfortable and scrollable, align the timer.
- **Sprint 2C — UX Enhancements** (~20h): Add motivational progression states, animations, and the badge detail experience.

All work targets the existing Kotlin/Jetpack Compose codebase. No new dependencies. Design token compliance required — no hardcoded hex or rgb values.

---

## Architecture Context

### Files Directly Affected

| File | Role | Issues addressed |
|------|------|------------------|
| `ui/screens/HomeScreen.kt` | Home screen layout, BadgeRow, PuzzleStatusChip, StreakDisplay | P0-1, P0-2, P0-3, P0-4, P1-5, P1-6, P2-13, P2-14, P2-15, P2-16 |
| `ui/components/DifficultySelector.kt` | DifficultyCard, DifficultySelector composables | P0-1, P2-14, P2-15 |
| `ui/screens/PuzzleScreen.kt` | Root puzzle layout, timer, grid + numpad arrangement | P1-7, P1-8, P1-11 |
| `ui/components/NumberPad.kt` | NumberButton, UndoButton, ClearButton | P1-9 |
| `ui/components/GridRenderer.kt` | Canvas-drawn grid, cell background colors | P1-10, P1-12 |
| `streak/StreakBadge.kt` | Badge enum (displayName, icon, requiredDays) | P0-2, P2-16 |

### Key Implementation Facts From Code Review

- `HomeScreen.kt` already has `verticalScroll(rememberScrollState())` — scrollability is wired but content may still overflow on short devices without testing.
- `DifficultySelector` renders all `Difficulty.entries` with `Modifier.weight(1f)` — 5 cards each get 1/5 of width, causing truncation. The fix is `LazyRow` with fixed-width cards or abbreviated labels.
- `BadgeRow` shows all `StreakBadge.entries` in a `Row` with `Arrangement.SpaceEvenly` plus a `displayName` text label — labels truncate at narrow widths.
- `BadgeItem` already applies `alpha = if (earned) 1f else 0.38f` — visual differentiation for earned vs. unearned badges is partially implemented at the composable level, but emoji rendering at 0.38 alpha may not appear grayscale on all devices.
- `PuzzleScreen.kt` has `Spacer(modifier = Modifier.weight(1f))` between the grid Box and the completion banner — this is the dead space mechanism causing the 30% gap.
- Timer lives as a standalone `Text` composable below the TopAppBar. Moving it to the TopAppBar trailing slot requires adding a `actions` lambda to the existing `TopAppBar` call.
- `NumberPad.kt`: `UndoButton` uses `OutlinedButton` with `onSurface` content color — visually identical outline style to `ClearButton`. No visual separator between digit buttons and action buttons.
- `GridRenderer.kt`: `userCellBg = Color(0xFFFFFBFF)` in `GridColors.defaults()` — this is a near-white value that will be invisible on dark backgrounds. The `gridColorsFromTheme()` function (not shown fully) likely overrides this, but the empty cell tint issue persists in dark mode.
- `PuzzleStatusChip` uses `difficulty.name.lowercase().replaceFirstChar { uppercaseChar() }` as label — produces full words "Beginner", "Easy", "Medium", "Hard", "Expert" that compress poorly at `weight(1f)`.

---

## Requirements

### Sprint 2A — Critical Layout Fixes

---

#### REQ-S2A-01: Fix difficulty card text truncation

**Source**: P0-1  
**Severity**: Critical  
**Effort**: ~4h

The `DifficultySelector` currently renders 5 `DifficultyCard` composables in a `Row` with `Modifier.weight(1f)` each. At 5 items, each card receives approximately 20% of screen width — insufficient to display multi-word labels like "Beginner" or "Medium" without line-breaking mid-character.

**Required change**: Replace the fixed `Row` layout in `DifficultySelector.kt` with a `LazyRow` (or `horizontalScroll` + `Row`) where each card has a fixed width of 80–88dp. Display approximately 3.5 cards by default to hint that scrolling is available. Cards remain tappable; the selected card is highlighted with a primary border. Scroll position does not need to persist across recompositions.

**Acceptance criteria**:

- **Given** the app is on the Home screen with 5 difficulty levels,  
  **When** the difficulty selector is rendered on any phone width >= 320dp,  
  **Then** all card labels ("Beginner", "Easy", "Medium", "Hard", "Expert") are displayed without word-breaking or clipping.

- **Given** only 3.5 cards are visible,  
  **When** the user swipes left on the difficulty row,  
  **Then** the remaining cards scroll into view smoothly.

- **Given** a difficulty card is selected,  
  **When** the row is in any scroll position,  
  **Then** the selected card retains its primary border and container color regardless of position.

- **Given** no design token violation test,  
  **When** the new card layout is rendered,  
  **Then** no hardcoded hex, rgb, or arbitrary pixel values appear in the changed composables.

**Files affected**: `ui/components/DifficultySelector.kt`

---

#### REQ-S2A-02: Fix badge row label truncation — emoji-only display

**Source**: P0-2  
**Severity**: High  
**Effort**: ~3h

`BadgeRow` renders a `Row` with 4 `BadgeItem` columns, each containing an emoji and a `displayName` text label (e.g., "Weekly Warrior"). At narrow screen widths the labels overflow or wrap, making badges illegible.

**Required change**: Remove the `displayName` `Text` from `BadgeItem`. Render only the emoji at `headlineSmall` typography size. Make each badge tappable. On tap, show the badge's name and required-days description in a `ModalBottomSheet` or `AlertDialog`. The tap-to-reveal interaction satisfies both readability (no label clutter) and discoverability (labels still accessible).

**Acceptance criteria**:

- **Given** the Home screen badge row,  
  **When** it renders on any screen width >= 320dp,  
  **Then** no badge label text is visible in the default row — only 4 large emoji icons are shown.

- **Given** an earned badge emoji is tapped,  
  **When** the bottom sheet or dialog opens,  
  **Then** the badge's `displayName` and earned status ("Earned") are shown.

- **Given** an unearned badge emoji is tapped,  
  **When** the bottom sheet or dialog opens,  
  **Then** the badge's `displayName`, required days (e.g., "Reach a 7-day streak"), and locked status are shown.

- **Given** the bottom sheet is open,  
  **When** the user swipes it down or taps outside,  
  **Then** the sheet dismisses and returns to the Home screen.

**Files affected**: `ui/screens/HomeScreen.kt`, `streak/StreakBadge.kt` (read `requiredDays` for description text)

---

#### REQ-S2A-03: Gray out unearned badges visually

**Source**: P2-16  
**Severity**: High  
**Effort**: ~2h

All 4 badge emoji appear in full color regardless of earned state. The existing `BadgeItem` composable applies `alpha = if (earned) 1f else 0.38f` but emoji rendering at 0.38 alpha may still appear colored on AMOLED/dark backgrounds and does not communicate "locked" clearly enough.

**Required change**: For unearned badges, apply a `ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })` on the emoji container, or wrap the emoji in a `Box` with a `Modifier.graphicsLayer { alpha = 0.38f; colorFilter = ... }` that desaturates to grayscale. Additionally, render a small "🔒" lock overlay or change the emoji opacity to clearly communicate locked state. Earned badges remain full color at 1.0 alpha.

**Acceptance criteria**:

- **Given** a user with 0 days streak (no badges earned),  
  **When** the Home screen renders,  
  **Then** all 4 badge icons appear visually muted — dimmed and/or grayscale — not in full vibrant color.

- **Given** a user who has earned the Weekly Warrior badge (7+ day streak),  
  **When** the Home screen renders,  
  **Then** the 🔥 badge icon is full color at full opacity, and the remaining 3 unearned badges appear dimmed.

- **Given** the transition from unearned to earned (streak crosses threshold),  
  **When** the Home screen recomposes,  
  **Then** the newly earned badge animates from dimmed to full color (or recomposes to full color without animation if animation adds significant complexity).

**Files affected**: `ui/screens/HomeScreen.kt` (BadgeItem composable)

---

#### REQ-S2A-04: Add empty cell background tint in dark mode

**Source**: P1-10  
**Severity**: High  
**Effort**: ~1h

In dark mode, user-fillable empty cells in the grid are nearly indistinguishable from the dark background. `GridColors.defaults()` sets `userCellBg = Color(0xFFFFFBFF)` (near-white) — this may be used in light mode but the dark mode path via `gridColorsFromTheme()` needs to apply a visible tint.

**Required change**: In the dark mode branch of `gridColorsFromTheme()`, set `userCellBg` to `MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)` or the equivalent theme token that provides a subtle but visible cell background in dark mode. The tint must be visible enough to indicate cell boundaries without relying solely on grid lines, but not so strong that it distracts from filled-in numbers.

**Acceptance criteria**:

- **Given** a puzzle is loaded in dark mode,  
  **When** the grid renders with empty user-fillable cells,  
  **Then** each empty cell has a faint but visible background tint that distinguishes it from the surrounding dark background.

- **Given** the grid renders,  
  **When** a cell contains a given (pre-filled) number,  
  **Then** the given cell background remains using the `givenCellBg` color (indigo tint) and is visually distinct from empty editable cells.

- **Given** light mode is active,  
  **When** the grid renders,  
  **Then** the existing light-mode cell backgrounds are unchanged (no regression).

- **Given** the fix is applied,  
  **When** `design-lint.js` is run,  
  **Then** no hardcoded hex values appear in the changed `gridColorsFromTheme()` function.

**Files affected**: `ui/components/GridRenderer.kt`

---

#### REQ-S2A-05: Add "tap a cell to begin" guidance hint

**Source**: P1-8  
**Severity**: High  
**Effort**: ~2h

On puzzle load, the number pad buttons are all disabled (gray) because no cell is selected. New users see a grid and inactive buttons with no instructions. There is no affordance indicating that they must tap a cell first.

**Required change**: Show a centered text hint "Tap an empty cell to start" between the grid and the number pad. The hint should be visible when `selectedCell == null` (no cell selected) and should disappear the moment any cell is tapped. The hint text should use `MaterialTheme.typography.bodyMedium` with `onSurfaceVariant` color. Optionally, the first empty cell may pulse with a subtle alpha animation to draw attention, but the text hint alone satisfies the acceptance criteria.

**Acceptance criteria**:

- **Given** a puzzle has just loaded and no cell is selected,  
  **When** the puzzle screen is visible,  
  **Then** a "Tap an empty cell to start" text hint is displayed between the grid and the number pad.

- **Given** the hint is visible,  
  **When** the user taps any empty cell,  
  **Then** the hint immediately disappears and the number pad becomes active.

- **Given** a cell is already selected (user is mid-puzzle),  
  **When** the puzzle screen recomposes,  
  **Then** the hint is not shown.

- **Given** a puzzle is complete,  
  **When** the completion state is visible,  
  **Then** the hint is not shown (number pad is hidden on completion anyway).

**Files affected**: `ui/screens/PuzzleScreen.kt`

---

### Sprint 2B — Layout and Spacing Polish

---

#### REQ-S2B-01: Eliminate dead space between grid and number pad

**Source**: P1-7  
**Severity**: High  
**Effort**: ~4h

`PuzzleScreen.kt` uses `Spacer(modifier = Modifier.weight(1f))` between the grid `Box` and the completion banner, which causes the numpad to be anchored to the very bottom while the grid sits at the top. On a 5x5 puzzle this leaves ~30% of the screen as empty black space.

**Required change**: Remove the `weight(1f)` spacer. Restructure the puzzle screen `Column` layout so the grid and number pad are vertically grouped. Use `verticalArrangement = Arrangement.Center` on a wrapping `Column`, or place the grid and numpad in a `Column` with a fixed 16dp gap between them, centered vertically within the available space using `Box(contentAlignment = Alignment.Center)`. The numpad must remain at the bottom of the grid grouping, not at the bottom of the screen.

**Acceptance criteria**:

- **Given** the puzzle screen is open with a 5x5 (Medium) grid,  
  **When** the screen renders on a standard phone height (>=640dp),  
  **Then** the gap between the bottom of the grid and the top of the number pad is no more than 24dp.

- **Given** the puzzle screen is open with a 7x7 (Expert) grid,  
  **When** the screen renders,  
  **Then** the grid and number pad are both visible without scrolling, with no excessive blank space.

- **Given** the screen is rotated to landscape (optional, non-blocking),  
  **When** the puzzle screen renders,  
  **Then** the layout does not overflow or clip critical elements.

- **Given** the puzzle is complete,  
  **When** the completion banner and share button are visible,  
  **Then** they appear in the space previously occupied by the numpad, without causing layout overflow.

**Files affected**: `ui/screens/PuzzleScreen.kt`

---

#### REQ-S2B-02: Verify Home screen scrollability on short devices

**Source**: P0-4  
**Severity**: High  
**Effort**: ~1h

`HomeScreen.kt` already applies `.verticalScroll(rememberScrollState())` to the main `Column`. However, with 5 puzzle status chips, badge row, countdown, 5 difficulty cards, and the Play button, the content may still be too tall on devices with <= 600dp usable height (e.g., Oppo with notch).

**Required change**: Verify in code that the `verticalScroll` modifier is applied before `padding(horizontal = 20.dp)` in the modifier chain (currently appears correct). Add an integration test or manual verification checkpoint confirming all content scrolls to view on a 600dp-height screen. No layout restructuring needed if scroll is confirmed working — this is a verification and test task. If content overflow is discovered, adjust `Spacer` heights between sections to reduce minimum height (e.g., reduce 28dp spacers to 16dp).

**Acceptance criteria**:

- **Given** a device or emulator with 600dp usable screen height,  
  **When** the Home screen loads with all 5 difficulties and badges,  
  **Then** scrolling down reveals the Play button and footer without content being permanently clipped.

- **Given** the user has scrolled to the bottom of the Home screen,  
  **When** they scroll back up,  
  **Then** the "SumGrid" title and streak display return to view.

- **Given** the scroll state,  
  **When** the Home screen recomposes due to streak or completion state changes,  
  **Then** the scroll position is preserved (standard `rememberScrollState()` behavior).

**Files affected**: `ui/screens/HomeScreen.kt`

---

#### REQ-S2B-03: Condense Today's Puzzles row — abbreviations or progress bar

**Source**: P0-3  
**Severity**: Medium  
**Effort**: ~2h

`PuzzleStatusChip` displays the full difficulty name ("Beginner", "Easy", etc.) with `weight(1f)`. At 5 items, labels compress and may overflow on narrow screens.

**Required change**: Change `PuzzleStatusChip` to display abbreviated labels: "BEG", "EASY", "MED", "HARD", "EXP" (or single initials "B", "E", "M", "H", "X"). Alternatively, replace the 5-chip row with a single horizontal progress bar showing "1/5 solved" with colored fill segments for each completed difficulty. The abbreviated chip approach is lower risk. Keep the check/circle icon as-is.

**Acceptance criteria**:

- **Given** the Home screen renders with all 5 difficulties,  
  **When** the Today's Puzzles row is displayed on a 320dp-wide screen,  
  **Then** all 5 status chips are visible without label overflow or clipping.

- **Given** a difficulty is completed,  
  **When** the chip for that difficulty renders,  
  **Then** the ✓ icon and completed background color remain visible and correct.

- **Given** no difficulty is completed,  
  **When** the row renders,  
  **Then** all 5 chips show the ○ (circle) icon with the uncompleted surface color.

**Files affected**: `ui/screens/HomeScreen.kt` (PuzzleStatusChip composable)

---

#### REQ-S2B-04: Move timer into TopAppBar trailing slot

**Source**: P1-11  
**Severity**: Low  
**Effort**: ~2h

The puzzle timer lives as a standalone `Text` composable between the `TopAppBar` and the grid, consuming ~28dp of vertical space. Moving it into the TopAppBar's `actions` slot consolidates the top bar and frees vertical space for the grid.

**Required change**: Remove the standalone timer `Text` composable and the following `Spacer(8.dp)` from `PuzzleScreen.kt`. Add an `actions` lambda to the existing `TopAppBar` call that renders the elapsed timer as a `Text` at `labelLarge` typography in the trailing position. The timer text must still update every second via the existing `LaunchedEffect` tick. If the puzzle has a Share icon in the TopAppBar actions in a future sprint, the timer and share icon can coexist in the actions row.

**Acceptance criteria**:

- **Given** a puzzle is in progress,  
  **When** the puzzle screen renders,  
  **Then** the elapsed timer (MM:SS format) appears in the top-right corner of the TopAppBar, not as a standalone element below it.

- **Given** the timer is in the TopAppBar,  
  **When** 1 second elapses,  
  **Then** the timer text updates in the TopAppBar (same LaunchedEffect behavior as before).

- **Given** the puzzle is complete,  
  **When** the completion state is shown,  
  **Then** the timer stops updating (existing behavior) and the final elapsed time remains visible in the TopAppBar.

- **Given** the vertical layout change,  
  **When** the grid renders,  
  **Then** the grid has at least 8dp more vertical space available compared to before (timer row removed).

**Files affected**: `ui/screens/PuzzleScreen.kt`

---

#### REQ-S2B-05: Increase grid width — reduce horizontal padding

**Source**: P1-12  
**Severity**: Medium  
**Effort**: ~1h

The `GridRenderer` is rendered with `Modifier.fillMaxWidth()` in a `Column` that has no explicit horizontal padding. However, `PuzzleScreen.kt` itself does not add horizontal padding to the grid box. The sum indicator labels on the right side of the grid may require some space. Visual observation on Oppo screenshots shows visible horizontal margins.

**Required change**: Confirm the root cause of the horizontal margin. If the margin comes from `Scaffold` inner padding, reduce it by wrapping the grid in a `Box` with `padding(horizontal = 4.dp)` instead of any default. If the grid's `BoxWithConstraints` inside `GridRenderer` is applying intrinsic padding, adjust the `padding` modifier on the grid call site in `PuzzleScreen.kt`. Target: sum indicators remain readable; left edge of grid is within 4–8dp of the screen edge.

**Acceptance criteria**:

- **Given** the puzzle screen renders on a 360dp-wide device,  
  **When** the grid is displayed,  
  **Then** the total horizontal margin on each side of the grid is no more than 8dp.

- **Given** the grid cells are wider,  
  **When** a user taps a cell near the left edge,  
  **Then** the tap registers correctly (no hit-target reduction).

- **Given** the sum indicator labels appear on the right of the grid,  
  **When** the grid renders at reduced horizontal padding,  
  **Then** the sum numbers are not clipped.

**Files affected**: `ui/screens/PuzzleScreen.kt`, `ui/components/GridRenderer.kt`

---

#### REQ-S2B-06: Visually distinguish undo and clear from digit buttons

**Source**: P1-9  
**Severity**: Medium  
**Effort**: ~2h

`UndoButton` and `ClearButton` in `NumberPad.kt` use `OutlinedButton` with the same shape and sizing as digit buttons. They blend into the numpad row visually — users must read the symbols to identify the action buttons.

**Required change**: Add a `Spacer(width = 8.dp)` or `Divider(vertical)` between the last digit button and the undo button in the second row of the 2-row layout. Give `UndoButton` a `secondaryContainer` background color (instead of transparent outlined) to distinguish it as a secondary action. Keep `ClearButton` as `OutlinedButton` with `error` color — its red outline already differentiates it — but make the border slightly thicker (`borderStroke = 1.5.dp`) for emphasis. Update the single-row layout similarly.

**Acceptance criteria**:

- **Given** the 2-row number pad is displayed (Medium/Hard/Expert difficulty),  
  **When** the second row renders,  
  **Then** there is a visible visual break (extra spacing or divider) between the last digit button and the undo button.

- **Given** the undo button renders,  
  **When** a user glances at the number pad,  
  **Then** the undo button (↶) is distinguishable from digit buttons by color or background treatment.

- **Given** the clear button renders,  
  **When** a user glances at the number pad,  
  **Then** the clear button (✕) has a visibly bolder or more prominent red outline compared to digit buttons.

- **Given** no regression test,  
  **When** the undo button is tapped,  
  **Then** the undo action fires correctly (`vm.undo()` called).

**Files affected**: `ui/components/NumberPad.kt`

---

#### REQ-S2B-07: Fix zero-streak display — show motivational text

**Source**: P1-5  
**Severity**: Medium  
**Effort**: ~1h

`StreakDisplay` always shows "🔥 0 days streak" when streak is 0, with a pulsing flame animation. A pulsing flame next to "0" is confusing and slightly misleading.

**Required change**: In `StreakDisplay`, add a conditional: when `streak == 0`, show "Start your streak!" (no flame pulse) using the same `secondaryContainer` surface. When `streak > 0`, show the existing animated flame + count display. This change is purely in `HomeScreen.kt`'s `StreakDisplay` composable.

**Acceptance criteria**:

- **Given** a user with a 0-day streak,  
  **When** the Home screen renders,  
  **Then** the streak widget shows "Start your streak!" without a pulsing flame emoji.

- **Given** a user with a 1-day streak,  
  **When** the Home screen renders,  
  **Then** the streak widget shows "🔥 1 day streak" with the pulsing flame animation.

- **Given** a user with a 7-day streak,  
  **When** the Home screen renders,  
  **Then** the streak widget shows "🔥 7 days streak" with the pulsing flame animation.

**Files affected**: `ui/screens/HomeScreen.kt` (StreakDisplay composable)

---

#### REQ-S2B-08: Switch Play button to amber/tertiary CTA color

**Source**: P2-13  
**Severity**: Medium  
**Effort**: ~1h

The Play button uses the default `Button` composable which applies `primaryContainer` (indigo/lavender). On the dark-themed Home screen, this blends with other primary-colored elements and does not stand out as the primary CTA.

**Required change**: Override the `ButtonDefaults.buttonColors` on the Play `Button` in `HomeScreen.kt` to use `containerColor = MaterialTheme.colorScheme.tertiaryContainer` and `contentColor = MaterialTheme.colorScheme.onTertiaryContainer`. This is the amber/warm color already used for the streak bar, which visually signals "take action".

**Acceptance criteria**:

- **Given** the Home screen renders in dark mode,  
  **When** the Play button is visible,  
  **Then** the Play button uses the tertiary/amber container color, visually distinguishing it from surrounding indigo elements.

- **Given** the Play button is disabled (no difficulty selected),  
  **When** it renders,  
  **Then** the disabled state uses the standard `disabledContainerColor` from `ButtonDefaults` (not the amber color, so it correctly indicates inactivity).

- **Given** light mode is active,  
  **When** the Play button renders,  
  **Then** the tertiary container color is used from the light mode theme (no hardcoded color regression).

**Files affected**: `ui/screens/HomeScreen.kt`

---

#### REQ-S2B-09: Improve unselected difficulty card visibility

**Source**: P2-14  
**Severity**: Medium  
**Effort**: ~1h

After fixing the scrollable row layout (REQ-S2A-01), the unselected difficulty cards still use `MaterialTheme.colorScheme.surface` as their background — which may be near-transparent or very dark in dark mode, making the cards barely visible.

**Required change**: Change the unselected `DifficultyCard` container color from `surface` to `MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)` or `surfaceContainerHigh`. The border for unselected cards should use `outlineVariant` at full opacity (currently correct) but with border width increased from 1dp to 1.5dp. This makes unselected cards clearly legible as distinct tap targets.

**Acceptance criteria**:

- **Given** the difficulty selector renders in dark mode,  
  **When** an unselected card is displayed,  
  **Then** the card has a faintly visible background that distinguishes it from the screen background.

- **Given** the selected card is highlighted,  
  **When** adjacent unselected cards render,  
  **Then** the contrast between selected (bright primary border) and unselected (faint outline) cards is clear at a glance.

- **Given** no design token violation,  
  **When** the change is applied,  
  **Then** no hardcoded colors are introduced in `DifficultySelector.kt`.

**Files affected**: `ui/components/DifficultySelector.kt`

---

### Sprint 2C — UX Enhancements

---

#### REQ-S2C-01: Mark completed puzzles in difficulty selector

**Source**: P2-15  
**Severity**: Medium  
**Effort**: ~2h

The difficulty selector cards are currently stateless — they don't reflect whether the user has already completed that difficulty today. The `HomeViewModel` exposes `puzzleStatuses` (the same data used by `PuzzleStatusChip`) that contains completion state per difficulty.

**Required change**: Pass the `puzzleStatuses` list (or a `Set<Difficulty>` of completed difficulties) into `DifficultySelector` and then into each `DifficultyCard`. When `isCompleted == true` for a card, render a small ✓ checkmark badge in the top-right corner of the card using a `Box` overlay. The card's main appearance (colors, label) should remain the same — only the completion indicator is added. If all difficulties for a day are complete and `allComplete == true`, the Play button already shows `AllDoneSection`, so no special button text change is needed.

**Acceptance criteria**:

- **Given** the user has completed Beginner and Easy today,  
  **When** the difficulty selector renders,  
  **Then** the Beginner and Easy cards each show a small ✓ indicator in their top-right corner.

- **Given** no puzzles are completed,  
  **When** the difficulty selector renders,  
  **Then** no ✓ indicators appear on any card.

- **Given** a card has the ✓ indicator,  
  **When** the user taps it,  
  **Then** it is still selectable (they may wish to replay or it may redirect to replay mode).

- **Given** `DifficultySelector` receives completion data,  
  **When** the `puzzleStatuses` state updates (e.g., user completes a puzzle and returns to Home),  
  **Then** the completion indicators update without requiring app restart.

**Files affected**: `ui/components/DifficultySelector.kt`, `ui/screens/HomeScreen.kt`

---

#### REQ-S2C-02: Build badge detail bottom sheet

**Source**: P0-2 (follow-up), P0-4  
**Severity**: Medium  
**Effort**: ~4h

This requirement builds on REQ-S2A-02 (emoji-only badge row). Having removed the text labels, the tap interaction must reveal badge details in a `ModalBottomSheet`. This is the primary means by which users discover badge names and progress.

**Required change**: Implement a `BadgeDetailBottomSheet` composable that accepts a `StreakBadge` and an earned state. It displays:
- The badge emoji at large size (48sp)
- The `displayName` ("Weekly Warrior", etc.)
- A description: "Earned for maintaining a {requiredDays}-day streak"
- Current status: "Earned ✓" (in `tertiary` color) or "Locked — {N} more days to go" based on current streak
- A dismiss button or rely on `ModalBottomSheet` swipe-to-dismiss

Wire the `BadgeItem` `clickable` modifier (added in REQ-S2A-02) to open this sheet. Manage which badge's sheet is open via a `selectedBadge: StreakBadge?` state variable in the Home screen or in a dedicated `BadgeRowViewModel`.

**Acceptance criteria**:

- **Given** the badge row is visible,  
  **When** the user taps any badge emoji,  
  **Then** a bottom sheet slides up from the bottom of the screen.

- **Given** the bottom sheet is open for an earned badge,  
  **When** the sheet content renders,  
  **Then** the badge emoji, name, and "Earned ✓" indicator are visible.

- **Given** the bottom sheet is open for an unearned badge (e.g., Century Solver, 100 days),  
  **When** the current streak is 5 days,  
  **Then** the sheet shows "95 more days to go" (or equivalent phrasing based on streak data).

- **Given** the bottom sheet is open,  
  **When** the user swipes it down,  
  **Then** the sheet dismisses and `selectedBadge` resets to null.

- **Given** the sheet is open,  
  **When** the system back button is pressed,  
  **Then** the sheet dismisses (standard `ModalBottomSheet` back-handler behavior).

**Files affected**: `ui/screens/HomeScreen.kt`, new `ui/components/BadgeDetailBottomSheet.kt`

---

#### REQ-S2C-03: Collapse badge row on short screens

**Source**: P0-4  
**Severity**: Medium  
**Effort**: ~3h

On very short devices (usable height <= 560dp), displaying the full badge emoji row plus all other Home screen content may cause the Play button to be scrolled far out of view. An option to collapse the badge section into a single "🏆 Badges" chip reduces vertical footprint while keeping badges accessible.

**Required change**: Detect available screen height using `LocalConfiguration.current.screenHeightDp`. When height is <= 560dp, replace the `BadgeRow` with a single `AssistChip` or `FilterChip` labeled "🏆 Badges" that is tappable. Tapping the chip expands to show the full badge row inline, or opens the same `BadgeDetailBottomSheet` showing all badges in a list. When height > 560dp, always show the full `BadgeRow` as in REQ-S2A-02.

**Acceptance criteria**:

- **Given** a device with screen height <= 560dp,  
  **When** the Home screen renders,  
  **Then** the badge row is replaced by a single "🏆 Badges" chip.

- **Given** the "🏆 Badges" chip is tapped,  
  **When** the interaction fires,  
  **Then** the badge detail view (all 4 badges) is displayed — either via inline expansion or bottom sheet.

- **Given** a device with screen height > 560dp,  
  **When** the Home screen renders,  
  **Then** the full badge emoji row is shown directly (no collapse chip).

- **Given** the screen height threshold,  
  **When** an emulator or device is used at exactly 560dp height,  
  **Then** the collapse chip is shown (boundary is inclusive).

**Files affected**: `ui/screens/HomeScreen.kt`

---

#### REQ-S2C-04: Add numpad enable/disable transition animation

**Source**: Sprint 2C task 18  
**Severity**: Low  
**Effort**: ~1h

When a cell is selected and the numpad becomes active, the transition from gray/disabled to colored/enabled buttons is abrupt. A subtle animation improves the perception of responsiveness.

**Required change**: Wrap each `NumberButton`'s `containerColor` and `contentColor` in `animateColorAsState` with a short `tween(durationMillis = 150)`. This animates the color transition when `enabled` changes from false to true (cell selected) and true to false (cell deselected or puzzle complete). No structural changes to `NumberPad.kt` — only color animation wrapping.

**Acceptance criteria**:

- **Given** no cell is selected (numpad disabled),  
  **When** the user taps a cell,  
  **Then** the number buttons animate from `surfaceVariant` (disabled color) to `primaryContainer` (enabled color) over ~150ms.

- **Given** a cell is selected,  
  **When** the user taps another cell or taps the same cell to deselect,  
  **Then** the transition animates smoothly (not an instant jump).

- **Given** the animation is running,  
  **When** the user taps a number button mid-animation,  
  **Then** the tap registers correctly (animation does not block input).

**Files affected**: `ui/components/NumberPad.kt`

---

#### REQ-S2C-05: Remove or wire "More by dgeek" footer

**Source**: P1-6  
**Severity**: Low  
**Effort**: ~30min

The "More by dgeek" `Text` at the bottom of `HomeScreen.kt` is not tappable and has no associated URL. It appears as dead text.

**Required change**: Either remove the `Text` composable entirely, or replace it with a `TextButton` composable that opens the developer's Play Store page (or a placeholder URL) via `Intent(Intent.ACTION_VIEW, Uri.parse(url))` in `LocalContext.current`. If no URL is available at implementation time, remove the footer. Do not leave dead text in the shipped build.

**Acceptance criteria**:

- **Given** the Home screen footer area,  
  **When** the user scrolls to the bottom,  
  **Then** either no footer text is visible, or a tappable "More by dgeek" link is shown.

- **Given** a tappable "More by dgeek" link is implemented,  
  **When** the user taps it,  
  **Then** the device's browser or Play Store opens to the target URL.

- **Given** the footer is removed,  
  **When** the layout renders,  
  **Then** the bottom padding (16dp) is preserved so content does not crowd the navigation bar.

**Files affected**: `ui/screens/HomeScreen.kt`

---

#### REQ-S2C-06: Add first-empty-cell pulse animation for onboarding

**Source**: Sprint 2C task 20  
**Severity**: Medium  
**Effort**: ~4h

This requirement extends REQ-S2A-05 (the text hint). The text hint alone tells users what to do; the pulse animation shows *where* to tap by drawing attention to the first empty editable cell in the grid.

**Required change**: When `selectedCell == null` on puzzle load, identify the first empty editable cell (row 0 or first row with an empty user-fillable cell, column 0 or first available). Apply an `infiniteRepeatable` `animateFloat` to pulse the alpha of a highlight border around that specific cell between 0.3 and 1.0 with a 1000ms tween. The animation stops when `selectedCell != null` (cell is tapped). Implementation requires `GridRenderer.kt` to accept a `pulsingCell: Pair<Int, Int>?` parameter that drives the pulsing border draw call on the Canvas.

**Acceptance criteria**:

- **Given** a puzzle loads with no cell selected,  
  **When** the grid renders,  
  **Then** the first empty editable cell has a pulsing highlight border (alpha cycling between ~0.3 and 1.0).

- **Given** the pulsing cell is visible,  
  **When** the user taps that cell or any other cell,  
  **Then** the pulse animation stops immediately and the selected cell highlight takes over.

- **Given** the user is mid-puzzle (selectedCell is not null from a previous tap),  
  **When** the user deselects a cell (taps selected cell again),  
  **Then** the pulse does NOT restart — it only plays on initial puzzle load, not on every deselection.

- **Given** a puzzle where row 0 is entirely composed of given (pre-filled) cells,  
  **When** the pulse animation searches for the first empty editable cell,  
  **Then** it correctly skips given cells and pulses the first user-fillable empty cell.

**Files affected**: `ui/components/GridRenderer.kt`, `ui/screens/PuzzleScreen.kt`

---

## Sprint Breakdown Summary

### Sprint 2A — Critical Layout Fixes (~12h)

| Req ID | Title | Effort | Severity |
|--------|-------|--------|----------|
| REQ-S2A-01 | Fix difficulty card text truncation | ~4h | Critical |
| REQ-S2A-02 | Fix badge row — emoji-only display | ~3h | High |
| REQ-S2A-03 | Gray out unearned badges | ~2h | High |
| REQ-S2A-04 | Add empty cell background tint | ~1h | High |
| REQ-S2A-05 | Add "tap a cell to begin" hint | ~2h | High |

**Sprint 2A gates**: All 5 requirements must pass before Sprint 2B begins. Sprint 2A is the minimum shippable slice — if only 2A ships, the app is no longer visually broken.

### Sprint 2B — Layout and Spacing Polish (~16h)

| Req ID | Title | Effort | Severity |
|--------|-------|--------|----------|
| REQ-S2B-01 | Eliminate dead space on puzzle screen | ~4h | High |
| REQ-S2B-02 | Verify Home screen scrollability | ~1h | High |
| REQ-S2B-03 | Condense Today's Puzzles row | ~2h | Medium |
| REQ-S2B-04 | Move timer into TopAppBar | ~2h | Low |
| REQ-S2B-05 | Increase grid width | ~1h | Medium |
| REQ-S2B-06 | Distinguish undo/clear buttons | ~2h | Medium |
| REQ-S2B-07 | Fix zero-streak display | ~1h | Medium |
| REQ-S2B-08 | Switch Play button to amber | ~1h | Medium |
| REQ-S2B-09 | Improve unselected card visibility | ~1h | Medium |

**Sprint 2B dependency**: Requires Sprint 2A complete (especially REQ-S2A-01 for card visibility work).

### Sprint 2C — UX Enhancements (~20h)

| Req ID | Title | Effort | Severity |
|--------|-------|--------|----------|
| REQ-S2C-01 | Mark completed puzzles in selector | ~2h | Medium |
| REQ-S2C-02 | Build badge detail bottom sheet | ~4h | Medium |
| REQ-S2C-03 | Collapse badge row on short screens | ~3h | Medium |
| REQ-S2C-04 | Numpad enable/disable animation | ~1h | Low |
| REQ-S2C-05 | Remove/wire dgeek footer | ~0.5h | Low |
| REQ-S2C-06 | First-cell pulse animation | ~4h | Medium |

**Sprint 2C dependency**: Requires Sprint 2A complete. REQ-S2C-02 depends on REQ-S2A-02 (tap handler already added). REQ-S2C-06 extends REQ-S2A-05 (hint text already added).

---

## Global Acceptance Criteria

These criteria apply across all sprints and represent the definition of "Phase 2 done":

1. Difficulty card labels ("Beginner" through "Expert") never truncate on any phone with >= 320dp screen width.
2. Badge row labels are not shown in the row — emoji only — and remain accessible via tap-to-reveal bottom sheet.
3. Earned vs. unearned badges are visually distinct at a glance (full color vs. dimmed/grayscale).
4. Empty grid cells are visible against the dark background in dark mode without relying solely on grid lines.
5. A new user sees a "Tap an empty cell to start" hint before their first cell tap.
6. The puzzle screen has no more than 24dp of unintended blank space between the grid and the number pad.
7. The Home screen is scrollable and all content is reachable on a 600dp-height device.
8. The Today's Puzzles row is readable with 5 difficulties on a 320dp screen.
9. Undo and clear buttons are visually distinguishable from digit buttons.
10. A 0-day streak shows "Start your streak!" instead of "🔥 0 days streak".
11. The Play button uses the amber/tertiary CTA color.
12. Completed difficulty cards show a ✓ indicator.
13. No hardcoded hex, rgb, or arbitrary pixel values are introduced in any changed file.
14. All existing functionality (5 difficulties, share, undo, streak persistence) continues to work without regression.
15. The "More by dgeek" footer is either removed or tappable with a real URL.

---

## Constraints and Non-Goals

**In scope**:
- All 20 requirements listed above
- Dark mode is the primary visual target; light mode must not regress
- Narrow devices (Oppo width ~360dp) are the primary layout constraint

**Out of scope**:
- New game mechanics or puzzle types
- Settings screen changes
- New difficulty levels beyond Expert (7×7)
- Persistent badge unlock animations (beyond the color transition in REQ-S2A-03)
- Landscape mode optimization (not blocked, but not a success criterion)
- New external dependencies

**No new dependencies**: All implementation must use existing AndroidX, Material3, and Kotlin Coroutines libraries already in the project.

---

*Reference doc: `sumgrid-phase2-improvements.md` · Device: Oppo (dark mode) · Build: Post-Epic-003*
