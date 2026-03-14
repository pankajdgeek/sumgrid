# Epic 004 — Tasks: SumGrid Phase 2 Layout Fixes

**Generated**: 2026-03-14  
**Total tasks**: 30  
**Sprint structure**: S2A (tasks T001–T010) → S2B (tasks T011–T021) → S2C (tasks T022–T030)  
**TDD approach**: Jetpack Compose UI is Canvas/composable-based. Where pure logic is testable via JVM
(source-scan tests, pure functions, data classes), full Red-Green-Refactor cycles apply. UI-only
composable changes use visual acceptance criteria in place of automated TDD where the runtime
requires an Android device.

---

## Sprint 2A — Critical Layout Fixes (~12h)

Gate: All 10 tasks must pass before Sprint 2B begins.  
Minimum shippable: Sprint 2A alone eliminates all visible layout breakage.

---

### T001 — [RED] Write failing test: DifficultySelector uses LazyRow

**Req**: REQ-S2A-01  
**File**: `app/src/test/kotlin/org/dgeek/sumgrid/ui/DifficultySelectorTest.kt` (new)  
**Effort**: ~30min  
**Depends on**: none  

Write a source-scan JVM test that asserts `DifficultySelector.kt` contains `LazyRow` and does not
contain `Modifier.weight(1f)` applied to individual cards. The test must fail against the current
source (which uses a `Row` with `weight(1f)` per card).

**Test to write**:
```kotlin
@Test
fun difficultySelector_usesLazyRow() {
    assertTrue(source.contains("LazyRow"))
}

@Test
fun difficultySelector_doesNotUseWeightPerCard() {
    // Current source has weight(1f) on each card — this test will fail until T002 is done
    assertFalse("DifficultySelector must not weight cards in a fixed Row",
        source.contains("Modifier.weight(1f)") && !source.contains("LazyRow"))
}

@Test
fun difficultySelector_cardsHaveFixedWidth() {
    assertTrue("Cards must have a fixed width (84.dp)",
        source.contains("84.dp") || source.contains("width = 84"))
}
```

**Acceptance**: Tests compile and fail (red) against current `DifficultySelector.kt`.

---

### T002 — [GREEN] Replace Row+weight with LazyRow in DifficultySelector

**Req**: REQ-S2A-01  
**File**: `ui/components/DifficultySelector.kt`  
**Effort**: ~3h  
**Depends on**: T001  

Replace the `Row { for (difficulty) { DifficultyCard(modifier = Modifier.weight(1f)) } }` in
`DifficultySelector` with a `LazyRow`. Each `DifficultyCard` receives `Modifier.width(84.dp)`
instead of `weight(1f)`. Add `LazyListState` with a `LaunchedEffect(selectedDifficulty)` that
calls `listState.animateScrollToItem(index)` when the selection changes, keeping the selected
card in view.

**Implementation steps**:
1. Add imports: `LazyRow`, `rememberLazyListState`, `LazyListState`
2. Replace `Row(modifier = modifier.fillMaxWidth(), ...)` with `LazyRow(state = listState, ...)`
3. Change `DifficultyCard` modifier from `Modifier.weight(1f)` to `Modifier.width(84.dp)`
4. Add `LaunchedEffect(selectedDifficulty)` to scroll to selected index
5. No change to `DifficultyCard` internal layout

**Acceptance**:
- "Beginner", "Easy", "Medium", "Hard", "Expert" all display without truncation at 320dp width
- Approximately 3–4 cards visible; horizontal swipe reveals remaining cards
- Selected card retains primary border at any scroll position
- T001 tests pass (green)

---

### T003 — [REFACTOR] Clean up DifficultySelector imports and remove dead code

**Req**: REQ-S2A-01  
**File**: `ui/components/DifficultySelector.kt`  
**Effort**: ~30min  
**Depends on**: T002  

Remove the now-unused `Row` import (replaced by `LazyRow`). Remove `Arrangement` import if no
longer needed. Verify `fillMaxWidth` is still used on the `LazyRow` modifier. No behavioral
change — tests from T001 continue to pass.

**Acceptance**: `DifficultySelector.kt` has no unused imports. T001 tests still pass.

---

### T004 — [RED] Write failing tests: BadgeItem emoji-only and clickable

**Req**: REQ-S2A-02  
**File**: `app/src/test/kotlin/org/dgeek/sumgrid/ui/BadgeRowTest.kt` (new)  
**Effort**: ~30min  
**Depends on**: none (parallel to T001)  

Write source-scan JVM tests asserting that `HomeScreen.kt` does not render `badge.displayName`
as a `Text` inside `BadgeItem`, and that `BadgeItem` has a `clickable` modifier. Tests must fail
against current source (which renders `displayName` as a Text label in `BadgeItem`).

**Tests to write**:
```kotlin
@Test
fun badgeItem_doesNotShowDisplayNameAsText() {
    // Current BadgeItem renders badge.displayName — this test must fail until T005
    assertFalse("BadgeItem must not render displayName Text in the row",
        source.contains("badge.displayName") && source.contains("labelSmall"))
}

@Test
fun badgeItem_hasClickableModifier() {
    assertTrue("BadgeItem must be clickable",
        source.contains("BadgeItem") && source.contains("clickable"))
}

@Test
fun homeScreen_hasSelectedBadgeState() {
    assertTrue("HomeScreen must have selectedBadge state variable",
        source.contains("selectedBadge"))
}
```

**Acceptance**: Tests compile and fail (red) against current `HomeScreen.kt`.

---

### T005 — [GREEN] Make BadgeItem emoji-only with clickable tap handler

