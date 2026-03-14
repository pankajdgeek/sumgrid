# SumGrid — UX/UI Improvements (Screenshot Review)

*Based on hands-on visual review of 6 app screenshots on Oppo device + full source-code audit.*

---

## Visual Issues Identified from Screenshots

### Home Screen (`01_home_screen.png`)

**1. "0 days streak" with pulsing flame is discouraging**
The amber streak bar proudly shows "🔥 0 days streak" for a new user. A pulsing fire icon next to zero feels ironic and deflating. First impressions matter.
- **Fix**: When streak = 0, show "Start your streak today!" or hide the streak section entirely. Only show the flame animation when streak ≥ 1.

**2. Play button blends into the background**
The Play button uses a muted indigo/lavender (`primaryContainer`) that barely stands out from the dark surface. On a dark theme, the CTA should pop.
- **Fix**: Use the amber/warm-amber tertiary color for the Play button. It's the natural eye-catching color already used for the streak bar. Make the button the brightest element on screen.

**3. Difficulty cards lack visual feedback for unselected state**
"Easy" and "Medium" cards look almost identical to the dark background — faint outline, dark fill. Only the selected card (Beginner) has a visible blue border and slightly lighter background.
- **Fix**: Give unselected cards a more visible outline or a slightly lighter surface color so they don't disappear into the background.

**4. "More by dgeek" is dead text at the bottom**
Visible in the screenshot but not tappable. It looks like it should be a link but does nothing. Broken affordance.
- **Fix**: Either make it a tappable link (open portfolio URL) or remove it entirely.

**5. Status chips are cryptic**
The three "○ Beginner / ○ Easy / ○ Medium" chips under "Today's Puzzles" use a small circle icon. New users won't know these represent today's completion status.
- **Fix**: Add a subtle label like "Solved: 0/3" or use checkbox-style icons (☐/☑) that are more universally understood.

---

### Puzzle Screen — Initial State (`02_easy_4x4_game.png`)

**6. Massive dead space between grid and number pad**
The 4×4 grid sits in the top third of the screen, and the number pad sits at the very bottom. The entire middle ~50% of the screen is empty black space. Feels like an unfinished layout.
- **Fix**: Either center the grid vertically, push the number pad closer to the grid, or use the empty space for contextual info (difficulty label, hint button, undo button). Consider a layout where the grid is centered and the pad floats just below it.

**7. No top bar, no back button, no context**
The only element above the grid is "00:00" in small text. There's no title, no difficulty label, no back/exit button. Users who land here have zero context about what difficulty they're playing.
- **Fix**: Add a TopAppBar with: back arrow, difficulty label ("Easy · 4×4"), and the timer. This also gives users a visible exit path.

**8. Number pad buttons look disabled even though puzzle just loaded**
All 7 number buttons + clear are gray/muted because no cell is selected. This is correct behavior, but the initial impression is "nothing works here." There's no hint that you need to tap a cell first.
- **Fix**: Add a subtle prompt text above the number pad or inside the grid area: "Tap a cell to begin" on first load. Disappears after first cell tap.

**9. Given cells (blue) vs empty cells (near-black) — poor empty cell visibility**
Given cells are clearly blue, but empty cells are nearly the same shade as the pure black background. They're only distinguishable by the faint grid lines. It's hard to tell where the grid ends and the background begins.
- **Fix**: Give empty cells a slightly lighter background (e.g., `surface` or `surfaceVariant` at 10-15% opacity) so they're visually distinct from the void.

**10. Timer starts at 00:00 and doesn't move until first cell tap**
The timer shows 00:00 on load. Users might think it's frozen or a label. There's no indication the timer is waiting for their first move.
- **Fix**: Either auto-start the timer on screen load (simpler) or show "Tap to start" near the timer. The current "start on first cell tap" is clever but confusing.

---

### Puzzle Screen — Cell Selected (`03_cell_selected.png`)

**11. Selection border is thin and easy to miss**
The selected cell (R1C4) has a thin amber/yellow outline. On a large dark cell, this thin border doesn't draw enough attention. A user scanning quickly might not realize which cell is active.
- **Fix**: Make the selection border thicker (3-4dp instead of ~1.5dp). Consider adding a subtle background fill (amber at 15% opacity) inside the selected cell, not just an outline.

**12. Number pad state change is abrupt**
When a cell is selected, the number pad buttons jump from gray to bright blue with no transition. The visual jump is jarring.
- **Fix**: Add a brief color transition animation (150-200ms ease-in) when buttons enable/disable.

---

### Puzzle Screen — Number Entered (`04_number_entered.png`)

