# Epic 004 — Sprint Plan

**Sprints**: S2A → S2B → S2C (sequential, each gate-locked on prior completion)  
**Total estimate**: ~48h across 3 sprints  
**Updated**: 2026-03-14

---

## Sprint dependency graph

```
S2A (Critical Layout Fixes, ~12h)
 │
 ├── REQ-S2A-01  Fix difficulty card text truncation          [LazyRow]
 ├── REQ-S2A-02  Badge row: emoji-only + clickable handler    [feeds S2C-02]
 ├── REQ-S2A-03  Gray out unearned badges (graphicsLayer)
 ├── REQ-S2A-04  Empty cell dark-mode tint
 └── REQ-S2A-05  "Tap a cell to begin" hint                  [feeds S2C-06]
          │
          ▼  [S2A gate: all 5 must pass]
S2B (Layout and Spacing Polish, ~16h)
 │
 ├── REQ-S2B-01  Eliminate dead space — remove weight(1f)     [depends S2A complete]
 ├── REQ-S2B-02  Verify Home screen scrollability
 ├── REQ-S2B-03  Condense Today's Puzzles row (abbreviate)
 ├── REQ-S2B-04  Move timer into TopAppBar actions
 ├── REQ-S2B-05  Increase grid width — reduce padding
 ├── REQ-S2B-06  Distinguish undo/clear from digit buttons
 ├── REQ-S2B-07  Fix zero-streak display
 ├── REQ-S2B-08  Play button → amber/tertiary
 └── REQ-S2B-09  Improve unselected card visibility           [depends S2A-01 LazyRow]
          │
          ▼  [S2B gate: all 9 must pass]
S2C (UX Enhancements, ~20h)
 │
 ├── REQ-S2C-01  Completed ✓ overlay in difficulty cards      [depends S2A-01 LazyRow]
 ├── REQ-S2C-02  Badge detail bottom sheet                    [depends S2A-02 clickable]
 ├── REQ-S2C-03  Collapse badge row on short screens
 ├── REQ-S2C-04  Numpad enable/disable color animation
 ├── REQ-S2C-05  Remove/wire dgeek footer
 └── REQ-S2C-06  First-empty-cell pulse animation             [depends S2A-05 hint param]
          │
          ▼  [S2C gate: all 6 must pass]
       DONE
```

---

## Sprint 2A — Critical Layout Fixes (~12h)

**Gate requirement**: All 5 features complete before S2B begins.  
**Minimum shippable**: Yes — if only S2A ships, the app is no longer visually broken.

| Feature ID | Req ID | Title | Effort | Severity | Files affected |
|-----------|--------|-------|--------|----------|----------------|
| S2A-F001 | REQ-S2A-01 | Fix difficulty card text truncation | ~4h | Critical | `DifficultySelector.kt` |
| S2A-F002 | REQ-S2A-02 | Badge row: emoji-only + tap handler | ~3h | High | `HomeScreen.kt` |
| S2A-F003 | REQ-S2A-03 | Gray out unearned badges via graphicsLayer | ~2h | High | `HomeScreen.kt` |
| S2A-F004 | REQ-S2A-04 | Empty cell dark-mode tint | ~1h | High | `GridRenderer.kt` |
| S2A-F005 | REQ-S2A-05 | "Tap a cell to begin" hint | ~2h | High | `PuzzleScreen.kt` |

**Dependency edges within S2A**:
- S2A-F002 must precede S2A-F003 (both modify `BadgeItem`; sequence avoids merge conflict)
- All others are independent and can be implemented in parallel

**Implementation order** (recommended):
1. S2A-F001 (highest risk, most visible fix)
2. S2A-F004 (isolated, 1h, confirms token approach for rest)
3. S2A-F002 + S2A-F003 (same file, do in sequence)
4. S2A-F005 (isolated PuzzleScreen change)

---

## Sprint 2B — Layout and Spacing Polish (~16h)

**Gate requirement**: S2A fully complete.  
**Key dependency**: REQ-S2B-09 requires the LazyRow from REQ-S2A-01 to be in place (card styling changes only make sense in the scrollable container).

| Feature ID | Req ID | Title | Effort | Severity | Files affected | Depends on |
|-----------|--------|-------|--------|----------|----------------|------------|
| S2B-F001 | REQ-S2B-01 | Eliminate dead space (remove weight=1f) | ~4h | High | `PuzzleScreen.kt` | — |
| S2B-F002 | REQ-S2B-02 | Verify Home screen scrollability | ~1h | High | `HomeScreen.kt` | — |
| S2B-F003 | REQ-S2B-03 | Abbreviate Today's Puzzles chips | ~2h | Medium | `HomeScreen.kt` | — |
| S2B-F004 | REQ-S2B-04 | Move timer into TopAppBar | ~2h | Low | `PuzzleScreen.kt` | S2B-F001 (same file, do after layout restructure) |
| S2B-F005 | REQ-S2B-05 | Increase grid width (reduce padding) | ~1h | Medium | `PuzzleScreen.kt`, `GridRenderer.kt` | S2B-F001 (layout must be stable first) |
| S2B-F006 | REQ-S2B-06 | Distinguish undo/clear buttons | ~2h | Medium | `NumberPad.kt` | — |
| S2B-F007 | REQ-S2B-07 | Fix zero-streak display | ~1h | Medium | `HomeScreen.kt` | — |
| S2B-F008 | REQ-S2B-08 | Play button → amber/tertiary | ~1h | Medium | `HomeScreen.kt` | — |
| S2B-F009 | REQ-S2B-09 | Improve unselected card visibility | ~1h | Medium | `DifficultySelector.kt` | S2A-F001 (LazyRow in place) |

