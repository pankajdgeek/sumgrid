# Sprint Plan
# Epic: 002-sumgrid-uiux-modernization-gridrenderer

**Created**: 2026-03-14

---

## Dependency Graph

```
S00 (Technical Debt)  ──────────────────────────────┐
  ├── S00-F001: GridRenderer color refactor           │
  ├── S00-F002: TalkBack accessibility semantics      │
  └── S00-F003: Wire CelebrationAnimation             │
        │                                             │
        ▼                                             ▼
S01 (Quick Wins)                              S01 also independent of S02/S03
  ├── S01-F001: Haptic feedback (needs S00-F001)
  ├── S01-F002: Animated transitions (no dependency)
  ├── S01-F003: Outfit font (no dependency)
  └── S01-F004: Predictive back (no dependency)
        │
        ▼
S02 (Visual Identity) — needs S00 + S01
  ├── S02-F001: Expanded palette (needs S00-F001)
  ├── S02-F002: Gradient backgrounds (needs S00-F001 + S02-F001)
  ├── S02-F003: Splash screen (no dependency)
  ├── S02-F004: Animated streak counter (no dependency)
  └── S02-F005: Dark mode OLED polish (needs S02-F001)
        │
        ▼
S03 (Gameplay Polish) — needs S00 + S01 + S02
  ├── S03-F001: Cell selection animation (needs S00-F001)
  ├── S03-F002: Sum indicator animations (needs S03-F001)
  ├── S03-F003: Enhanced celebration (needs S00-F003)
  └── S03-F004: Edge-to-edge polish (no dependency)
```

---

## Sprint S00 — Fix Technical Debt First

**Goal**: Unblock all theme work. Mandatory prerequisite for S01-S03.
**Estimated effort**: ~1 day
**Releasable**: Yes (internal/alpha — grid theme-correct, TalkBack fixed, celebration wired)

### S00-F001: GridRenderer Color Refactor to Theme Tokens

**Files changed**: `GridRenderer.kt`

**Approach**:
1. Add `data class GridColors(givenCellBg, userCellBg, gridLine, selectedBorder, cellText, givenText, sumGreen, sumRed, sumGray: Color)` inside `GridRenderer.kt`.
2. Add companion/factory `fun GridColors.Companion.fromTheme(): GridColors` as a composable-scope extension (or top-level `@Composable fun gridColorsFromTheme(): GridColors`) that reads `MaterialTheme.colorScheme.*`.
3. In `GridRenderer()` composable: call `val colors = gridColorsFromTheme()` before `Canvas { }`.
4. Pass `colors` as parameter to `drawGrid(...)` and all private `DrawScope` helpers.
5. Delete the 9 `private val Color*` constants (lines 30-38).

**Color mappings** (theme token → replaced constant):
- `givenCellBg` ← `colorScheme.primaryContainer` (was `IndigoContainer90 = 0xFFDDE1FF`)
- `userCellBg` ← `colorScheme.surface` (was `Neutral99 = 0xFFFFFBFF`)
- `gridLine` ← `colorScheme.onSurface` (was `Neutral10 = 0xFF1B1B1F`)
- `selectedBorder` ← `colorScheme.secondary` (was `Amber80 = 0xFFEFC400`)
- `cellText` ← `colorScheme.onSurface` (was `Neutral10`)
- `givenText` ← `colorScheme.primary` (was `Indigo20 = 0xFF0001AC`)
- `sumGreen` ← `colorScheme.tertiary` (will be added in S02-F001; use `colorScheme.secondary` as interim)
- `sumRed` ← `colorScheme.error` (was `ErrorRed40`)
- `sumGray` ← `colorScheme.onSurfaceVariant` (was `Neutral49`)

**Test coverage required**:
- Unit test: `GridColors.fromTheme()` returns expected values for light/dark scheme (test with explicit `MaterialTheme` in Compose test rule)
- Screenshot/render test: `GridRenderer` composable renders without crash in both light and dark theme
- Regression: all 552 existing tests remain green

