# Epic 004 — SumGrid Phase 2: Plan

**Architecture approach**: Targeted UI fixes within existing Jetpack Compose component tree. No new architecture layers, no new dependencies, no ViewModel changes beyond data-flow wiring already in place.

**Created**: 2026-03-14  
**Codebase state**: Post-Epic-003, Kotlin/Jetpack Compose, Material3, single-module Android app.

---

## Architecture

This epic requires no new architecture. All changes are constrained to:

- **Presentation layer only** — composable functions in `ui/screens/` and `ui/components/`
- **Existing data flow** — `HomeViewModel.uiState` already exposes `puzzleStatuses`, `earnedBadges`, `currentStreak`, and `allComplete`. No new state is needed; a few new derived values are wired from existing state.
- **One new file** — `ui/components/BadgeDetailBottomSheet.kt` (Sprint 2C). All other requirements modify existing files in-place.
- **Design token compliance** — all color changes use `MaterialTheme.colorScheme.*` tokens, no hex/rgb literals introduced.

### Architectural decisions

1. **LazyRow for DifficultySelector** — Replace the fixed-weight `Row` with `LazyRow` + 84dp fixed-width cards. This is the standard Material3 scrollable chip pattern and avoids any layout reflow cascade.

2. **Emoji-only BadgeItem with tap-to-reveal** — Remove the `displayName` Text from `BadgeItem`. Wire a `clickable` modifier and manage `selectedBadge: StreakBadge?` state locally in `HomeScreen` (no ViewModel needed — it is ephemeral UI state).

3. **Grayscale via ColorFilter, not alpha alone** — `Modifier.graphicsLayer` with a custom `ColorMatrix` set to saturation=0 on unearned badges, replacing the fragile `alpha=0.38f` approach that fails on AMOLED dark.

4. **Dark-mode cell tint via `surfaceContainerHigh`** — In `gridColorsFromTheme()`, set `userCellBg = cs.surfaceContainerHigh` when dark mode is active. This token is non-zero even in dark themes and avoids any hardcoded hex.

5. **Remove `Spacer(weight=1f)` on PuzzleScreen** — Replace the existing `Column` layout with a `Box(contentAlignment = Alignment.Center)` wrapping a `Column { grid + 16dp gap + numpad }`, so the pair is vertically centered with zero unintended dead space.

6. **Timer into TopAppBar `actions` slot** — The existing `TopAppBar` call gains an `actions` lambda; the standalone Timer `Text` and its `Spacer(8.dp)` are removed. The same `LaunchedEffect` drives the update.

7. **Pulse animation via Canvas `pulsingCell` parameter** — `GridRenderer` accepts a new optional `pulsingCell: Pair<Int,Int>?` that draws an `infiniteRepeatable` animated border. `PuzzleScreen` computes the first empty cell once on load (using `remember { firstEmptyCell(state) }`) and sets `pulsingCell` to null once `selectedCell != null`.

8. **BadgeDetailBottomSheet as a standalone composable** — A new `ui/components/BadgeDetailBottomSheet.kt` houses `BadgeDetailBottomSheet(badge, currentStreak, onDismiss)`. It is shown from `HomeScreen` via a `selectedBadge` state variable; this avoids polluting the ViewModel with ephemeral sheet state.

9. **Height-adaptive badge row** — Sprint 2C collapses the badge row using `LocalConfiguration.current.screenHeightDp <= 560` to a single `AssistChip` on short screens. This uses only the standard Compose `LocalConfiguration` — no new infrastructure.

10. **Sequential sprint dependency enforced in code** — Sprint 2B's unselected card visibility fix (REQ-S2B-09) depends on the LazyRow being in place (REQ-S2A-01). Sprint 2C's `BadgeDetailBottomSheet` (REQ-S2C-02) depends on the `clickable` handler added in REQ-S2A-02. Sprint 2C's pulse animation (REQ-S2C-06) depends on the `pulsingCell` parameter stub added in REQ-S2A-05's implementation. These ordering constraints are the reason sprints are sequential.

---

## Files to modify per sprint

### Sprint 2A — Critical Layout Fixes

