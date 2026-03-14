# SumGrid Phase 1 MVP — Sprint Plan

**Epic**: 001-sumgrid-phase-1-mvp
**Generated**: 2026-03-14
**Total dev-days**: 32 across 3 sprints (~8 calendar weeks at 4 productive days/week)

---

## Sprint Dependency Graph

```
┌─────────────────────────────────────────┐
│  SPRINT S01 — Core Engine + Grid UI     │
│  10 dev-days  |  9 features             │
│  No dependencies (critical path start)  │
└──────────────────────┬──────────────────┘
                       │
          Gate: 10,000-puzzle test suite
          passes with zero failures (CI)
                       │
                       ▼
┌─────────────────────────────────────────┐
│  SPRINT S02 — Daily Puzzle Loop         │
│  11 dev-days  |  11 features            │
│  Depends on: S01 complete               │
└──────────────────────┬──────────────────┘
                       │
          Gate: All S02 features implemented
          and PuzzleViewModel tests passing
                       │
                       ▼
┌─────────────────────────────────────────┐
│  SPRINT S03 — Polish + Firebase + Store │
│  11 dev-days  |  12 features            │
│  Depends on: S01 + S02 complete         │
└──────────────────────┬──────────────────┘
                       │
          Gate: APK < 4.5 MB, Firebase
          verified, all screens tested
                       │
                       ▼
              Play Store Submission
```

---

## Sprint S01 — Core Engine and Grid UI

**Goal**: Build the puzzle engine foundation and interactive grid. This is the hardest and most critical technical work. S02 cannot begin until the 10,000-puzzle test suite passes.

**Estimated duration**: 10 dev-days (calendar: ~2.5 weeks)
**Dependencies**: None — this is the critical path start
**Exit criterion**: `./gradlew test` passes with zero failures in < 60 seconds, 10,000 puzzles validated unique

### Feature Execution Order

Within S01, features must be built in two parallel tracks, then merged:

**Track A — Engine (build first, ~5 dev-days)**

| Order | Feature ID | Feature Name | Est. Days | Internal Deps |
|-------|-----------|-------------|-----------|---------------|
| 1 | S01-F001 | xorshift128 PRNG | 1 | — |
| 2 | S01-F003 | Constraint propagation unique-solution validator | 2 | — |
| 3 | S01-F002 | Puzzle generator (seeded, deterministic) | 1.5 | F001, F003 |
| 4 | S01-F004 | Difficulty calibration (Beginner/Easy/Medium) | 0.5 | F003 |
| 5 | S01-F005 | 10,000-puzzle automated test suite | 1 | F001-F004 |

**Track B — Grid UI (can start after F001 models exist, ~5 dev-days)**

| Order | Feature ID | Feature Name | Est. Days | Internal Deps |
|-------|-----------|-------------|-----------|---------------|
| 1 | S01-F006 | Compose Canvas grid renderer | 2 | F001 (Puzzle model) |
| 2 | S01-F007 | Tap-to-select cell interaction | 1 | F006 |
| 3 | S01-F008 | Bottom number pad input | 1 | F007 |
| 4 | S01-F009 | Real-time row/column sum validation | 1 | F008 |

**Note**: Track B can begin once `Puzzle`, `Cell`, and `Difficulty` data models are defined (mid Track A). Tracks merge when both F005 and F009 are complete.

### S01 Key Files Created

```
engine/
  Xorshift128.kt
  UniqueSolutionValidator.kt
  PuzzleGenerator.kt
  DifficultyCalibrator.kt
  models/Puzzle.kt
  models/Cell.kt
  models/Difficulty.kt

ui/components/
  GridRenderer.kt
  NumberPad.kt
  SumIndicator.kt (basic version)

viewmodel/
  PuzzleViewModel.kt (basic state, no daily system yet)

test/engine/
  Xorshift128Test.kt
  UniqueSolutionValidatorTest.kt
  PuzzleGeneratorTest.kt         ← 10,000-puzzle suite
  DifficultyTest.kt
```

### S01 Exit Gate (hard gate — CI must pass before S02)