**Recommended implementation order**:
1. S2B-F001 (PuzzleScreen layout restructure — highest risk, do first)
2. S2B-F004 (timer into TopAppBar — same file, while PuzzleScreen is open)
3. S2B-F005 (grid width — PuzzleScreen + GridRenderer, while open)
4. S2B-F006 (NumberPad — independent)
5. S2B-F002, S2B-F003, S2B-F007, S2B-F008 (HomeScreen changes — group into one edit session)
6. S2B-F009 (DifficultySelector — independent)

---

## Sprint 2C — UX Enhancements (~20h)

**Gate requirement**: S2A and S2B fully complete.  
**Key dependencies**:
- S2C-F002 depends on S2A-F002 (the `clickable` modifier and `selectedBadge` state must already exist)
- S2C-F001 depends on S2A-F001 (needs `LazyRow` + `DifficultyCard` to accept `isCompleted`)
- S2C-F006 depends on S2A-F005 (the `pulsingCell` parameter stub is introduced with the hint text in S2A)

| Feature ID | Req ID | Title | Effort | Severity | Files affected | Depends on |
|-----------|--------|-------|--------|----------|----------------|------------|
| S2C-F001 | REQ-S2C-01 | Completed ✓ overlay in difficulty cards | ~2h | Medium | `DifficultySelector.kt`, `HomeScreen.kt` | S2A-F001 |
| S2C-F002 | REQ-S2C-02 | Badge detail bottom sheet | ~4h | Medium | `HomeScreen.kt`, new `BadgeDetailBottomSheet.kt` | S2A-F002 |
| S2C-F003 | REQ-S2C-03 | Collapse badge row on short screens | ~3h | Medium | `HomeScreen.kt` | S2A-F002, S2C-F002 |
| S2C-F004 | REQ-S2C-04 | Numpad enable/disable color animation | ~1h | Low | `NumberPad.kt` | — |
| S2C-F005 | REQ-S2C-05 | Remove/wire dgeek footer | ~0.5h | Low | `HomeScreen.kt` | — |
| S2C-F006 | REQ-S2C-06 | First-empty-cell pulse animation | ~4h | Medium | `GridRenderer.kt`, `PuzzleScreen.kt` | S2A-F005 |

**Recommended implementation order**:
1. S2C-F005 (30 min, trivial — do first to clear backlog)
2. S2C-F004 (1h, isolated NumberPad — low risk)
3. S2C-F001 (DifficultySelector completion overlay — builds on established card structure)
4. S2C-F002 (BadgeDetailBottomSheet — new file, medium complexity)
5. S2C-F003 (Collapse badge row — depends on S2C-F002 bottom sheet being available)
6. S2C-F006 (Pulse animation — highest complexity, do last when layout is stable)

---

## Cross-sprint dependency summary

| Dependency | From | To | Type |
|------------|------|----|------|
| LazyRow structure in place | S2A-F001 | S2B-F009 | Card styling requires scrollable container |
| LazyRow structure in place | S2A-F001 | S2C-F001 | Completion overlay added to existing cards |
| `clickable` + `selectedBadge` state | S2A-F002 | S2C-F002 | Bottom sheet wired to existing tap handler |
| Badge row structure | S2A-F002 | S2C-F003 | Collapse replaces badge row from S2A |
| `pulsingCell` param stub | S2A-F005 | S2C-F006 | Pulse uses the GridRenderer param added in S2A |
| PuzzleScreen layout stable | S2B-F001 | S2B-F004, S2B-F005 | Timer and grid width changes require stable Column |
| Badge bottom sheet exists | S2C-F002 | S2C-F003 | Collapse chip opens the same bottom sheet |

---

## Global acceptance criteria checklist

Tracked across all 3 sprints:

- [ ] Difficulty card labels never truncate on >= 320dp width (S2A-F001)
- [ ] Badge row shows emoji only; labels accessible via tap (S2A-F002)
- [ ] Earned badges full color; unearned visually muted/grayscale (S2A-F003)
- [ ] Empty grid cells have visible tint in dark mode (S2A-F004)
- [ ] New user sees "Tap an empty cell to start" hint (S2A-F005)
- [ ] Puzzle screen gap between grid and numpad <= 24dp (S2B-F001)
- [ ] Home screen scrollable on 600dp height device (S2B-F002)
- [ ] Today's Puzzles row readable at 320dp with 5 difficulties (S2B-F003)
- [ ] Timer in TopAppBar, not standalone (S2B-F004)
- [ ] Grid horizontal margin <= 8dp each side (S2B-F005)
- [ ] Undo/clear visually distinct from digit buttons (S2B-F006)
- [ ] Zero streak shows "Start your streak!" (S2B-F007)
- [ ] Play button uses amber/tertiary CTA color (S2B-F008)
- [ ] Completed difficulty cards show ✓ indicator (S2C-F001)
- [ ] Badge detail bottom sheet opens on tap (S2C-F002)
- [ ] dgeek footer removed or tappable (S2C-F005)
- [ ] First empty cell pulses on puzzle load (S2C-F006)
- [ ] No hardcoded hex, rgb, or arbitrary px values in changed files (all sprints)
- [ ] Existing functionality unchanged: 5 difficulties, share, undo, streak (all sprints)
