# Tasks: SumGrid UI/UX Modernization
# Epic: 002-sumgrid-uiux-modernization-gridrenderer

**Created**: 2026-03-14
**Total tasks**: 28
**TDD workflow**: Each task begins with failing tests [RED], then implementation [GREEN], then cleanup [REFACTOR].

---

## How to read this file

Each task has:
- **ID**: T001–T028, grouped by sprint
- **Feature**: which sprint feature this task belongs to
- **Phase**: [RED] write failing test | [GREEN] implement to pass | [REFACTOR] clean up
- **[P]**: high-priority task (blocks others or high user impact)
- **Tests**: test file and assertion(s) to write first
- **Files**: production files to modify
- **Depends on**: task IDs that must complete first
- **Parallelizable**: whether this task can run concurrently with its sprint peers

---

## Sprint S00 — Fix Technical Debt First

Goal: Unblock all theme work. All S00 tasks are prerequisites for S01 onward.
Estimated effort: ~1 day.

---

### T001 [P] [RED] Write failing tests for GridColors data class

**Feature**: S00-F001 — GridRenderer color refactor
**Sprint**: S00
**Depends on**: none
**Parallelizable**: yes (parallel with T004, T007)

**What to test**:
Write unit tests in `GridRendererTest.kt` that assert:
1. A `GridColors` instance can be constructed with all 9 named color fields.
2. `GridColors.fromTheme()` (a `@Composable` factory) returns non-null colors for each field when called inside a `MaterialTheme` composition with a light color scheme.
3. `GridColors.fromTheme()` returns different values for dark vs light color scheme (e.g., `givenCellBg` changes).
4. The `gridLine` field equals `MaterialTheme.colorScheme.onSurface` in the light scheme.

**Tests file**:
`app/src/test/kotlin/org/dgeek/sumgrid/ui/GridRendererTest.kt` (create new)

**Note**: These tests will fail at compile time because `GridColors` does not yet exist. That is the expected RED state.

**Files to create**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/GridRendererTest.kt`

---

### T002 [P] [GREEN] Implement GridColors data class and extract 9 color constants

**Feature**: S00-F001 — GridRenderer color refactor
**Sprint**: S00
**Depends on**: T001
**Parallelizable**: no (blocks T003, T009, T020)

**What to implement**:
Inside `GridRenderer.kt`:
1. Define `data class GridColors(val givenCellBg: Color, val userCellBg: Color, val gridLine: Color, val selectedBorder: Color, val cellText: Color, val givenText: Color, val sumGreen: Color, val sumRed: Color, val sumGray: Color)`.
2. Add top-level `@Composable fun gridColorsFromTheme(): GridColors` that maps `MaterialTheme.colorScheme.*` to all 9 fields using the mappings from sprint-plan.md (S00-F001 color mappings section). Use `colorScheme.secondary` for `sumGreen` as the interim value (tertiary is added in S02-F001).
3. In the `GridRenderer()` composable body (before `Canvas { }`), call `val colors = gridColorsFromTheme()`.
4. Pass `colors: GridColors` as a new parameter to the private `DrawScope` helper functions `drawGrid`, `drawCell`, and any other private functions that reference the 9 old constants.
5. Replace every reference to `ColorGivenCellBg`, `ColorUserCellBg`, `ColorGridLine`, `ColorSelectedBorder`, `ColorCellText`, `ColorGivenText`, `ColorSumGreen`, `ColorSumRed`, `ColorSumGray` with `colors.givenCellBg`, etc.
6. Delete the 9 `private val Color*` declarations at lines 30–38.

**Constraint**: Do NOT touch any coordinate math — `cellSize`, `gutterFraction`, `n`, `Offset(...)`, `Size(...)`, `strokeWidth` — these are read-only.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt`

---

### T003 [REFACTOR] Verify GridColors passes all existing tests, clean up imports

**Feature**: S00-F001 — GridRenderer color refactor
**Sprint**: S00
**Depends on**: T002
**Parallelizable**: no

**What to do**:
1. Run `./gradlew test` and confirm all 552 tests remain green.
2. Remove any unused `import androidx.compose.ui.graphics.Color` static imports if the 9 constants were the only callers.
3. Ensure `gridColorsFromTheme` is placed before `GridRenderer()` in the file for readability.
4. Add a KDoc comment to `GridColors` explaining each field and its theme mapping.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt`

---

### T004 [P] [RED] Write failing tests for TalkBack semantics on Canvas grid

**Feature**: S00-F002 — TalkBack accessibility semantics
**Sprint**: S00
**Depends on**: none
**Parallelizable**: yes (parallel with T001, T007)

**What to test**:
Write Compose UI tests in `GridAccessibilityTest.kt`:
1. Compose test rule renders `GridRenderer` with a 3x3 puzzle stub where cell (0,0) has value 3 and `isGiven = true`.
2. Assert `composeTestRule.onNodeWithContentDescription("Row 1, Column 1, 3, given").assertExists()`.
3. Assert `composeTestRule.onNodeWithContentDescription("Row 1, Column 2, empty, editable").assertExists()` for an empty user cell.
4. Assert that no visual change occurs — test that the existing screenshot (or a reference bitmap) matches before/after adding semantics.

**Tests file**:
`app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/GridAccessibilityTest.kt` (create new)

**Note**: Tests fail because no semantics nodes exist on the Canvas yet. Expected RED state.

**Files to create**:
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/GridAccessibilityTest.kt`

