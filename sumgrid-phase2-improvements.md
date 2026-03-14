# SumGrid — Phase 2 UX/UI Improvements

*Based on updated screenshots (post-Phase 1) on Oppo device, March 14 2026.*

---

## Phase 1 Wins (Already Shipped)

Before diving into Phase 2, here's what's been successfully implemented:

- ✅ Back button + difficulty label in TopAppBar ("← Medium")
- ✅ 2-row number pad for Medium/Hard/Expert (1-5 top, 6-9 + undo + clear bottom)
- ✅ Undo button (↶) added to number pad
- ✅ Share button wired to puzzle completion screen
- ✅ Streak badges now visible on Home screen (4 emoji badges in a row)
- ✅ Hard (6×6, ~10-20 min) and Expert (7×7, ~15-30 min) difficulty levels added
- ✅ Today's Puzzles expanded to 5 difficulties with completion tracking

---

## New Issues Found in Updated Screenshots

### Home Screen — Layout Breaking Under New Content

**P0-1. Difficulty card text is truncating/word-breaking**
With 5 difficulty cards now in a row, the text is breaking mid-word: "Begin ner", "Mediu m", "Exper t". The labels are unreadable. This is the most visually broken element in the app right now.
- **Severity**: Critical (broken text renders)
- **Fix options**:
  - A) Switch to a horizontally scrollable row with fixed-width cards (~80dp each). Show 3.5 cards with a peek to hint at scrolling.
  - B) Use abbreviated labels: "3×3", "4×4", "5×5", "6×6", "7×7" as primary labels, with difficulty names as small secondary text.
  - C) Switch to a 2-row grid (3 top + 2 bottom) or a dropdown/segmented control.
- **Recommended**: Option A (scroll row) — it scales to any number of difficulties and keeps cards readable.

**P0-2. Badge row labels are truncated and cramped**
The 4 badges (🔥 Weekly Warrior, ⭐ Monthly Master, 💎 Century Solver, 👑 Year of Logic) are squeezed into a single row. Labels are cut off — you can barely read them on the Oppo screen width.
- **Severity**: High (badges are supposed to motivate; illegible labels undermine that)
- **Fix options**:
  - A) Use emoji-only display with tooltips on tap (🔥 ⭐ 💎 👑 as large circular badges, tap to reveal name).
  - B) 2×2 grid layout with full names.
  - C) Horizontally scrollable badge carousel with larger cards.
- **Recommended**: Option A — emoji-only badges are cleaner and more game-like. Show name + description in a bottom sheet or tooltip on tap.

**P0-3. Today's Puzzles row is also cramped with 5 items**
The status chips (✓ Beginner, ○ Easy, ○ Medium, ○ Hard, ○ Expert) are tight. Text is still readable but spacing feels compressed.
- **Severity**: Medium
- **Fix**: Reduce chip text to just difficulty initials or abbreviations (B, E, M, H, X) with the icon. Or use a progress bar: "1/5 solved" with colored segments.

**P0-4. Home screen is vertically overloaded**
With badges, 5 status chips, countdown, 5 difficulty cards, and the Play button, the Home screen is trying to show too much in one view. On shorter devices, content likely overflows.
- **Severity**: High
- **Fix**: Wrap in a scroll container (if not already). Consider collapsing the badge row into a single "🏆 Badges" tappable chip that opens a detail view. Move countdown below the Play button or into a secondary area.

**P1-5. Still showing "🔥 0 days streak"**
From Phase 1 review — this wasn't addressed. The pulsing flame on zero still looks odd.
- **Severity**: Medium
- **Fix**: Show "Start your streak!" or hide the section entirely when streak = 0.

**P1-6. "More by dgeek" still visible as dead text**
Partially visible at the bottom. Still not tappable.
- **Severity**: Low
- **Fix**: Remove or make tappable.

---

### Puzzle Screen (Medium 5×5) — Layout & Interaction Polish

**P1-7. Massive dead space between grid and number pad**
The 5×5 grid occupies roughly the top 55% of the screen. The 2-row number pad sits at the very bottom. There's ~30% of the screen that's empty black space. Feels like a gap in the design.
- **Severity**: High
- **Fix options**:
  - A) Center the grid vertically and push the number pad directly below it with ~16dp gap.
  - B) Add useful content in the gap: difficulty label chip, hint button, or a subtle "Tap a cell to begin" prompt.
  - C) Use a `verticalArrangement = Arrangement.SpaceEvenly` to distribute elements instead of `Spacer(weight(1f))`.
- **Recommended**: Option A + B — center grid, pad below, and add a "tap a cell" hint in the transition area.

**P1-8. No "tap a cell to begin" guidance**
Number pad buttons are all gray/disabled on load. A first-time player sees a grid and grayed-out buttons with no instruction. The puzzle screenshot at 00:00 timer confirms nothing is interactive until a cell is tapped.
- **Severity**: High (first-time confusion)
- **Fix**: Show an animated hint — either a pulsing glow on the first empty cell, or a centered text "Tap an empty cell to start" that disappears after the first tap.

**P1-9. Undo and Clear buttons don't stand out from number buttons**
In the 2-row layout, the undo (↶) and clear (✕) buttons use the same outlined style as the rest. At a glance, they blend in with the numbers. Users need to scan to find them.
- **Severity**: Medium
- **Fix**: Give action buttons a distinct visual treatment:
  - Undo: Use a subtle surface tint or secondary color outline.
  - Clear: Keep the existing error/red outline but make it slightly bolder.
  - Add a small separator (thin vertical line or extra spacing) between numbers and action buttons.

