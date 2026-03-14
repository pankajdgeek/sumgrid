# Sprint Plan — Epic 003: SumGrid UX/UI Overhaul

**Epic**: 003-sumgrid-uxui-overhaul-wire-share-system  
**Date**: 2026-03-14  
**Total sprints**: 4  
**Total estimated hours**: ~141h  
**Sequencing**: S01 → S02 → S03 → S04 (each sprint is independently shippable)

---

## Sprint Dependency Graph

```
S01: Quick Wins & Critical Fixes (~15h)
  ├── S01-F001: Wire share button
  ├── S01-F002: Display streak badges     ← depends on: HomeViewModel StreakRepository fix (BLOCKER)
  ├── S01-F003: Skip option in onboarding
  ├── S01-F004: Rules overlay in onboarding
  ├── S01-F005: Back/exit button (TopAppBar)
  ├── S01-F006: Make HomeScreen scrollable
  └── S01-F007: All-done state HomeScreen
         │
         ▼ (S01 must ship before S02 starts)
         
S02: Core Game UX (~36h)
  ├── S02-F001: Completion card            ← depends on: S01-F001 (share button proven in UI)
  ├── S02-F002: Undo functionality         ← no S01 dependency; can start in parallel with S02-F001
  ├── S02-F003: 2-row number pad           ← no S01 dependency
  ├── S02-F004: Dark mode contrast audit   ← no S01 dependency
  ├── S02-F005: Font scaling test & fix    ← no S01 dependency
  └── S02-F006: State persistence / auto-save  ← no S01 dependency
         │
         ▼ (S02 must ship before S03 starts)

S03: Retention & Engagement (~42h)
  ├── S03-F001: Statistics screen          ← depends on: S02-F006 (persistence layer extended)
  ├── S03-F002: Daily push notification   ← no S02 dependency (parallel with S03-F001)
  ├── S03-F003: Practice mode             ← no S02 dependency (parallel with S03-F001/S03-F002)
  └── S03-F004: Enhanced celebration      ← depends on: S02-F001 (completion card must exist to coordinate)
         │
         ▼ (S03 must ship before S04 starts)

S04: Polish & Accessibility (~48h)
  ├── S04-F001: Reduce motion support      ← no S03 dependency
  ├── S04-F002: Error indicator animation  ← no S03 dependency
  ├── S04-F003: Pencil/notes mode          ← depends on: S02-F002 (undo pattern established)
  ├── S04-F004: Landscape layout or lock   ← no S03 dependency
  ├── S04-F005: TalkBack focus ordering    ← no S03 dependency
  └── S04-F006: Hard & Expert difficulty   ← depends on: S02-F003 (2-row number pad must exist)
```

---

## Exact Dependency Edges

| Feature | Depends on | Dependency type | Rationale |
|---------|-----------|-----------------|-----------|
| S02-F001 (CompletionCard) | S01-F001 (share button) | Soft (shares share CTA pattern) | CompletionCard embeds the share button; S01-F001 proves the context/intent API works in the UI |
| S03-F001 (StatsScreen) | S02-F006 (auto-save) | Hard | Stats reads historical completion data; reliable persistence must exist first |
| S03-F004 (celebration) | S02-F001 (CompletionCard) | Hard | Enhanced celebration fires after CompletionCard appears; the card controls the completion UI layer |
| S04-F003 (pencil mode) | S02-F002 (undo) | Soft (pattern consistency) | Both modify PuzzleViewModel state management; establishing undo history first informs the notes state design |
| S04-F006 (Hard/Expert) | S02-F003 (2-row pad) | Hard | 6x6 and 7x7 need adequate number pad layout; single-row is broken for maxVal=9 |

---

## Sprint 1: Quick Wins & Critical Fixes

**Estimated**: ~15 hours | **Target**: 1 week  
**Goal**: Ship all free wins. Zero new architecture required.

