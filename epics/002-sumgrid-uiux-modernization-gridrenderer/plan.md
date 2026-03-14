# Architecture and Implementation Plan
# Epic: 002-sumgrid-uiux-modernization-gridrenderer

**Phase**: Plan
**Created**: 2026-03-14
**Stack**: Kotlin + Jetpack Compose + Material3, minSdk 24
**APK budget**: 4.5 MB hard cap (~3.5 MB current, ~1 MB headroom)

---

## Architecture Overview

SumGrid uses a single-activity MVVM architecture with Jetpack Compose. The complete layer stack is:

```
MainActivity
  └── SumGridTheme (MaterialTheme wrapper)
        └── SumGridNavHost (NavHost — Compose Navigation)
              ├── OnboardingScreen  → OnboardingViewModel
              ├── HomeScreen        → HomeViewModel
              └── PuzzleScreen      → PuzzleViewModel
                    ├── GridRenderer (Canvas-based)
                    ├── NumberPad
                    └── CelebrationAnimation (currently unwired)
```

The modernization epic adds visual, accessibility, and animation layers without restructuring this hierarchy.

---

## Architecture Decisions

### Decision 1: Color Extraction via Composable Parameter Passing (S00-F001)

**Pattern**: Dependency injection of theme-derived colors via function parameters

The 9 hardcoded `private val` color constants in `GridRenderer.kt` (lines 30-38) will be replaced with a `GridColors` data class whose values are derived from `MaterialTheme.colorScheme` inside the `@Composable` `GridRenderer` function and passed down to all private `DrawScope` helpers.

```
GridRenderer() {
  val colors = GridColors.fromTheme()  // reads MaterialTheme inside composable
  Canvas(...) {
    drawGrid(state, n, cellSize, textMeasurer, colors)
  }
}
```

**Why**: The `Canvas` DrawScope is not a composable scope — it cannot call `MaterialTheme.colorScheme` directly. The composable parent must read theme tokens and pass colors as plain parameters. This is the canonical Compose pattern for theming Canvas content.

**Constraint honored**: Zero changes to any coordinate math (cellSize calculations, gutter fractions, offset math, strokeWidth).

**Reuse**: `GridColors` data class is defined in `GridRenderer.kt` alongside the renderer — no separate file needed for a small data carrier.

### Decision 2: Accessibility via Modifier.semantics on the Canvas Wrapper (S00-F002)

**Pattern**: Composite semantics — single `Canvas` composable with merged cell-level semantics

TalkBack cannot introspect a `DrawScope` — it needs semantics in the Compose node tree. The approach:

1. Wrap the `Canvas` in a `Box` that carries `Modifier.semantics { contentDescription = ... }` for the full grid.
2. For per-cell granularity, overlay an invisible `Layout` of zero-size `Box` nodes, each with `semantics { contentDescription = "Row R, Column C, value V, given" }`, positioned to match each Canvas cell.

**Why not LazyGrid overlay**: The spec explicitly bans a Canvas-to-LazyGrid rewrite. Invisible overlay nodes preserve the Canvas coordinate system while satisfying TalkBack traversal. This is the canonical approach for Canvas accessibility in Compose.

**Performance**: Zero rendering cost — invisible nodes are not drawn. Compose a11y tree is separate from draw tree.

### Decision 3: CelebrationAnimation Wiring via State Observation (S00-F003)

**Pattern**: State-driven animation trigger in PuzzleScreen

`CelebrationAnimation.kt` already provides `rememberCelebrationState(isComplete, gridSize)` and `CelebrationCellWrapper`. The wiring gap is in `PuzzleScreen.kt`:

- `currentState.isCompleted` exists on `PuzzleUiState`
- `rememberCelebrationState(isComplete = currentState.isCompleted, gridSize = n)` is never called

Fix: Add `rememberCelebrationState` call in `PuzzleScreen`, overlay `CelebrationCellWrapper` cells when `isComplete = true`. Since `GridRenderer` is Canvas-based, the celebration overlay uses a `Box` with `zIndex` above the `GridRenderer` composable — no Canvas internals modified.

**S03 enhancement**: The S03 multi-phase celebration (ripple + confetti + stats card) will enhance `CelebrationAnimation.kt` in-place, adding new animation phases to the existing `CelebrationState`.

### Decision 4: Haptic Feedback via LocalHapticFeedback (S01-F001)

**Pattern**: Compose built-in `LocalHapticFeedback` — zero APK cost, zero new dependency

Inject `LocalHapticFeedback.current` in `PuzzleScreen` and pass a `hapticFeedback` lambda to `GridRenderer` and `NumberPad`. Trigger points:
- Cell tap: `HapticFeedbackType.TextHandleMove`
- Number entry: `HapticFeedbackType.TextHandleMove`
- Clear action: `HapticFeedbackType.LongPress`
- Puzzle completion: `HapticFeedbackType.LongPress`

