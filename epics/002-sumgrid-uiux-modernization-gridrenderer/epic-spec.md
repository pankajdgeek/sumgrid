# Epic Specification: SumGrid UI/UX Modernization

**Epic ID**: 002-sumgrid-uiux-modernization-gridrenderer
**Epic Branch**: epic/002-sumgrid-uiux-modernization-gridrenderer
**Created**: 2026-03-14
**Status**: In Progress
**Stack**: Kotlin + Jetpack Compose + Material3, Android minSdk 24

---

## Objective

Transform SumGrid from a functional puzzle app into a premium daily ritual by delivering targeted
visual polish, tactile feedback, and accessibility fixes across four sprint-sized increments. Each
sprint is independently releasable. The work begins with mandatory technical-debt elimination
(S00), proceeds through quick wins (S01) and visual identity (S02), and concludes with deep
gameplay polish (S03). The entire epic must stay within the 4.5 MB APK hard cap and maintain
zero regressions against the 552 existing passing tests.

---

## Background

SumGrid has solid puzzle mechanics but a utilitarian UI: default Roboto font, 13-token color
palette, a Canvas-drawn grid with 9 hardcoded color constants and zero TalkBack support, flat
layouts, no micro-interactions, and bare `NavHost` transitions. Competitors such as NYT Games
and Duolingo have established premium minimalism and habit-forming micro-interactions as baseline
expectations. First-time users may uninstall before solving their first puzzle because the app
"looks plain" compared to those alternatives.

The technical root cause is that `GridRenderer.kt` owns 9 private color constants
(`ColorGivenCellBg`, `ColorUserCellBg`, `ColorGridLine`, `ColorSelectedBorder`, `ColorCellText`,
`ColorGivenText`, `ColorSumGreen`, `ColorSumRed`, `ColorSumGray`) that hard-code hex values
instead of referencing `MaterialTheme` tokens. This blocks all subsequent theme work. Additionally,
`CelebrationAnimation.kt` / `CelebrationCellWrapper` exist in the codebase but are completely
unwired — `PuzzleScreen.kt` shows only a static text banner on completion.

---

## Involved Subsystems

- [x] Frontend (Jetpack Compose screens, Canvas components, theme system)
- [ ] Backend
- [ ] Database
- [ ] Infrastructure

---

## Kill Criteria (Immediate Stop)

Any of the following triggers an immediate halt to all sprint work:

| Criterion | Threshold | Action |
|-----------|-----------|--------|
| APK size | Exceeds 4.5 MB | Hard stop; revert last change |
| Frame time | > 12 ms on API 24 emulator | Remove offending animation |
| Accessibility regression | Visual-only state without TalkBack parallel | Revert |
| Test regression | Any of 552 tests turn red | Block merge |
| GridRenderer scope creep | Canvas-to-LazyGrid rewrite required | Defer to separate ticket |
| Time overrun | > 5 dev days before first releasable increment | Ship what exists, stop |

---

## APK Budget Tracking

| Item | Estimated Size | Running Total |
|------|---------------|---------------|
| Baseline (current) | 3.5 MB | 3.5 MB |
| Outfit Variable font (S01-F003) | ~100 KB | ~3.6 MB |
| core-splashscreen library (S02-F003) | ~20 KB | ~3.62 MB |
| All other changes (pure Compose/Canvas) | 0 KB | ~3.62 MB |
| **Hard cap** | — | **4.5 MB** |
| **Remaining headroom** | — | **~880 KB** |

---

## Sprint Overview

| Sprint | Name | Goal | Days | Ship Criteria |
|--------|------|------|------|---------------|
| S00 | Fix Technical Debt First | Unblock theme work; fix accessibility | Day 1 | Theme tokens + TalkBack + celebration wired |
| S01 | Quick Wins | Max polish at zero APK cost | Day 2-3 | App feels noticeably more polished; APK < 4 MB |
| S02 | Visual Identity | Distinctive premium visual language | Day 4-6 | Recognizable identity; dark mode intentional; APK < 4.5 MB |
| S03 | Gameplay Polish | Every interaction feels rewarding | Week 2 | Puzzle completion is rewarding; no jank on API 24 |

---

## S00 — Fix Technical Debt First

### Goal

Unblock all subsequent theme work, fix the critical accessibility gap, and wire the dead
`CelebrationAnimation` code. This sprint is mandatory regardless of whether S01–S03 proceed.

### S00-F001 — GridRenderer Color Refactor to Theme Tokens

**Priority**: P0 — blocks all other theme work
**File**: `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt`

#### Context

`GridRenderer.kt` lines 30–38 define 9 private `val` color constants with hardcoded hex values.
These bypass `MaterialTheme` entirely, meaning the grid ignores light/dark mode and any palette
changes in `Color.kt`.

Current constants and their semantic mapping:

| Constant | Hex | MaterialTheme mapping |
|----------|-----|-----------------------|
| `ColorGivenCellBg` | `0xFFDDE1FF` | `colorScheme.primaryContainer` |
| `ColorUserCellBg` | `0xFFFFFBFF` | `colorScheme.surface` |
| `ColorGridLine` | `0xFF1B1B1F` | `colorScheme.outline` |
| `ColorSelectedBorder` | `0xFFEFC400` | `colorScheme.secondary` (amber) |
| `ColorCellText` | `0xFF1B1B1F` | `colorScheme.onSurface` |
| `ColorGivenText` | `0xFF0001AC` | `colorScheme.primary` |
| `ColorSumGreen` | `0xFF1B6C2E` | `colorScheme.tertiary` (to be added in S02) / semantic green |
| `ColorSumRed` | `0xFFBA1A1A` | `colorScheme.error` |
| `ColorSumGray` | `0xFF49454F` | `colorScheme.onSurfaceVariant` |

#### Implementation Approach

Replace private color constants with parameters on `GridRenderer` composable that receive
`MaterialTheme`-derived values at the call site. The canvas coordinate math — including all
`cellSize`, `gutterFraction`, `aspectRatio`, tap detection math, and text centering calculations
— MUST NOT change.

Signature change (illustrative):