### Pre-conditions
- **BLOCKER to resolve first**: `HomeViewModel` must receive `StreakRepository` (DataStore-backed) instead of `InMemoryStreakRepository`. This is a one-file fix in `SumGridNavigation.kt` + `HomeViewModel.kt`. Without this, S01-F002 (badge display) shows empty badges at runtime.

### Task sequence within S01

**Parallel group A (can be done simultaneously — all single-file changes):**
- S01-F001: Wire share button (~2h) — modify `PuzzleScreen.kt`
- S01-F003: Skip option in onboarding (~2h) — modify `OnboardingScreen.kt`
- S01-F004: Rules overlay in onboarding (~3h) — modify `OnboardingScreen.kt` (same file, sequence after F003)
- S01-F005: Back/exit button (~2h) — modify `PuzzleScreen.kt` (same file as F001, sequence after F001)
- S01-F006: Make HomeScreen scrollable (~1h) — modify `HomeScreen.kt`
- S01-F007: All-done state HomeScreen (~2h) — modify `HomeScreen.kt` (same file as F006, sequence after F006)

**Requires BLOCKER fix first:**
- S01-F002: Display streak badges (~3h) — modify `HomeScreen.kt` + `HomeViewModel.kt`

**Recommended parallelization within S01:**
```
[BLOCKER FIX: StreakRepository wiring] (~1h)
  │
  ├─── S01-F001 + S01-F005 (both in PuzzleScreen.kt) — sequential, ~4h total
  ├─── S01-F002 (HomeScreen badges after BLOCKER) — ~3h
  ├─── S01-F003 + S01-F004 (both in OnboardingScreen.kt) — sequential, ~5h total
  └─── S01-F006 + S01-F007 (both in HomeScreen.kt) — sequential, ~3h total
```

### Integration gate (S01 → S02)
- [ ] Share button visible on PuzzleScreen completion state
- [ ] At least 1 badge visible on HomeScreen (or all locked/grayed if streak < 7)
- [ ] "Skip tutorial" link navigates to HomeScreen
- [ ] PuzzleScreen TopAppBar back arrow navigates to HomeScreen
- [ ] HomeScreen scrolls on 360dp viewport (test in emulator)
- [ ] All-done state shows when all 3 difficulties complete

---

## Sprint 2: Core Game UX

**Estimated**: ~36 hours | **Target**: 1–2 weeks  
**Goal**: Build the #1 retention touchpoint (completion card), add undo, fix layout issues, ensure persistence.

### Pre-conditions
- S01 integration gate must pass
- S01-F001 (share button) must be working — CompletionCard reuses the same share logic

### Task sequence within S02

**High complexity, start first:**
- S02-F001: Completion card / bottom sheet (~8h) — new file `CompletionCard.kt` + modify `PuzzleScreen.kt`
- S02-F002: Undo functionality (~6h) — modify `PuzzleViewModel.kt` + `NumberPad.kt`

**Medium complexity, parallel with above:**
- S02-F003: 2-row number pad (~4h) — modify `NumberPad.kt` (can parallelize if different developer from S02-F002)
- S02-F004: Dark mode contrast audit (~6h) — modify `Theme.kt`, `GridRenderer.kt`
- S02-F005: Font scaling test & fix (~6h) — app-wide layout audit
- S02-F006: State persistence / auto-save (~6h) — modify `PuzzleViewModel.kt`, `CompletionStore.kt`, `DataStoreCompletionStore.kt`

**Note on S02-F002 and S02-F006 both touching PuzzleViewModel:**  
These should be sequenced — complete S02-F002 (undo) first since it is a smaller, cleaner change, then S02-F006 (auto-save) which touches persistence lifecycle.

**Recommended parallelization within S02:**
```
S02-F001 (CompletionCard)           — ~8h  [start immediately]
S02-F002 → S02-F006 (VM changes)   — ~12h total, sequential [start immediately, different developer]
S02-F003 (NumberPad 2-row)         — ~4h  [parallel, independent]
S02-F004 (dark mode)               — ~6h  [parallel, independent]
S02-F005 (font scaling)            — ~6h  [parallel, independent]
```

