# SumGrid — Product UX/UI Review

*Based on full source-code audit of all 3 screens, 7 components, theme system, and game logic.*

---

## Executive Summary

SumGrid is a well-architected daily number puzzle game with a solid foundation: clean Material 3 theming, proper accessibility scaffolding, haptic feedback, and a thoughtful streak/badge system. However, several critical gaps are holding back retention and user delight. Most notably, the **share system and badge rewards are fully built in code but never surfaced in the UI** — these are free wins. The puzzle completion moment (your #1 retention touchpoint) is severely underbuilt, and onboarding forces a 3-day commitment with no skip option and no rule explanation.

**31 findings** across 7 areas. **31 improvement tasks** organized into 4 sprints (~141h total).

---

## UX Review Findings

### 1. Onboarding & First-Time Experience

| Finding | Severity | Recommendation |
|---------|----------|----------------|
| **No skip option** — Users must complete a 3-puzzle sequence across 3 separate app launches (days) before reaching the real game. Advanced users are trapped. | Critical | Add a "Skip tutorial" link. Let users jump to Home immediately. Consider completing onboarding in a single session. |
| **No rule explanation** — Title says "Let's learn SumGrid" but there's zero text explaining what row/column sums mean or how to play. Users are dropped into a puzzle cold. | Critical | Add a brief rules overlay or coach marks on the first puzzle: "Fill cells so each row and column hits its target sum." |
| **No haptic feedback** — PuzzleScreen has haptics on cell tap and number entry, but OnboardingScreen doesn't. Inconsistent feel. | Medium | Mirror PuzzleScreen's haptic behavior in OnboardingScreen. |
| **No progress indicator** — No indication of how many onboarding puzzles remain (1 of 3, 2 of 3, etc.). | Medium | Add step indicator dots or "1/3" counter at the top. |

### 2. Home Screen

| Finding | Severity | Recommendation |
|---------|----------|----------------|
| **Weak visual hierarchy** — Streak, status chips, countdown, difficulty selector, and Play button all have similar visual weight. No clear focal point. | High | Make the Play button larger and use the tertiary/amber CTA color. Reduce visual weight of countdown and status chips. |
| **Zero-streak feels bad** — Shows "0 days streak" with a pulsing flame for new users. A pulsing flame on zero is discouraging. | Medium | Hide streak section or show "Start your streak today!" when streak is 0. Only animate flame when streak ≥ 1. |
| **No "all done" state** — When all 3 puzzles are completed, the Play button is still active with no differentiation. Users can replay but it's unclear. | High | Show a celebratory "All done for today!" state. If replay is allowed, label it "Replay" separately. |
| **Anxiety-inducing countdown** — HH:MM:SS with seconds ticking creates unnecessary urgency for a casual daily puzzle. | Low | Show approximate time: "New puzzles in ~5 hours" or just "New puzzles tomorrow." |
| **Dead footer link** — "More by dgeek" is plain text with no tap handler. Dead UI element. | Medium | Make it tappable (link to portfolio/store) or remove it. |
| **No scroll** — On small/compact devices (360dp width), content may clip below the Play button. | High | Wrap content in `verticalScroll`. Test on compact devices like Pixel 4a. |

### 3. Puzzle Screen

| Finding | Severity | Recommendation |
|---------|----------|----------------|
| **No back/exit button** — No visible way to exit mid-puzzle. Users must rely on system back gesture, which many don't know. | High | Add a TopAppBar with back arrow and difficulty label. Consider a pause/menu icon. |
| **Underwhelming timer** — Elapsed timer is small `labelLarge` text with minimal padding. Doesn't feel like a game timer. | Medium | Style with monospace font, slightly larger size, or a subtle chip container. |
| **No undo** — Users can only clear the current cell. No undo button for reverting the last move. Standard feature in puzzle games. | High | Add undo button alongside clear on the NumberPad. Track move history in PuzzleViewModel. |
| **Silent error feedback** — Sum indicators change color (green/red/gray) but there's no animation when a sum exceeds the target. Easy to miss. | Medium | Add a brief shake or color-flash when a sum indicator turns red. |
| **Bare completion experience** — Shows only "Puzzle complete! 🎉" text and hides the number pad. No share button, no stats, no next-puzzle prompt. **This is the #1 retention moment and it's wasted.** | Critical | Show a completion card with: solve time, difficulty, share button, "Next puzzle" / "Back to Home" CTAs. |
| **Small touch targets on 5×5** — Canvas-based tap detection on Medium (5×5) grids may produce cells below 44dp on narrow phones. | Medium | Ensure minimum 44dp touch target per cell. Consider giving the grid more vertical space. |
| **Cramped number pad** — 9 buttons + clear = 10 buttons in a single 56dp row on Medium difficulty. Too tight on narrow phones. | High | Switch to 2-row layout (5+5 or 5+4+clear) when maxVal ≥ 7. |
| **No pencil/notes mode** — No way to mark candidate numbers. Standard feature in number puzzle games (Sudoku etc.). Most valuable for Medium difficulty. | Medium | Add a pencil toggle that lets users mark possible values in cell corners. |

### 4. Visual Design & Theme

| Finding | Severity | Recommendation |
|---------|----------|----------------|
| **Dark mode contrast concern** — OLED dark theme defined (OledBlack `#090909`) but given cells (`primaryContainer`) may have low contrast against dark surface. | High | Audit dark mode contrast ratios. Ensure given vs. user cells are distinguishable. Target WCAG AA (4.5:1). |
| **No brand mark** — App icon, splash, and title are text-only "SumGrid." No logo or visual mark. Weak brand recall. | Medium | Design a simple logomark (stylized grid with sum symbols) for icon, splash, and home screen. |
| **Color monotony** — Deep indigo dominates everything. Amber accent only appears on streak and selection. Feels monochromatic. | Low | Use amber/warm-amber more for CTAs, achievements, and interactive elements. Tertiary palette is underutilized. |
| **Subtle celebration** — Scale 1.0→1.2→1.0 per cell with 50ms stagger. For a game completion moment, it's underwhelming. | Medium | Add confetti particles, sound effect, or more dramatic visuals. This is the reward moment. |

### 5. Accessibility

| Finding | Severity | Recommendation |
|---------|----------|----------------|
| **Font scaling overflow** — Fixed `sp` values throughout. Large font accessibility settings may cause overflow or clipping. | High | Test with 200% font scale. Ensure HomeScreen and NumberPad adapt. |
| **Screen reader ordering** — Grid has invisible TalkBack overlays, but navigation order may read all cells before reaching number pad. | Medium | Add traversal groups and focus ordering: timer → grid → number pad. Add "puzzle complete" announcement. |
| **No motion sensitivity** — Pulsing flame runs infinitely with no respect for system "Reduce motion" setting. | Medium | Check `reduceMotion` preference. Disable animations when enabled. |

### 6. Engagement & Retention

| Finding | Severity | Recommendation |
|---------|----------|----------------|
| **No statistics screen** — No way to view solve history, average/best times, or completion rate. | High | Add Stats screen: total solved, avg/best times per difficulty, completion calendar heatmap. |
| **Invisible badges** — `StreakBadge` enum (Weekly Warrior 🔥, Monthly Master ⭐, Century Solver 💎, Year of Logic 👑) is fully defined but **never displayed in the UI.** | Critical | Show earned badges on HomeScreen or a dedicated achievements section. Show locked badges to motivate progression. |
| **No push notifications** — No daily reminder. Key retention mechanic for daily games. | High | Add opt-in daily reminder: "Your daily SumGrid is ready!" at user-chosen time. |
| **Share system not wired** — `ShareCardGenerator` and `ShareIntentLauncher` are fully implemented but **no share button exists anywhere in the UI.** | Critical | Add prominent Share button on the completion card. The entire share system is built and waiting. |
| **No difficulty progression** — Only 3 static levels (3×3, 4×4, 5×5). Players who master Medium have no next challenge. | Medium | Plan Hard (6×6) and Expert (7×7). Consider weekly challenges or timed modes. |
| **Nothing after daily puzzles** — Once all 3 are done, there's zero content. No archive, no practice mode, no random puzzles. | High | Add Practice mode with unlimited random puzzles (non-streak-counted). Or a past-dailies archive. |

### 7. Technical UX

| Finding | Severity | Recommendation |
|---------|----------|----------------|
| **State persistence unclear** — If the app is killed mid-puzzle, progress may be lost. No auto-save indicator. | High | Ensure puzzle state persists on every cell change. Show subtle save indicator. |
| **Inconsistent loading states** — HomeScreen uses `CircularProgressIndicator`, PuzzleScreen shows "Loading puzzle…" text. | Low | Unify with a branded shimmer/skeleton effect. |
| **No landscape support** — No landscape layout. On tablets or landscape phones, the vertical Column will stretch oddly. | Medium | Add landscape layout (grid left, pad right) or lock to portrait in manifest. |
| **Review trigger timing** — `InAppReviewTrigger` exists but trigger conditions may fire at wrong moments. | Low | Trigger after 3rd completion or 7-day streak (positive moments only). Never during gameplay. |

---

## Prioritized Task Backlog

### P0 — Do First (Blocks Retention)

| # | Task | Effort | Impact | Screen |
|---|------|--------|--------|--------|
| 1 | Wire share button to completion screen | S (~2h) | High | PuzzleScreen |
| 2 | Display earned streak badges | S (~3h) | High | HomeScreen |
| 3 | Add skip option to onboarding | S (~2h) | High | OnboardingScreen |
| 4 | Add rules explanation to onboarding | S (~3h) | High | OnboardingScreen |
| 5 | Add back/exit button to puzzle screen | S (~2h) | High | PuzzleScreen |
| 6 | Build completion card / bottom sheet | M (~8h) | High | PuzzleScreen |

### P1 — Next Sprint

| # | Task | Effort | Impact | Screen |
|---|------|--------|--------|--------|
| 7 | Add undo functionality | M (~6h) | High | PuzzleScreen, NumberPad |
| 8 | 2-row number pad for Medium difficulty | M (~4h) | High | NumberPad |
| 9 | Add statistics screen | M (~12h) | High | New: StatsScreen |
| 10 | Make HomeScreen scrollable | S (~1h) | High | HomeScreen |
| 11 | Daily reminder notification | M (~8h) | High | New: NotificationManager |
| 12 | Practice / random puzzle mode | L (~16h) | High | New: PracticeScreen |
| 13 | All-done state for HomeScreen | S (~2h) | Medium | HomeScreen |
| 15 | Dark mode contrast audit | M (~6h) | High | GridRenderer, Theme |
| 23 | Font scaling test & fix | M (~6h) | High | App-wide |
| 31 | State persistence & auto-save | M (~6h) | High | PuzzleViewModel |

### P2 — Polish

| # | Task | Effort | Impact | Screen |
|---|------|--------|--------|--------|
| 14 | Enhanced celebration animation | M (~6h) | Medium | CelebrationAnimation |
| 16 | Reduce motion accessibility | S (~3h) | Medium | HomeScreen, GridRenderer |
| 17 | Error indicator animation | S (~3h) | Medium | GridRenderer |
| 18 | Brand logomark design | M (~6h) | Medium | App-wide |
| 19 | Pencil/notes mode | L (~16h) | Medium | PuzzleScreen, GridRenderer |
| 20 | Onboarding progress indicator | S (~2h) | Medium | OnboardingScreen |
| 21 | Fix zero-streak display | S (~2h) | Medium | HomeScreen |
| 22 | Make footer link tappable | S (~1h) | Low | HomeScreen |
| 24 | Landscape layout or lock | M (~6h) | Medium | App-wide |
| 28 | Screen reader focus ordering | M (~4h) | Medium | PuzzleScreen |
| 30 | Hard & Expert difficulty levels | L (~16h) | Medium | Difficulty, Engine |

### P3 — Nice to Have

| # | Task | Effort | Impact | Screen |
|---|------|--------|--------|--------|
| 25 | Timer styling upgrade | S (~2h) | Low | PuzzleScreen |
| 26 | Friendly countdown format | S (~1h) | Low | HomeScreen |
| 27 | Unified loading states | S (~2h) | Low | HomeScreen, PuzzleScreen |
| 29 | Smart in-app review trigger | S (~2h) | Low | App-wide |

*Effort: S = Small (<4h) · M = Medium (4–16h) · L = Large (16h+)*

---

## Suggested Sprint Plan

### Sprint 1: Quick Wins & Critical Fixes (~15h, 1 week)

Tasks #1, #2, #3, #4, #5, #10, #13 — Wire up the share button and badges that are already built. Fix onboarding friction. Add back button. Make HomeScreen scrollable. Add all-done state.

### Sprint 2: Core Game UX (~36h, 1–2 weeks)

Tasks #6, #7, #8, #15, #23, #31 — Build the completion card (your biggest retention lever). Add undo. Fix the cramped number pad. Audit dark mode and font scaling. Ensure state persistence.

### Sprint 3: Retention & Engagement (~42h, 2 weeks)

Tasks #9, #11, #12, #14 — Add statistics screen, daily notifications, practice mode, and enhanced celebrations. These drive repeat usage.

### Sprint 4: Polish & Accessibility (~48h, 1–2 weeks)

Tasks #16, #17, #19, #24, #28, #30 — Motion sensitivity, error animations, pencil mode, landscape support, screen reader improvements, and new difficulty levels.

---

*Review date: March 14, 2026*