---

### T005 [P] [GREEN] Implement TalkBack semantics overlay on GridRenderer

**Feature**: S00-F002 — TalkBack accessibility semantics
**Sprint**: S00
**Depends on**: T004 (test exists), T002 (GridColors, to avoid merge conflicts)
**Parallelizable**: no

**What to implement**:
In `GridRenderer.kt`:
1. Wrap the existing `Canvas(...)` in a `Box` using `BoxWithConstraints` to measure available width.
2. Derive `val cellSizeDp: Dp = (maxWidth - outerPaddingDp.dp * 2) / (n + 0.6f)` for overlay positioning.
3. After the `Canvas(...)`, add an invisible overlay using `Layout` or nested `Box` children — one per cell — each with:
   - `Modifier.size(cellSizeDp).offset(x = col * cellSizeDp, y = row * cellSizeDp)`
   - `Modifier.semantics { contentDescription = "Row ${row + 1}, Column ${col + 1}, ${valueStr}, ${givenStr}" }`
   - `Modifier.alpha(0f)` — invisible, no visual impact
4. `valueStr` = cell value as string, or `"empty"` if 0.
5. `givenStr` = `"given"` or `"editable"` based on `cell.isGiven`.
6. Also add outer-Box semantics: `contentDescription = "$n by $n SumGrid puzzle"`.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt`

---

### T006 [REFACTOR] Verify TalkBack overlay has zero visual/perf impact, clean up

**Feature**: S00-F002 — TalkBack accessibility semantics
**Sprint**: S00
**Depends on**: T005
**Parallelizable**: no

**What to do**:
1. Run `./gradlew test` — 552 tests + new accessibility tests must all pass.
2. Verify overlay `Box` nodes use `alpha(0f)` and are not drawn by Compose (they are in the layout tree only).
3. Add a comment block above the overlay loop explaining the approach (Canvas a11y pattern).
4. Extract the content description string format to a private helper `fun cellDescription(row: Int, col: Int, value: Int, isGiven: Boolean): String` to avoid duplication.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt`

---

### T007 [P] [RED] Write failing tests for CelebrationAnimation wiring in PuzzleScreen

**Feature**: S00-F003 — Wire CelebrationAnimation
**Sprint**: S00
**Depends on**: none
**Parallelizable**: yes (parallel with T001, T004)

**What to test**:
Write Compose UI tests in `PuzzleScreenCelebrationTest.kt`:
1. Render `PuzzleScreen` (or a preview-compatible wrapper) with `PuzzleUiState(isCompleted = false, ...)`.
2. Assert that no node with `testTag("celebration_overlay")` exists.
3. Update state to `isCompleted = true`.
4. Assert that a node with `testTag("celebration_overlay")` exists.
5. Assert that `CelebrationCellWrapper` composables are present (by tag or contentDescription) after completion.

**Tests file**:
`app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/PuzzleScreenCelebrationTest.kt` (create new)

**Files to create**:
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/PuzzleScreenCelebrationTest.kt`

---

### T008 [P] [GREEN] Wire rememberCelebrationState + overlay in PuzzleScreen

**Feature**: S00-F003 — Wire CelebrationAnimation
**Sprint**: S00
**Depends on**: T007
**Parallelizable**: no (blocks T025)

**What to implement**:
In `PuzzleScreen.kt`:
1. Add `val celebrationState = rememberCelebrationState(isComplete = currentState.isCompleted, gridSize = currentState.puzzle.size)`.
2. Wrap the `GridRenderer(...)` call in a `Box(modifier = Modifier.fillMaxWidth())`.
3. When `currentState.isCompleted == true`, overlay a grid of `CelebrationCellWrapper` nodes above the `GridRenderer` using `Modifier.zIndex(1f)` on the overlay Box.
4. The overlay Box should carry `Modifier.testTag("celebration_overlay")`.
5. Pass `celebrationState` to the overlay cells correctly.
6. Keep the existing text completion banner as a TalkBack fallback — do not remove it.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt`

---

### T009 [REFACTOR] S00 integration pass — run all tests, fix any regressions

**Feature**: S00 — all features
**Sprint**: S00
**Depends on**: T003, T006, T008
**Parallelizable**: no

**What to do**:
1. Run full test suite `./gradlew test` and `./gradlew connectedAndroidTest`.
2. Confirm test count is 552+ (no existing tests lost, new tests counted).
3. Confirm `GridRenderer.kt` has zero `private val Color*` constants remaining.
4. Confirm `PuzzleScreen.kt` calls `rememberCelebrationState`.
5. Confirm `GridRenderer.kt` has semantics overlay present.
6. Tag commit as `s00-complete` on the epic branch.