**Req**: REQ-S2A-02  
**File**: `ui/screens/HomeScreen.kt`  
**Effort**: ~2.5h  
**Depends on**: T004  

In `BadgeItem`: remove the `displayName` `Text` composable. Keep only the emoji `Text` at
`headlineSmall` size. Add `clickable` modifier to the `Column` in `BadgeItem`. Change
`BadgeItem` signature to accept an `onClick: () -> Unit` parameter.

In `BadgeRow`: thread the click handler through to each `BadgeItem`.

In `HomeScreen` (root composable): add `var selectedBadge by remember { mutableStateOf<StreakBadge?>(null) }`.
Pass `onClick = { selectedBadge = badge }` into each `BadgeItem` via `BadgeRow`. Add a stub
`if (selectedBadge != null) { /* bottom sheet placeholder — wired in T024 */ }` so the state
exists for Sprint 2C.

**Acceptance**:
- Badge row shows 4 emoji icons with no text labels visible below them
- Each badge is tappable (no crash; `selectedBadge` state updates)
- `contentDescription` accessibility label is preserved on the emoji Text
- T004 tests pass (green)

---

### T006 — [GREEN] Apply graphicsLayer grayscale filter to unearned badges

**Req**: REQ-S2A-03  
**File**: `ui/screens/HomeScreen.kt` (BadgeItem composable)  
**Effort**: ~2h  
**Depends on**: T005 (same composable, do in sequence)  

Replace the `Modifier.alpha(if (earned) 1f else 0.38f)` on the `Column` in `BadgeItem` with a
`Modifier.graphicsLayer { ... }` approach. For unearned badges, apply both reduced alpha (0.38f)
and a `ColorMatrix` with `setToSaturation(0f)` as the `colorFilter` to desaturate to grayscale.
For earned badges, no `colorFilter` and `alpha = 1f`.

**Implementation**:
```kotlin
// Wrap the emoji Text in a Box and apply graphicsLayer on the Box:
Box(
    modifier = Modifier.graphicsLayer {
        alpha = if (earned) 1f else 0.38f
        colorFilter = if (earned) null else ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0f) }
        )
    }
) {
    Text(text = badge.icon, style = MaterialTheme.typography.headlineSmall, ...)
}
```

Add imports: `androidx.compose.ui.graphics.ColorFilter`, `androidx.compose.ui.graphics.ColorMatrix`.

**Acceptance**:
- Unearned badges appear visually dimmed AND desaturated (grayscale) on AMOLED dark backgrounds
- Earned badges remain full color at full opacity
- No regression to clickable behavior from T005

---

### T007 — [RED] Write failing test: gridColorsFromTheme uses surfaceContainerHigh

**Req**: REQ-S2A-04  
**File**: `app/src/test/kotlin/org/dgeek/sumgrid/ui/GridRendererTest.kt` (extend existing)  
**Effort**: ~20min  
**Depends on**: none (parallel)  

Add a source-scan test to the existing `GridRendererTest.kt` asserting that `GridRenderer.kt`
references `surfaceContainerHigh` (the token for the dark-mode cell tint). Must fail against
current source which uses `cs.surface` for `userCellBg`.

**Test to add**:
```kotlin
private val gridRendererSource: String by lazy {
    java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt").readText()
}

@Test
fun gridColorsFromTheme_usesSurfaceContainerHighForUserCellBg() {
    assertTrue("gridColorsFromTheme must use surfaceContainerHigh for userCellBg",
        gridRendererSource.contains("surfaceContainerHigh"))
}

@Test
fun gridColorsFromTheme_noHardcodedHexInThemeFunction() {
    // Verify the function body after "fun gridColorsFromTheme" has no 0xFF literals
    val fnBody = gridRendererSource.substringAfter("fun gridColorsFromTheme")
        .substringBefore("\n}")
    assertFalse("gridColorsFromTheme must not contain hardcoded hex colors",
        fnBody.contains("Color(0x"))
}
```

**Acceptance**: Tests compile and fail (red) against current `GridRenderer.kt`.

---

### T008 — [GREEN] Fix dark-mode cell tint in gridColorsFromTheme

**Req**: REQ-S2A-04  
**File**: `ui/components/GridRenderer.kt`  
**Effort**: ~45min  
**Depends on**: T007  

In `gridColorsFromTheme()`, change `userCellBg = cs.surface` to
`userCellBg = cs.surfaceContainerHigh`. This token is non-zero in Material3 dark themes and
provides a visible faint tint for empty user-fillable cells without hardcoding any hex.

Before implementing, verify `build.gradle` declares `androidx.compose.material3` version >= 1.2.0
where `surfaceContainerHigh` was introduced. If the version is older, use the fallback
`cs.surfaceVariant.copy(alpha = 0.15f)` instead.

**Acceptance**:
- Empty grid cells have a visible faint background in dark mode distinguishable from the dark screen
- Given (pre-filled) cells remain unaffected (`givenCellBg` unchanged)
- Light mode behavior unchanged (surfaceContainerHigh is non-white in light mode too, providing
  the correct subtle fill)
- T007 tests pass (green)

---

### T009 — [RED] Write failing test: PuzzleScreen has hint text composable

**Req**: REQ-S2A-05  
**File**: `app/src/test/kotlin/org/dgeek/sumgrid/ui/PuzzleScreenTopBarTest.kt` (extend existing)  
**Effort**: ~20min  
**Depends on**: none (parallel)  

Add source-scan tests asserting `PuzzleScreen.kt` contains the hint text string and gates it on
`selectedCell == null`. Must fail against current source which has no hint text.