```kotlin
@Composable
fun GridRenderer(
    state: PuzzleUiState,
    onCellTap: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
    outerPaddingDp: Float = 16f,
    // New theme-derived color parameters with MaterialTheme defaults
    givenCellBg: Color = MaterialTheme.colorScheme.primaryContainer,
    userCellBg: Color = MaterialTheme.colorScheme.surface,
    gridLineColor: Color = MaterialTheme.colorScheme.outline,
    selectedBorderColor: Color = MaterialTheme.colorScheme.secondary,
    cellTextColor: Color = MaterialTheme.colorScheme.onSurface,
    givenTextColor: Color = MaterialTheme.colorScheme.primary,
    sumGreenColor: Color = Color(0xFF1B6C2E),   // keep literal until S02 adds tertiary
    sumRedColor: Color = MaterialTheme.colorScheme.error,
    sumGrayColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

The private constants are deleted. `drawGrid` receives the color values via parameters.
`indicatorColor()` becomes a pure function receiving the color triple as parameters.

#### User Story

**Given** SumGrid is running in dark mode
**When** `GridRenderer` renders the puzzle grid
**Then** all cell backgrounds, grid lines, text, sum indicators, and selection borders use
colors derived from the active `MaterialTheme.colorScheme` rather than hardcoded hex values,
so the grid is visually coherent with the rest of the dark-mode UI.

#### Acceptance Criteria

- [ ] All 9 `private val Color(...)` constants are removed from `GridRenderer.kt`
- [ ] `GridRenderer` composable accepts color parameters with `MaterialTheme` defaults
- [ ] Grid renders in both light and dark mode with correct colors (visual inspection)
- [ ] `formatSumLabel` internal function and all canvas coordinate math are unchanged
- [ ] `./gradlew test` passes all 552 tests with no regressions
- [ ] APK size does not increase

---

### S00-F002 — Grid TalkBack Accessibility Semantics

**Priority**: P0 — critical accessibility fix
**File**: `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt`

#### Context

The `Canvas` composable in `GridRenderer` is a single opaque element to the Android accessibility
framework. TalkBack sees a single unlabeled drawing surface — it cannot navigate individual cells,
announce values, or distinguish given cells from editable cells. This excludes all TalkBack users
(approximately 15–20% of users with accessibility needs).

#### Implementation Approach

Add accessibility semantics as an invisible overlay. The `Canvas` modifier chain receives a
`semantics(mergeDescendants = false)` block that describes all grid cells using
`customActions` or a flat list of `SemanticsPropertyReceiver` entries.

Preferred pattern: add a sibling invisible `Layout` or use `Modifier.semantics` on the `Canvas`
itself with a `contentDescription` describing the entire grid summary, plus a second approach
using `AccessibilityNodeInfoCompat` or Compose's `SemanticsNode` per cell via custom semantics.

Minimum viable approach: add `Modifier.semantics` to the `Canvas` with a dynamically generated
`contentDescription` that enumerates all cells in reading order:

```
"Row 1: Column 1, 3, given. Column 2, empty, editable. Column 3, 5, editable selected.
 Row 1 sum: 8, correct.
 Row 2: ..."