**P1-10. Empty cells invisible against dark background**
Still the same issue from Phase 1 — empty cells on the 5×5 grid (e.g., row 2 which is entirely empty) are nearly indistinguishable from the black background. Only the thin grid lines give any clue.
- **Severity**: High
- **Fix**: Apply a very subtle background to empty editable cells: `surfaceVariant.copy(alpha = 0.08f)` or similar. Just enough to see the cell boundaries without grid lines.

**P1-11. Timer placement is disconnected from the game context**
Timer "00:00" sits between the TopAppBar ("← Medium") and the grid. It's small, centered, and feels like floating metadata rather than part of the game.
- **Severity**: Low
- **Fix**: Move the timer into the TopAppBar as a trailing element (right side): "← Medium ··· 00:00". This consolidates the top bar and frees up vertical space.

**P1-12. Grid doesn't use full available width**
The 5×5 grid has visible horizontal padding on both sides. On a phone this narrow, every pixel matters. The grid could expand slightly to make cells larger and more tappable.
- **Severity**: Medium
- **Fix**: Reduce horizontal padding from what appears to be 16dp to 8dp or even 4dp for the grid area. Sum targets on the right need space, but the left edge can be tighter.

---

### Visual Design & Polish

**P2-13. Play button is still muted indigo**
The Play button uses `primaryContainer` (lavender/indigo), the same palette as everything else. It doesn't pop as a primary CTA on the dark background.
- **Severity**: Medium
- **Fix**: Use `tertiaryContainer` (warm amber) for the Play button. It's the natural attention-grabbing color already used for the streak bar.

**P2-14. Difficulty cards selected state could be stronger**
The selected card (Beginner or Medium) has a blue border, but the unselected cards are barely visible — dark rectangles with faint borders and truncated text.
- **Severity**: Medium (worsened by the truncation bug)
- **Fix**: Once truncation is fixed (P0-1), give unselected cards a more visible `surfaceVariant` background (alpha 0.15) and a clearer outline.

**P2-15. No visual feedback for completed vs. available puzzles in difficulty selector**
Beginner shows ✓ in Today's Puzzles but the difficulty card for Beginner looks identical to others. Users can't tell at a glance which puzzles they've already solved from the selector.
- **Severity**: Medium
- **Fix**: Add a small ✓ overlay or subtle green tint to difficulty cards for completed puzzles. Or show "Replay" as the button text when all puzzles for that difficulty are done.

**P2-16. Badge icons are not grayed when unearned**
All 4 badges appear in full color (🔥 ⭐ 💎 👑) even though the user has 0 days streak and clearly hasn't earned any of them. This removes the motivational "unlock" feeling.
- **Severity**: High
- **Fix**: Show unearned badges as grayscale/locked (🔒 or dimmed emoji). Only show full-color badges when earned. This creates a clear progression path.

---

## Phase 2 Prioritized Task List

### Sprint 2A: Critical Layout Fixes (~12h, immediate)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 1 | Fix difficulty card text truncation (scrollable row or abbreviated labels) | ~4h | Critical — text is visibly broken |
| 2 | Fix badge row truncation (emoji-only with tap-to-reveal) | ~3h | High — badges are unreadable |
| 3 | Gray out unearned badges | ~2h | High — kills the progression feeling |
| 4 | Add empty cell background tint in dark mode | ~1h | High — cells are invisible |
| 5 | Add "tap a cell to begin" hint on puzzle load | ~2h | High — new users are confused |

### Sprint 2B: Layout & Spacing Polish (~16h)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 6 | Reduce dead space on puzzle screen (center grid + pad) | ~4h | High — 30% wasted screen |
| 7 | Make Home screen scrollable (if not already) | ~1h | High — overflows on short devices |
| 8 | Condense Today's Puzzles row (abbreviations or progress bar) | ~2h | Medium |
| 9 | Move timer into TopAppBar trailing slot | ~2h | Low — frees vertical space |
| 10 | Increase grid width (reduce horizontal padding) | ~1h | Medium — bigger touch targets |
| 11 | Visually distinguish undo/clear from number buttons | ~2h | Medium |
| 12 | Fix zero-streak display | ~1h | Medium |
| 13 | Switch Play button to amber/tertiary CTA color | ~1h | Medium |
| 14 | Improve unselected difficulty card visibility | ~1h | Medium |

### Sprint 2C: UX Enhancements (~20h)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 15 | Mark completed puzzles in difficulty selector (✓ overlay) | ~2h | Medium |
| 16 | Build badge detail bottom sheet (tap badge to see name + progress) | ~4h | Medium |
| 17 | Collapse badge row into "🏆 Badges" chip if screen is short | ~3h | Medium |
| 18 | Add number pad enable/disable transition animation | ~1h | Low |
| 19 | Remove or link "More by dgeek" footer | ~30min | Low |
| 20 | Add first-empty-cell pulse animation for onboarding | ~4h | Medium |

---

## Summary

Phase 1 successfully shipped the structural features (back button, undo, 2-row pad, badges, new difficulties, share). Phase 2 is about fixing the layout breakage caused by adding more content to a screen designed for less, and polishing the visual gaps that remain.

The single most urgent fix is **difficulty card text truncation** — it's visibly broken in the current build and affects every user on every session. Sprint 2A tasks should ship within a few days; they're all small, high-impact fixes.

**Phase 2 total estimate: ~48h across 3 sprints.**

---

*Review date: March 14, 2026 · Device: Oppo (dark mode) · Build: Post-Phase 1*