**Files to modify**: none (verification task)

---

## Sprint S01 — Quick Wins

Goal: Maximum perceptible polish at near-zero APK cost.
Estimated effort: ~2 days.
Prerequisite: T009 (S00 complete).

---

### T010 [RED] Write failing tests for haptic feedback on PuzzleScreen interactions

**Feature**: S01-F001 — Haptic feedback
**Sprint**: S01
**Depends on**: T009
**Parallelizable**: yes (parallel with T013, T015, T017)

**What to test**:
Write unit tests in `HapticFeedbackTest.kt`:
1. Create a fake `HapticFeedback` implementation that records calls to `performHapticFeedback(type)`.
2. Provide it via `CompositionLocalProvider(LocalHapticFeedback provides fakeHaptic)` when rendering `PuzzleScreen`.
3. Simulate a cell tap — assert `fakeHaptic` received `HapticFeedbackType.TextHandleMove` exactly once.
4. Simulate a number button tap — assert `HapticFeedbackType.TextHandleMove` fired.
5. Simulate the clear button tap — assert `HapticFeedbackType.LongPress` fired.
6. Set `isCompleted = true` in state — assert `HapticFeedbackType.LongPress` fired on completion.

**Tests file**:
`app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/HapticFeedbackTest.kt` (create new)

**Files to create**:
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/HapticFeedbackTest.kt`

---

### T011 [GREEN] Implement haptic feedback in PuzzleScreen and NumberPad

**Feature**: S01-F001 — Haptic feedback
**Sprint**: S01
**Depends on**: T010
**Parallelizable**: no

**What to implement**:
1. In `PuzzleScreen.kt`: obtain `val haptic = LocalHapticFeedback.current`.
2. In the `onCellTap` lambda passed to `GridRenderer`: add `haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)` before or after the existing `viewModel.selectCell(row, col)` call.
3. In the `onNumberTap` lambda passed to `NumberPad`: add `haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)`.
4. In the `onClearTap` lambda passed to `NumberPad`: add `haptic.performHapticFeedback(HapticFeedbackType.LongPress)`.
5. Add a `LaunchedEffect(currentState.isCompleted)` block: when `isCompleted` transitions to `true`, call `haptic.performHapticFeedback(HapticFeedbackType.LongPress)`.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt`

---

### T012 [REFACTOR] Clean up haptic wiring, verify no duplicate firings

**Feature**: S01-F001 — Haptic feedback
**Sprint**: S01
**Depends on**: T011
**Parallelizable**: no

**What to do**:
1. Verify the completion haptic `LaunchedEffect` key is `currentState.isCompleted` (not `Unit`) so it only fires on the `false → true` transition, not every recomposition.
2. Confirm `LocalHapticFeedback.current` is obtained once at the composable top level, not inside lambdas.
3. Run haptic tests — assert zero duplicate firings on re-render.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt`

---

### T013 [RED] Write failing tests for animated screen transitions

**Feature**: S01-F002 — Animated screen transitions
**Sprint**: S01
**Depends on**: T009
**Parallelizable**: yes (parallel with T010, T015, T017)

**What to test**:
Write Compose navigation tests in `NavigationTransitionTest.kt`:
1. Render `SumGridNavHost` with a test `NavController`.
2. Navigate from Home to Puzzle — assert no crash occurs and destination composable is visible after transition.
3. Navigate back — assert Home is visible after pop transition.
4. (Smoke test only — transition animation correctness is verified visually; this test just ensures no exceptions.)

**Tests file**:
`app/src/androidTest/kotlin/org/dgeek/sumgrid/navigation/NavigationTransitionTest.kt` (create new)

**Files to create**:
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/navigation/NavigationTransitionTest.kt`

---

### T014 [GREEN] Implement animated NavHost transitions in SumGridNavigation

**Feature**: S01-F002 — Animated screen transitions
**Sprint**: S01
**Depends on**: T013
**Parallelizable**: no

**What to implement**:
In `SumGridNavigation.kt`:
1. Add `enterTransition`, `exitTransition`, `popEnterTransition`, `popExitTransition` lambda parameters to the existing `NavHost(...)` call (these are available in `androidx.navigation:navigation-compose:2.7+` — no new dependency needed).
2. Forward transition: `fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 4 }`.
3. Exit transition: `fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 4 }`.
4. Pop enter: `fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 4 }`.
5. Pop exit: `fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 4 }`.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/navigation/SumGridNavigation.kt`

---

### T015 [RED] Write failing test for Outfit font in SumGridTypography

**Feature**: S01-F003 — Custom typography
**Sprint**: S01
**Depends on**: T009
**Parallelizable**: yes (parallel with T010, T013, T017)

**What to test**:
Write a unit test in `TypographyTest.kt`:
1. Assert that `SumGridTypography.bodyLarge.fontFamily` is not `FontFamily.Default` (it must be `OutfitFontFamily`).
2. Assert that `SumGridTypography.displayLarge.fontFamily` equals `OutfitFontFamily`.
3. (These tests will fail until `OutfitFontFamily` is defined and wired into `Type.kt`.)

**Tests file**:
`app/src/test/kotlin/org/dgeek/sumgrid/ui/theme/TypographyTest.kt` (create new — the existing `ThemeColorTest.kt` covers colors only)

**Files to create**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/theme/TypographyTest.kt`