**Zero-change contract**: `cellSize`, `gutterFraction`, `n`, all `Offset(...)` and `Size(...)` calculations — READ ONLY.

---

### S00-F002: Grid TalkBack Accessibility Semantics

**Files changed**: `GridRenderer.kt`

**Approach**:
1. Add `state.puzzle.cells` to the `pointerInput` key to ensure semantics update when puzzle changes.
2. Wrap the existing `Canvas(modifier = ...)` in a `Box` that carries the accessibility semantics.
3. Overlay an invisible grid of zero-alpha `Box` nodes using `Layout` or absolute-positioned `Box` children, each with `Modifier.semantics { contentDescription = "Row $r, Column $c, value $v, ${if (isGiven) "given" else "editable"}" }`.
4. Invisible overlay nodes use `Modifier.size(cellSizeDp).offset(x = ..., y = ...)` to spatially align with Canvas cells.
5. Cell size in dp is derived from `BoxWithConstraints` parent width divided by `(n + 0.6f)`.

**Accessibility announcement format**: `"Row {r+1}, Column {c+1}, {value or "empty"}, {given or editable}"`

**Test coverage required**:
- Compose semantic test: `composeTestRule.onNodeWithContentDescription("Row 1, Column 1, 3, given").assertExists()`
- Verify zero visual change: screenshot comparison passes

---

### S00-F003: Wire CelebrationAnimation to PuzzleScreen

**Files changed**: `PuzzleScreen.kt`

**Approach**:
1. In `PuzzleScreen`, add `val celebrationState = rememberCelebrationState(isComplete = currentState.isCompleted, gridSize = currentState.puzzle.size)`.
2. Add a `Box(modifier = Modifier.fillMaxWidth())` wrapping the `GridRenderer` call.
3. When `currentState.isCompleted`, overlay a grid of `CelebrationCellWrapper` nodes above the `GridRenderer` (using `zIndex(1f)` or placing after the `GridRenderer` in the `Box`).
4. Replace the plain text completion banner with a `CelebrationCellWrapper`-driven visual. The text banner can remain as a fallback for TalkBack.

**Note on S03 enhancement**: This S00 wiring is intentionally minimal (uses existing `CelebrationAnimation.kt` as-is). S03-F003 will enhance `CelebrationAnimation.kt` to add ripple + confetti + stats card phases.

**Test coverage required**:
- `PuzzleScreen` renders `CelebrationCellWrapper` when `isCompleted = true`
- `rememberCelebrationState` starts animation when `isComplete` transitions from false to true

---

## Sprint S01 — Quick Wins

**Goal**: Maximum perceptible polish at near-zero APK cost.
**Estimated effort**: ~2 days
**Releasable**: Yes (user-facing release candidate)
**Prerequisite**: S00 complete

### S01-F001: Haptic Feedback

**Files changed**: `PuzzleScreen.kt`, `NumberPad.kt`

**Approach**:
1. Obtain `val haptic = LocalHapticFeedback.current` in `PuzzleScreen`.
2. In `GridRenderer` `onCellTap` lambda: call `haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)`.
3. In `NumberPad` `onNumberTap` and `onClearTap` callbacks: call appropriate haptic type.
4. On puzzle completion (`isCompleted` transition): `haptic.performHapticFeedback(HapticFeedbackType.LongPress)`.

**API compatibility**: `LocalHapticFeedback` is available API 1+. No compat wrapper needed.

---

### S01-F002: Animated Screen Transitions

**Files changed**: `SumGridNavigation.kt`

**Approach**:
Replace bare `NavHost(...)` with the transition-capable overload:

```kotlin
NavHost(
    navController = navController,
    startDestination = resolvedStart,
    modifier = modifier,
    enterTransition = {
        fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 4 }
    },
    exitTransition = {
        fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 4 }
    },
    popEnterTransition = {
        fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 4 }
    },
    popExitTransition = {
        fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 4 }
    }
)
```

