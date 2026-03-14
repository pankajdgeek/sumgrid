# SumGrid Feasibility Analysis

**Analyst:** Feasibility Analyst | **Date:** 2026-03-14
**Idea:** SumGrid -- Daily number logic puzzle, Android-native, offline-first, < 5MB

---

## 1. Recommended Tech Stack

### Kotlin + Jetpack Compose (RECOMMENDED) vs Flutter

| Factor | Kotlin/Compose | Flutter |
|--------|---------------|---------|
| **APK Size** | ~1.5-3 MB (minified release) | ~5-8 MB minimum (Dart runtime bundled) |
| **< 5MB Target** | Achievable with room to spare | Tight -- may exceed target with Firebase deps |
| **Offline-first** | Native SharedPreferences/DataStore, zero overhead | Hive/SharedPreferences plugin, slight overhead |
| **Emerging Markets** | Smaller APK = faster install on slow networks | Larger APK is a real barrier in Egypt, Ethiopia, Cote d'Ivoire |
| **Solo Dev Velocity** | Fast if dev knows Kotlin; Compose is declarative | Fast for cross-platform; overkill for Android-only |
| **Phase 4 PWA** | No web target -- would need separate web codebase | Flutter Web exists but performance is poor for games |
| **Wear OS (Phase 4)** | Native Compose for Wear OS -- first-class support | Limited Wear OS support |
| **Google Play Integration** | Native -- Play Games Services, in-app review, widgets are trivial | Plugin-dependent, occasional version lag |
| **Animation/Canvas** | Compose Canvas API is excellent for grid rendering | CustomPaint equivalent, also excellent |
| **Learning Curve** | Moderate (if already a Kotlin dev) | Moderate (Dart is easy but new ecosystem) |

### VERDICT: Kotlin + Jetpack Compose

**Rationale:**
1. **App size is a hard constraint.** The < 5MB target for emerging markets is non-negotiable. Kotlin/Compose produces ~1.5-3 MB APKs. Flutter starts at ~5 MB before you add ANY dependencies. Adding Firebase SDK to Flutter puts you well over 5 MB.
2. **Android-only for Phases 1-3.** Flutter's cross-platform advantage is irrelevant until Phase 4 (2027). Don't pay the size/complexity tax now for a benefit 12+ months away.
3. **Phase 4 PWA pivot.** Neither framework excels at PWA. When Phase 4 arrives, a lightweight web version (vanilla JS/TypeScript + Canvas) sharing the same puzzle algorithm (ported to TS) would be far better than Flutter Web. The puzzle algorithm is ~200-400 lines -- trivial to port.
4. **Wear OS.** Compose for Wear OS is first-class Google-supported. Flutter Wear OS support is experimental.
5. **dgeek ecosystem.** If other dgeek apps are native Android, maintaining consistency matters.

### Recommended Stack Detail

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| Language | Kotlin | Modern, concise, Android-first |
| UI | Jetpack Compose | Declarative, excellent Canvas API for grid |
| Storage | Jetpack DataStore (Preferences) | Replaces SharedPreferences; async, type-safe, coroutine-native. Negligible size impact. |
| Analytics | Firebase Analytics | Already in dgeek project, ~1 MB SDK |
| Crash Reporting | Firebase Crashlytics | Already configured, bundled with Analytics |
| Build | Gradle with R8 (full mode) | Aggressive code/resource shrinking for size target |
| Min SDK | API 24 (Android 7.0) | 95%+ device coverage |
| CI/CD | GitHub Actions | Free tier sufficient for solo dev |
| Testing | JUnit 5 + Compose UI Testing | Standard, well-documented |

**Estimated APK size breakdown:**
- Base Kotlin/Compose app: ~1.5 MB
- Firebase Analytics + Crashlytics: ~1.0-1.5 MB
- Sound assets (compressed OGG): ~0.3 MB
- App logic + resources: ~0.5 MB
- **Total estimate: ~3.5-4.0 MB** (well under 5 MB target)

---

## 2. MVP Scope Assessment

### Phase 1 Scope Review