---

### T016 [GREEN] Bundle Outfit Variable font and wire into Type.kt

**Feature**: S01-F003 — Custom typography
**Sprint**: S01
**Depends on**: T015
**Parallelizable**: no

**What to implement**:
1. Download `Outfit[wght].ttf` (variable weight font) from Google Fonts (https://fonts.google.com/specimen/Outfit). Target file size: ~100 KB raw.
2. Copy to `app/src/main/res/font/outfit_variable.ttf`.
3. In `Type.kt`, add:
   ```kotlin
   val OutfitFontFamily = FontFamily(Font(R.font.outfit_variable))
   ```
4. In `SumGridTypography` (the `Typography(...)` instantiation), replace every `fontFamily = FontFamily.Default` with `fontFamily = OutfitFontFamily`.
5. Verify APK delta: run `./gradlew assembleRelease` before and after, confirm delta ≤ 110 KB.

**Files to modify**:
- `app/src/main/res/font/outfit_variable.ttf` (new binary asset)
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Type.kt`

---

### T017 [RED] Write failing test for predictive back manifest flag

**Feature**: S01-F004 — Predictive back gesture
**Sprint**: S01
**Depends on**: T009
**Parallelizable**: yes (parallel with T010, T013, T015)

**What to test**:
Write a manifest parsing test in `ManifestFlagsTest.kt`:
1. Parse `AndroidManifest.xml` programmatically (read as text/XML in a unit test).
2. Assert that the `<application>` element contains `android:enableOnBackInvokedCallback="true"`.

**Tests file**:
`app/src/test/kotlin/org/dgeek/sumgrid/ManifestFlagsTest.kt` (create new)

**Files to create**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ManifestFlagsTest.kt`

---

### T018 [GREEN] Enable predictive back gesture in AndroidManifest.xml

**Feature**: S01-F004 — Predictive back gesture
**Sprint**: S01
**Depends on**: T017
**Parallelizable**: no

**What to implement**:
In `AndroidManifest.xml`:
1. Add `android:enableOnBackInvokedCallback="true"` to the `<application>` element.
2. No Kotlin changes required — Compose Navigation 2.7+ automatically handles `OnBackPressedDispatcher` for predictive back when this flag is set.
3. Verify with `./gradlew lint` that no lint warnings arise from this flag on minSdk 24 (the flag is silently ignored on API < 33).

**Files to modify**:
- `app/src/main/AndroidManifest.xml`

---

## Sprint S02 — Visual Identity

Goal: Recognizable premium visual language. Dark mode looks intentional.
Estimated effort: ~3 days.
Prerequisite: T009 (S00 complete), T018 (S01 complete recommended but S02-F003 and S02-F004 can start earlier).

---

### T019 [P] [RED] Write failing tests for expanded Material3 palette tokens

**Feature**: S02-F001 + S02-F005 — Expanded palette + OLED dark mode
**Sprint**: S02
**Depends on**: T009
**Parallelizable**: yes (parallel with T022, T024)

**What to test**:
Extend `ThemeColorTest.kt` (existing file) with new assertions:
1. `LightColorScheme.tertiary` equals `Green30` (`Color(0xFF1B6C2E)`).
2. `LightColorScheme.tertiaryContainer` equals `Green90` (`Color(0xFFA8F5B2)`).
3. `DarkColorScheme.background` equals `NeutralOLED` (`Color(0xFF000000)`).
4. `DarkColorScheme.surface` equals `NeutralOLEDSurface` (`Color(0xFF0D0D0D)`).
5. `DarkColorScheme.tertiary` equals `Green80` (`Color(0xFF6EDB82)`).
6. `LightColorScheme.surfaceVariant` equals `NeutralVariant90` (`Color(0xFFE8DEF8)`).

**Tests file**:
`app/src/test/kotlin/org/dgeek/sumgrid/ui/theme/ThemeColorTest.kt` (extend existing)

**Files to modify**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/theme/ThemeColorTest.kt`

---

### T020 [P] [GREEN] Add expanded palette tokens to Color.kt and update Theme.kt

**Feature**: S02-F001 + S02-F005 — Expanded palette + OLED dark mode
**Sprint**: S02
**Depends on**: T019, T002 (GridColors uses `colorScheme.tertiary` as interim — this finalizes it)
**Parallelizable**: no (blocks T021)

**What to implement**:
In `Color.kt`, add (additive only — no removal):
```kotlin
val Green30  = Color(0xFF1B6C2E)
val Green80  = Color(0xFF6EDB82)
val Green90  = Color(0xFFA8F5B2)
val NeutralVariant50 = Color(0xFF79747E)
val NeutralVariant70 = Color(0xFFAEAAB4)
val NeutralVariant90 = Color(0xFFE8DEF8)
val NeutralOLED = Color(0xFF000000)
val NeutralOLEDSurface = Color(0xFF0D0D0D)
val NeutralOLEDSurface2 = Color(0xFF1A1A1A)
```

In `Theme.kt`, update `DarkColorScheme`:
- `background = NeutralOLED`
- `surface = NeutralOLEDSurface`
- `surfaceVariant = NeutralOLEDSurface2`
- `tertiary = Green80`
- `onTertiary = Neutral10`
- `tertiaryContainer = Green30`
- `onTertiaryContainer = Green90`

In `Theme.kt`, update `LightColorScheme`:
- `tertiary = Green30`
- `onTertiary = Neutral99`
- `tertiaryContainer = Green90`
- `onTertiaryContainer = Green30`
- `surfaceVariant = NeutralVariant90`
- `onSurfaceVariant = NeutralVariant50`

After this task, `GridColors.fromTheme()` will automatically resolve `colorScheme.tertiary` to the correct green (the interim `secondary` mapping from T002 is superseded).

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Color.kt`
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Theme.kt`

---

### T021 [RED] Write failing tests for gradient backgrounds

**Feature**: S02-F002 — Gradient backgrounds
**Sprint**: S02
**Depends on**: T020
**Parallelizable**: yes (parallel with T022, T024 after T020)

**What to test**:
Write Compose rendering tests in `GradientBackgroundTest.kt`:
1. Render `HomeScreen` with a light `MaterialTheme`.
2. Assert that the root `Box` background uses a `Brush` (check that the `Modifier.background(Brush...)` modifier is present using semantics or a custom test rule).
3. Render `PuzzleScreen` and assert the same.
4. (Visual regression: if screenshot testing is available, assert no pixel outside `±5%` tolerance from baseline.)

**Tests file**:
`app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/GradientBackgroundTest.kt` (create new)

**Files to create**:
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/GradientBackgroundTest.kt`

---

### T022 [RED] Write failing tests for splash screen installation

**Feature**: S02-F003 — Splash screen
**Sprint**: S02
**Depends on**: T009
**Parallelizable**: yes (parallel with T019, T024)

**What to test**:
Write a unit test in `SplashScreenTest.kt`:
1. Parse `res/values/themes.xml` and assert that a style named `Theme.SumGrid.Splash` exists.
2. Assert the style has `android:windowSplashScreenBackground` attribute set.
3. Parse `AndroidManifest.xml` and assert `MainActivity` has `android:theme="@style/Theme.SumGrid.Splash"`.
4. Check `MainActivity.kt` source text contains `installSplashScreen()` call.

**Tests file**:
`app/src/test/kotlin/org/dgeek/sumgrid/SplashScreenTest.kt` (create new)

**Files to create**:
- `app/src/test/kotlin/org/dgeek/sumgrid/SplashScreenTest.kt`

---

### T023 [GREEN] Implement gradient backgrounds on HomeScreen and PuzzleScreen

**Feature**: S02-F002 — Gradient backgrounds
**Sprint**: S02
**Depends on**: T021
**Parallelizable**: no

**What to implement**:
In both `HomeScreen.kt` and `PuzzleScreen.kt`:
1. Locate the top-level `Scaffold` content lambda `Column` or `Box`.
2. Wrap content in a `Box` (or apply to existing `Box`) with:
   ```kotlin
   Modifier.background(
       Brush.verticalGradient(
           colors = listOf(
               MaterialTheme.colorScheme.background,
               MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
           )
       )
   ).fillMaxSize()
   ```
3. The gradient is subtle — bottom stop is 40% alpha to avoid obscuring content.
4. No layout restructuring — apply modifier only.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt`
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt`

---

### T024 [GREEN] Implement splash screen with core-splashscreen

**Feature**: S02-F003 — Splash screen
**Sprint**: S02
**Depends on**: T022
**Parallelizable**: yes (parallel with T023)

**What to implement**:
1. In `app/build.gradle.kts`, add inside `dependencies { }`:
   ```kotlin
   implementation("androidx.core:core-splashscreen:1.0.1")
   ```
2. In `app/src/main/res/values/themes.xml`, add:
   ```xml
   <style name="Theme.SumGrid.Splash" parent="Theme.SplashScreen">
       <item name="windowSplashScreenBackground">@color/splash_bg</item>
       <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
       <item name="postSplashScreenTheme">@style/Theme.SumGrid</item>
   </style>
   ```
   Also add `<color name="splash_bg">#1A1B6C</color>` in `res/values/colors.xml` (or existing colors resource).
3. In `AndroidManifest.xml`, on the `<activity android:name=".MainActivity">` element, set `android:theme="@style/Theme.SumGrid.Splash"`.
4. In `MainActivity.kt`, in `onCreate()` before `setContent { }`, add `installSplashScreen()`.
5. Verify APK delta ≤ 25 KB.

**Files to modify**:
- `app/build.gradle.kts`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values/colors.xml` (or create if absent)
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/org/dgeek/sumgrid/MainActivity.kt`

---

### T025 [RED] Write failing tests for animated streak counter

**Feature**: S02-F004 — Animated streak counter
**Sprint**: S02
**Depends on**: T009
**Parallelizable**: yes (parallel with T019, T022 — no dependency on T020)

**What to test**:
Write Compose UI tests in `StreakDisplayTest.kt`:
1. Render the `StreakDisplay` composable (or `HomeScreen` with controllable streak state) with `streak = 3`.
2. Update streak to `4`.
3. Assert that the streak number text scale is > 1.0 during the animation frame (verify `Animatable` fires by checking scale state with a test clock).
4. Assert that after animation settles, scale returns to 1.0.

**Tests file**:
`app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/StreakDisplayTest.kt` (create new)

**Files to create**:
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/StreakDisplayTest.kt`

---

### T026 [GREEN] Implement animated streak counter in HomeScreen

**Feature**: S02-F004 — Animated streak counter
**Sprint**: S02
**Depends on**: T025
**Parallelizable**: no

**What to implement**:
In `HomeScreen.kt`, locate the streak display area (streak number text):
1. Add:
   ```kotlin
   var previousStreak by remember { mutableStateOf(streak) }
   val scale = remember { Animatable(1f) }
   LaunchedEffect(streak) {
       if (streak > previousStreak) {
           scale.animateTo(1.15f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
           scale.animateTo(1f, spring())
       }
       previousStreak = streak
   }
   ```
2. Apply `Modifier.scale(scale.value)` to the streak number `Text` composable.
3. For the flame emoji/icon: add a separate `InfiniteTransition` pulse when `streak > 0`:
   ```kotlin
   val flameScale by infiniteTransition.animateFloat(
       initialValue = 1f, targetValue = 1.08f,
       animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
   )
   ```
4. Apply `Modifier.scale(flameScale)` to the flame icon.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt`

---

## Sprint S03 — Gameplay Polish

Goal: Every interaction feels intentional. Completing a puzzle feels rewarding.
Estimated effort: ~4 days.
Prerequisite: T020 (S02 palette complete — needed for animation colors).

---

### T027 [P] [RED] Write failing tests for Canvas cell selection spring animation

**Feature**: S03-F001 — Cell selection animation
**Sprint**: S03
**Depends on**: T020
**Parallelizable**: yes (parallel with T030)

**What to test**:
Extend `GridRendererTest.kt` with animation tests:
1. Render `GridRenderer` with a test clock (`TestCoroutineScheduler`/`runTest`).
2. Tap a cell to trigger selection.
3. Advance clock by 50ms — assert that `selectedPulse.value` is in range `(1.0f, 1.05f]` (animation in progress).
4. Advance clock by 300ms — assert `selectedPulse.value` is approximately `1.0f` (settled).
5. Assert that the amber glow `drawRect` fill is present in the DrawScope output (verify via a testable `DrawScope` spy or screenshot assertion).

**Tests file**:
`app/src/test/kotlin/org/dgeek/sumgrid/ui/GridRendererTest.kt` (extend existing from T001)

---

### T028 [P] [GREEN] Implement cell selection spring pulse and sum indicator animations

**Feature**: S03-F001 + S03-F002 — Cell selection + sum indicator animations
**Sprint**: S03
**Depends on**: T027
**Parallelizable**: no (combined because both touch DrawScope parameter list)

**What to implement**:
In `GridRenderer.kt`:

**Cell selection pulse (S03-F001)**:
1. Add `val selectedPulse = remember { Animatable(1f) }` in `GridRenderer` composable scope.
2. Add `LaunchedEffect(state.selectedCell) { selectedPulse.animateTo(1.05f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)); selectedPulse.animateTo(1.0f, spring()) }`.
3. Add `selectedPulseScale: Float` parameter to the `drawGrid()` private function.
4. In the selected cell border drawing section, apply `withTransform { scale(selectedPulseScale, pivot = cellCenter) }` around `drawRect(...)`.
5. Add amber glow: `drawRect(color = colors.selectedBorder.copy(alpha = 0.25f), ...)` filled behind the border stroke.

**Sum indicator animations (S03-F002)**:
1. Add `val sumCorrectPulse = remember { Animatable(1f) }` and `val sumOverShake = remember { Animatable(0f) }` in `GridRenderer` composable scope.
2. Add `LaunchedEffect(state.rowSumIndicators, state.colSumIndicators)` to detect GREEN transitions → trigger `sumCorrectPulse` (1.0 → 1.1 → 1.0) and RED transitions → trigger `sumOverShake` shake sequence (0 → 3 → -3 → 3 → 0 px via sequential `animateTo` calls).
3. Pass `sumCorrectPulseScale: Float` and `sumOverShakeOffsetX: Float` as parameters to `drawGrid()`.
4. Apply `withTransform { scale(sumCorrectPulseScale) }` around correct sum label draw.
5. Apply `withTransform { translate(left = sumOverShakeOffsetX) }` around over-sum label draw.

**Frame time constraint**: Verify <12ms by limiting to max 3 active `Animatable` instances at any frame.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt`

---

### T029 [RED] Write failing tests for multi-phase CelebrationAnimation

**Feature**: S03-F003 — Enhanced celebration animation
**Sprint**: S03
**Depends on**: T008 (S00-F003 wiring complete)
**Parallelizable**: yes (parallel with T027)

**What to test**:
Write tests in `CelebrationAnimationTest.kt`:
1. Construct `CelebrationState` — assert it has a `rippleRadius: Animatable<Float>` field.
2. Assert `CelebrationState` has a `particles: List<ConfettiParticle>` field with 30–40 elements.
3. Assert `ConfettiParticle` has fields: `x`, `y`, `vx`, `vy`, `color`, `rotation`, `size`.
4. Assert `CelebrationState` has a `confettiProgress: Animatable<Float>` field.
5. Render `PuzzleScreen` with `isCompleted = true` — advance clock 900ms — assert a node with `testTag("stats_card")` is visible.

**Tests file**:
`app/src/test/kotlin/org/dgeek/sumgrid/ui/CelebrationAnimationTest.kt` (create new)
`app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/PuzzleScreenCelebrationTest.kt` (extend T007 file)

**Files to create/modify**:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/CelebrationAnimationTest.kt`
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/PuzzleScreenCelebrationTest.kt`

---

### T030 [GREEN] Implement multi-phase CelebrationAnimation (ripple + confetti + stats card)

**Feature**: S03-F003 — Enhanced celebration animation
**Sprint**: S03
**Depends on**: T029
**Parallelizable**: no

**What to implement**:

**Phase 1 — Ripple wave (0–600ms)**:
In `CelebrationAnimation.kt`:
1. Add `val rippleRadius: Animatable<Float> = Animatable(0f)` to `CelebrationState`.
2. In `rememberCelebrationState` `LaunchedEffect`, animate `rippleRadius` from 0 to `maxRadius` (half the grid diagonal, passed as a parameter) over 600ms using `tween`.
3. Render ripple in `PuzzleScreen` as a new `Canvas` overlay `Box` (`Modifier.testTag("ripple_overlay")`) drawn above `GridRenderer` when `isCompleted`, using `drawCircle(style = Stroke, alpha = 1f - (radius / maxRadius))`.

**Phase 2 — Confetti particles (200–1200ms)**:
In `CelebrationAnimation.kt`:
1. Add `data class ConfettiParticle(val x: Float, val y: Float, val vx: Float, val vy: Float, val color: Color, val rotation: Float, val size: Float)`.
2. Add `val particles: List<ConfettiParticle>` to `CelebrationState`, initialized with 35 randomly-positioned particles using the grid center as origin. Colors cycle through `primary`, `secondary`, `tertiary` from theme passed as constructor argument.
3. Add `val confettiProgress: Animatable<Float> = Animatable(0f)`.
4. In `rememberCelebrationState`, after a 200ms `delay`, animate `confettiProgress` from 0 to 1 over 1000ms.
5. In `PuzzleScreen`, render confetti in the ripple overlay `Canvas` using `particles.forEach { p -> drawRoundRect(topLeft = Offset(p.x + p.vx * progress, p.y + p.vy * progress), ...) }`.

**Phase 3 — Stats card slide-up (800–1600ms)**:
In `CelebrationAnimation.kt`:
1. Add `@Composable fun StatsCard(elapsedMs: Long, difficulty: String, modifier: Modifier = Modifier)` composable showing "Puzzle complete · $time · $difficulty" with `contentDescription = "Puzzle complete. Time: $time. Difficulty: $difficulty."` for TalkBack.
2. In `PuzzleScreen`, add `AnimatedVisibility(visible = isCompleted, enter = slideInVertically(tween(400, delayMillis = 800)) { it })` wrapping `StatsCard(...)`.
3. Apply `Modifier.testTag("stats_card")` to `StatsCard`.
4. `StatsCard` uses `MaterialTheme.colorScheme.surfaceVariant` background with 12dp rounded corners.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/CelebrationAnimation.kt`
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt`

---

### T031 [GREEN] Implement edge-to-edge status bar coloring in MainActivity

**Feature**: S03-F004 — Edge-to-edge polish
**Sprint**: S03
**Depends on**: T020 (palette tokens needed for `darkTheme` detection)
**Parallelizable**: yes (parallel with T028, T030 — independent file)

**What to implement**:
In `MainActivity.kt`:
1. After `setContent { SumGridTheme(...) { ... } }`, add a `SideEffect` inside the `setContent` lambda to configure status bar icons based on current theme:
   ```kotlin
   val insetsController = WindowCompat.getInsetsController(window, window.decorView)
   insetsController.isAppearanceLightStatusBars = !darkTheme
   ```
   where `darkTheme` is the same boolean used to select `DarkColorScheme` vs `LightColorScheme`.
2. `enableEdgeToEdge()` is already called — do not move or duplicate it.
3. In `HomeScreen.kt` and `PuzzleScreen.kt`: confirm `Scaffold` uses `contentWindowInsets = ScaffoldDefaults.contentWindowInsets` (which handles system bar padding automatically). If either screen overrides `contentWindowInsets`, restore the default.
4. `WindowCompat` is from `androidx.core:core-ktx` already in `build.gradle.kts` — no new dependency needed.
5. API 24 compat: `WindowInsetsControllerCompat.isAppearanceLightStatusBars` works API 21+ via `core-ktx` compat layer.

**Files to modify**:
- `app/src/main/kotlin/org/dgeek/sumgrid/MainActivity.kt`
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt` (verify only — likely no change)
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt` (verify only — likely no change)

---

## Task Summary

| ID | Sprint | Feature | Phase | Priority | Parallelizable |
|----|--------|---------|-------|----------|----------------|
| T001 | S00 | S00-F001 | [RED] | [P] | yes |
| T002 | S00 | S00-F001 | [GREEN] | [P] | no |
| T003 | S00 | S00-F001 | [REFACTOR] | — | no |
| T004 | S00 | S00-F002 | [RED] | [P] | yes |
| T005 | S00 | S00-F002 | [GREEN] | [P] | no |
| T006 | S00 | S00-F002 | [REFACTOR] | — | no |
| T007 | S00 | S00-F003 | [RED] | [P] | yes |
| T008 | S00 | S00-F003 | [GREEN] | [P] | no |
| T009 | S00 | S00-ALL | [REFACTOR] | — | no |
| T010 | S01 | S01-F001 | [RED] | — | yes |
| T011 | S01 | S01-F001 | [GREEN] | — | no |
| T012 | S01 | S01-F001 | [REFACTOR] | — | no |
| T013 | S01 | S01-F002 | [RED] | — | yes |
| T014 | S01 | S01-F002 | [GREEN] | — | no |
| T015 | S01 | S01-F003 | [RED] | — | yes |
| T016 | S01 | S01-F003 | [GREEN] | — | no |
| T017 | S01 | S01-F004 | [RED] | — | yes |
| T018 | S01 | S01-F004 | [GREEN] | — | no |
| T019 | S02 | S02-F001+F005 | [RED] | [P] | yes |
| T020 | S02 | S02-F001+F005 | [GREEN] | [P] | no |
| T021 | S02 | S02-F002 | [RED] | — | yes |
| T022 | S02 | S02-F003 | [RED] | — | yes |
| T023 | S02 | S02-F002 | [GREEN] | — | no |
| T024 | S02 | S02-F003 | [GREEN] | — | yes |
| T025 | S02 | S02-F004 | [RED] | — | yes |
| T026 | S02 | S02-F004 | [GREEN] | — | no |
| T027 | S03 | S03-F001 | [RED] | [P] | yes |
| T028 | S03 | S03-F001+F002 | [GREEN] | [P] | no |
| T029 | S03 | S03-F003 | [RED] | — | yes |
| T030 | S03 | S03-F003 | [GREEN] | — | no |
| T031 | S03 | S03-F004 | [GREEN] | — | yes |

**Total tasks: 31**
**RED tasks: 12** (T001, T004, T007, T010, T013, T015, T017, T019, T021, T022, T025, T027, T029)
**GREEN tasks: 13** (T002, T005, T008, T011, T014, T016, T018, T020, T023, T024, T026, T028, T030, T031)
**REFACTOR tasks: 4** (T003, T006, T009, T012)
**Priority [P] tasks: 10** (T001, T002, T004, T005, T007, T008, T019, T020, T027, T028)

---

## Parallelization map (within sprints)

**S00 start**: T001, T004, T007 can run in parallel (all [RED], independent test files).

**S01 start**: T010, T013, T015, T017 can run in parallel after T009.

**S02 start**: T019, T022, T025 can run in parallel after T009. T021 runs after T020. T023 and T024 can run in parallel after their respective RED tasks.

**S03 start**: T027 and T029 can run in parallel after T020. T031 is independent throughout S03.

---

## File creation checklist (new files only)

Test files to create:
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/GridRendererTest.kt`
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/theme/TypographyTest.kt`
- `app/src/test/kotlin/org/dgeek/sumgrid/ManifestFlagsTest.kt`
- `app/src/test/kotlin/org/dgeek/sumgrid/SplashScreenTest.kt`
- `app/src/test/kotlin/org/dgeek/sumgrid/ui/CelebrationAnimationTest.kt`
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/GridAccessibilityTest.kt`
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/PuzzleScreenCelebrationTest.kt`
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/HapticFeedbackTest.kt`
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/navigation/NavigationTransitionTest.kt`
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/GradientBackgroundTest.kt`
- `app/src/androidTest/kotlin/org/dgeek/sumgrid/ui/StreakDisplayTest.kt`

Asset to add:
- `app/src/main/res/font/outfit_variable.ttf`