Uses `androidx.navigation:navigation-compose` already in `build.gradle.kts` — no new dependency.

---

### S01-F003: Custom Typography — Outfit Variable Font

**Files changed**: `app/src/main/res/font/outfit_variable.ttf` (new), `Type.kt`

**Approach**:
1. Download Outfit Variable font from Google Fonts; copy OTF/TTF to `app/src/main/res/font/outfit_variable.ttf`.
2. In `Type.kt`, add `val OutfitFontFamily = FontFamily(Font(R.font.outfit_variable))`.
3. Replace all `fontFamily = FontFamily.Default` instances in `SumGridTypography` with `fontFamily = OutfitFontFamily`.
4. Verify APK delta ≤ 110 KB (measure with `./gradlew assembleRelease` before/after).

**No per-screen changes needed**: All screens use `MaterialTheme.typography.*` which is backed by `SumGridTypography`.

---

### S01-F004: Predictive Back Gesture

**Files changed**: `AndroidManifest.xml`

**Approach**:
Add `android:enableOnBackInvokedCallback="true"` to the `<application>` element. Compose Navigation 2.7+ handles the back gesture animation automatically when this flag is set.

No Kotlin changes required — Compose Navigation already intercepts back events via `OnBackPressedDispatcher`.

---

## Sprint S02 — Visual Identity

**Goal**: Recognizable premium visual language. Dark mode looks intentional.
**Estimated effort**: ~3 days
**Releasable**: Yes
**Prerequisites**: S00 + S01 complete

### S02-F001 + S02-F005: Expanded Palette + OLED Dark Mode

**Files changed**: `Color.kt`, `Theme.kt`

**Approach**:

New tokens in `Color.kt`:
```kotlin
// Tertiary — semantic success green
val Green30  = Color(0xFF1B6C2E)
val Green80  = Color(0xFF6EDB82)
val Green90  = Color(0xFFA8F5B2)

// Neutral variants (surface tones)
val NeutralVariant50 = Color(0xFF79747E)
val NeutralVariant70 = Color(0xFFAEAAB4)
val NeutralVariant90 = Color(0xFFE8DEF8)

// OLED dark backgrounds
val NeutralOLED = Color(0xFF000000)
val NeutralOLEDSurface = Color(0xFF0D0D0D)
val NeutralOLEDSurface2 = Color(0xFF1A1A1A)
```

Updated `DarkColorScheme` in `Theme.kt`:
- `background = NeutralOLED` (true black)
- `surface = NeutralOLEDSurface`
- `surfaceVariant = NeutralOLEDSurface2`
- `tertiary = Green80`
- `onTertiary = Neutral10`
- `tertiaryContainer = Green30`
- `onTertiaryContainer = Green90`

Updated `LightColorScheme` in `Theme.kt`:
- `tertiary = Green30`
- `onTertiary = Neutral99`
- `tertiaryContainer = Green90`
- `onTertiaryContainer = Green30`
- `surfaceVariant = NeutralVariant90`
- `onSurfaceVariant = NeutralVariant50`

After S02-F001: `GridColors.fromTheme()` will correctly resolve `colorScheme.tertiary` to the green token (fixing the interim value from S00-F001).

---

### S02-F002: Gradient Backgrounds

**Files changed**: `HomeScreen.kt`, `PuzzleScreen.kt`

**Approach**:
Replace the `Scaffold` content `Column` background with a `Box` using:
```kotlin
Modifier.background(
    Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    )
)
```
The gradient is subtle (40% alpha bottom stop). Zero APK cost.

---

### S02-F003: Splash Screen

**Files changed**: `build.gradle.kts` (add dep), `MainActivity.kt`, `res/values/themes.xml`

**Approach**:
1. Add `implementation("androidx.core:core-splashscreen:1.0.1")` to `build.gradle.kts`.
2. In `res/values/themes.xml`, add `<style name="Theme.SumGrid.Splash" parent="Theme.SplashScreen">` with `windowSplashScreenBackground`, `windowSplashScreenAnimatedIcon`.
3. Set `android:theme="@style/Theme.SumGrid.Splash"` on `MainActivity` in manifest.
4. In `MainActivity.onCreate()`, call `installSplashScreen()` before `setContent { ... }`.