| File | Change |
|------|--------|
| `ui/components/DifficultySelector.kt` | Replace `Row + weight(1f)` with `LazyRow` + 84dp fixed cards |
| `ui/screens/HomeScreen.kt` | Remove `displayName` Text from `BadgeItem`; add `clickable` + `selectedBadge` state; apply `graphicsLayer` grayscale filter on unearned badges |
| `ui/components/GridRenderer.kt` | Fix `gridColorsFromTheme()` — set `userCellBg = cs.surfaceContainerHigh` in dark mode |
| `ui/screens/PuzzleScreen.kt` | Add "Tap an empty cell to start" `AnimatedVisibility` hint between grid and numpad, gated on `selectedCell == null` |

### Sprint 2B — Layout and Spacing Polish

| File | Change |
|------|--------|
| `ui/screens/PuzzleScreen.kt` | Remove `Spacer(weight=1f)`; restructure grid+numpad into centered `Box`+`Column`; move timer into `TopAppBar actions`; verify/reduce grid horizontal padding via `outerPaddingDp` |
| `ui/screens/HomeScreen.kt` | Abbreviate `PuzzleStatusChip` labels ("BEG"/"EASY"/"MED"/"HARD"/"EXP"); fix `StreakDisplay` zero-streak text; switch Play button to `tertiaryContainer` colors |
| `ui/components/DifficultySelector.kt` | Update unselected card `containerColor` to `surfaceContainerHigh`; increase unselected border to 1.5dp |
| `ui/components/NumberPad.kt` | Add `Spacer(width=8.dp)` before undo; set `UndoButton` background to `secondaryContainer`; set `ClearButton` border to 1.5dp |

### Sprint 2C — UX Enhancements

| File | Change |
|------|--------|
| `ui/components/DifficultySelector.kt` | Accept `completedDifficulties: Set<Difficulty>` param; add ✓ `Box` overlay per card |
| `ui/screens/HomeScreen.kt` | Pass `puzzleStatuses` completion set to `DifficultySelector`; height-adaptive badge row collapse; wire `selectedBadge` to `BadgeDetailBottomSheet`; remove/wire footer |
| `ui/components/BadgeDetailBottomSheet.kt` | **New file** — `ModalBottomSheet` with badge emoji, name, earned/locked state and "N more days" copy |
| `ui/components/NumberPad.kt` | Wrap `NumberButton` colors in `animateColorAsState(tween(150))` |
| `ui/components/GridRenderer.kt` | Accept `pulsingCell: Pair<Int,Int>?` param; add `infiniteRepeatable` alpha-pulsing border draw call for that cell |
| `ui/screens/PuzzleScreen.kt` | Compute `firstEmptyCell` on load; pass to `GridRenderer`; clear when `selectedCell != null` |

---

## Reuse analysis

### REUSE: Existing animation infrastructure
`HomeScreen.kt` already imports `animateFloat`, `infiniteRepeatable`, `tween`, `RepeatMode`, `rememberInfiniteTransition`. The pulse animation in REQ-S2C-06 reuses this exact pattern — no new animation imports needed.

### REUSE: `gridColorsFromTheme()` color derivation pattern
The `gridColorsFromTheme()` function already exists and returns theme-derived colors. REQ-S2A-04 simply changes one field (`userCellBg`) in that function — the pattern is fully established.

### REUSE: `animateFloatAsState` with spring in GridRenderer
`GridRenderer.kt` already uses `animateFloatAsState + spring` for selection scale. The pulse animation adds `rememberInfiniteTransition + animateFloat + infiniteRepeatable` which follows the same Compose animation API surface already used in `HomeScreen.kt`.

### REUSE: `ModalBottomSheet` pattern
Material3 `ModalBottomSheet` is already a transitive dependency (Material3 is declared). No new library needed for REQ-S2C-02.

### REUSE: `ButtonDefaults.buttonColors` override pattern
The `NumberButton` in `NumberPad.kt` already shows how to override `containerColor` and `contentColor` via `ButtonDefaults.buttonColors`. The Play button amber fix (REQ-S2B-08) uses the identical override pattern in `HomeScreen.kt`.