```bash
./gradlew test
# Must produce:
# - BUILD SUCCESSFUL
# - 0 failures
# - 10,000 puzzles tested: UNIQUE assertion passed for all
# - Duration < 60 seconds
```

---

## Sprint S02 — Daily Puzzle Loop

**Goal**: Wire up the complete daily play loop. Players can open the app, pick a difficulty, solve today's puzzle, celebrate, share a result card, and build a streak. The home screen shows status for all three difficulties.

**Estimated duration**: 11 dev-days (calendar: ~3 weeks)
**Dependencies**: S01 complete (all 9 features passing)
**Exit criterion**: Full daily puzzle loop playable end-to-end on a physical device or emulator

### Feature Execution Order

| Order | Feature ID | Feature Name | Est. Days | Internal Deps |
|-------|-----------|-------------|-----------|---------------|
| 1 | S02-F001 | Daily puzzle system (date seed, midnight reset) | 2 | S01 complete |
| 2 | S02-F002 | Background precompute (WorkManager) | 1 | S02-F001 |
| 3 | S02-F003 | Puzzle completion detection | 1 | S01-F009 |
| 4 | S02-F005 | Elapsed timer | 0.5 | S02-F003 |
| 5 | S02-F007 | Share card generator | 1 | S02-F003 |
| 6 | S02-F009 | Streak counter (DataStore backed) | 1 | S02-F003 |
| 7 | S02-F010 | Streak milestone badges (7/30/100/365) | 0.5 | S02-F009 |
| 8 | S02-F004 | Celebration animation | 1.5 | S02-F003, S01-F006 |
| 9 | S02-F008 | One-tap share via Android Intent | 0.5 | S02-F007, S02-F004 |
| 10 | S02-F006 | Difficulty selector UI | 1 | S02-F001 |
| 11 | S02-F011 | Home screen | 2 | S02-F006, F009, F010 |

**Parallelizable**: After S02-F003 is complete, features F005, F007, and F009 can be built concurrently. Features F004 and F008 depend on F003 and F007 respectively so must follow.

### S02 Key Files Created

```
daily/
  DailyPuzzleRepository.kt
  PuzzlePrecomputeWorker.kt

share/
  ShareCardGenerator.kt

streak/
  StreakRepository.kt
  StreakBadgeProvider.kt

ui/components/
  CelebrationAnimation.kt
  DifficultySelector.kt
  CompletionCard.kt

ui/screens/
  HomeScreen.kt
  PuzzleScreen.kt          ← fully wired with daily system

viewmodel/
  PuzzleViewModel.kt       ← extended: completion, timer, streak
  HomeViewModel.kt

test/share/
  ShareCardGeneratorTest.kt
```

### S02 Exit Gate

- Daily puzzle loop plays end-to-end on API 24 emulator
- Completion triggers celebration animation and shows CompletionCard
- Share button opens Android native share sheet with correct emoji grid
- Streak increments on second-day completion (test with a manually overridden date or DataStore injection)
- Home screen shows correct status for all three difficulties

---

## Sprint S03 — Polish, Firebase, Onboarding, and Play Store

**Goal**: Ship a production-quality app. Visual design, dark mode, responsive layouts, Firebase integration, onboarding FTUE, in-app review, and Play Store submission.

**Estimated duration**: 11 dev-days (calendar: ~2.5 weeks)
**Dependencies**: S01 + S02 complete
**Exit criterion**: APK < 4.5 MB, Firebase events verified in DebugView, Play Store internal testing track submitted

### Feature Execution Order

| Order | Feature ID | Feature Name | Est. Days | Internal Deps |
|-------|-----------|-------------|-----------|---------------|
| 1 | S03-F001 | Deep indigo/amber Material3 theme | 1 | — |
| 2 | S03-F002 | Dark mode (system-following) | 0.5 | S03-F001 |
| 3 | S03-F006 | Firebase Analytics + event tracking | 1 | — |
| 4 | S03-F007 | Firebase Crashlytics integration | 0.5 | S03-F006 |
| 5 | S03-F008 | Onboarding puzzle sequence (1→2→4 cells) | 1 | S01-F006-F009 |
| 6 | S03-F009 | First-time user experience navigation | 1 | S03-F008, S02-F004 |
| 7 | S03-F003 | Responsive layout (360dp minimum) | 1 | S01-F006, F008, S02-F011 |
| 8 | S03-F004 | Colorblind-friendly sum indicators | 0.5 | S01-F009, S03-F001 |
| 9 | S03-F005 | Dynamic text sizing (system font scale) | 0.5 | S03-F003 |
| 10 | S03-F010 | In-app review prompt (after 3rd completion) | 0.5 | S02-F003 |
| 11 | S03-F011 | Play Store screenshots (5 required) | 2 | S03-F001, F002, S02-F008, F011 |
| 12 | S03-F012 | Play Store feature graphic + ASO listing | 1.5 | S03-F011 |

