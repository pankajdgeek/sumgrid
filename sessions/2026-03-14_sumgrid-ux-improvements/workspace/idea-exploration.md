# Idea Exploration: SumGrid UX Improvements & Growth

*Idea Explorer | Session: 2026-03-14*

---

## 1. Problem Statement

Number puzzle games attract a broad demographic -- from casual commuters wanting a quick mental workout to dedicated enthusiasts who solve daily puzzles as part of their routine. The market is dominated by Sudoku (billions of downloads across apps) and its variants (KenKen, Kakuro, Nonograms), but these games share common problems: steep learning curves for newcomers, stale gameplay loops for veterans, and weak social/sharing mechanics.

**What makes SumGrid unique:**
- **Simpler core mechanic than Sudoku** -- fill cells so rows and columns hit target sums. No "no repeats" constraint to explain. Approachable in seconds.
- **Scalable difficulty via grid size** -- 3x3 to 7x7 provides a natural difficulty ramp without changing the rules. Sudoku is always 9x9.
- **Daily puzzle structure** -- Like Wordle, everyone gets the same puzzle. Creates shared experience and shareable moments.
- **Fast solve times** -- Beginner puzzles take 30-90 seconds vs 10-30 minutes for Sudoku. Fits micro-break usage.

**The core problem to solve now:** SumGrid has solid game mechanics and architecture (Kotlin/Jetpack Compose, deterministic puzzle generation via Xorshift128, unique solution validation) but is under-leveraging its retention and growth potential. The share system is built but unwired. Stats exist as a route but show placeholder text. Practice mode is similarly stubbed. Badges are defined but invisible. The completion moment -- the single most important retention touchpoint -- is underdeveloped. Epic 003 shipped significant improvements (undo, pencil mode, 2-row numpad, Hard/Expert difficulties, stats screen, practice mode), but growth features (social, notifications, themes, achievements beyond badges) remain unexplored.

---

## 2. Personas

### Persona 1: The Casual Puzzler -- "Sarah"
- **Age/context**: 28, marketing manager, solves puzzles during commute and lunch breaks
- **Behavior**: Opens app 3-4 times/week. Plays Beginner and Easy. Rarely finishes Medium. Shares results on Instagram stories when she gets a fast time.
- **Needs**: Quick dopamine hit, satisfying completion moment, low cognitive load, attractive share cards
- **Frustration**: Gets bored after finishing daily puzzles. No reason to come back until tomorrow. Doesn't understand why she should try harder difficulties.
- **Goal**: Feel smart for 2 minutes, share accomplishment, move on

### Persona 2: The Daily Ritual Player -- "Marcus"
- **Age/context**: 45, accountant, solves all 3+ daily puzzles every morning with coffee
- **Behavior**: Never misses a day. Cares deeply about his streak. Plays all available difficulties. Checks stats obsessively.
- **Needs**: Streak protection, detailed statistics, sense of progression, daily notification reminder
- **Frustration**: No way to see historical performance trends. Streak feels fragile (miss one day, lose everything). After completing dailies, nothing left to do.
- **Goal**: Maintain streak, track improvement over time, have a reliable daily ritual

### Persona 3: The Competitive Solver -- "Priya"
- **Age/context**: 22, CS student, speedruns puzzle games, active in puzzle communities
- **Needs**: Leaderboards, time-based challenges, Hard/Expert content, proof of skill
- **Behavior**: Solves Expert puzzles in under 2 minutes. Wants to compare times with others. Screenshots results and posts to Discord.
- **Frustration**: No leaderboards. No way to prove she's fast. No competitive mode. Share cards don't show difficulty prominently enough.
- **Goal**: Be the best, prove it, compete with others

---

## 3. Value Proposition

**SumGrid is the number puzzle that anyone can learn in 10 seconds but takes months to master -- a daily brain workout that scales from relaxing 30-second solves to challenging 7x7 grids, designed for sharing your wins.**

---

## 4. Feature Ideas (20)

### Already Planned / Partially Built (from Tier 4 and stubs in code)

1. **Wire Stats Screen** -- StatsViewModel exists, route exists, but shows placeholder. Display solve history, avg/best times per difficulty, completion rate, and a calendar heatmap of activity. *Status: StatsViewModel built in Epic 003, needs UI.*

2. **Wire Practice Mode Screen** -- PracticeViewModel exists, route stubbed. Unlimited random puzzles outside daily rotation. *Status: ViewModel built in Epic 003, needs UI.*

3. **Daily Reminder Notifications** -- Push notification at user-chosen time: "Your daily SumGrid is ready!" Key retention mechanic for daily games. *Status: Not started.*

4. **Landscape Layout Support** -- Grid left, pad right on landscape. Or lock to portrait. Currently stretches oddly on tablets. *Status: Not started.*