```

**Announcement format per cell**: "Row {r+1}, Column {c+1}, {value or 'empty'}, {given or
editable}{, selected}".

**TalkBack live region**: When `state.selectedCell` changes, a `liveRegion` announcement fires
with the newly selected cell's description.

#### User Story

**Given** David uses TalkBack to navigate the puzzle grid
**When** TalkBack focus moves to the grid
**Then** TalkBack announces each cell's row, column number, value (or "empty"), and whether the
cell is given or editable, so David can understand the puzzle state without visual reference.

**Given** David taps a cell
**When** the selection changes
**Then** TalkBack announces "Row N, Column M, {value or empty}, editable, selected" as a live
region update.

#### Acceptance Criteria

- [ ] `GridRenderer` composable has `Modifier.semantics` applied to its `Canvas`
- [ ] Content description enumerates all cells in reading order with row, column, value, and state
- [ ] A live region or `SemanticsActions.ScrollBy` equivalent fires when selected cell changes
- [ ] No visual change to the grid (semantics are invisible overlay only)
- [ ] `./gradlew test` passes all 552 tests with no regressions
- [ ] Manual TalkBack verification: navigate grid, confirm cell announcements

---

### S00-F003 — Wire CelebrationAnimation to PuzzleScreen

**Priority**: P1
**Files**: `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt`,
`app/src/main/kotlin/org/dgeek/sumgrid/ui/components/CelebrationAnimation.kt`

#### Context

`CelebrationAnimation.kt` contains a fully implemented `CelebrationState`, `rememberCelebrationState`,
and `CelebrationCellWrapper`. The animation: staggered cell scale-pulse (1.0→1.2→1.0, 300ms per
cell, 50ms stagger), color wash from surface to `MaterialTheme.colorScheme.tertiary`. However,
`PuzzleScreen.kt` does not import or call any of these — it shows only:

```kotlin
if (currentState.isCompleted) {
    Text(text = "Puzzle complete! 🎉", ...)
}
```

`GridRenderer` is a monolithic Canvas composable; `CelebrationCellWrapper` is a Compose Box
wrapper designed to wrap individual Compose cells. Since GridRenderer is Canvas-based, the
celebration must be overlaid differently — a full-Canvas overlay or a Box overlay on top of the
Canvas, NOT by wrapping individual cells (which do not exist as Compose nodes).

#### Implementation Approach

Add a `Box` overlay on top of `GridRenderer` that renders when `isCompleted == true`:

```kotlin
Box(modifier = Modifier.fillMaxWidth()) {
    GridRenderer(...)
    if (currentState.isCompleted) {
        CelebrationOverlay(
            gridSize = currentState.puzzle.size,
            modifier = Modifier.matchParentSize()
        )
    }
}
```

`CelebrationOverlay` is a new `@Composable` that uses `rememberCelebrationState` and draws the
animation using the existing state machinery, adapted to work as a Canvas overlay rather than
cell-wrapping. The `CelebrationCellWrapper` API remains as-is for potential future use with
non-Canvas grids.

#### User Story

**Given** Priya completes a SumGrid puzzle
**When** the last cell is filled and all row/column sums are correct
**Then** a celebration animation plays over the grid — cells pulse with scale and the grid washes
to a success color — before settling on the completion state, giving Priya a visceral reward for
completing the puzzle.

#### Acceptance Criteria

- [ ] `PuzzleScreen` calls `rememberCelebrationState(isComplete = currentState.isCompleted, ...)`
- [ ] When `isCompleted` transitions from false to true, the animation plays over the grid
- [ ] Animation respects `ANIMATOR_DURATION_SCALE` (reads system animation scale)
- [ ] Static "Puzzle complete!" text banner remains visible after animation completes
- [ ] `CelebrationAnimation.kt` API is unchanged (no breaking changes to existing code)
- [ ] `./gradlew test` passes all 552 tests with no regressions

---

## S01 — Quick Wins

### Goal

Maximum visual impact at zero APK cost. All four features use Compose built-in APIs or a single
small font bundle. Combined dev time: 1–2 days. APK must stay under 4 MB after this sprint.

### S01-F001 — Haptic Feedback

**Priority**: P1
**Files**: `PuzzleScreen.kt`, `GridRenderer.kt` (tap handler), `NumberPad.kt`

#### Context

`PuzzleScreen` routes cell taps via `vm.selectCell(row, col)` and number entry via `vm.enterNumber(number)`.
`NumberPad` handles its own button clicks. `vm.clearCell()` is called from `NumberPad`. None of these
interactions produce haptic feedback. The Compose built-in `LocalHapticFeedback` API is available
at all API levels (including minSdk 24) at zero APK cost.

#### Implementation Approach

Retrieve `LocalHapticFeedback.current` at the composable level and call
`hapticFeedback.performHapticFeedback(HapticFeedbackType.*)` at each interaction point:

| Interaction | Haptic type |
|-------------|------------|
| Cell tap (selects a cell) | `LongPress` (soft click) |
| Number entry (digit button) | `TextHandleMove` or `LongPress` |
| Clear action | `LongPress` |
| Puzzle completion | `LongPress` (repeated 2×, 50ms apart, for emphasis) |

#### User Story

**Given** a user taps a cell, enters a number, taps clear, or completes a puzzle
**When** the interaction is registered
**Then** the device produces appropriate haptic feedback, making each action feel tangible and
confirming the input was received.

#### Acceptance Criteria

- [ ] `LocalHapticFeedback.current` is retrieved in `PuzzleScreen` / `NumberPad`
- [ ] Haptic fires on: cell tap, number entry, clear tap, puzzle completion
- [ ] Haptic type is appropriate (not jarring on repeated rapid taps)
- [ ] No APK size increase
- [ ] `./gradlew test` passes all 552 tests

---

### S01-F002 — Animated Screen Transitions

**Priority**: P1
**File**: `app/src/main/kotlin/org/dgeek/sumgrid/navigation/SumGridNavigation.kt`

#### Context

`SumGridNavigation.kt` uses bare `NavHost` (line 97). All screen transitions are instant cuts —
no enter or exit animations. Compose Navigation's `AnimatedNavHost` (from
`androidx.navigation:navigation-compose`) is a drop-in replacement that accepts
`enterTransition`, `exitTransition`, `popEnterTransition`, `popExitTransition` lambdas.

#### Implementation Approach

Replace `NavHost` with the navigation-compose `AnimatedNavHost` equivalent (note: as of Compose
Navigation 2.7+, `NavHost` itself supports animation parameters — verify current API). Define
transitions:

```kotlin
NavHost(
    navController = navController,
    startDestination = resolvedStart,
    modifier = modifier,
    enterTransition = {
        fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 }
    },
    exitTransition = {
        fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 }
    },
    popEnterTransition = {
        fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 }
    },
    popExitTransition = {
        fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 4 }
    }
)
```

Transitions must respect `ANIMATOR_DURATION_SCALE`. If the scale is 0, transitions are instant.

#### User Story

**Given** a user taps "Play" to navigate to the puzzle screen
**When** the navigation transition occurs
**Then** the Home screen fades out and slides left while the Puzzle screen fades in and slides
in from the right, giving the navigation a smooth, polished feel consistent with premium apps.

**Given** a user presses back from the puzzle screen
**When** the back navigation occurs
**Then** the reverse transition plays (puzzle slides right and fades out, home slides in from left).

#### Acceptance Criteria

- [ ] `NavHost` in `SumGridNavigation.kt` uses enter/exit transition parameters
- [ ] Home→Puzzle: fade + slide right (enter), fade + slide left (exit)
- [ ] Puzzle→Home (back): fade + slide left (enter), fade + slide right (exit)
- [ ] Transitions are <= 300ms
- [ ] Transitions respect `ANIMATOR_DURATION_SCALE = 0` (become instant)
- [ ] No APK size increase
- [ ] `./gradlew test` passes all 552 tests

---

### S01-F003 — Custom Typography — Outfit Variable Font

**Priority**: P1
**File**: `app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Type.kt`
**Asset**: `app/src/main/res/font/outfit_variable.ttf` (or `.otf`)

#### Context

`Type.kt` uses `FontFamily.Default` (system Roboto) for all `TextStyle` definitions. Outfit is a
geometric sans-serif variable font available from Google Fonts (OFL license). The variable font
file covers all weights in a single ~100 KB file, fitting comfortably within the ~880 KB APK
headroom.

#### Implementation Approach

1. Download `Outfit[wght].ttf` (variable font) from Google Fonts.
2. Place at `app/src/main/res/font/outfit_variable.ttf`.
3. In `Type.kt`, declare the font family:

```kotlin
private val OutfitFont = FontFamily(
    Font(R.font.outfit_variable, weight = FontWeight.W100, style = FontStyle.Normal),
    // Variable fonts: a single entry covers all weights
)
```

4. Replace all `FontFamily.Default` references in `SumGridTypography` with `OutfitFont`.

#### User Story

**Given** a user opens SumGrid for the first time after the update
**When** the home screen renders
**Then** the app title "SumGrid", body text, and all UI labels use the Outfit geometric sans-serif
font instead of system Roboto, giving the app a distinctive, premium typographic personality.

#### Acceptance Criteria

- [ ] `outfit_variable.ttf` (or equivalent) is present in `res/font/`
- [ ] `Type.kt` declares `OutfitFont` using `FontFamily(Font(R.font.outfit_variable, ...))`
- [ ] All 8 `TextStyle` entries in `SumGridTypography` reference `OutfitFont` (no `FontFamily.Default` remaining)
- [ ] APK growth <= 110 KB (font file size)
- [ ] No text overflow or layout issues on any existing screen (visual inspection)
- [ ] `./gradlew test` passes all 552 tests

---

### S01-F004 — Predictive Back Gesture

**Priority**: P2
**File**: `app/src/main/AndroidManifest.xml`, `SumGridNavigation.kt`

#### Context

Predictive back (Android 13+ API 33) allows users to preview the destination before committing
to the back gesture. It is enabled app-wide via `android:enableOnBackInvokedCallback="true"` in
the `<application>` tag. Compose Navigation 2.6+ handles the back gesture hook automatically
once enabled in the manifest. On API < 33, the attribute is silently ignored by the platform.

#### Implementation Approach

1. Add to `AndroidManifest.xml` `<application>` tag:
   ```xml
   android:enableOnBackInvokedCallback="true"
   ```
2. Verify Compose Navigation version supports predictive back (2.6+ required; check
   `build.gradle.kts`).
3. No additional Compose code changes are required if using standard `NavHost` back stack
   management. If `BackHandler` is used anywhere, it must be updated to use the `OnBackPressedDispatcher`
   predictive-back APIs.

#### User Story

**Given** a user is viewing the puzzle screen on Android 13+
**When** they perform a back swipe gesture slowly
**Then** the system shows a peek-preview of the Home screen behind the puzzle screen, confirming
where the back gesture will navigate, before the user commits to the swipe.

#### Acceptance Criteria

- [ ] `android:enableOnBackInvokedCallback="true"` is present in `AndroidManifest.xml`
- [ ] Back navigation from Puzzle screen works correctly on API 24 (no regression)
- [ ] Back navigation from Puzzle screen shows predictive preview on API 33+ (manual verification)
- [ ] No `BackHandler` usages broken by the change
- [ ] `./gradlew test` passes all 552 tests
- [ ] APK size does not increase

---

## S02 — Visual Identity

### Goal

Establish a distinctive premium visual language for SumGrid. After this sprint, the app has a
recognizable identity distinct from generic Material3. Dark mode looks intentional, not automated.

### S02-F001 — Expanded Tonal Material3 Palette

**Priority**: P1 — prerequisite for S02-F002 and S02-F005
**Files**: `app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Color.kt`,
`app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Theme.kt`

#### Context

Current `Color.kt` defines 13 color values across Indigo, Amber, Neutral, and Error palettes.
`Theme.kt` defines `LightColorScheme` and `DarkColorScheme` with approximately 13 filled slots
out of Material3's ~30 token slots. Missing: `tertiary`/`onTertiary`/`tertiaryContainer`/
`onTertiaryContainer`, `surfaceVariant`/`onSurfaceVariant`, `surfaceTint`, `outline`/`outlineVariant`,
and `inverseSurface`/`inverseOnSurface`/`inversePrimary`.

`HomeScreen.kt` already references `colorScheme.tertiaryContainer` and `colorScheme.onTertiaryContainer`
in `PuzzleStatusChip` — these currently fall back to Material3 defaults rather than the SumGrid
palette.

#### Implementation Approach

Extend `Color.kt` with the following additional values:

```
// Tertiary — semantic success green (for correct sum indicators and completion chips)
val Green30  = Color(0xFF1B6C2E)
val Green80  = Color(0xFF65D278)
val Green90  = Color(0xFFB8F5C4)
val Green10  = Color(0xFF003912)