The roadmap's Phase 1 scope is **well-defined and appropriate** for an MVP, with minor adjustments recommended.

#### KEEP (true MVP essentials):
- 1.1 Puzzle generation algorithm -- THE core product
- 1.2 Unique-solution validator -- non-negotiable for puzzle integrity
- 1.3 Difficulty calibration -- but **reduce to 3 levels for MVP** (Beginner 3x3, Easy 4x4, Medium 5x5)
- 1.4 Grid rendering engine -- the UI IS the product
- 1.5 Number input system -- core interaction
- 2.1 Daily puzzle system -- THE engagement mechanic
- 2.2 Real-time sum validation -- core feedback loop
- 2.3 Completion detection + celebration -- essential dopamine hit
- 2.5 Share card generation -- viral growth engine, must be Day 1
- 2.6 Streak counter -- #1 retention mechanic
- 3.1 Visual design (indigo/amber + dark mode) -- first impressions matter
- 3.4 Firebase integration -- need analytics from Day 1
- 3.7 Testing + Play Store submission -- launch gate

#### DEFER to v1.1 (saves ~5 dev-days):
- **1.3 partial: Hard + Expert difficulties** -- 3 levels are enough for launch. 5x5 Hard and 6x6 Expert can come in v1.1. Reduces calibration and testing burden.
- **3.2 Sound effects + haptics** -- Nice but not essential. Visual feedback (green/red sums) is sufficient. Saves 2 days.
- **3.3 Colorblind mode** -- Important but can ship in v1.0.1 within first week. Use shape indicators (checkmarks/X) alongside colors. Saves 1 day from critical path but should be fast-follow.
- **2.4 Timer** -- Can show completion time without a visible running timer. Simple `System.currentTimeMillis()` delta. Saves 0.5 days.
- **2.7 Difficulty selector** -- With only 3 levels, a simpler selector suffices. Saves 0.5 days.

#### ADD (missing from Phase 1):
- **Puzzle caching/precomputation** -- Generate tomorrow's puzzle in background. If constraint propagation is slow for 5x5+, this prevents UI jank. Add 1 day.
- **Edge case: timezone handling** -- Date seed must use a consistent reference. Add 0.5 days for timezone-aware date normalization.
- **Onboarding puzzle** -- The first-time experience IS a puzzle, but it should be a curated, non-daily puzzle that's always the same. Ensures perfect FTUE. Add 0.5 days.

### Revised MVP: ~30-32 dev-days (vs original 35)

The 5-day savings provides buffer for the inevitable unknowns in puzzle algorithm development.

---

## 3. Feature Complexity Ratings

### Phase 1 Features

| ID | Feature | Complexity | Rationale |
|----|---------|-----------|-----------|
| 1.1 | Puzzle generation (seeded, deterministic) | **Complex** | Seeded PRNG + grid fill + sum calculation. The algorithm itself is moderate, but ensuring determinism across Kotlin versions requires careful PRNG selection. |
| 1.2 | Unique-solution validator | **Very Complex** | Constraint propagation solver that must prove exactly ONE solution exists. This is the hardest technical challenge in the entire project. Must be correct AND performant. |
| 1.3 | Difficulty calibration | **Complex** | Tuning cell removal count + constraint step counting to produce consistent difficulty. Requires empirical testing with hundreds of generated puzzles. |
| 1.4 | Grid rendering (responsive) | **Medium** | Compose Canvas API handles this well. Responsive sizing for different screen sizes requires careful layout math but is well-understood. |
| 1.5 | Number input system | **Simple** | Standard tap-to-select + number pad. Well-trodden UI pattern. |
| 2.1 | Daily puzzle system | **Medium** | Date -> seed -> puzzle. The tricky part is timezone handling (see risks). |
| 2.2 | Real-time sum validation | **Simple** | Sum row/column on each input, compare to target. Trivial math. Color state is reactive in Compose. |
| 2.3 | Completion + celebration | **Medium** | Detection is simple (all cells filled + all sums match). Animation requires Compose animation APIs -- moderate learning curve but well-documented. |
| 2.4 | Timer | **Simple** | Start/stop timestamps. Display formatting. |
| 2.5 | Share card generation | **Medium** | Generate colored grid text (Unicode squares). Share via Android Intent. Image-based share card is harder (Canvas bitmap export) -- recommend text-only for MVP. |
| 2.6 | Streak counter | **Simple** | Increment on daily completion, reset on miss. DataStore persistence. |
| 2.7 | Difficulty selector | **Simple** | UI-only. List of difficulty options. |
| 3.1 | Visual design + dark mode | **Medium** | Compose Material3 theming handles dark mode. Custom indigo/amber palette requires design tokens. 3 days is appropriate. |
| 3.2 | Sound + haptics | **Simple** | Android SoundPool for short effects. HapticFeedback in Compose. Small OGG files. |
| 3.3 | Colorblind + accessibility | **Medium** | TalkBack contentDescription on grid cells. Shape indicators alongside colors. Dynamic text sizing. |
| 3.4 | Firebase integration | **Simple** | Standard dependency + initialization. Event logging is ~10 lines per event. |
| 3.5 | Play Store listing | **Simple** | Non-code work. Screenshots, description, graphics. |
| 3.6 | In-app review prompt | **Simple** | Google Play In-App Review API. ~20 lines of code. |
| 3.7 | Testing + bug fixes | **Medium** | Unit tests for puzzle gen (critical), UI tests for grid interaction, integration testing. |