### REUSE: `LaunchedEffect` for ticking state
Both `HomeScreen` and `PuzzleScreen` already use `LaunchedEffect(Unit) { while(true) { delay(1000); ... } }`. No new pattern needed for the timer relocation.

### REUSE: Semantics + contentDescription pattern
`BadgeItem` already sets `contentDescription` on the emoji Text for accessibility. The new `clickable` + bottom sheet pattern sits alongside this existing semantics layer.

**Reuse count: 7 identified reuse opportunities.**

---

## Risk areas

### Risk 1 — LazyRow scroll state and selected card scrollability (REQ-S2A-01)
LazyRow does not auto-scroll to the selected item across recompositions. If the selected difficulty is off-screen, the user cannot see the selected state without scrolling. Mitigation: use `LazyListState.animateScrollToItem` in a `LaunchedEffect(selectedDifficulty)` to scroll to the selected card index whenever selection changes.

### Risk 2 — Canvas pulse animation and performance (REQ-S2C-06)
Adding an `infiniteRepeatable` animation that triggers redraws of the Canvas on every frame creates a continuous recomposition cycle. Mitigation: use `rememberInfiniteTransition` scoped to `GridRenderer` and ensure the animation stops (transition removed or ignored) once `pulsingCell == null`. Compose's `remember` and stable key patterns prevent unnecessary recompositions of the outer hierarchy.

### Risk 3 — `graphicsLayer` ColorFilter on Text composables (REQ-S2A-03)
Compose's `graphicsLayer { colorFilter = ... }` with a `ColorMatrix` is well-supported from Compose 1.3+, but applying it to a `Text` composable that renders emoji may have device-specific behavior on some OEMS. Mitigation: wrap the emoji `Text` in a `Box` and apply `graphicsLayer` on the `Box`, not on the `Text` directly.

### Risk 4 — `surfaceContainerHigh` token availability (REQ-S2A-04)
`MaterialTheme.colorScheme.surfaceContainerHigh` was added in Material3 1.2.0. If the project targets an older Material3 version, this token may not exist. Mitigation: check `build.gradle` for `androidx.compose.material3` version before implementing. Fallback: use `cs.surfaceVariant.copy(alpha = 0.15f)`.

### Risk 5 — PuzzleScreen layout restructure regression (REQ-S2B-01)
Removing `Spacer(weight=1f)` and restructuring the Column is the highest-impact change in Sprint 2B. The completion banner, share button, and numpad all depend on their relative positions in the Column. Mitigation: implement and test each grid size (3×3 through 7×7) individually. The `Box(contentAlignment = Alignment.Center)` approach is safe because it does not change the layout semantics of child composables.

### Risk 6 — TopAppBar `actions` slot and share icon coexistence (REQ-S2B-04)
The PuzzleScreen already has a Share icon in the `OutlinedButton` below the grid. Moving the timer to `actions` must not conflict with a future share icon in the TopAppBar. Mitigation: the spec notes this explicitly — timer and share can coexist in the actions row. No refactor needed for S2B; share icon remains below grid in the completion state.

---

## Constraints confirmed from code review

- No hardcoded hex values may be introduced. `GridColors.defaults()` is not called from `GridRenderer` (it calls `gridColorsFromTheme()`), so the existing hardcoded hex values in `defaults()` are not a compliance issue for new code.
- `DifficultySelector` does not receive `puzzleStatuses` today — this data flow is added in Sprint 2C (REQ-S2C-01) only. Sprint 2A and 2B do not require this wiring.
- `StreakDisplay` has no `streak == 0` guard — the pulsing flame runs unconditionally. The fix (REQ-S2B-07) adds a single `if (streak == 0)` branch before the existing Row.
- The `formatElapsed()` helper in `PuzzleScreen.kt` is a private top-level function and is reused as-is when the timer moves to the TopAppBar actions slot.
- `BadgeDetailBottomSheet` requires `@OptIn(ExperimentalMaterial3Api::class)` since `ModalBottomSheet` is still experimental in Material3 at the time of implementation.

---

## Definition of done

All 20 requirements implemented, all global acceptance criteria from `epic-spec.md` satisfied, no hardcoded hex/rgb values introduced, no regression on existing 5-difficulty / share / undo / streak functionality.