APK delta: ~20 KB.

---

### S02-F004: Animated Streak Counter

**Files changed**: `HomeScreen.kt`

**Approach**:
Convert `StreakDisplay` to read streak changes and animate:
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
Apply `Modifier.scale(scale.value)` to the streak number text. The flame emoji gets a separate 800ms `infiniteTransition` pulse when streak > 0.

---

## Sprint S03 — Gameplay Polish

**Goal**: Every interaction feels intentional. Completing a puzzle feels rewarding.
**Estimated effort**: ~4 days
**Releasable**: Yes (full public release quality)
**Prerequisites**: S00 + S01 + S02 complete

### S03-F001: Cell Selection Animation

**Files changed**: `GridRenderer.kt`

**Approach**:
1. Add `val selectedPulse = remember { Animatable(1f) }` in `GridRenderer` composable scope.
2. `LaunchedEffect(state.selectedCell)`: animate `selectedPulse` 1.0 → 1.05 → 1.0 using `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)`, total ~200ms.
3. In `drawGrid()`, pass `selectedPulseScale: Float` parameter.
4. When drawing selected cell border: scale the cell rect around its center using `withTransform { scale(selectedPulseScale, pivot = cellCenter) }` before `drawRect`.
5. Add amber glow: draw a `drawRect` with `color = colors.selectedBorder.copy(alpha = 0.25f)` filled (not stroked) behind the border.

**Frame time constraint**: Single `Animatable<Float>` — trivially <1ms per frame on any device.

---

### S03-F002: Sum Indicator Animations

**Files changed**: `GridRenderer.kt`

**Approach**:
Two new `Animatable` instances per indicator type:
- `sumCorrectPulse: Animatable<Float>` — scale pulse for green indicators (1.0 → 1.1 → 1.0)
- `sumOverShake: Animatable<Float>` — horizontal offset for red indicators (0 → 3 → -3 → 3 → 0 px)

`LaunchedEffect(state.rowSumIndicators, state.colSumIndicators)`: detect GREEN/RED transitions and trigger corresponding animatable.

In `DrawScope.drawGrid()`: pass `rowPulses: List<Float>`, `rowShakes: List<Float>`, `colPulses: List<Float>`, `colShakes: List<Float>` as plain Float parameters.

Apply via `withTransform` in the sum label drawing section.

---

### S03-F003: Enhanced Multi-Phase Celebration

**Files changed**: `CelebrationAnimation.kt`, `PuzzleScreen.kt`

**Three-phase architecture**:

**Phase 1 — Ripple wave** (0-600ms):
- New `val rippleRadius: Animatable<Float>` in `CelebrationState`, starts at 0, animates to `maxRadius` (half the grid diagonal).
- Rendered in `PuzzleScreen` as a `Canvas` overlay `Box` above `GridRenderer`, drawing a `drawCircle` with `style = Stroke` and `alpha` fading as radius grows.

**Phase 2 — Confetti particles** (200-1200ms):
- New `data class ConfettiParticle(x, y, vx, vy, color, rotation, size)` in `CelebrationAnimation.kt`.
- `val particles: List<ConfettiParticle>` initialized with 30-40 particles in `CelebrationState`.
- Animated via a single `Animatable<Float>` progress value (0 → 1 over 1000ms).
- Particle positions are computed from initial position + velocity * progress.
- Colors cycle through `primary`, `secondary`, `tertiary` from theme.
- Drawn via `DrawScope.drawCircle` and `DrawScope.drawRoundRect` (no bitmaps, no Lottie).

**Phase 3 — Stats card slide-up** (800-1600ms):
- New `StatsCard` composable (defined in `CelebrationAnimation.kt`) showing elapsed time, puzzle difficulty.
- Animated with `animateFloatAsState` translationY from +200dp to 0dp.
- Displayed in `PuzzleScreen` as an `AnimatedVisibility` with `slideInVertically` after 800ms delay.