**Why pass lambda not LocalCompositionLocal**: `GridRenderer` already receives `onCellTap` — haptic is fired in the same callback site. No new composition locals needed.

### Decision 5: AnimatedNavHost Transition Replacement (S01-F002)

**Pattern**: Drop-in `AnimatedNavHost` from `accompanist-navigation-animation` — or use Compose Navigation 2.7+ built-in `enterTransition`/`exitTransition` parameters

`SumGridNavigation.kt` uses bare `NavHost`. Replace with `NavHost` with `enterTransition`/`exitTransition` lambda parameters (available since `androidx.navigation:navigation-compose:2.7.0` without Accompanist):

```kotlin
NavHost(
    enterTransition = { fadeIn() + slideInHorizontally { it / 4 } },
    exitTransition  = { fadeOut() + slideOutHorizontally { -it / 4 } },
    ...
)
```

**APK impact**: Zero — uses existing navigation-compose dependency already in `build.gradle.kts`.

**Animation respect**: Transitions read `LocalAnimationSpec` from Compose, which respects `ANIMATOR_DURATION_SCALE`.

### Decision 6: Typography — Outfit Variable Font as FontFamily (S01-F003)

**Pattern**: `res/font/` resource with `FontFamily` declaration in `Type.kt`

Bundle `outfit_variable.ttf` (~100 KB) in `app/src/main/res/font/`. Declare `val OutfitFontFamily = FontFamily(Font(R.font.outfit_variable))` and substitute into every `TextStyle` in `SumGridTypography`.

**APK impact**: ~100 KB raw. With R8/ProGuard + resource shrinking already enabled in release build, actual APK delta ≈ 85-95 KB (font is not shrinkable but may benefit from AAPT2 compression). Well within 1 MB headroom.

**REUSE**: `SumGridTypography` in `Type.kt` is the single source of truth — all screens consume it via `MaterialTheme.typography`. No per-screen font changes needed.

### Decision 7: Expanded Material3 Palette via Color.kt + Theme.kt (S02-F001, S02-F005)

**Pattern**: Additive color tokens — no removal, only addition

Current palette: 13 color values (Indigo, Amber, Neutral, Error scales). Target: ~30 tokens adding:
- Tertiary palette: `Green30`, `Green80`, `Green90` (semantic success)
- Surface variants: `NeutralVariant50`, `NeutralVariant70`, `NeutralVariant90`
- Tonal elevation: `Surface1` through `Surface5` (tonal surface tiers)
- Dark OLED: `NeutralOLED = Color(0xFF000000)`, `NeutralOLEDSurface = Color(0xFF0D0D0D)`

`DarkColorScheme` and `LightColorScheme` in `Theme.kt` expanded to use new tokens. `GridColors.fromTheme()` will automatically benefit for free once theme is updated (S02 tokens feed S03 animations).

### Decision 8: Canvas Animation via Animatable inside DrawScope Delegation (S03-F001, S03-F002)

**Pattern**: `Animatable` state hoisted to composable scope, `DrawScope` reads `.value`

Canvas `DrawScope` is not a coroutine scope and cannot launch animations. The pattern:

```kotlin
@Composable
fun GridRenderer(...) {
  val selectedPulse = remember { Animatable(1f) }
  LaunchedEffect(state.selectedCell) {
    selectedPulse.animateTo(1.05f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    selectedPulse.animateTo(1.0f, spring())
  }
  Canvas(...) {
    drawGrid(..., selectedPulseScale = selectedPulse.value)
  }
}
```

`DrawScope.drawGrid()` receives the scale as a plain `Float` — no animation APIs in draw code.

**Performance constraint**: Frame time <12ms on API 24. Spring animations on a single cell scale are trivial (single float interpolation per frame). Verified safe at API 24.

**ANIMATOR_DURATION_SCALE**: Compose Animatable respects `LocalAnimationSpec` which reads system scale. No explicit check needed.

### Decision 9: Splash Screen via core-splashscreen (S02-F003)

**Pattern**: `androidx.core:core-splashscreen` (~20 KB), no Lottie, no additional animation library

Add dependency in `build.gradle.kts`. Call `installSplashScreen()` before `setContent` in `MainActivity`. Add `postSplashScreenTheme` in `styles.xml`. APK delta: ~20 KB.

**REUSE**: Uses existing `@mipmap/ic_launcher` — no new asset.

### Decision 10: Edge-to-Edge Completion (S03-F004)

**Pattern**: Extend existing `enableEdgeToEdge()` call in `MainActivity`

`MainActivity.onCreate()` already calls `enableEdgeToEdge()`. The remaining work:
- Apply `WindowInsetsCompat` padding to all `Scaffold` composables using `Modifier.windowInsetsPadding(WindowInsets.systemBars)`
- Dynamic status bar icon color: `WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS` based on theme darkness
- This is already partially handled by `Scaffold`'s `innerPadding` — only the status bar color logic is missing