### Phase 2 Features

| ID | Feature | Complexity | Rationale |
|----|---------|-----------|-----------|
| 4.1 | Statistics dashboard | **Medium** | DataStore aggregation + Compose UI. Multiple metrics to track and display. |
| 4.2 | Practice mode | **Simple** | Random seed instead of date seed. Same puzzle engine. |
| 4.3 | Pencil/notes mode | **Medium** | Multiple small numbers per cell. UI complexity in rendering and toggling. |
| 4.4 | Theme system | **Medium** | Multiple Material3 color schemes. Milestone unlock logic. |
| 4.5 | Undo button | **Simple** | Stack of moves. Pop to undo. |
| 4.6 | Hint system | **Medium** | Must solve puzzle internally, reveal one cell. Uses existing solver. |
| 4.7 | Background music | **Simple** | MediaPlayer with bundled assets. Toggle UI. But adds ~1-2 MB to APK. |
| 4.8 | Push notification | **Medium** | WorkManager for scheduling. Notification channels. Permission handling (Android 13+). |
| 4.9 | Weekly summary card | **Simple** | Aggregate weekly stats. Display card UI. |
| 4.10 | Home screen widget | **Complex** | Glance (Compose for widgets) is still maturing. Widget lifecycle is notoriously finicky. |

---

## 4. Build vs Buy Decisions

| Component | Decision | Rationale |
|-----------|---------|-----------|
| **Puzzle Engine** | **BUILD** | Core IP. No existing library does exactly this. Constraint propagation solver for sum-grid puzzles is niche. ~400-600 lines of Kotlin. |
| **Grid UI** | **BUILD** | Custom Compose Canvas rendering. No off-the-shelf grid component matches the required interaction model. |
| **Analytics** | **BUY (Firebase)** | Already in dgeek project. Free tier is generous. No reason to build. |
| **Crash Reporting** | **BUY (Crashlytics)** | Bundled with Firebase. Industry standard. |
| **Share Card** | **BUILD** | Text-based share (Unicode colored squares) is trivial to build. Image-based share card (Canvas -> Bitmap -> Share) is moderate but still build -- no library needed. |
| **Sound Effects** | **BUY (free assets)** | Use royalty-free UI sound packs (freesound.org, mixkit.co). Don't compose original audio. |
| **PRNG** | **BUILD (wrapper)** | Use `java.util.Random(seed)` NOT `kotlin.random.Random(seed)`. Kotlin's Random explicitly warns sequence may change across Kotlin versions. Java's Random algorithm is stable and documented. Wrap in a SumGridRandom class. |
| **Local Storage** | **BUY (DataStore)** | Jetpack DataStore. Modern replacement for SharedPreferences. Async, type-safe, Compose-friendly. |
| **Animations** | **BUILD** | Compose animation APIs are sufficient. No need for Lottie or third-party animation libraries (saves APK size). |
| **In-App Review** | **BUY (Google API)** | Play In-App Review library. ~50 KB. Standard. |
| **Push Notifications (Phase 2)** | **BUILD (local)** | Local notifications via WorkManager. No FCM/server needed for "+23 hours" reminders. |
| **Leaderboards (Phase 3)** | **BUY (Firebase/Play Games)** | Firebase Realtime DB or Play Games Services. Don't build backend infrastructure. |