**13. User-entered numbers don't stand out from given numbers**
The "5" entered by the user (R1C4) appears as white text on a dark cell. Given numbers are white text on blue cells. While the distinction exists (blue bg vs dark bg), both use white text and the user's entry blends into the grid too easily.
- **Fix**: User-entered numbers should have a distinct visual treatment. Options: use the amber/secondary color for user text, add a subtle dot or underline below user numbers, or give user cells a very faint non-blue tint.

**14. No row/column sum feedback visible yet**
With only one number entered ("5" in R1C4), the row target "15" on the right is still plain white. There's no partial-sum indicator showing progress (e.g., "current sum: 11/15").
- **Fix**: Consider showing a running partial sum beneath each target, or changing the target number color progressively as the sum approaches the target.

---

### Beginner 3×3 Game (`05_beginner_3x3_game.png`)

**15. 3×3 grid cells are disproportionately large**
Each cell takes up roughly 1/3 of the screen width and height. The cells are ~180dp tall. This feels oversized, almost cartoonishly large. Lots of wasted vertical space below the grid.
- **Fix**: Cap cell size at ~100-120dp even on 3×3. Use the freed space for game info, hints, or a more balanced layout. The grid should feel "right-sized," not stretched.

**16. Sum indicator green check (✓) is good but inconsistent coloring**
Row 3 shows "11 ✓" in amber/yellow-green. The column sums below (9, 11, 10) are plain white/gray. The mix of amber checks and gray plain numbers creates visual inconsistency.
- **Fix**: This is actually correct (complete rows show ✓, incomplete show plain). But consider using a dim red or gray variant for incomplete sums so the difference is clearer. Currently incomplete sums and non-started sums look identical.

**17. Number pad disabled state offers no guidance**
On 3×3 beginner, the number pad shows 1-5 + ✕, all grayed out. First-time users staring at gray buttons and huge empty cells have no idea what to do.
- **Fix**: On first puzzle (especially beginner), show an animated hand/pointer gesture on an empty cell, or pulse the first empty cell with a "tap here" glow.

---

### Puzzle Complete (`06_puzzle_complete.png`)

**18. Completion screen is just one line of text at the bottom**
"Puzzle complete! 🎉" is tiny text stuck to the very bottom of the screen. Above it is the solved grid (with an oddly lingering yellow selection border on one cell) and then vast empty space. This is the most important moment in the game — the reward — and it's completely underwhelming.
- **Fix**: Show a completion card/bottom sheet that slides up with:
  - A big congrats message or animation
  - Solve time prominently displayed (not just the timer at the top)
  - Difficulty badge
  - **Share button** (ShareCardGenerator is built but never surfaced!)
  - "Next Puzzle" / "Back to Home" buttons
  - Streak update ("Day 1! Keep it going tomorrow")

**19. Selection border persists after completion**
Cell R1C2 still shows the yellow selection border even though the puzzle is solved and the number pad is hidden. This is a visual artifact that shouldn't linger.
- **Fix**: Clear `selectedCell` to `null` when `isCompleted` becomes true. The selection border has no purpose on a completed puzzle.

**20. Timer keeps showing at the top after completion**
The timer reads "04:51" at the top in the same small label style. On completion, the time is the most interesting stat, but it's still displayed as a tiny afterthought.
- **Fix**: On completion, either move the solve time into the completion card (prominently) or visually promote it (larger, centered, with a "Your time" label).

**21. Number pad area is just empty space after completion**
The number pad disappears (`isVisible = false` when complete) but the space it occupied is just black void. Combined with the single-line completion text, the bottom 60% of the screen is wasted.
- **Fix**: Use that space for the completion card with share/replay/home buttons.

**22. No share button anywhere**
The `ShareCardGenerator` creates beautiful spoiler-free emoji share cards. `ShareIntentLauncher` handles the Android share sheet. Both are fully implemented. But there is literally no button in the UI to trigger them. This is the single biggest missed opportunity in the app.
- **Fix**: Add a prominent Share button on the completion card. This is a zero-effort high-impact win — the code is already written.

---

## Prioritized Improvement List

### Tier 1: Ship This Week (Critical, mostly quick fixes)

| # | Task | Effort | Why Now |
|---|------|--------|---------|
| 1 | **Add Share button to completion screen** | ~2h | Code exists, just needs a button. Biggest retention lever you're missing. |
| 2 | **Build completion card/bottom sheet** | ~8h | Replace the single text line with a proper card: time, share, next puzzle, streak update. |
| 3 | **Clear selection border on completion** | ~15min | One-line fix: set `selectedCell = null` when `isCompleted = true`. |
| 4 | **Add TopAppBar with back button + difficulty label** | ~2h | Users need a way out and context about what they're playing. |
| 5 | **Add "Tap a cell to begin" hint on first load** | ~1h | Solves the "everything looks disabled" confusion for new users. |
| 6 | **Fix empty cell visibility in dark mode** | ~1h | Give empty cells a subtle background tint so they don't vanish into black. |
| 7 | **Fix streak display for zero streak** | ~1h | Show encouraging message instead of "🔥 0 days streak". |

