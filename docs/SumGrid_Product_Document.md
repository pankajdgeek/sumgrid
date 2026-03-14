# SUMGRID — Product Requirements Document

### Daily Number Logic Puzzle

**Version:** 1.0
**Date:** March 2026
**Developer:** dgeek
**Philosophy:** Free. No Ads. Impact Over Income.

---

## 1. Product Overview

### 1.1 What is SumGrid?

SumGrid is a daily number logic puzzle game where players fill a grid so that each row and column adds up to a target sum. Think of it as the love child of Sudoku and Kakuro, but simpler, faster, and designed for mobile-first play sessions of 2–5 minutes.

Every day, every player worldwide receives the same puzzle. One grid. One solution. Solve it, see your time, share your result. The mechanic is instantly understandable: fill in the empty cells with numbers so each row and column hits its target. No math beyond basic addition. No language required. Universal appeal.

### 1.2 Why This Game?

**Market opportunity:** The puzzle game market surpassed $8.2 billion in revenue in 2025, growing 7.6% year-over-year. Downloads are up 9.9%. It is the single largest category in casual gaming, accounting for 43% of all casual game revenue.

**Gap in the market:** The top number puzzle games (Sudoku, 2048, Threes) are either complex (Sudoku's 9x9 grid intimidates casual players) or lack daily engagement hooks (2048 has no daily challenge). Sumplete and similar web games have proven the daily number grid concept works but have minimal native Android presence.

**Strategic fit:** SumGrid fills the missing "logic puzzle" slot in the dgeek portfolio. It cross-promotes naturally with pacmaze (spatial), Brain Puzzle (visual), and Beyond (wellness). It works globally without localization because numbers are universal.

**dgeek advantage:** Free, no ads, beautiful design. Every top competitor in this space is loaded with ads, interstitials, and paywalls. A clean, respectful alternative will stand out immediately.

### 1.3 Product Principles

- **One puzzle a day.** Scarcity creates value. The daily puzzle is the core retention mechanic.
- **30 seconds to learn.** The rules fit in one sentence. No tutorial needed beyond the first puzzle.
- **Language-free.** Numbers only. Works in every country without translation.
- **Share without spoiling.** Share results as a colored grid (like Wordle) so others can see your performance without seeing the solution.
- **Respect the player.** No ads, no dark patterns, no manipulative notifications.

---

## 2. Game Mechanics

### 2.1 Core Mechanic

The player is presented with a square grid (starting at 3x3, scaling to 6x6). Some cells are pre-filled with numbers. The remaining cells are empty. Each row has a target sum displayed on the right. Each column has a target sum displayed at the bottom.

The player fills in the empty cells with numbers (1–9) so that every row and column adds up to its target sum. Each puzzle has exactly one valid solution.

### Example: 3x3 Beginner Puzzle

```
         Col 1   Col 2   Col 3   → Sum
Row 1  [  2  ] [  ?  ] [  1  ]  →  6
Row 2  [  ?  ] [  5  ] [  ?  ]  → 12
Row 3  [  4  ] [  ?  ] [  6  ]  → 12
         ↓ 10    ↓ 10    ↓ 10
```

**Answer:** The ?s are 3, 4, 3, 2 respectively. Each row and column sums to its target.

### 2.2 Difficulty Levels

| Level | Grid Size | Pre-filled Cells | Number Range | Est. Time | Target Audience |
|-------|-----------|-----------------|--------------|-----------|-----------------|
| Beginner | 3x3 | 5 of 9 | 1–5 | 30–60s | New players |
| Easy | 4x4 | 8 of 16 | 1–7 | 1–2 min | Casual players |
| Medium | 5x5 | 10 of 25 | 1–9 | 2–4 min | Regular players |
| Hard | 5x5 | 7 of 25 | 1–9 | 3–6 min | Enthusiasts |
| Expert | 6x6 | 10 of 36 | 1–9 | 5–10 min | Puzzle masters |

### 2.3 Input Mechanics

Tap an empty cell to select it. A number pad (1–9) appears at the bottom of the screen. Tap a number to fill the cell. Tap the same cell again to clear it. The sum indicators update in real-time as the player fills cells — turning green when a row/column reaches its target sum, red when it exceeds the target, and gray when incomplete.

Optional: pencil mode for noting possible numbers in a cell (small superscript numbers). Toggle pencil mode with a small pencil icon.

### 2.4 Validation & Completion

The puzzle is complete when all cells are filled and every row and column sum matches its target. On completion: cells animate with a satisfying cascade effect, the timer stops, the completion card appears showing time, difficulty, and the share button.

There is no "check" button during play. The real-time sum indicators are the only feedback. This preserves the puzzle-solving feeling without hand-holding.

---

## 3. Daily Puzzle System

### 3.1 How It Works

Every day at midnight (local time), a new puzzle is generated. All players worldwide get the same puzzle for each difficulty level. The puzzle is available for 24 hours. After completion, the player sees their time and can share results.

Puzzle generation uses a seeded algorithm: the date is the seed, ensuring deterministic generation. The server generates nothing — the puzzle is computed client-side from the date seed. This means no backend infrastructure is needed for the daily puzzle.

### 3.2 The Share Card

After completing the daily puzzle, the player can share a result card. The card shows:

- **SumGrid #[day number] — [difficulty]**
- A colored grid showing which cells were pre-filled (gray) and which the player solved (green/yellow/red based on attempts)
- Completion time
- "Play free at sumgrid.dgeek.org" link

The grid reveals NO numbers — only colors. This lets players share their achievement without spoiling the puzzle for others. This is the exact mechanic that made Wordle go viral.

### Example Share Card:

```
SumGrid #1 — Medium ⏱ 2:34

⬜🟩⬜🟩⬜
🟩⬜⬜⬜🟩
⬜🟩🟩⬜⬜
🟩⬜⬜🟩🟩
⬜⬜🟩⬜⬜

Play free → sumgrid.dgeek.org
```

### 3.3 Streak System

Consecutive days of completing at least one daily puzzle build a streak. The streak is shown prominently on the home screen.

**Milestones:**

| Days | Badge Name | Icon |
|------|-----------|------|
| 7 | Weekly Warrior | 🔥 |
| 30 | Monthly Master | ⭐ |
| 100 | Century Solver | 💎 |
| 365 | Year of Logic | 👑 |

Streaks are the single most powerful retention mechanic in daily puzzle games. Humans are more motivated by loss aversion (not wanting to break a streak) than by gain.

---

## 4. User Experience

### 4.1 First-Time Experience (FTUE)

The first interaction IS the first puzzle. No splash screens, no account creation, no tutorial screens. The app opens directly to a simple 3x3 grid with most cells pre-filled. The player solves it in under 30 seconds. On completion, the celebration animation plays, and a subtle tooltip says: "Come back tomorrow for a new puzzle."

After the first completion, the home screen shows: today's puzzle status (completed/available), the streak counter (starting at 1), difficulty selector, and the countdown to tomorrow's puzzle.

### 4.2 Visual Design

Minimalist, clean, and calming. The grid is the hero — large cells with clear numbers, ample whitespace around the grid.

**Color palette:** Deep indigo background, white grid cells, amber accents for sums and highlights. Dark mode supported from launch.

**Aesthetic:** Like a premium math notebook — thoughtful, not flashy.

### 4.3 Sound Design

- Soft "click" when placing a number
- Gentle chime when a row/column sum turns green
- Satisfying cascade of tones on puzzle completion
- Haptic feedback on number placement
- All sound toggleable. No background music in v1 (added in v2 as optional ambient)

### 4.4 Accessibility

- **Colorblind-friendly:** Use shapes (checkmark, X) alongside colors for sum indicators
- **Dynamic text sizing:** Respects system font size settings
- **TalkBack/screen reader support** for all grid cells and interactions
- **One-handed play:** Number pad positioned at bottom within thumb reach
- **Language-free UI:** Icons and numbers only, minimal text labels

---

## 5. Technical Architecture

### 5.1 Platform

| Component | Choice |
|-----------|--------|
| Framework | Native Android (Kotlin) or Flutter |
| Min SDK | Android 7.0 (API 24) — covers 95%+ of active devices |
| Backend | None (offline-first). Puzzle generation is client-side from date seed. |
| Analytics | Firebase Analytics (already in dgeek project) |
| Crash Reporting | Firebase Crashlytics (already configured) |
| Storage | Local SharedPreferences for stats, streaks, settings |
| App Size | Target < 5 MB (lightweight for emerging markets) |

### 5.2 Puzzle Generation Algorithm

The puzzle generator works in three steps:

1. **Generate a complete valid grid:** Fill an NxN grid with random numbers (1–range) using the date as the random seed. Calculate row and column sums.

2. **Remove cells:** Remove cells one at a time, checking after each removal that the puzzle still has exactly one solution (using constraint propagation).

3. **Verify difficulty:** Count the number of constraint propagation steps needed to solve. This determines difficulty rating. If it doesn't match the target difficulty, regenerate with a different seed offset.

Because the seed is deterministic (based on date + difficulty level), every player gets the same puzzle. No server needed.

### 5.3 Offline-First Design

The entire app works offline. Puzzles are generated locally. Stats are stored locally. The only network call is optional: Firebase Analytics events. If offline, events are queued and sent when connectivity returns. This is critical for the target markets (Egypt, Ethiopia, Côte d'Ivoire) where connectivity may be intermittent.

---

## 6. Competitive Analysis

| App | Downloads | Daily Puzzle | Ads | Sharing | SumGrid Edge |
|-----|-----------|-------------|-----|---------|-------------|
| Sudoku.com | 100M+ | Yes | Heavy | No | Simpler, cleaner |
| 2048 | 100M+ | No | Heavy | No | Daily hook + social |
| Sumplete | Web only | Yes | Minimal | Basic | Native app + streak |
| Kakuro | 10M+ | Some | Heavy | No | Simpler rules |
| Number Sum | 100K+ | No | Heavy | No | Everything |

The pattern is clear: every established competitor monetizes through ads. SumGrid's differentiator is simple — the same game quality, zero ads, beautiful design. In a market where users are trained to expect interstitials between every puzzle, an ad-free experience is remarkable.

---

## 7. Monetization Strategy

**None. Intentionally.**

SumGrid follows the dgeek philosophy: free, no ads, impact over income. The game exists to grow the dgeek user base and build trust. Every user who installs SumGrid and finds a respectful, high-quality experience becomes someone who trusts dgeek apps.

Future monetization options (2027+, if desired):

- Optional premium theme packs (purely cosmetic)
- "SumGrid Pro" with unlimited practice puzzles and statistics
- **Never ads. Never.**

---

## 8. Success Metrics

### 8.1 Launch Targets (First 30 Days)

| Metric | Target | Why It Matters |
|--------|--------|---------------|
| Total Installs | 500 | Baseline organic discovery |
| Day 1 Retention | >25% | Daily puzzle hook working |
| Day 7 Retention | >15% | Streak mechanic working |
| Avg Session Time | >3 min | Puzzles are engaging enough |
| Rating | 4.5★+ | Quality perception |
| Share Rate | >5% of completions | Viral growth engine active |

### 8.2 Six-Month Targets

| Metric | Target |
|--------|--------|
| Total Installs | 10,000 |
| MAU | 3,000 |
| Day 1 Retention | >30% |
| Day 7 Retention | >20% |
| Average Rating | 4.7★+ |

---

## 9. ASO Strategy

**App Name:** "SumGrid — Daily Number Puzzle"
**Package:** org.dgeek.sumgrid

**Short Description:** "Free daily number puzzle. Fill the grid, match the sums. No ads, no accounts, just logic. New puzzle every day."

**Long Description Keywords:** number puzzle, daily challenge, math game, logic puzzle, brain teaser, sudoku alternative, free, no ads, offline, number grid

**Feature Graphic:** A beautifully designed 4x4 grid with some cells filled, amber sum indicators, on a deep indigo background. Clean, minimal, premium-feeling.

**Screenshots (5):**

1. Daily puzzle with timer running
2. Completion celebration card with time and share button
3. Share result grid (colored squares, no numbers)
4. Streak counter at 7 days with "Weekly Warrior" badge
5. Difficulty selection screen showing all 5 levels

**Trailer Video:** 15-second clip — hand taps cells, numbers appear, sums turn green one by one, final cell placed, celebration animation, share card appears. No narration, just satisfying sound effects.

---

## 10. Cross-App Ecosystem Integration

SumGrid completes the dgeek app family:

- **pacmaze** — Spatial thinking (arcade)
- **Brain Puzzle** — Visual relaxation (jigsaw)
- **SumGrid** — Logical reasoning (number puzzle)
- **Ring Light** — Creator utility
- **Beyond** — Digital wellness hub

**Ecosystem message:** "Your phone should serve you, not the other way around."

### Integration Points:

- **Beyond → SumGrid:** "Your focus session is done. Sharpen your mind with today's SumGrid puzzle."
- **pacmaze → SumGrid:** "Finished your daily maze? Try today's number puzzle."
- **Brain Puzzle → SumGrid:** "Need a different kind of challenge? Try SumGrid."
- **SumGrid → All:** "More by dgeek" section in settings — tasteful, not intrusive.

---

*Document created March 2026 | dgeek | Confidential*