**Tests to add**:
```kotlin
@Test
fun puzzleScreen_hasTapHintText() {
    assertTrue("PuzzleScreen must show 'Tap an empty cell to start' hint",
        puzzleScreenSource.contains("Tap an empty cell to start"))
}

@Test
fun puzzleScreen_hintGatedOnSelectedCell() {
    assertTrue("Hint must be gated on selectedCell == null",
        puzzleScreenSource.contains("selectedCell == null"))
}

@Test
fun puzzleScreen_hintUsesAnimatedVisibility() {
    assertTrue("Hint should use AnimatedVisibility for smooth appear/disappear",
        puzzleScreenSource.contains("AnimatedVisibility"))
}
```

**Acceptance**: Tests compile and fail (red) against current `PuzzleScreen.kt`.

---

### T010 — [GREEN] Add "Tap an empty cell to start" hint in PuzzleScreen

**Req**: REQ-S2A-05  
**File**: `ui/screens/PuzzleScreen.kt`  
**Effort**: ~1.5h  
**Depends on**: T009  

Between the grid `Box` and the `Spacer(weight(1f))` (which will be removed in T011), add an
`AnimatedVisibility(visible = currentState.selectedCell == null)` block containing a centered
`Text("Tap an empty cell to start", style = bodyMedium, color = onSurfaceVariant)`.

Also add the `pulsingCell` parameter stub to `GridRenderer` (as `pulsingCell: Pair<Int,Int>? = null`)
so Sprint 2C's T030 can wire the pulse animation without modifying the call site again. Pass
`pulsingCell = null` for now from `PuzzleScreen`.

**Implementation steps**:
1. Add `AnimatedVisibility` import
2. Insert hint block between grid Box and Spacer
3. Add `pulsingCell: Pair<Int,Int>? = null` parameter to `GridRenderer` function signature
4. Pass `pulsingCell = null` from `PuzzleScreen` to `GridRenderer`
5. T009 tests pass (green)

**Acceptance**:
- "Tap an empty cell to start" text is visible when no cell is selected on puzzle load
- Text disappears immediately when any cell is tapped (AnimatedVisibility fade-out)
- Hint is absent when the puzzle is complete (numpad already hidden, selectedCell is irrelevant)
- T009 tests pass (green)

---

## Sprint 2B — Layout and Spacing Polish (~16h)

Gate: All 11 tasks must pass before Sprint 2C begins.  
Prerequisite: Sprint 2A (all 10 tasks) complete.

---

### T011 — [RED] Write failing tests: PuzzleScreen layout restructure

**Req**: REQ-S2B-01  
**File**: `app/src/test/kotlin/org/dgeek/sumgrid/ui/PuzzleScreenTopBarTest.kt` (extend)  
**Effort**: ~20min  
**Depends on**: Sprint 2A complete  

Add source-scan tests asserting `PuzzleScreen.kt` does not contain `Modifier.weight(1f)` as a
`Spacer` between grid and numpad, and that it uses a centering layout construct.

**Tests to add**:
```kotlin
@Test
fun puzzleScreen_doesNotHaveWeightSpacer() {
    assertFalse("PuzzleScreen must not have Spacer(weight(1f)) between grid and numpad",
        puzzleScreenSource.contains("Modifier.weight(1f)"))
}

@Test
fun puzzleScreen_hasCenteredLayout() {
    assertTrue("PuzzleScreen must use contentAlignment = Alignment.Center",
        puzzleScreenSource.contains("contentAlignment = Alignment.Center") ||
        puzzleScreenSource.contains("verticalArrangement = Arrangement.Center"))
}
```

**Acceptance**: Tests compile and fail (red) against current `PuzzleScreen.kt`.

---

### T012 — [GREEN] Remove dead-space Spacer, restructure puzzle screen layout

**Req**: REQ-S2B-01  
**File**: `ui/screens/PuzzleScreen.kt`  
**Effort**: ~3.5h  
**Depends on**: T011  

Replace the current `Column { grid Box + Spacer(weight=1f) + completion banner + NumberPad }`
structure with a layout where the grid and numpad are vertically grouped and centered, with no
`weight(1f)` spacer creating dead space.

**Approach**: Wrap the main content in a `Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center)`. Inside it, place a `Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally)` containing: the grid Box, the `AnimatedVisibility` hint (from T010), the completion banner (conditional), and the `NumberPad`.

Remove the `Spacer(modifier = Modifier.weight(1f))` line entirely.

**Acceptance**:
- Gap between bottom of grid and top of numpad is <= 24dp on a standard phone (>=640dp height)
- Both grid and numpad are simultaneously visible for all grid sizes (3×3 through 7×7)
- Completion banner appears in the correct position (between grid and numpad area)
- Share button remains accessible when puzzle is complete
- T011 tests pass (green)

---

### T013 — [GREEN] Move timer into TopAppBar actions slot

**Req**: REQ-S2B-04  
**File**: `ui/screens/PuzzleScreen.kt`  
**Effort**: ~2h  
**Depends on**: T012 (same file; do after layout is stable)  

Remove the standalone timer `Text` composable (lines ~151–161 in current source) and its
following `Spacer(height = 8.dp)`. Add an `actions` lambda to the existing `TopAppBar` call
that renders the elapsed time as `Text(formatElapsed(currentState.elapsedSeconds), style = labelLarge)`.
The `formatElapsed` private function remains unchanged.