**Parallelizable**: S03-F001/F002 and S03-F006/F007 can be built concurrently at the start of S03. S03-F011/F012 are the final blocking tasks before submission.

### S03 Key Files Created

```
ui/theme/
  Theme.kt
  Color.kt
  Type.kt
  Shape.kt

onboarding/
  OnboardingRepository.kt

review/
  InAppReviewTrigger.kt

analytics/
  AnalyticsTracker.kt
  CrashlyticsInitializer.kt

navigation/
  AppNavigation.kt

SumGridApplication.kt      ← Firebase init, DI root, WorkManager trigger

ui/screens/
  OnboardingScreen.kt

play-store-assets/          ← Outside codebase: screenshots, feature graphic
```

### S03 Exit Gate (pre-submission)

```bash
# APK size check
./gradlew assembleRelease
ls -la app/build/outputs/apk/release/*.apk
# Must be < 4.5 MB

# Checklist:
# [ ] Firebase Analytics DebugView shows all 7 events
# [ ] Crashlytics receives test crash (5-tap debug trigger)
# [ ] Dark mode: all screens render correctly (manual test)
# [ ] Light mode: all screens render correctly (manual test)
# [ ] 360dp emulator: no clipping on grid, number pad, home screen
# [ ] Offline: airplane mode → solve puzzle → share works
# [ ] Onboarding: fresh install → 3 progressive puzzles → home screen
# [ ] Streak: day 1 → day 2 → streak = 2 (date override test)
# [ ] API 24 emulator: app runs, no crashes
# [ ] API 35 emulator: app runs, no crashes
# [ ] Font scale 1.3x: layout does not break
# [ ] 5 Play Store screenshots captured at 1080x1920
# [ ] Feature graphic at 1024x500
# [ ] Internal testing track submitted on Play Console
```

---

## Timeline

| Week | Sprint | Milestone |
|------|--------|-----------|
| 1–2 | S01 | Engine (PRNG + Validator + Generator + Calibrator + Test Suite) |
| 2–3 | S01 | Grid UI (GridRenderer + NumberPad + Sum Validation) |
| 3 | S01 | **S01 Gate: 10,000-puzzle test suite passes** |
| 4–5 | S02 | Daily system + completion + share + streak |
| 5–6 | S02 | Home screen + celebration animation |
| 6 | S02 | **S02 Gate: Full daily loop playable** |
| 7 | S03 | Theme + Firebase + Onboarding |
| 7–8 | S03 | Responsive polish + accessibility + review prompt |
| 8 | S03 | Screenshots + ASO listing + Play Store submission |
| 8 | S03 | **S03 Gate: Internal testing track live** |

**Target launch**: End of May 2026 (all of April + first half of May = ~7 calendar weeks from project start, with buffer week for Play Store review)

---

## Critical Path Summary

The critical path through the epic is linear:

```
S01-F001 (Xorshift128)
  → S01-F003 (Validator)
    → S01-F002 (Generator)
      → S01-F004 (Calibrator)
        → S01-F005 (10k Test Suite) ← S01 GATE
          → S02-F001 (DailyPuzzleRepo)
            → S02-F003 (CompletionDetection)
              → S02-F011 (HomeScreen) ← S02 GATE
                → S03-F001 (Theme)
                  → S03-F011 (Screenshots) ← S03 GATE → LAUNCH
```

Any delay in S01-F003 (UniqueSolutionValidator) delays the entire epic. This component must be prioritized and built first.