5. **Full WCAG AA Contrast Audit** -- Verify all dark mode color combinations hit 4.5:1 ratio. Some given cells may be hard to distinguish. *Status: Partial fixes in Epic 003.*

6. **Font Scaling / Accessibility Audit** -- Test with 200% font scale. Ensure HomeScreen and NumberPad adapt without clipping. *Status: Not started.*

### New Growth Features

7. **Achievement System** -- Expand beyond StreakBadge to include skill-based achievements: "Speed Demon" (solve Expert < 90s), "Perfect Week" (all puzzles Mon-Sun), "Size Matters" (solve all 5 difficulty levels in one day), "No Eraser" (solve without undo). Display in a dedicated achievements screen with locked/unlocked states.

8. **Global & Friends Leaderboards** -- Daily leaderboard showing anonymized solve times per difficulty. Optional friend codes for private leaderboards. Weekly/monthly rankings. Requires lightweight backend (Firebase Realtime DB or Firestore).

9. **Themes & Customization** -- Unlockable color themes (Ocean, Forest, Sunset, Neon, Pastel) earned through achievements or streaks. Custom grid styles (rounded cells, minimal lines). Dark/Light/OLED toggle already exists -- extend to full theme packs.

10. **Seasonal Events & Limited Puzzles** -- Monthly themed challenges (e.g., "March Mathness" -- solve 31 puzzles in March). Holiday-themed share cards. Special difficulty modifiers (e.g., "Inverse Mode" where you figure out the targets, not the cells). Time-limited events create urgency and re-engagement.

11. **Interactive Tutorial System** -- Replace the current 3-day onboarding with a single-session guided tutorial. Animated hand pointing at cells, step-by-step rule explanation, progressive complexity (start with 2x2 demo). Completion unlocks a "Tutorial Graduate" badge.

12. **Home Screen Widget** -- Android widget showing today's puzzle status (solved/unsolved per difficulty), current streak, and a "Play Now" button. Glanceable, drives daily opens without needing notifications.

13. **Timed Challenge Mode** -- Solve as many puzzles as possible in 5 minutes. Puzzles start at Beginner, scale up in difficulty as you clear them. Score = total puzzles solved + time bonuses. Shareable score card. Weekly challenge resets.

14. **Multiplayer Race Mode** -- Two players get the same puzzle simultaneously. First to solve wins. Matchmaking via invite link or random. Real-time progress indicator shows opponent's completion %. Requires WebSocket backend (or Firebase Realtime DB).

15. **Puzzle Archive & Calendar** -- Browse and solve past daily puzzles. Calendar view shows which days were completed with gold/silver/bronze medals based on solve time. Fills the "nothing to do after dailies" gap.

16. **Hint System** -- Tiered hints: (1) highlight a row/column that's close to complete, (2) reveal one cell's value, (3) show which cells have errors. Limited free hints per day, or earned via streaks. Helps casual players without ruining the challenge.

17. **Sound Design & Haptic Polish** -- Satisfying click sounds on cell selection, whoosh on number entry, chime on row/column completion, fanfare on puzzle completion. Toggleable. Haptics already exist for some interactions -- extend to all.

18. **Streak Freeze / Protection** -- Allow players to "freeze" their streak for 1 day (earned via 7-day streaks or watching an ad). Reduces streak anxiety for Marcus-type players. Common in Duolingo, proven retention mechanic.

19. **Difficulty Recommendations** -- After solving 5+ puzzles at a difficulty, suggest the next level: "You're averaging 45 seconds on Easy -- ready to try Medium?" Smooth progression nudge based on actual performance data.

20. **Cross-Platform (iOS / Web)** -- Kotlin Multiplatform for shared game engine. Compose Multiplatform or SwiftUI for iOS. Web version via Kotlin/JS or standalone React app. Same daily puzzles, synced progress via cloud account. Massive TAM expansion.

---

## 5. Vision Statement

**In 12 months, SumGrid should be the go-to daily number puzzle for players who want something faster and more shareable than Sudoku.** The app should have:

- A polished, accessible experience across all 5 difficulty levels with a completion moment that drives sharing and return visits
- A retention flywheel powered by streaks (with freeze protection), achievements, daily notifications, and a home screen widget
- Social proof through share cards, leaderboards, and multiplayer race mode
- Enough content to fill any amount of player time: daily puzzles, practice mode, puzzle archive, timed challenges, and seasonal events
- An iOS version (or web) to double the addressable market
- A small but engaged community of competitive solvers who drive organic growth through social sharing

The north star metric is **daily active solvers completing at least one puzzle per day**, with secondary metrics on share rate, streak length distribution, and difficulty progression (% of players who advance beyond Beginner).

---

*Findings written to workspace/idea-exploration.md*