**Implementation**:
```kotlin
TopAppBar(
    title = { Text(...) },
    navigationIcon = { ... },
    actions = {
        Text(
            text = formatElapsed(currentState.elapsedSeconds),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 16.dp)
        )
    },
    colors = TopAppBarDefaults.topAppBarColors(...)
)
```

**Acceptance**:
- Timer appears in the top-right corner of the TopAppBar in MM:SS format
- Timer still updates every second (same LaunchedEffect drives it)
- No standalone timer Text exists below the TopAppBar
- At least 8dp more vertical space is available for the grid compared to before
- Existing `PuzzleScreenTopBarTest` tests continue to pass

---

### T014 — [GREEN] Reduce grid horizontal padding in PuzzleScreen

**Req**: REQ-S2B-05  
**Files**: `ui/screens/PuzzleScreen.kt`, `ui/components/GridRenderer.kt`  
**Effort**: ~1h  
**Depends on**: T012  

`GridRenderer` accepts `outerPaddingDp: Float = 16f`. In `PuzzleScreen.kt`, change the call to
`GridRenderer(outerPaddingDp = 4f)` to reduce horizontal margins to ~4dp each side. Verify the
sum indicator labels remain readable at this narrower padding by checking the `labelX` offset
calculation in `drawGrid` (which adds `cellSize * 0.08f` — still sufficient at narrower widths).

**Acceptance**:
- Total horizontal margin on each side of the grid is <= 8dp on a 360dp device
- Sum indicator numbers are not clipped after the padding reduction
- Cell tap registration is unaffected

---

### T015 — [RED] Write failing tests: PuzzleScreen timer in TopAppBar

**Req**: REQ-S2B-04  
**File**: `app/src/test/kotlin/org/dgeek/sumgrid/ui/PuzzleScreenTopBarTest.kt` (extend)  
**Effort**: ~15min  
**Depends on**: T013  

Add a test that the timer text is wired to the `actions` slot (source contains `actions = {`
containing `formatElapsed`), and that no standalone timer Text exists outside the TopAppBar.

**Tests to add**:
```kotlin
@Test
fun puzzleScreen_timerIsInTopAppBarActions() {
    val topBarSection = puzzleScreenSource
        .substringAfter("TopAppBar(")
        .substringBefore("}) { innerPadding ->")
    assertTrue("Timer must be in TopAppBar actions slot",
        topBarSection.contains("formatElapsed") || topBarSection.contains("elapsedSeconds"))
}

@Test
fun puzzleScreen_noStandaloneTimerText() {
    // After the TopAppBar closing brace, no formatElapsed call should appear in the Column
    val afterTopBar = puzzleScreenSource.substringAfter("}) { innerPadding ->")
    assertFalse("No standalone timer Text outside TopAppBar",
        afterTopBar.contains("formatElapsed"))
}
```

**Acceptance**: Tests pass (green) after T013 implementation.

---

### T016 — [RED] Write failing test: HomeScreen PuzzleStatusChip uses abbreviated labels

**Req**: REQ-S2B-03  
**File**: `app/src/test/kotlin/org/dgeek/sumgrid/ui/HomeScreenLayoutTest.kt` (extend)  
**Effort**: ~15min  
**Depends on**: Sprint 2A complete  

Add tests asserting that `HomeScreen.kt` uses abbreviated chip labels ("BEG", "EASY", "MED",
"HARD", "EXP") instead of full difficulty names rendered via `name.lowercase().replaceFirstChar`.

**Tests to add**:
```kotlin
@Test
fun puzzleStatusChip_usesAbbreviatedLabels() {
    assertTrue("PuzzleStatusChip must use abbreviated labels",
        homeScreenSource.contains("\"BEG\"") || homeScreenSource.contains("abbreviatedLabel"))
}

@Test
fun puzzleStatusChip_doesNotUseLowercaseReplaceFirstChar() {
    assertFalse("PuzzleStatusChip must not produce full-word labels from name.lowercase()",
        homeScreenSource.contains("replaceFirstChar") &&
        homeScreenSource.contains("PuzzleStatusChip"))
}
```

**Acceptance**: Tests compile and fail (red) against current `HomeScreen.kt`.

---

### T017 — [GREEN] Abbreviate PuzzleStatusChip labels and fix zero-streak display

**Req**: REQ-S2B-03, REQ-S2B-07  
**File**: `ui/screens/HomeScreen.kt`  
**Effort**: ~2h  
**Depends on**: T016  

**Change 1 — Abbreviate chip labels**: Change the label string passed to `PuzzleStatusChip` from
`status.difficulty.name.lowercase().replaceFirstChar { it.uppercaseChar() }` to a `when`
expression mapping each `Difficulty` to a short string:
```kotlin
val chipLabel = when (status.difficulty) {
    Difficulty.BEGINNER -> "BEG"
    Difficulty.EASY     -> "EASY"
    Difficulty.MEDIUM   -> "MED"
    Difficulty.HARD     -> "HARD"
    Difficulty.EXPERT   -> "EXP"
}
```

**Change 2 — Zero-streak display**: In `StreakDisplay`, add a guard before the animated `Row`:
```kotlin
if (streak == 0) {
    Text(
        text = "Start your streak!",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer
    )
} else {
    Row(...) { /* existing animated flame + count */ }
}
```

**Acceptance**:
- All 5 chips fit without overflow on a 320dp screen
- "✓" and "○" icons remain visible and correct
- A 0-day streak shows "Start your streak!" without a flame emoji
- A 1+ day streak shows the animated flame and count
- T016 tests pass (green)

---

### T018 — [GREEN] Switch Play button to amber/tertiary CTA color