---

## Component Reuse Map

| Reuse ID | Existing Component | Reused In Sprint | How |
|---|---|---|---|
| REUSE:1 | `CelebrationAnimation.kt` (`CelebrationState`, `rememberCelebrationState`, `CelebrationCellWrapper`) | S00-F003, S03-F003 | Wire in S00; enhance in-place in S03 |
| REUSE:2 | `SumGridTypography` in `Type.kt` | S01-F003 | Replace `FontFamily.Default` — all screens inherit automatically |
| REUSE:3 | `Color.kt` palette tokens | S02-F001, S02-F002, S02-F005 | Add new tokens; `GridColors.fromTheme()` picks them up |
| REUSE:4 | `MaterialTheme.colorScheme` access pattern (already used in HomeScreen, PuzzleScreen) | S00-F001 | Same pattern applied to GridRenderer |
| REUSE:5 | `Scaffold` `innerPadding` propagation pattern (already in HomeScreen, PuzzleScreen) | S03-F004 | Extend with `windowInsetsPadding` |
| REUSE:6 | `NavHost` in `SumGridNavigation.kt` | S01-F002 | Replace with animated variant — same composable call site |
| REUSE:7 | `mipmap/ic_launcher` | S02-F003 (splash screen) | Reuse existing icon asset |

**Total reuse count: 7**

---

## APK Budget Tracking

| Sprint | Additions | Estimated Delta | Cumulative |
|---|---|---|---|
| Baseline | — | — | ~3.5 MB |
| S00 | Zero new dependencies | 0 KB | ~3.5 MB |
| S01 | Outfit Variable font | +95 KB | ~3.6 MB |
| S02 | core-splashscreen library | +20 KB | ~3.62 MB |
| S03 | No new libraries | 0 KB | ~3.62 MB |
| **Total** | | **+~115 KB** | **~3.62 MB** |

Headroom remaining: ~880 KB (well under 4.5 MB kill criterion).

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| TalkBack overlay nodes misaligned with Canvas cells | Medium | High | Use `onGloballyPositioned` to measure Canvas bounds; compute node positions relative to Canvas top-left |
| Outfit font exceeds 110 KB APK impact | Low | Medium | Use variable font (single file) + measure actual APK delta before committing |
| Canvas `Animatable` causes >12ms frame time on API 24 | Low | High | Limit animations to 1 active Animatable per frame; benchmark on API 24 emulator before S03 ship |
| core-splashscreen compat issues on API 24-30 | Low | Medium | Test on API 24 emulator; splashscreen library has explicit compat path for pre-31 |
| `PuzzleStatusChip` uses `tertiaryContainer` / `onTertiaryContainer` (not yet defined in Theme.kt) | Medium | Low | S02-F001 adds these tokens; S00/S01 run first so it won't be an issue in execution order |

---

## Implementation Constraints (Non-Negotiable)

1. GridRenderer Canvas coordinate math (cellSize, gutter fractions, offset calculations) is **read-only** — no changes permitted in S00 or any sprint.
2. All animations MUST respect `ANIMATOR_DURATION_SCALE` system setting.
3. No Lottie dependency — all animations use Canvas `DrawScope` + Compose `Animatable`.
4. No visual-only state information — every animation has a TalkBack-visible parallel.
5. minSdk 24 — `WindowInsetsController` requires API 30; use `WindowInsetsControllerCompat` from `core-ktx` (already a dependency).
6. APK hard cap: stop all work if 4.5 MB is exceeded.

---

## File Change Surface (by sprint)

### S00
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt` — Extract 9 color constants to `GridColors` data class; add semantics overlay
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt` — Wire `rememberCelebrationState` + overlay

### S01
- `app/src/main/res/font/outfit_variable.ttf` — New asset
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Type.kt` — Replace `FontFamily.Default` with `OutfitFontFamily`
- `app/src/main/kotlin/org/dgeek/sumgrid/navigation/SumGridNavigation.kt` — AnimatedNavHost transitions
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt` — Haptic feedback wiring
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/NumberPad.kt` — Haptic on number/clear tap
- `app/src/main/AndroidManifest.xml` — `android:enableOnBackInvokedCallback="true"`

### S02
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Color.kt` — Expanded palette tokens
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/theme/Theme.kt` — Updated color schemes with new tokens
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt` — Gradient background, animated streak
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt` — Gradient background
- `app/src/main/kotlin/org/dgeek/sumgrid/MainActivity.kt` — `installSplashScreen()`
- `app/src/main/res/values/themes.xml` — Splash screen theme entry
- `app/build.gradle.kts` — `core-splashscreen` dependency

### S03
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt` — Cell selection spring pulse, sum indicator animations
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/components/CelebrationAnimation.kt` — Multi-phase enhancement
- `app/src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt` — Edge-to-edge insets, celebration phases