### Integration gate (S02 → S03)
- [ ] CompletionCard shows on puzzle completion with solve time, difficulty badge, share CTA, next-puzzle and back-home buttons
- [ ] Undo button visible on NumberPad; `canUndo` correctly reflects stack state
- [ ] Medium (5x5) puzzle shows 2-row number pad
- [ ] Dark mode contrast ratio ≥ 4.5:1 for given vs. user cells (verified via `DarkModeContrastTest`)
- [ ] 200% font scale: no overflow on HomeScreen or NumberPad
- [ ] Mid-puzzle state restored after process kill (test via "Don't keep activities" developer option)

---

## Sprint 3: Retention & Engagement

**Estimated**: ~42 hours | **Target**: 2 weeks  
**Goal**: Drive repeat usage with statistics, notifications, unlimited practice content, and a satisfying win moment.

### Pre-conditions
- S02 integration gate must pass
- S02-F006 (auto-save) must be complete — StatsScreen depends on reliable completion data

### Task sequence within S03

**Parallel group (all can start simultaneously — independent subsystems):**
- S03-F001: Statistics screen (~12h) — new `StatsScreen.kt`, `StatsViewModel.kt`
- S03-F002: Daily push notification (~8h) — new `DailyReminderWorker.kt`, `NotificationScheduler.kt`
- S03-F003: Practice mode (~16h) — new `PracticeScreen.kt`, `PracticeViewModel.kt`
- S03-F004: Enhanced celebration (~6h) — modify `CelebrationAnimation.kt`; **sequence after S03-F001/F002/F003 are underway** since it only modifies `CelebrationAnimation.kt` independently

**Note**: S03-F003 is the largest feature (L = ~16h). Start it first or in parallel with S03-F001 to avoid becoming the bottleneck.

**Recommended parallelization within S03:**
```
S03-F003 (PracticeScreen)      — ~16h [start day 1, longest path]
S03-F001 (StatsScreen)         — ~12h [start day 1, parallel]
S03-F002 (Notifications)       — ~8h  [start day 2, after nav route groundwork in F003 is visible]
S03-F004 (Celebration)         — ~6h  [start day 3, independent, can complete in parallel]
```

Navigation additions (Stats + Practice routes in `SumGridNavigation.kt`) can be a shared prerequisite task (~1h) done before F001 and F003 start.

### Integration gate (S03 → S04)
- [ ] Stats screen accessible from HomeScreen; shows total solved, avg/best times per difficulty
- [ ] Calendar heatmap renders at least 30 days of history
- [ ] "Practice" mode button visible on HomeScreen all-done state; generates random puzzles
- [ ] Practice puzzles do not affect streak counter
- [ ] Daily notification opt-in flow works (permission dialog → schedule → notification fires at test time)
- [ ] Enhanced celebration (confetti or equivalent) fires on completion
- [ ] Celebration animation respects future reduce-motion flag (tie-in with S04-F001)

---

## Sprint 4: Polish & Accessibility

**Estimated**: ~48 hours | **Target**: 1–2 weeks  
**Goal**: Close all accessibility gaps, add power-user features (pencil mode, new difficulties), ensure landscape behavior.

### Pre-conditions
- S03 integration gate must pass
- S02-F003 (2-row number pad) must be complete before S04-F006 (Hard/Expert)
- S02-F002 (undo) should be stable before S04-F003 (pencil mode, touches same ViewModel)

### Task sequence within S04

**Parallel group (all independent):**
- S04-F001: Reduce motion (~3h) — modify `HomeScreen.kt`, `GridRenderer.kt`
- S04-F002: Error indicator animation (~3h) — modify `GridRenderer.kt`
- S04-F004: Landscape layout or portrait lock (~6h) — `AndroidManifest.xml` + optional layout changes
- S04-F005: TalkBack focus ordering (~4h) — modify `PuzzleScreen.kt`