**Req**: REQ-S2B-08  
**File**: `ui/screens/HomeScreen.kt`  
**Effort**: ~45min  
**Depends on**: Sprint 2A complete (independent change)  

In `HomeScreen`, find the Play `Button` composable. Add `colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)`. The disabled state uses `ButtonDefaults` defaults automatically (no explicit `disabledContainerColor` needed — Material3 handles it).

Add `ButtonDefaults` import if not already present (it is already imported in `NumberPad.kt`,
confirm for `HomeScreen.kt`).

**Acceptance**:
- Play button uses amber/warm tertiary color in dark mode, visually distinct from indigo elements
- Disabled state (no difficulty selected) correctly shows the muted disabled color
- Light mode tertiary color is used (no hardcoded hex introduced)

---

### T019 — [GREEN] Improve unselected difficulty card visibility

**Req**: REQ-S2B-09  
**File**: `ui/components/DifficultySelector.kt`  
**Effort**: ~45min  
**Depends on**: T002 (LazyRow must be in place)  

In `DifficultyCard`, change the unselected card container color from `MaterialTheme.colorScheme.surface`
to `MaterialTheme.colorScheme.surfaceContainerHigh`. Change unselected border width from `1.dp`
to `1.5.dp`. The selected card remains at `primaryContainer` with `2.dp` primary border.

**Acceptance**:
- Unselected cards have a faintly visible background in dark mode
- Clear visual contrast between selected (bright primary border) and unselected cards
- No hardcoded color values introduced

---

### T020 — [GREEN] Distinguish undo and clear buttons from digit buttons

**Req**: REQ-S2B-06  
**File**: `ui/components/NumberPad.kt`  
**Effort**: ~2h  
**Depends on**: Sprint 2A complete (independent)  

**Changes**:

1. In the 2-row layout (`maxVal >= 7`), insert `Spacer(modifier = Modifier.width(8.dp))` before
   `UndoButton` on the last row to create a visible visual gap.

2. In `UndoButton`, change from `OutlinedButton` to `Button` with
   `colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)`.
   This gives undo a filled secondary container background vs. the transparent outlined digit style.

3. In `ClearButton`, keep `OutlinedButton` but add
   `border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)` to the `OutlinedButton`
   parameters for a slightly bolder red outline. Add `BorderStroke` import.

4. Apply the same spacer change to the single-row layout (insert before `UndoButton` there too).

**Acceptance**:
- Visible spacing gap before undo button in 2-row layout
- Undo button has a secondary filled background, visually distinct from digit buttons
- Clear button has a bolder red outline (1.5dp) than digit buttons
- `vm.undo()` still fires correctly when undo is tapped (no regression)
- Existing `NumberPadLayoutTest` tests pass

---

### T021 — [GREEN] Verify Home screen scrollability (content audit)

**Req**: REQ-S2B-02  
**File**: `ui/screens/HomeScreen.kt`  
**Effort**: ~1h  
**Depends on**: T017 (abbreviations reduce chip height needs), T018  

Audit the `HomeScreen.kt` content column for `Spacer` heights. Reduce any `Spacer(height = 28.dp)`
between sections to `Spacer(height = 16.dp)` to lower the minimum required height. Confirm
`verticalScroll(rememberScrollState())` remains before `padding(horizontal = 20.dp)` in the
modifier chain (currently correct).

Add a source-scan test confirming the scroll modifier ordering:

```kotlin
// Add to HomeScreenLayoutTest.kt
@Test
fun homeScreen_verticalScrollBeforeHorizontalPadding() {
    val scrollIdx = homeScreenSource.indexOf("verticalScroll")
    val paddingIdx = homeScreenSource.indexOf("padding(horizontal = 20.dp)")
    assertTrue("verticalScroll must appear before horizontal padding",
        scrollIdx < paddingIdx && scrollIdx != -1 && paddingIdx != -1)
}
```

**Acceptance**:
- All content on the Home screen is reachable by scrolling on a 600dp-height device
- Play button is visible when scrolled to the bottom
- Scroll position preserved across recompositions (standard `rememberScrollState` behavior)
- New source-scan test passes (green)

---

## Sprint 2C — UX Enhancements (~20h)

Gate: All 9 tasks must pass for Epic 004 completion.  
Prerequisite: Sprint 2A and Sprint 2B (all 21 tasks) complete.

---

### T022 — [GREEN] Remove or wire dgeek footer link

**Req**: REQ-S2C-05  
**File**: `ui/screens/HomeScreen.kt`  
**Effort**: ~30min  
**Depends on**: Sprint 2A + 2B complete  

Remove the dead `Text("More by dgeek", ...)` composable from `HomeScreen.kt`. Preserve the
`Spacer` or `padding(bottom = 16.dp)` below it so the last content item does not crowd the
navigation bar. If a real Play Store URL becomes available, this task can instead wrap it in a
`TextButton` with `Intent(ACTION_VIEW, Uri.parse(url))`.

Add a source-scan test:
```kotlin
// Add to HomeScreenLayoutTest.kt
@Test
fun homeScreen_noDgekFooterDeadText() {
    // Dead text "More by dgeek" with no click handler must not exist
    assertFalse("dgeek footer must be removed or made tappable",
        homeScreenSource.contains("\"More by dgeek\"") &&
        !homeScreenSource.contains("TextButton") &&
        !homeScreenSource.contains("ACTION_VIEW"))
}
```

**Acceptance**:
- No dead non-tappable "More by dgeek" text in the shipped build
- Bottom padding (16dp) preserved
- Source-scan test passes (green)

---

### T023 — [GREEN] Add numpad color animation on cell select/deselect