---

## 5. Technical Risks

### RISK 1: Puzzle Generation Determinism Across Environments (Severity: HIGH)

**The Problem:** The entire daily puzzle system relies on every player getting the same puzzle from the same date seed. If the PRNG produces different sequences on different Android versions, devices, or after Kotlin runtime updates, players get different puzzles.

**Details:**
- `kotlin.random.Random(seed)` documentation explicitly states: "Future versions of Kotlin may change the algorithm so that it will return a sequence of values different from the current one for a given seed."
- This is a ticking time bomb. A Kotlin stdlib update could silently break puzzle consistency.

**Mitigation:**
- Use `java.util.Random(long seed)` which uses a documented Linear Congruential Generator (LCG) algorithm that has been stable since Java 1.0.
- Alternatively, implement a simple custom PRNG (e.g., xorshift128) in pure Kotlin -- ~20 lines, fully controlled.
- Write a test that generates puzzles for 1000 consecutive dates and stores expected output hashes. Run on every build. Any algorithm change breaks the test.
- **Recommendation: Custom xorshift128 PRNG.** Zero dependency on runtime version. 100% portable (also trivially portable to TypeScript for Phase 4 PWA).

### RISK 2: Unique-Solution Validation Performance (Severity: MEDIUM-HIGH)

**The Problem:** The puzzle generator must verify that after removing cells, exactly one solution exists. This requires running a constraint propagation solver, potentially with backtracking, for every cell removal attempt. For a 6x6 Expert grid, removing 26 of 36 cells means ~26 solver runs during generation.

**Details:**
- Constraint propagation for Sudoku-like puzzles typically runs in 3-100ms on modern hardware.
- SumGrid's constraints are simpler than Sudoku (only row/column sums, no uniqueness-within-group constraint), so propagation should be faster.
- However, low-end Android devices (e.g., Samsung Galaxy A03, popular in emerging markets) have significantly slower CPUs.
- Worst case: 26 solver runs x 100ms = 2.6 seconds for Expert puzzle generation.

**Mitigation:**
- **Precompute puzzles.** Generate puzzles in a background coroutine when the app opens. Cache today's and tomorrow's puzzles.
- **Optimize solver.** Use array-based constraint propagation (no object allocation during solving) to minimize GC pressure on low-end devices.
- **Set timeout.** If generation takes > 500ms, try a different seed offset. Don't let the UI block.
- **Profile on actual low-end device.** Buy/borrow a Samsung Galaxy A03 or equivalent. Don't assume emulator performance matches reality.
- **Fallback:** For 6x6 Expert grids, if generation is consistently slow, pre-embed a library of 365 puzzles (~50 KB JSON). Daily seed selects which one. This sidesteps generation entirely for the hardest difficulty.

### RISK 3: Timezone and Date Seed Edge Cases (Severity: MEDIUM)

**The Problem:** "Midnight local time" means different users see different puzzles at any given UTC instant. A user traveling across timezones mid-day could theoretically see two different "daily" puzzles, or miss a streak.

**Details:**
- Seed = `YYYYMMDD` as integer (e.g., 20260415 = April 15, 2026) using LOCAL device date.
- User changes timezone: device date changes, potentially showing a different puzzle.
- User manually sets device clock forward: can access tomorrow's puzzle today.

**Mitigation:**
- Use device local date for seed. Accept that timezone changes may show a different puzzle. This is how Wordle works -- it's an accepted UX compromise.
- For streaks: store the last completed date (local). A streak is maintained if `lastDate == yesterday OR lastDate == today`. Don't use UTC -- use whatever the device thinks "today" is.
- Clock manipulation: accept it. For a free game with no prizes, preventing cheating is not worth the engineering cost. No server-side validation needed.
- Document this decision clearly in code comments.