// Surface variants
val NeutralVariant30 = Color(0xFF46464F)
val NeutralVariant50 = Color(0xFF77767F)
val NeutralVariant80 = Color(0xFFC8C5D0)
val NeutralVariant90 = Color(0xFFE4E1EC)

// Outline
val Outline30    = Color(0xFF777680)
val OutlineVariant80 = Color(0xFFC8C5D0)
```

Update `LightColorScheme` and `DarkColorScheme` in `Theme.kt` to fill the new slots:

| Token | Light | Dark |
|-------|-------|------|
| `tertiary` | `Green30` | `Green80` |
| `onTertiary` | white (`Neutral99`) | `Green10` |
| `tertiaryContainer` | `Green90` | `Green30` |
| `onTertiaryContainer` | `Green10` | `Green90` |
| `surfaceVariant` | `NeutralVariant90` | `NeutralVariant30` |
| `onSurfaceVariant` | `NeutralVariant30` | `NeutralVariant80` |
| `outline` | `Outline30` | `NeutralVariant50` |
| `outlineVariant` | `NeutralVariant80` | `NeutralVariant30` |

The `ColorSumGreen` in `GridRenderer.kt` can be updated (as part of S02 or by threading
`colorScheme.tertiary` through the color parameter added in S00-F001) to use the palette-aligned green.

#### User Story

**Given** a user opens SumGrid in light mode
**When** the puzzle status chips render on the Home screen
**Then** completed difficulty chips show in the SumGrid palette green (`tertiaryContainer`) rather
than the Material3 default teal, so the color reinforces the "correct / success" semantic consistently
with the sum indicator colors.

#### Acceptance Criteria

- [ ] `Color.kt` defines Green, NeutralVariant, and Outline palette values as listed
- [ ] `LightColorScheme` and `DarkColorScheme` fill `tertiary`, `surfaceVariant`, `outline`, and `outlineVariant` tokens
- [ ] `PuzzleStatusChip` completed state shows green derived from `tertiaryContainer`
- [ ] No visual regression on HomeScreen, PuzzleScreen, or OnboardingScreen (light and dark mode)
- [ ] `./gradlew test` passes all 552 tests
- [ ] APK size does not increase

---

### S02-F002 — Gradient Backgrounds

**Priority**: P2
**Files**: `HomeScreen.kt`, `PuzzleScreen.kt`

#### Context

Both `HomeScreen` and `PuzzleScreen` use flat `Scaffold` with `MaterialTheme.colorScheme.background`
as the surface. A subtle vertical gradient (top-of-screen slightly lighter/tinted vs. bottom
neutral) gives the screens depth without changing the content layout. Uses `Brush.verticalGradient`
from Compose `ui-graphics`, which is already a transitive dependency — zero APK cost.

#### Implementation Approach

In both screens, replace the `Scaffold` `containerColor` or add a gradient background modifier
to the root `Column`:

```kotlin
val gradientBrush = Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
        MaterialTheme.colorScheme.background
    )
)