**Req**: REQ-S2C-04  
**File**: `ui/components/NumberPad.kt`  
**Effort**: ~1h  
**Depends on**: T020 (numpad visuals stabilized)  

In `NumberButton`, wrap `containerColor` and `contentColor` in `animateColorAsState`:

```kotlin
val animatedContainerColor by animateColorAsState(
    targetValue = if (enabled) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant,
    animationSpec = tween(durationMillis = 150),
    label = "numpadContainerColor"
)
val animatedContentColor by animateColorAsState(
    targetValue = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
    animationSpec = tween(durationMillis = 150),
    label = "numpadContentColor"
)
```

Pass `animatedContainerColor` and `animatedContentColor` to `ButtonDefaults.buttonColors(...)`.

Add imports: `animateColorAsState`, `tween` (already imported in `HomeScreen`; add to `NumberPad`).

**Acceptance**:
- Number buttons animate from `surfaceVariant` to `primaryContainer` over ~150ms when a cell is selected
- Animation does not block button input (taps register mid-animation)
- Existing `NumberPadLayoutTest` continues to pass

---

### T024 — [RED] Write failing tests: BadgeDetailBottomSheet exists and is wired

**Req**: REQ-S2C-02  
**File**: `app/src/test/kotlin/org/dgeek/sumgrid/ui/BadgeDetailBottomSheetTest.kt` (new)  
**Effort**: ~30min  
**Depends on**: T005 (selectedBadge state stub in HomeScreen)  

Write a source-scan test that `BadgeDetailBottomSheet.kt` exists and that `HomeScreen.kt` imports
and calls `BadgeDetailBottomSheet`. Must fail until T025 is implemented.

**Tests to write**:
```kotlin
class BadgeDetailBottomSheetTest {

    private val sheetSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/components/BadgeDetailBottomSheet.kt")
            .readText()
    }

    private val homeScreenSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt").readText()
    }

    @Test
    fun badgeDetailBottomSheet_fileExists() {
        assertTrue("BadgeDetailBottomSheet.kt must exist",
            java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/components/BadgeDetailBottomSheet.kt").exists())
    }

    @Test
    fun badgeDetailBottomSheet_hasModalBottomSheet() {
        assertTrue("BadgeDetailBottomSheet must use ModalBottomSheet",
            sheetSource.contains("ModalBottomSheet"))
    }

    @Test
    fun homeScreen_usesBadgeDetailBottomSheet() {
        assertTrue("HomeScreen must use BadgeDetailBottomSheet",
            homeScreenSource.contains("BadgeDetailBottomSheet"))
    }

    @Test
    fun badgeDetailBottomSheet_showsRequiredDays() {
        assertTrue("BadgeDetailBottomSheet must display requiredDays",
            sheetSource.contains("requiredDays"))
    }
}
```

**Acceptance**: Tests compile and fail (red) — file does not exist yet.

---

### T025 — [GREEN] Create BadgeDetailBottomSheet composable

**Req**: REQ-S2C-02  
**File**: `ui/components/BadgeDetailBottomSheet.kt` (new file)  
**Effort**: ~3h  
**Depends on**: T024, T005 (clickable + selectedBadge state already wired)  