### RISK 4: APK Size Creep (Severity: MEDIUM)

**The Problem:** Firebase SDKs are not small. Adding features over phases could push past 5 MB.

**Details:**
- Firebase Analytics: ~800 KB
- Firebase Crashlytics: ~400 KB
- Compose runtime + material3: ~1.5 MB (after R8 shrinking)
- Sound assets: variable
- Each new Phase 2-3 feature adds dependencies

**Mitigation:**
- Enable R8 full mode from Day 1. Configure aggressive shrinking rules.
- Monitor APK size in CI (fail build if > 4.5 MB to leave headroom).
- Use AAB (Android App Bundle) -- Google Play delivers optimized APKs per device, reducing effective install size.
- For sound: use OGG Vorbis (smallest), keep each effect under 50 KB.
- For Phase 2 music: consider streaming from a CDN instead of bundling (but this breaks offline-first -- may need to accept the size increase).

### RISK 5: Compose Animation Jank on Low-End Devices (Severity: LOW-MEDIUM)

**The Problem:** The celebration animation (cascade effect on completion) and real-time color transitions could drop frames on low-end devices.

**Mitigation:**
- Use Compose's `animateXAsState` APIs (hardware-accelerated).
- Keep animations simple: color transitions, scale changes. Avoid particle systems.
- Test on a low-end device. If celebration animation drops below 30fps, simplify to a fade-in effect.
- Provide a "reduce motion" setting that respects Android's system animation scale.

---

## 6. Solo Dev Feasibility Assessment

### Can one person build MVP in 6 weeks?

**YES, with caveats.**

#### Favorable Factors:
1. **No backend.** Offline-first eliminates the entire server infrastructure burden (deployment, APIs, databases, auth). This alone saves weeks.
2. **Small surface area.** One screen (puzzle), one home screen, one settings screen. No complex navigation.
3. **Existing Firebase setup.** Analytics/Crashlytics already configured in the dgeek project. Copy config.
4. **Well-defined scope.** The product doc is exceptionally clear. No ambiguity in what to build.
5. **Deterministic puzzle generation.** The algorithm is well-understood (fill grid -> remove cells -> validate). Not research-level CS.

#### Risk Factors:
1. **Unique-solution validator is the hardest piece.** A solo dev with no constraint-solving experience may spend 3-5 days instead of the estimated 2 days. Budget 4 days.
2. **Difficulty calibration requires playtesting.** This is iterative, not linear. The dev must play hundreds of puzzles across difficulties to tune parameters. Budget 3 days (estimate says 2).
3. **Visual polish always takes longer than expected.** "Deep indigo background, white grid cells, amber accents" sounds simple, but getting spacing, typography, and animation right is design work. Budget 4 days (estimate says 3).
4. **Play Store review can take 3-7 days.** Not dev time, but calendar time. Submit early, expect rejection on first try (common for new developer accounts).

#### Realistic Solo Dev Timeline:

| Week | Focus | Dev Days |
|------|-------|----------|
| 1 | Project setup + PRNG + grid generation + solution validator | 5 |
| 2 | Difficulty calibration + grid rendering + input system | 5 |
| 3 | Daily puzzle system + sum validation + completion flow | 5 |
| 4 | Share card + streak + difficulty selector + timer | 5 |
| 5 | Visual design + dark mode + Firebase + accessibility basics | 5 |
| 6 | Testing (unit + manual) + bug fixes + Play Store prep + submission | 5 |
| Buffer | Play Store review + fix rejection issues | 3-5 |

**Total: 30 dev-days + 3-5 buffer days = ~7 calendar weeks**

This is tight but achievable for an experienced Kotlin/Android developer. If the developer is learning Jetpack Compose for the first time, add 1 week (total ~8 weeks).

### Can one person build Phase 2 in 4 weeks?