### Tier 2: Next Sprint (High impact, medium effort)

| # | Task | Effort | Why |
|---|------|--------|-----|
| 8 | **Reduce dead space on puzzle screen** | ~4h | Center grid + push number pad closer. Use freed space for context. |
| 9 | **Make Play button visually pop (amber CTA)** | ~1h | Switch to tertiary/amber color. Current indigo blends into dark theme. |
| 10 | **Thicken cell selection border + add fill** | ~2h | 3-4dp amber border + faint amber fill inside selected cell. |
| 11 | **Cap 3×3 cell size, rebalance layout** | ~3h | Cells are ~180dp tall on beginner. Cap at ~120dp for better proportions. |
| 12 | **Differentiate user-entered vs given numbers** | ~2h | Use amber text or subtle underline for user numbers. White-on-dark vs white-on-blue isn't enough. |
| 13 | **Add undo button** | ~6h | Standard puzzle game feature. Track move history in ViewModel. |
| 14 | **Display earned streak badges** | ~3h | StreakBadge enum is fully defined but never rendered. Show them on Home. |
| 15 | **Add rules explanation / coach marks** | ~3h | First-time users get zero explanation of how the game works. |

### Tier 3: Polish Sprint (Medium impact)

| # | Task | Effort | Why |
|---|------|--------|-----|
| 16 | **Animate number pad enable/disable transition** | ~1h | Buttons jump from gray to blue with no transition. Add 150ms ease. |
| 17 | **Show partial sums or progressive target coloring** | ~4h | Help users track progress toward row/col targets as they fill cells. |
| 18 | **First-puzzle onboarding gesture animation** | ~4h | Pulse/animate the first empty cell to guide tapping. |
| 19 | **Improve unselected difficulty card visibility** | ~1h | Slightly lighter surface or visible outline for non-selected cards. |
| 20 | **Promote timer on completion** | ~1h | Move solve time into the completion card, larger and labeled. |
| 21 | **Make status chips more intuitive** | ~2h | Replace "○" with checkbox-style icons. Add "Solved: 0/3" label. |
| 22 | **Enhanced celebration animation** | ~6h | Confetti, sound, or more dramatic visuals for the reward moment. |

### Tier 4: Future Improvements

| # | Task | Effort | Why |
|---|------|--------|-----|
| 23 | **Statistics screen** | ~12h | Solve history, avg/best times, completion calendar. |
| 24 | **Daily reminder notification** | ~8h | Push notification for daily puzzle. Key retention mechanic. |
| 25 | **Practice / random puzzle mode** | ~16h | Content for users who finish all 3 dailies. |
| 26 | **Pencil/notes mode for candidates** | ~16h | Mark possible values per cell. Essential for 5×5 Medium. |
| 27 | **Hard (6×6) and Expert (7×7) difficulties** | ~16h | Progression for players who master Medium. |
| 28 | **Dark mode full contrast audit (WCAG AA)** | ~6h | Verify all color combinations hit 4.5:1 ratio. |
| 29 | **Landscape layout support** | ~6h | Grid left, pad right. Or lock to portrait. |
| 30 | **Accessibility: font scaling + screen reader** | ~8h | Test 200% font. Fix TalkBack focus order. |

---

## Key Takeaways from Visual Review

The three biggest problems visible in the screenshots:

1. **The completion screen is a missed opportunity.** The entire share system is built and ready — `ShareCardGenerator` creates emoji share cards, `ShareIntentLauncher` handles the share sheet — but there's no button. Meanwhile, the completion "celebration" is a single line of text in a sea of black. This is where users decide to come back tomorrow or uninstall.

2. **The puzzle screen has a layout problem.** ~50% of the screen is dead black space between the grid and the number pad. The grid floats at the top with no context (no title bar, no difficulty label, no back button), and the pad sits at the very bottom. It feels unfinished.

3. **Dark mode hides too much.** Empty cells vanish into the background. Unselected difficulty cards disappear. The Play button blends in. The app's dark theme is *too* dark in places — it needs more subtle contrast layering to give depth and make interactive elements visible.

---

*Review date: March 14, 2026 · Device: Oppo (dark mode) · Screenshots: 6 screens covering home, gameplay, and completion flows*