Create a new file `ui/components/BadgeDetailBottomSheet.kt`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeDetailBottomSheet(
    badge: StreakBadge,
    currentStreak: Int,
    earnedBadges: Set<StreakBadge>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val earned = badge in earnedBadges

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = badge.icon, fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(text = badge.displayName, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(text = "Earned for a ${badge.requiredDays}-day streak",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            if (earned) {
                Text("Earned ✓", color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            } else {
                val daysLeft = (badge.requiredDays - currentStreak).coerceAtLeast(1)
                Text("Locked — $daysLeft more day${if (daysLeft != 1) "s" else ""} to go",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
```

Then wire it in `HomeScreen.kt`: replace the `if (selectedBadge != null) { /* stub */ }` from T005
with the actual `BadgeDetailBottomSheet(badge = selectedBadge!!, currentStreak = state.currentStreak, earnedBadges = state.earnedBadges, onDismiss = { selectedBadge = null })`.

**Acceptance**:
- Tapping any badge emoji slides up the bottom sheet
- Sheet shows badge emoji (48sp), name, description, and earned/locked status
- Locked badge shows correct "N more days to go" count based on current streak
- Swipe-to-dismiss closes the sheet and resets selectedBadge to null
- System back button dismisses the sheet (standard ModalBottomSheet behavior)
- T024 tests pass (green)

---

### T026 — [REFACTOR] Extract badge days-left logic to pure function and test it

**Req**: REQ-S2C-02  
**File**: `ui/components/BadgeDetailBottomSheet.kt`; new test in `BadgeDetailBottomSheetTest.kt`  
**Effort**: ~45min  
**Depends on**: T025  

Extract the days-left calculation from the composable into a pure `internal` function:
```kotlin
internal fun daysLeft(badge: StreakBadge, currentStreak: Int): Int =
    (badge.requiredDays - currentStreak).coerceAtLeast(1)
```

Add unit tests:
```kotlin
@Test
fun daysLeft_zeroStreak_returnsBadgeRequiredDays() {
    assertEquals(7, daysLeft(StreakBadge.WEEKLY_WARRIOR, 0))
}

@Test
fun daysLeft_streakExceedsRequired_returnsOne() {
    assertEquals(1, daysLeft(StreakBadge.WEEKLY_WARRIOR, 100))
}

@Test
fun daysLeft_streakEqualsRequired_returnsOne() {
    // At exactly required days, badge is earned — daysLeft returns 1 (coerced)
    assertEquals(1, daysLeft(StreakBadge.WEEKLY_WARRIOR, 7))
}
```

**Acceptance**: Pure function extracted, 3 unit tests pass.

---

### T027 — [GREEN] Add completed puzzle checkmark overlay to DifficultyCard

**Req**: REQ-S2C-01  
**File**: `ui/components/DifficultySelector.kt`, `ui/screens/HomeScreen.kt`  
**Effort**: ~2h  
**Depends on**: T002 (LazyRow in place), T019  

**Step 1 — DifficultySelector.kt**: Add `completedDifficulties: Set<Difficulty> = emptySet()` parameter
to `DifficultySelector`. Thread it to each `DifficultyCard` as `isCompleted: Boolean`. In `DifficultyCard`,
add a `Box` overlay in the top-right corner showing a "✓" `Text` in `tertiary` color when `isCompleted`:

```kotlin
// Inside the Card, wrap content in a Box:
Box {
    Column(modifier = Modifier.padding(...)) { /* existing label, gridSize, time */ }
    if (isCompleted) {
        Text(
            text = "✓",
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
        )
    }
}
```

**Step 2 — HomeScreen.kt**: Derive the completed set from `state.puzzleStatuses`:
```kotlin
val completedDifficulties = state.puzzleStatuses
    .filter { it.isCompleted }
    .map { it.difficulty }
    .toSet()
```
Pass `completedDifficulties = completedDifficulties` into `DifficultySelector`.

**Acceptance**:
- Completed difficulty cards show a ✓ in the top-right corner
- Uncompleted cards show no indicator
- Completed cards remain tappable
- Completion indicators update when `puzzleStatuses` state changes

---

### T028 — [GREEN] Collapse badge row on short screens (<= 560dp height)

**Req**: REQ-S2C-03  
**File**: `ui/screens/HomeScreen.kt`  
**Effort**: ~2.5h  
**Depends on**: T025 (BadgeDetailBottomSheet available to open from collapse chip)  

In `HomeScreen`, wrap the `BadgeRow(...)` call in a height-adaptive conditional:
```kotlin
val screenHeightDp = LocalConfiguration.current.screenHeightDp
if (screenHeightDp <= 560) {
    AssistChip(
        onClick = { showAllBadges = true },
        label = { Text("Badges") },
        leadingIcon = { Text("\uD83C\uDFC6") }   // trophy emoji
    )
} else {
    BadgeRow(earnedBadges = state.earnedBadges)
}
```

Add `var showAllBadges by remember { mutableStateOf(false) }` state. When `showAllBadges == true`,
open a `BadgeDetailBottomSheet`-style view listing all 4 badges (or reuse the existing `selectedBadge`
mechanism by iterating — simplest approach is showing the bottom sheet for each badge in a `LazyColumn`
inside a new `ModalBottomSheet` for this case). On dismiss, set `showAllBadges = false`.

Add `LocalConfiguration` import.

**Acceptance**:
- Device with height <= 560dp shows a "Badges" chip instead of the badge row
- Tapping the chip reveals all 4 badges (via bottom sheet or inline expansion)
- Device with height > 560dp always shows the full badge row
- Exactly 560dp height threshold shows the chip (boundary inclusive)

---

### T029 — [RED] Write failing test: GridRenderer accepts pulsingCell parameter

**Req**: REQ-S2C-06  
**File**: `app/src/test/kotlin/org/dgeek/sumgrid/ui/GridRendererTest.kt` (extend)  
**Effort**: ~20min  
**Depends on**: T010 (pulsingCell parameter stub added to GridRenderer signature)  

Add source-scan tests asserting `GridRenderer.kt` has the `pulsingCell` parameter and that
`PuzzleScreen.kt` computes the first empty cell and passes it.

**Tests to add**:
```kotlin
@Test
fun gridRenderer_hasPulsingCellParameter() {
    assertTrue("GridRenderer must accept a pulsingCell parameter",
        gridRendererSource.contains("pulsingCell"))
}

@Test
fun puzzleScreen_computesFirstEmptyCell() {
    val puzzleScreenSource = java.io.File(
        "src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt").readText()
    assertTrue("PuzzleScreen must compute firstEmptyCell for pulse animation",
        puzzleScreenSource.contains("firstEmptyCell") || puzzleScreenSource.contains("pulsingCell"))
}
```

**Acceptance**: Tests pass immediately for `pulsingCell` parameter (added in T010 stub); the
`firstEmptyCell` test fails until T030.

---

### T030 — [GREEN] Implement first-empty-cell pulse animation

**Req**: REQ-S2C-06  
**Files**: `ui/components/GridRenderer.kt`, `ui/screens/PuzzleScreen.kt`  
**Effort**: ~3.5h  
**Depends on**: T010 (parameter stub), T012 (layout stable), T029  

**Step 1 — PuzzleScreen.kt**: Compute the first empty editable cell once on puzzle load:
```kotlin
val firstEmptyCellForPulse: Pair<Int, Int>? = remember(currentState.puzzle.id) {
    outer@ for (r in 0 until currentState.puzzle.size) {
        for (c in 0 until currentState.puzzle.size) {
            if (!currentState.puzzle.cells[r][c].isGiven &&
                currentState.displayValueAt(r, c) == 0) {
                return@outer Pair(r, c)
            }
        }
    }
    null
}
val pulsingCell = if (currentState.selectedCell == null) firstEmptyCellForPulse else null
```

Pass `pulsingCell = pulsingCell` to `GridRenderer`.

**Step 2 — GridRenderer.kt**: Add pulse animation using `rememberInfiniteTransition`:
```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "cellPulse")
val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
    label = "pulseAlpha"
)
```

In `drawGrid`, add a new drawing call after the selected-border block:
```kotlin
// Pulsing border on first empty cell (onboarding hint)
if (pulsingCell != null) {
    val (pr, pc) = pulsingCell
    drawRect(
        color = colors.selectedBorder.copy(alpha = pulseAlpha),
        topLeft = Offset(pc * cellSize, pr * cellSize),
        size = Size(cellSize, cellSize),
        style = Stroke(width = 3f)
    )
}
```

Stop the pulse when `pulsingCell == null` (Compose disposes the infinite transition automatically
when the composable stops observing it).

**Acceptance**:
- First empty editable cell pulses with a border animation (alpha 0.3–1.0, 1s period) on puzzle load
- Pulse stops immediately when any cell is tapped (pulsingCell becomes null)
- Pulse does NOT restart when a cell is deselected mid-puzzle
- Given cells are skipped correctly — pulse targets only user-fillable cells
- T029 tests pass (green)

---

## Task Summary

### By Sprint

| Sprint | Tasks | IDs | Effort |
|--------|-------|-----|--------|
| S2A — Critical Layout Fixes | 10 | T001–T010 | ~12h |
| S2B — Layout and Spacing Polish | 11 | T011–T021 | ~16h |
| S2C — UX Enhancements | 9 | T022–T030 | ~20h |
| **Total** | **30** | **T001–T030** | **~48h** |

### By TDD Phase

| Phase | Count | Task IDs |
|-------|-------|----------|
| [RED] | 9 | T001, T004, T007, T009, T011, T015, T016, T024, T029 |
| [GREEN] | 18 | T002, T005, T006, T008, T010, T012, T013, T014, T017, T018, T019, T020, T021, T022, T023, T025, T027, T028, T030 |
| [REFACTOR] | 2 | T003, T026 |

Note: T030 is listed under GREEN as it is the primary implementation; T029 is its RED precursor.
Total count including T030 in GREEN: RED=9, GREEN=19, REFACTOR=2 = 30 tasks.

### By File

| File | Tasks |
|------|-------|
| `ui/components/DifficultySelector.kt` | T002, T003, T019, T027 |
| `ui/screens/HomeScreen.kt` | T005, T006, T017, T018, T021, T022, T027, T028 |
| `ui/components/GridRenderer.kt` | T008, T014, T030 |
| `ui/screens/PuzzleScreen.kt` | T010, T012, T013, T014, T030 |
| `ui/components/NumberPad.kt` | T020, T023 |
| `ui/components/BadgeDetailBottomSheet.kt` (new) | T025, T026 |
| Test files (new/extended) | T001, T004, T007, T009, T011, T015, T016, T021, T022, T024, T026, T029 |

### Cross-Sprint Dependencies

| Task | Depends on | Reason |
|------|-----------|--------|
| T003 | T002 | Cleanup after LazyRow migration |
| T005 | T004 | Implements what T004's tests require |
| T006 | T005 | Modifies BadgeItem — do after T005 to avoid conflicts |
| T008 | T007 | Implements what T007's tests require |
| T010 | T009 | Implements hint + pulsingCell stub |
| T011 | S2A complete | PuzzleScreen layout restructure gated on S2A |
| T012 | T011 | Implements layout restructure |
| T013 | T012 | Timer relocation — do after layout is stable |
| T014 | T012 | Grid padding — layout must be stable |
| T015 | T013 | Verifies timer relocation |
| T019 | T002 | Card styling on LazyRow cards |
| T020 | S2A | Numpad visuals independent but S2A gate enforced |
| T021 | T017, T018 | Spacer audit after abbreviation and color changes |
| T022 | S2A+S2B complete | S2C gate enforced |
| T023 | T020 | Numpad colors stabilized before animation wrapping |
| T025 | T024, T005 | Implements what T024 tests + needs T005's click wiring |
| T026 | T025 | Refactors code created in T025 |
| T027 | T002, T019 | LazyRow + card styling must be in place |
| T028 | T025 | Uses BadgeDetailBottomSheet for collapse chip tap |
| T030 | T010, T012, T029 | Needs param stub, stable layout, and test gate |

### Priority Tasks (highest value / critical path)

| Priority | Task | Reason |
|----------|------|--------|
| P1 — Critical | T002 | Most visible fix (card truncation); unblocks T019, T027 |
| P1 — Critical | T005 | Fixes badge label overflow; unblocks T006, T025, T028 |
| P1 — Critical | T012 | Removes dead space; unblocks T013, T014 |
| P2 — High | T006 | Grayscale badges — high perceived quality improvement |
| P2 — High | T008 | Dark-mode cell tint — affects every puzzle session |
| P2 — High | T017 | Chip abbreviation + zero-streak display (two fixes in one) |
| P3 — Medium | T025 | Badge bottom sheet — core discoverability feature for S2C |
| P3 — Medium | T030 | Pulse animation — onboarding polish, extends T010 stub |

---

## Global Acceptance Checklist

- [ ] T001–T010 all pass (S2A gate)
- [ ] T011–T021 all pass (S2B gate)
- [ ] T022–T030 all pass (S2C gate)
- [ ] No hardcoded hex, rgb, or arbitrary px values in any changed file
- [ ] Existing functionality intact: 5 difficulties, share, undo, streak persistence
- [ ] `design-lint.js` passes on all changed files
- [ ] Dark mode is the primary visual target; no light-mode regression