**YES, more confidently.** By Phase 2, the developer has the codebase established, Compose patterns learned, and the puzzle engine working. The Phase 2 features are predominantly UI work (stats dashboard, themes, pencil mode) and small additions (undo, hints). The home screen widget (4.10) is the only high-risk item.

**Recommendation:** Defer 4.10 (widget) to Phase 3 or later. Glance framework is still maturing and widgets are disproportionately complex for their value.

---

## 7. Timeline Reality Check

### Phase 1: 35 Dev-Days Estimate

**Assessment: Slightly generous but achievable.**

The original estimate of 35 dev-days for 6 calendar weeks (with buffer) is reasonable. My revised estimate is 30-32 dev-days of actual work, which means the 35-day budget includes ~3-5 days of buffer. This is appropriate.

However, the **calendar time** estimate of 6 weeks assumes:
- 5 productive dev-days per week (unrealistic for a solo dev who likely has other responsibilities)
- No major blockers on the puzzle engine
- Play Store approval within the 6-week window

**More realistic calendar time: 7-8 weeks** (assuming 4 productive dev-days per week on average).

### Phase-by-Phase Reality Check

| Phase | Estimated | My Assessment | Calendar Weeks (realistic) |
|-------|-----------|--------------|---------------------------|
| Phase 1 (MVP) | 35 dev-days / 6 weeks | 30-32 dev-days | 7-8 weeks |
| Phase 2 (Engage) | 20 dev-days / 4 weeks | 18-22 dev-days (widget may inflate) | 5-6 weeks |
| Phase 3 (Social) | 23 dev-days / 6 weeks | 25-30 dev-days (leaderboards need backend) | 7-9 weeks |
| Phase 4 (Scale) | Ongoing / 2027 | Highly variable | Depends on traction |

### Key Timeline Concerns:

1. **Phase 3 is underestimated.** Leaderboards (5.2) and "Challenge a Friend" (5.3) require backend infrastructure (Firebase Realtime DB or Firestore). This adds: data modeling, security rules, offline sync, abuse prevention. The 7 combined dev-days for these two features is optimistic. Budget 12-14 days.

2. **Phase 1's constraint solver could be a blocker.** If the unique-solution validator doesn't work correctly, everything downstream is broken. This should be the FIRST thing built, not parallelized with UI work.

3. **Play Store review timeline is external.** New developer accounts face stricter review. Budget 1-2 weeks from submission to approval. Submit a minimal build early (even if incomplete) to establish the listing.

### Recommended Development Order (Phase 1):

1. **Week 1:** PRNG + puzzle generation + unique-solution validator + 10,000-puzzle test suite
2. **Week 2:** Difficulty calibration + extensive playtesting of generated puzzles
3. **Week 3:** Grid UI + input system + real-time validation + completion detection
4. **Week 4:** Daily puzzle system + share card + streak + timer
5. **Week 5:** Visual design pass + dark mode + Firebase + basic accessibility
6. **Week 6:** Bug fixes + Play Store listing + closed testing track submission

This front-loads the highest-risk work (puzzle engine) and defers the lowest-risk work (polish) to the end.

---

## Summary

| Dimension | Assessment |
|-----------|-----------|
| **Tech Stack** | Kotlin + Jetpack Compose. Clear winner for Android-only, < 5MB, offline-first. |
| **MVP Scope** | Phase 1 is well-scoped. Defer sound/haptics and 2 difficulty levels. Add timezone handling and puzzle precomputation. |
| **Hardest Problem** | Unique-solution validator (constraint propagation). Build and test this FIRST. |
| **Biggest Risk** | PRNG determinism across Kotlin versions. Mitigate with custom xorshift128 or java.util.Random. |
| **Solo Dev Feasible?** | Yes, for experienced Android/Kotlin dev. 7-8 calendar weeks realistic for Phase 1. |
| **35 Dev-Day Estimate** | Slightly generous (30-32 actual) but the buffer is welcome. Calendar time is 7-8 weeks, not 6. |
| **Phase 3 Warning** | Leaderboards and social features are underestimated. They need backend work not accounted for. |
| **APK Size** | Achievable at ~3.5-4.0 MB with Kotlin/Compose + Firebase. Monitor in CI. |