Column(
    modifier = Modifier
        .fillMaxSize()
        .background(gradientBrush)
        .padding(innerPadding)
)
```

The gradient is subtle (alpha 0.10–0.20 on the tint color) to avoid overwhelming the content.
Dark mode: gradient uses `surface` to `background` with a slight primary tint at top.

#### User Story

**Given** a user opens the Home screen
**When** the screen renders
**Then** the background has a subtle vertical gradient from a faint indigo-tinted top to a neutral
background at the bottom, giving the screen gentle depth without distracting from the puzzle content.

#### Acceptance Criteria

- [ ] `HomeScreen` root column has a `Brush.verticalGradient` background modifier
- [ ] `PuzzleScreen` root column has a `Brush.verticalGradient` background modifier
- [ ] Gradient uses `MaterialTheme.colorScheme` tokens only (no hardcoded colors)
- [ ] Gradient is visually subtle (tint alpha <= 0.20)
- [ ] Both light and dark mode gradients look correct (dark mode does not produce washed-out background)
- [ ] No layout shifts or padding changes
- [ ] No APK size increase
- [ ] `./gradlew test` passes all 552 tests

---

### S02-F003 — Splash Screen

**Priority**: P2
**Files**: `AndroidManifest.xml`, `app/build.gradle.kts`, `res/values/themes.xml`,
`app/src/main/kotlin/org/dgeek/sumgrid/MainActivity.kt` (or equivalent)

#### Context

SumGrid currently uses a legacy `windowBackground` approach (or the default system splash). The
`androidx.core:core-splashscreen` library (~20 KB) provides the modern Android 12 splash screen
API with backward compat to API 23. It shows the app icon centered on a background color during
the cold start window, eliminating the white flash.

#### Implementation Approach

1. Add dependency to `app/build.gradle.kts`:
   ```kotlin
   implementation("androidx.core:core-splashscreen:1.0.1")
   ```
2. Create / update `res/values/themes.xml`:
   ```xml
   <style name="Theme.SumGrid.Starting" parent="Theme.SplashScreen">
       <item name="windowSplashScreenBackground">@color/splash_background</item>
       <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
       <item name="postSplashScreenTheme">@style/Theme.SumGrid</item>
   </style>
   ```
3. In `AndroidManifest.xml`, set `android:theme="@style/Theme.SumGrid.Starting"` on the launcher
   `Activity`.
4. In `MainActivity.onCreate()` (before `setContent`):
   ```kotlin
   installSplashScreen()
   ```

#### User Story

**Given** a user cold-starts SumGrid
**When** the app launches
**Then** a branded splash screen with the SumGrid icon on an indigo background is shown during
the initialization window, replacing the blank white system screen and creating a polished first impression.

#### Acceptance Criteria

- [ ] `core-splashscreen:1.0.1` (or latest stable) is in `app/build.gradle.kts`
- [ ] `Theme.SumGrid.Starting` parent is `Theme.SplashScreen` with correct icon and background color
- [ ] `installSplashScreen()` is called in `MainActivity.onCreate()` before `setContent`
- [ ] Splash screen background uses `Indigo40` (light) / `Indigo10` (dark) or the brand indigo
- [ ] No white flash on cold start (verified on API 24 emulator)
- [ ] APK growth <= 30 KB
- [ ] `./gradlew test` passes all 552 tests

---

### S02-F004 — Animated Streak Counter

**Priority**: P2
**File**: `HomeScreen.kt` (`StreakDisplay` private composable)

#### Context

`StreakDisplay` in `HomeScreen.kt` renders a `Surface` with a fire emoji and streak count. It is
a static display — the number does not animate when it changes. Compose `Animatable` and
`animateIntAsState` can add a scale-up on value change and a pulse on the flame emoji at zero
APK cost.

#### Implementation Approach

```kotlin
@Composable
private fun StreakDisplay(streak: Int, ...) {
    val animatedStreak by animateIntAsState(targetValue = streak, label = "streak")
    val scale by animateFloatAsState(
        targetValue = if (streak > 0) 1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "streakScale"
    )
    // Apply scale modifier to the streak number text
    // Flame icon: use InfiniteTransition for a gentle pulse when streak > 0
}
```

The flame emoji pulses with a subtle scale cycle (1.0→1.1→1.0, 2s period) when `streak > 0`.
The streak number scales up with a spring bounce when the value increments.

Animation must respect `ANIMATOR_DURATION_SCALE` (0 = no animation).

#### User Story

**Given** Marcus opens SumGrid and sees his 47-day streak
**When** the Home screen loads
**Then** the flame emoji gently pulses and the streak number appears with a scale-in spring bounce,
reinforcing the emotional weight of his streak without being distracting.

#### Acceptance Criteria

- [ ] Streak number uses `animateIntAsState` or `animateFloatAsState` for scale-in on change
- [ ] Flame emoji has a gentle infinite pulse animation (scale or alpha oscillation)
- [ ] Animations respect `ANIMATOR_DURATION_SCALE` (set to 0 → no animation)
- [ ] Animation does not replay on every recomposition (only on value change)
- [ ] No APK size increase
- [ ] `./gradlew test` passes all 552 tests

---

### S02-F005 — Dark Mode OLED Polish

**Priority**: P2 (depends on S02-F001)
**Files**: `Color.kt`, `Theme.kt`

#### Context

The current `DarkColorScheme` uses `Neutral10` (`0xFF1B1B1F`) for both `background` and `surface`.
This is a very dark gray, not true black. On OLED displays, true black (`0xFF000000`) saves
battery and provides higher contrast. Additionally, elevated surfaces in dark mode should use
Material3 tonal elevation (a surface tint color added at a specified elevation) rather than
plain transparency, making cards and dialogs visually distinct on OLED.

#### Implementation Approach

Introduce OLED-specific dark values in `Color.kt`:

```kotlin
val OledBlack   = Color(0xFF000000)
val OledSurface = Color(0xFF0D0D10)  // near-black with subtle blue tint
```

Update `DarkColorScheme`:
- `background = OledBlack`
- `surface = OledSurface`
- Grid line luminosity: update `GridRenderer`'s `gridLineColor` parameter default for dark mode
  to use `colorScheme.outlineVariant` (slightly lighter than `outline` to be visible on OLED black)

#### User Story

**Given** David uses SumGrid in dark mode on his OLED phone with TalkBack
**When** the app is open
**Then** the app background is true black and the grid surface is near-black with a subtle indigo
tint, conserving battery on his OLED screen and providing high contrast between content and background.

#### Acceptance Criteria

- [ ] `DarkColorScheme.background` is OLED black or near-black (luminance <= 0.01)
- [ ] `DarkColorScheme.surface` is near-black with a subtle brand tint
- [ ] Grid lines in dark mode are visible but not glaring (use `outlineVariant` color token)
- [ ] Elevated surfaces (e.g., `StreakDisplay Surface`) show tonal elevation tint, not flat dark
- [ ] Light mode colors are UNCHANGED by this modification
- [ ] `./gradlew test` passes all 552 tests

---

## S03 — Gameplay Polish

### Goal

Make every puzzle interaction feel intentional and rewarding. Cell selection feels immediate.
Sum indicators communicate feedback viscerally. Puzzle completion is a memorable moment.
All animations run at < 12 ms frame time on API 24 and respect `ANIMATOR_DURATION_SCALE`.

### S03-F001 — Cell Selection Animation

**Priority**: P1 (depends on S00-F001)
**File**: `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt`

#### Context

Currently, cell selection shows a static amber `Stroke(width = 4f)` border drawn in `drawGrid`.
There is no animation when a cell is selected or deselected. Adding a spring-physics scale pulse
inside the `DrawScope` requires reading an `Animatable<Float>` value that is driven by a
`LaunchedEffect` keyed to the selected cell.

#### Implementation Approach

In `GridRenderer` composable scope (before the `Canvas` block):

```kotlin
val selectionScale = remember { Animatable(1f) }
LaunchedEffect(state.selectedCell) {
    if (state.selectedCell != null) {
        selectionScale.snapTo(1f)
        selectionScale.animateTo(
            targetValue = 1.05f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        selectionScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
}
```

In `drawGrid`, the selected cell rectangle is drawn scaled about its center using `withTransform`:

```kotlin
if (isSelected) {
    val scale = selectionScale  // pass as parameter
    val cx = c * cellSize + cellSize / 2f
    val cy = r * cellSize + cellSize / 2f
    withTransform({
        scale(scale, scale, Offset(cx, cy))
    }) {
        drawRect(color = selectedBorderColor, ..., style = Stroke(width = 4f))
    }
}
```

Total animation duration: ~200ms. Canvas coordinate math outside the selected cell's `withTransform`
block is unchanged.

#### User Story

**Given** a user taps a cell in the puzzle grid
**When** the cell becomes selected
**Then** the amber border on the cell briefly pulses outward with a spring bounce (scale 1.0→1.05→1.0
over ~200ms) before settling, providing kinesthetic confirmation that the tap registered.

#### Acceptance Criteria

- [ ] `GridRenderer` has an `Animatable<Float>` keyed to `state.selectedCell`
- [ ] On selection change, the border animates from scale 1.0 to 1.05 back to 1.0 with spring physics
- [ ] Total animation duration is ≤ 200ms
- [ ] Frame time stays < 12ms on API 24 emulator (measure with GPU profiler)
- [ ] Animation respects `ANIMATOR_DURATION_SCALE = 0` (instant selection, no animation)
- [ ] Canvas coordinate math for unselected cells is unchanged
- [ ] `./gradlew test` passes all 552 tests

---

### S03-F002 — Sum Indicator Animations

**Priority**: P2 (depends on S03-F001)
**File**: `GridRenderer.kt` (drawGrid → row/col sum label drawing)

#### Context

Sum labels are drawn via `drawCenteredText` with a static color (green/red/gray). When the sum
state transitions to GREEN (correct) or RED (over), there is no animation — the color simply
changes on recomposition. Adding feedback: correct sum gets a green pulse + checkmark scale-in;
over sum gets a horizontal shake (3px × 3 cycles = 300ms).

#### Implementation Approach

Two `Animatable` instances per sum axis are impractical (6–8 for a 4×4 grid). Instead, use a
single `Animatable<Float>` keyed to the composite sum state, restarting on transition:

```kotlin
// Per-row correction animations (map: row -> Animatable)
val rowPulseAnimatables = remember(n) { List(n) { Animatable(0f) } }
val rowShakeAnimatables = remember(n) { List(n) { Animatable(0f) } }

// LaunchedEffect per row, keyed to indicator state
for (r in 0 until n) {
    LaunchedEffect(state.rowSumIndicators[r]) {
        when (state.rowSumIndicators[r]) {
            SumIndicatorColor.GREEN -> {
                rowPulseAnimatables[r].snapTo(0f)
                rowPulseAnimatables[r].animateTo(1f, tween(200))
                rowPulseAnimatables[r].animateTo(0f, tween(300))
            }
            SumIndicatorColor.RED -> {
                rowShakeAnimatables[r].snapTo(0f)
                // 3-cycle shake: -3px → +3px → 0
                rowShakeAnimatables[r].animateTo(3f, tween(50))
                rowShakeAnimatables[r].animateTo(-3f, tween(100))
                rowShakeAnimatables[r].animateTo(3f, tween(100))
                rowShakeAnimatables[r].animateTo(0f, tween(50))
            }
            else -> { /* no animation */ }
        }
    }
}
```

The `drawCenteredText` call for the sum label receives a `translationX = rowShakeAnimatables[r].value`
offset and a `scale = 1f + rowPulseAnimatables[r].value * 0.1f` transform applied via `withTransform`.

#### User Story

**Given** Marcus fills in the last number completing row 2
**When** row 2's sum becomes correct
**Then** the row 2 sum label briefly pulses green (scale up then down) and a checkmark appears,
giving a satisfying micro-confirmation that the row is complete.

**Given** a user enters a number that causes row 3's sum to exceed the target
**When** row 3's sum transitions to the "over" state
**Then** the row 3 sum label shakes horizontally (3px, 3 cycles, 300ms), visually communicating
that the sum is wrong without requiring the user to read the red color alone.

#### Acceptance Criteria

- [ ] Correct sum transition triggers green pulse animation on the sum label (scale-up via `withTransform`)
- [ ] Over sum transition triggers horizontal shake animation on the sum label
- [ ] Animations complete within: pulse ≤ 500ms, shake ≤ 300ms
- [ ] Same animations apply to column sum labels
- [ ] Frame time < 12ms on API 24 emulator during animation
- [ ] Animations respect `ANIMATOR_DURATION_SCALE = 0` (instant state change)
- [ ] Existing `formatSumLabel` and `indicatorColor` logic is unchanged
- [ ] `./gradlew test` passes all 552 tests

---

### S03-F003 — Enhanced Celebration Animation

**Priority**: P2 (depends on S00-F003)
**File**: `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/CelebrationAnimation.kt`,
`PuzzleScreen.kt`

#### Context

S00-F003 wires the existing `CelebrationCellWrapper` animation (staggered cell scale-pulse).
S03-F003 upgrades that to a multi-phase celebration:

**Phase 1 — Ripple Wave** (0–400ms): A translucent circle expands from the grid center, reaching
the grid edges by 400ms. Drawn with `drawCircle` using `Brush.radialGradient` from `colorScheme.tertiary`
alpha 0.4 → 0. Implemented in a Canvas overlay composable.

**Phase 2 — Canvas Confetti Particles** (0–1200ms): 30–50 small colored rectangles (2×6dp) are
emitted from the grid center with randomized velocity vectors. Each particle: initial velocity
(random angle, random speed), gravity deceleration, fade out from alpha 1.0 → 0 over 1200ms.
All drawn on Canvas via `drawRect`. NO Lottie — evaluate Canvas sufficiency first (it is sufficient
for this design). APK impact: 0 bytes.

**Phase 3 — Stats Card Slide-Up** (600ms–1200ms): A Material3 `Card` with completion stats
(elapsed time, difficulty) slides up from the bottom of the screen via `animateFloatAsState`
offset, overlaying the grid. This replaces the current static "Puzzle complete! 🎉" text.

#### Implementation Approach

`CelebrationAnimation.kt` gains two new top-level composables:

```kotlin
@Composable
fun RippleOverlay(isComplete: Boolean, gridBounds: Rect, modifier: Modifier)

@Composable  
fun ConfettiOverlay(isComplete: Boolean, modifier: Modifier)
```

`PuzzleScreen.kt` arranges them in a `Box` stack above the grid:

```kotlin
Box(modifier = Modifier.fillMaxWidth()) {
    GridRenderer(...)
    if (currentState.isCompleted) {
        RippleOverlay(isComplete = true, ...)
        ConfettiOverlay(isComplete = true, ...)
    }
}
// Stats card is positioned outside the box, below the grid, with slide-up offset
```

Confetti particle data class:

```kotlin
data class Particle(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val color: Color,
    val rotation: Float
)
```

Particles are generated once (`remember`) when `isComplete` becomes true, then animated via
a single `Animatable<Float>` from 0 to 1 over 1200ms using `withFrameNanos` or
`animateTo(1f, tween(1200))`. Each particle's position at time `t` is calculated deterministically
from its initial state.

#### User Story

**Given** Priya solves a SumGrid puzzle for the first time
**When** the last correct number is entered
**Then** she sees a three-phase celebration: first a ripple wave expands from the center of the
grid, then colorful confetti particles burst across the screen, and finally her completion stats
slide up from the bottom — giving her the dopamine hit that keeps her coming back tomorrow.

#### Acceptance Criteria

- [ ] Phase 1: Ripple circle expands from grid center, fades out by 400ms
- [ ] Phase 2: 30–50 Canvas-drawn confetti particles burst and fade over 1200ms
- [ ] Phase 3: Stats card (elapsed time + difficulty) slides up starting at 600ms
- [ ] No Lottie library is used — Canvas only
- [ ] All three phases respect `ANIMATOR_DURATION_SCALE` (0 = instant show completion state)
- [ ] Frame time < 12ms during celebration on API 24 emulator
- [ ] APK size does not increase from this feature
- [ ] `./gradlew test` passes all 552 tests

---

### S03-F004 — Edge-to-Edge Polish

**Priority**: P3
**Files**: `MainActivity.kt`, `PuzzleScreen.kt`, `HomeScreen.kt`

#### Context

`enableEdgeToEdge()` is already called in the app (per domain-memory.yaml: "enableEdgeToEdge()
is already called — this completes the visual treatment"). This means the app's content draws
behind the status bar and navigation bar. However, without proper insets handling, content can
be clipped behind system bars.

The remaining work: (1) ensure `Scaffold` components apply `WindowInsets.navigationBars` padding
to bottom content, (2) apply dynamic status bar content color (light icons on dark background /
dark icons on light background) using `WindowInsetsController`.

#### Implementation Approach

In `MainActivity.kt`:

```kotlin
WindowCompat.getInsetsController(window, window.decorView).apply {
    isAppearanceLightStatusBars = !isSystemInDarkTheme()
}
```

Or the Compose equivalent via `SideEffect` in `SumGridTheme`:

```kotlin
SideEffect {
    val controller = WindowCompat.getInsetsController(window, view)
    controller.isAppearanceLightStatusBars = !darkTheme
    controller.isAppearanceLightNavigationBars = !darkTheme
}
```

`Scaffold` in both screens: verify `contentWindowInsets` is properly set or that `innerPadding`
from `Scaffold` already includes navigation bar insets (it does by default in Compose 1.4+).
This is mostly a verification + status bar color wiring task.

#### User Story

**Given** a user is on the Home screen in dark mode
**When** the status bar is visible at the top of the screen
**Then** the status bar icons are white (light) against the dark background, matching the screen's
color scheme rather than appearing as dark icons on a dark background.

#### Acceptance Criteria

- [ ] Status bar icon color is light in dark mode and dark in light mode (dynamic based on theme)
- [ ] No content is visually clipped behind status bar or navigation bar on any screen
- [ ] `Scaffold` `innerPadding` correctly accounts for system bars on all API levels 24–34
- [ ] Bottom content (NumberPad in PuzzleScreen) has navigation bar inset clearance
- [ ] APK size does not increase
- [ ] `./gradlew test` passes all 552 tests

---

## Non-Functional Requirements

| ID | Requirement | Acceptance Threshold |
|----|-------------|---------------------|
| NFR-001 | APK size | Hard cap 4.5 MB; stop all work if exceeded |
| NFR-002 | Frame time | < 12 ms on API 24 / 2 GB RAM emulator for all animated code paths |
| NFR-003 | Animation scale | All animations MUST read `ANIMATOR_DURATION_SCALE`; scale = 0 means instant |
| NFR-004 | Test continuity | All 552 existing tests pass at every sprint boundary |
| NFR-005 | Accessibility | Zero visual-only state information introduced; every visual state has a TalkBack parallel |
| NFR-006 | Backward compat | All features work correctly at minSdk 24; API > 24 features use compat or conditional |
| NFR-007 | Canvas math safety | GridRenderer Canvas coordinate math (`cellSize`, `gutterFraction`, `aspectRatio`, tap detection) is never modified |

---

## Dependencies

### Internal

| Feature | Depends On |
|---------|-----------|
| S01-F001 (haptics) | S00-F001 (theme tokens, for correct color parameters at call site) |
| S02-F001 (expanded palette) | S00-F001 (theme tokens, to complete the color token coverage) |
| S02-F002 (gradients) | S00-F001, S02-F001 |
| S02-F005 (OLED dark mode) | S02-F001 |
| S03-F001 (cell selection animation) | S00-F001 |
| S03-F002 (sum indicator animation) | S03-F001 |
| S03-F003 (enhanced celebration) | S00-F003 |
| S03-F004 (edge-to-edge) | None (independent, but should be done after S02 gradient is stable) |

### External Libraries

| Library | Feature | Size | Justification |
|---------|---------|------|---------------|
| `androidx.core:core-splashscreen:1.0.1` | S02-F003 | ~20 KB | Only way to implement modern splash screen with API 12 compat |
| `Outfit Variable font` | S01-F003 | ~100 KB | Brand typography; single variable file replaces multiple weight files |

All other changes use existing Compose runtime, Navigation, and Material3 dependencies.

---

## Risks

| # | Risk | Severity | Likelihood | Sprint | Mitigation |
|---|------|----------|------------|--------|------------|
| 1 | `GridRenderer` refactor accidentally shifts tap detection or text centering | High | Medium | S00 | Color-only refactor; strict test coverage on `formatSumLabel`; manual tap testing on 4×4 grid |
| 2 | Canvas confetti (S03-F003) exceeds 12ms frame time on API 24 | Medium | Medium | S03 | Limit particle count; use `withFrameNanos` not per-frame recomposition; GPU profiler gate |
| 3 | `core-splashscreen` adds more than 30 KB to APK | Low | Low | S02 | Verify with `./gradlew assembleRelease` and `apkanalyzer` after adding dep |
| 4 | Outfit font not available as variable OTF/TTF download via OFL | Low | Very low | S01 | DM Sans is fallback (same license, same aesthetic, similar file size) |
| 5 | Predictive back breaks existing back navigation on API 24 | Medium | Low | S01 | Test on API 24 emulator; back gesture tested before merge |
| 6 | OLED true-black combined with gradient (S02-F002+F005) produces banding artifacts | Low | Medium | S02 | Blend at very low alpha; test on OLED emulator |

---

## Out of Scope

- **Phase 4 features**: Theme variants ("Midnight", "Forest"), dynamic color opt-in, sound effects,
  stats dashboard — explicitly deferred to post-launch
- **Hard/Expert difficulty, practice mode, share card** — separate epic; not touched in this epic
- **Canvas-to-LazyGrid `GridRenderer` rewrite** — major architectural change deferred to separate ticket
- **Lottie animation library** — Canvas particles evaluated first; Lottie added only if Canvas is insufficient
- **Per-user animation toggle in app settings** — deferred; `ANIMATOR_DURATION_SCALE` is the single control

---

## Success Metrics

| Metric | Baseline | Target | Measurement |
|--------|----------|--------|-------------|
| APK size | 3.5 MB | ≤ 4.5 MB | `./gradlew assembleRelease` + `apkanalyzer` |
| Test suite | 552 passing | 552+ passing (zero regressions) | `./gradlew test` |
| Frame time (animated) | N/A | < 12 ms | Android Studio GPU profiler, API 24 emulator |
| TalkBack grid coverage | 0 cells announced | All N×N cells + sum labels announced | Manual TalkBack test |
| Celebration fires on completion | Never | 100% of puzzle completions | Manual test + instrumented test |
| Haptic on cell tap | Never | Every tap | Manual test |
| Transitions | Instant cut | Fade+slide ≤ 300ms | Visual inspection |
| Day-7 retention (post-ship) | TBD | +5% absolute | Play Store Console |

---

## Appendix A — File Reference Map

| File | Sprint(s) | Change Type |
|------|-----------|-------------|
| `ui/components/GridRenderer.kt` | S00, S03 | Refactor (colors), Add (semantics, animations) |
| `ui/components/CelebrationAnimation.kt` | S00, S03 | Add (overlay composables) |
| `ui/screens/PuzzleScreen.kt` | S00, S01, S03 | Add (celebration wiring, haptics, celebration upgrade) |
| `ui/screens/HomeScreen.kt` | S01, S02 | Add (haptics, gradient, streak animation) |
| `ui/theme/Color.kt` | S02 | Extend (new palette tokens) |
| `ui/theme/Theme.kt` | S02 | Extend (fill new colorScheme slots) |
| `ui/theme/Type.kt` | S01 | Replace (FontFamily.Default → OutfitFont) |
| `navigation/SumGridNavigation.kt` | S01 | Modify (NavHost transitions, predictive back) |
| `AndroidManifest.xml` | S01, S02 | Add (predictive back flag, splash theme) |
| `res/font/outfit_variable.ttf` | S01 | Add (new asset) |
| `res/values/themes.xml` | S02 | Add/Modify (splash theme) |
| `MainActivity.kt` | S02, S03 | Add (installSplashScreen, status bar color) |

---

## Appendix B — Testing Strategy

### Unit Tests (extend `./gradlew test`)

- `GridRendererColorTest`: verify `formatSumLabel` still correct after color param refactor (already tested; no change expected)
- `CelebrationStateTest`: verify `rememberCelebrationState` animates correctly when `isComplete = true`
- `ThemeTokenTest`: verify all 9 color parameters in `GridRenderer` resolve to non-zero, non-transparent colors in both light and dark `MaterialTheme`

### UI / Instrumented Tests

- `TalkBackSemanticsTest`: assert `SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, ...)` on `GridRenderer` node
- `PuzzleCompletionTest`: assert celebration overlay is displayed when `isCompleted = true`
- `NavigationTransitionTest`: assert `NavHost` destinations transition (smoke test)

### Manual QA Checklist (per sprint boundary)

- [ ] S00: Light mode / dark mode grid visual comparison; TalkBack navigation of full grid; celebration fires on puzzle completion
- [ ] S01: Haptic felt on each interaction; transitions smooth on real device; font visible on Home and Puzzle screens; back gesture works on API 33+
- [ ] S02: Gradient backgrounds visible but not overpowering; splash screen shown on cold start; streak animates on Home screen; dark mode is OLED-optimized
- [ ] S03: Cell selection pulse visible; sum shake visible on over-sum; celebration is multi-phase; status bar icons correct in both themes