**Requires S02-F002 (undo) complete:**
- S04-F003: Pencil/notes mode (~16h) — modify `PuzzleViewModel.kt`, `GridRenderer.kt`, `NumberPad.kt`

**Requires S02-F003 (2-row pad) complete:**
- S04-F006: Hard & Expert difficulty levels (~16h) — modify `Difficulty.kt`, test engine coverage

**Note**: S04-F003 and S04-F006 are both Large (16h) features. They touch different files (VM/GridRenderer vs. Difficulty enum/engine), so can run in parallel if resources allow.

**Recommended parallelization within S04:**
```
S04-F001 + S04-F002 (animations)    — ~6h total [start day 1, fast]
S04-F004 (landscape)                — ~6h [start day 1, parallel]
S04-F005 (TalkBack)                 — ~4h [start day 1, parallel]
S04-F003 (pencil mode)              — ~16h [start day 1, longest path]
S04-F006 (Hard/Expert)              — ~16h [start day 1, parallel to F003]
```

### Integration gate (epic complete)
- [ ] Pulsing flame animation disabled when Android "Remove animations" / "Reduce motion" is on
- [ ] Error sum indicator shakes/flashes on transition to RED state
- [ ] Pencil mode toggle visible; candidate numbers render in cell corners for Medium+
- [ ] App does not distort in landscape (either proper layout or portrait lock in manifest)
- [ ] TalkBack traversal: timer → grid → number pad (verified with TalkBack enabled)
- [ ] "Puzzle complete" accessibility announcement fires on completion
- [ ] Hard (6x6) puzzle is selectable and playable
- [ ] Expert (7x7) puzzle is selectable and playable
- [ ] All 31 UX review findings addressed

---

## Effort Summary

| Sprint | Features | Estimated Hours | Complexity profile |
|--------|----------|----------------|--------------------|
| S01 | 7 | ~15h | All Small — fast, high impact, no new architecture |
| S02 | 6 | ~36h | Mix of Medium — new CompletionCard, ViewModel extensions |
| S03 | 4 | ~42h | 2 Medium + 2 Large — new screens, new subsystems |
| S04 | 6 | ~48h | 2 Large + 4 Small/Medium — pencil mode + difficulty are L |
| **Total** | **20** | **~141h** | |

---

## Critical Path

The critical path through the epic is:

```
BLOCKER fix (StreakRepository)  ~1h
  → S01 Quick Wins              ~15h
      → S02 Core UX             ~36h  [longest sequential dependency chain]
          → S03-F003 Practice   ~16h  [longest parallel task within S03]
              → S04-F003 Pencil ~16h  [longest parallel task within S04]
```

**Critical path total**: ~84h (serial minimum).  
**Actual delivery time** with 2-person parallelism: approximately 6–8 weeks.

---

## Risk Register

| Risk | Sprint | Probability | Impact | Mitigation |
|------|--------|-------------|--------|------------|
| StreakRepository not wired (HomeViewModel) | S01 | Confirmed | High | **BLOCKER** — fix before any S01 work starts |
| WorkManager not in Gradle dependencies | S03 | Low | Medium | Check `build.gradle.kts` at sprint start; add if absent |
| DataStore prefix-scan for stats getAll() | S03 | Medium | Low | Use `data.first().asMap().filterKeys { it.name.startsWith("completion_") }` |
| Pencil mode increases PuzzleUiState size | S04 | Low | Low | `Array<Array<Set<Int>>>` for notes is bounded; acceptable for ≤7x7 grid |
| Hard/Expert puzzle generation time (backtracking) | S04 | Medium | Medium | Pre-compute puzzles using `PuzzlePrecomputeWorker` (already exists); extend for new difficulties |
| POST_NOTIFICATIONS permission denied on Android 13+ | S03 | Medium | Medium | Graceful degradation — show in-app reminder if permission denied |