**TalkBack**: `StatsCard` carries `contentDescription = "Puzzle complete. Time: $time. Difficulty: $difficulty."`.

---

### S03-F004: Edge-to-Edge Polish

**Files changed**: `PuzzleScreen.kt`, `HomeScreen.kt`, `MainActivity.kt`

**Approach**:
1. `enableEdgeToEdge()` already called in `MainActivity`. No change needed there.
2. In `PuzzleScreen` and `HomeScreen` `Scaffold`: ensure `WindowInsets.systemBars` are handled. Scaffold already does this via `contentWindowInsets = ScaffoldDefaults.contentWindowInsets`, but status bar coloring is not set.
3. Add `SideEffect` in `MainActivity` after `setContent` to configure status bar icons:
```kotlin
val insetsController = WindowCompat.getInsetsController(window, window.decorView)
insetsController.isAppearanceLightStatusBars = !darkTheme
```
Uses `WindowCompat` from `core-ktx` (already a dependency). Works API 21+.

---

## Sprint Dependency Graph (Machine-Readable)

```yaml
sprints:
  S00:
    depends_on: []
    features: [S00-F001, S00-F002, S00-F003]
    unblocks: [S01, S02]

  S01:
    depends_on: [S00]
    features: [S01-F001, S01-F002, S01-F003, S01-F004]
    unblocks: [S02]
    note: "S01-F002, S01-F003, S01-F004 have no feature-level dependency on S00 — can start in parallel with S00 if needed"

  S02:
    depends_on: [S00, S01]
    features: [S02-F001, S02-F002, S02-F003, S02-F004, S02-F005]
    unblocks: [S03]
    note: "S02-F003 (splash) and S02-F004 (streak anim) are independent of S00/S01 at feature level"

  S03:
    depends_on: [S00, S01, S02]
    features: [S03-F001, S03-F002, S03-F003, S03-F004]
    unblocks: []
```

---

## Quality Gate Checkpoints

| Sprint | APK Cap | Frame Time | Test Count | TalkBack |
|---|---|---|---|---|
| S00 | <4.5 MB | N/A | 552+ (no regression) | Grid cells announced |
| S01 | <4.0 MB | N/A | 552+ | N/A |
| S02 | <4.5 MB | N/A | 552+ | Streak announced |
| S03 | <4.5 MB | <12ms (API 24) | 552+ | Celebration announced |

Each sprint boundary is a releasable increment. If S03 frame time exceeds 12ms, remove the offending animation before shipping.

---

## Test Strategy

### S00 tests (new)
- `GridRendererTest.kt`: `GridColors.fromTheme()` returns theme-concordant values
- `GridRendererTest.kt`: semantics tree contains expected `contentDescription` for each cell
- `PuzzleScreenTest.kt`: `CelebrationCellWrapper` nodes exist when `isCompleted = true`

### S01 tests (new)
- `HapticFeedbackTest.kt`: `performHapticFeedback` called on cell tap, number entry, clear, completion
- `NavigationTest.kt`: transition animations play (smoke test — verify no crash)
- `TypeographyTest.kt`: `SumGridTypography.bodyLarge.fontFamily` is `OutfitFontFamily`

### S02 tests (new)
- `ThemeTest.kt`: `DarkColorScheme.background` equals `NeutralOLED` (true black)
- `ThemeTest.kt`: `LightColorScheme.tertiary` equals `Green30`
- `StreakDisplayTest.kt`: `Animatable` scale pulse fires when streak increments

### S03 tests (new)
- `GridRendererTest.kt`: `selectedPulse.value` != 1.0 during selection animation
- `CelebrationAnimationTest.kt`: `CelebrationState` has `rippleRadius`, `particles`, all phases initialized
- `PuzzleScreenTest.kt`: `StatsCard` appears after `isCompleted = true`
