# Roadmap: SumGrid Growth Strategy

> Generated on: 2026-03-14
> Idea Clarity: SPECIFIC (existing app, post-Epic-003)

---

## 1. Idea Summary

### Problem Statement
Number puzzle enthusiasts (commuters, coffee-break players, competitive solvers) are underserved by a market dominated by Sudoku clones with dated UIs, ad-heavy experiences, and zero social features. Players who enjoy Wordle's daily ritual format have no equivalent in the number puzzle space that combines daily challenges with progressive difficulty and shareable results.

### Value Proposition
SumGrid is the daily number puzzle that anyone can learn in 10 seconds but takes months to master -- scaling from relaxing 30-second solves to challenging 7x7 grids, designed for sharing your wins.

### Target Users
| Persona | Description | Pain Point | Current Workaround |
|---------|-------------|------------|--------------------|
| Casual Puzzler "Sarah" | 28, plays during commute, 3-4x/week | Bored after finishing daily puzzles, wants quick dopamine hit | Wordle, Connections, random Sudoku apps |
| Daily Ritual Player "Marcus" | 45, solves every morning, cares about streaks | No historical stats, streak feels fragile (miss 1 day = lose everything) | Wordle (but wants more math-oriented) |
| Competitive Solver "Priya" | 22, CS student, speedruns puzzles | No leaderboards, no way to prove skill, no competitive mode | Screenshots results to Discord manually |

### North Star Metric
**Daily active solvers completing at least one puzzle per day**, with secondary metrics on share-to-install conversion rate, streak length distribution, and difficulty progression (% advancing beyond Beginner).

---

## 2. Market Validation

### Competitive Landscape
| Competitor | What They Do | Pricing | Strengths | Weaknesses | Our Differentiation |
|------------|-------------|---------|-----------|------------|---------------------|
| Sudoku.com (Easybrain) | Classic Sudoku | Free + $3.99/mo | 100M+ installs, clean UI | Fixed 9x9, no sharing | Simpler rules, scaling grid, share cards |
| Crossmath | Math grid crossword | Free + Ads | 37M+ downloads, 3x3-9x9 | Heavy ads, no daily mode | Daily + practice, no ads, modern UI |
| Sumplete | Grid sum-delete | Free + Ads | Closest mechanic, daily | Unsolvable puzzles (15-20%), ad complaints | Guaranteed solvable, polished UX |
| KenKen | Arithmetic + Sudoku | Free + Ads | Educational brand | Dated UI, no social | Modern Material 3, share cards |
| Kakuro (Conceptis) | Cross-sum numbers | Free + IAP | Deep logic, 200+ puzzles | 2010-era design, no daily | Beautiful design, daily format |
| Nerdle | Daily math equation | Free | Wordle virality, share cards | Single mechanic, word-guess format | Grid-based, multiple difficulties |
| Mathler | Daily equation | Free | Clean, multiple modes | No mobile app polish | Native Android, full game experience |
| 2048 | Sliding merge | Free | 17M+ downloads, iconic | No daily mode, stale | Fresh mechanic, social, progression |

### Market Gaps
1. **Ugly, ad-heavy interfaces** -- most number puzzles look like they were designed in 2010
2. **No social/viral sharing** -- no number puzzle has Wordle-quality share cards
3. **Fixed difficulty** -- no competitor scales grid size with difficulty (3x3 to 7x7)
4. **No daily + practice hybrid** -- either daily-only (Nerdle) or unlimited-only (Kakuro)
5. **Broken puzzle generation** -- Sumplete has 15-20% unsolvable puzzles at 5x5

### Demand Signals
- Wordle averaged **4M+ daily active users in 2025** (still growing, not declining)
- Multiple number puzzle games launched 2024-2025 (Mathler, Summle, Numerate)
- "Tired of Wordle" articles consistently recommend number puzzles as the next frontier
- Top Play Store complaints for competitors: "too many ads," "ugly UI," "wish I could share"
- Sumplete growing organically despite quality issues -- validates the sum-puzzle mechanic

### Market Size
- Mobile puzzle market: **$6.1B (2025)**, growing 7-9% CAGR
- Number puzzle sub-segment: **$500-750M**
- Even **0.01% market capture = $600K+** annual revenue potential

### Positioning
SumGrid occupies a unique gap: the **only** number grid puzzle with both daily and practice modes AND Wordle-style social sharing with modern Material 3 design.

```
                    Daily Only <----------> Unlimited Only
                         |                      |
    Social/Viral    Nerdle, Wordle          (nobody here)
         ^               |                      |
         |          * SumGrid *                  |
         |           (BOTH modes)                |
         v               |                      |
    No Sharing      Mathler, Summle     Sudoku.com, KenKen,
                                        Kakuro, Crossmath
```

---

## 3. Technical Feasibility

### Current Tech Stack
| Layer | Choice | Rationale |
|-------|--------|-----------|
| Language | Kotlin 2.0.21 | Modern, concise, KMP-ready |
| UI | Jetpack Compose + Material 3 | Declarative, clean architecture |
| Architecture | MVVM (ViewModel + StateFlow) | Standard Android, well-tested |
| Persistence | DataStore Preferences | Key-value, sufficient for Phase 1-2 |
| Background | WorkManager | Already in deps, handles notifications |
| Analytics | Firebase Analytics + Crashlytics | Conditionally applied (google-services.json gitignored) |
| Testing | JUnit 4, 872 tests passing | 53 test files, well-covered |
| DI | Manual (AppContainer) | Simple but needs Koin migration before Phase 2 |
| Rendering | Custom Canvas (GridRenderer) | High perf, but TalkBack-opaque |

### Build vs Buy Decisions
| Component | Decision | Reasoning |
|-----------|----------|-----------|
| Notifications | Platform API (WorkManager) | Already available, no new deps |
| Leaderboards | Google Play Games SDK | Official, well-documented |
| Ads | AdMob + UMP SDK | Industry standard, handles consent |
| Premium/IAP | Play Billing v7 (or RevenueCat) | RevenueCat simplifies edge cases, free up to $2.5K MRR |
| Themes | Build custom | Extend existing SumGridTheme |
| Widget | Glance (Jetpack) | Compose-like API, official |
| Multiplayer | Firebase Realtime DB | Start async, avoid real-time complexity |
| iOS port | Compose Multiplatform | Engine is pure Kotlin, ports cleanly |

### Feature Complexity Matrix
| Feature | Complexity | Dependencies | Phase |
|---------|------------|--------------|-------|
| Daily notifications | Simple | WorkManager | 1 |
| Themes/customization | Medium | DataStore | 1 |
| Font scaling a11y | Medium | Canvas text | 1 |
| Landscape layout | Medium | WindowSizeClass | 1 |
| Ads (AdMob) | Medium | AdMob SDK, UMP | 2 |
| Premium/IAP | Complex | Play Billing v7 | 2 |
| Leaderboards (GPG) | Medium | Play Games SDK | 3 |
| Home screen widget | Complex | Glance | 3 |
| Screen reader (TalkBack) | Complex | Compose semantics | 3 |
| Multiplayer (async) | Very Complex | Firebase | 4 |
| iOS port | Very Complex | KMP, CMP | 4 |

### Solo Dev Feasibility
**7 of 9 features are achievable in 6 months.** Multiplayer AND iOS port cannot both fit -- pick one as a stretch goal. Recommended: 70 working days for Phase 1-3, then evaluate before committing to Phase 4.

---

## 4. Risk Assessment

### Top Risks
| # | Risk | Severity | Likelihood | Mitigation |
|---|------|----------|------------|------------|
| 1 | **Discoverability** -- zero organic discovery, Play Store burial | Critical | High | Build web version first for SEO + shareable link; ASO + Reddit/PH launch |
| 2 | **Retention cliff at Day 14** -- 3 min of daily content is thin | High | High | Add daily puzzles at ALL 5 difficulties; streak freeze; puzzle archive |
| 3 | **No monetization** -- $0 revenue while consuming dev time | High | High | Decision point at Month 3: if DAU > 100, add tip jar/cosmetic IAP |
| 4 | **Solo dev burnout** -- 78+ dev-days for zero revenue | High | Medium | Cut roadmap to Phase 1-2 only; evaluate at Month 3 before Phase 3 |
| 5 | **Platform risk** -- Android-only excludes iOS spenders | Medium | Medium | Web version covers iOS users; track user-agents; iOS only if >20% demand |

### Key Assumptions (Unvalidated)
| Assumption | Risk Level | Validation Experiment | Cost to Validate |
|------------|------------|----------------------|-----------------|
| Users want sum puzzles vs Sudoku | High | Web prototype with 100+ players, track completion + return rates | 2 weeks |
| Share cards drive virality | High | Track share-to-install conversion post-launch; target >5% share rate | Instrumentation only |
| Practice mode retains users | Medium | Compare D7 retention: daily-only vs daily+practice cohorts | Analytics |
| "No ads" is a competitive advantage | Medium | Survey 50 users: "would you switch from Sudoku.com for no-ads?" | 1 week |
| dgeek cross-promotion has value | Low | Track cross-install rate from other dgeek apps | Instrumentation |

### Pre-Mortem
> It's March 2027 and SumGrid has 50 downloads. Here's what went wrong:

1. **Invisible Launch** -- Published to Play Store, nobody noticed. ASO keywords saturated. 47 installs from dgeek cross-promo. Zero organic growth.
2. **Retention Cliff** -- 500 installs from Product Hunt. Day 7: 16%. Day 14: 4%. Users complete 3 daily puzzles in 5 minutes, nothing else to do. Broken streaks cause churn, no recovery.
3. **Wordle Fatigue** -- Another "daily puzzle + share card" game in a saturated category. Users already have 5 daily games. Don't want a 6th.
4. **Solo Dev Burnout** -- Disappointing launch (200 installs) drains motivation. Phase 3 backend complexity explodes. Paying client project appears. "Pause" becomes permanent.
5. **Mechanic doesn't land** -- Users describe it as "basic math homework" not "puzzle addiction." Missing the deductive reasoning "aha" that makes Sudoku addictive.

### Kill Criteria
| Signal | Threshold | Action |
|--------|-----------|--------|
| < 200 installs after 60 days | Hard kill | Stop feature development, maintain as portfolio piece |
| < 20 DAU after 90 days | Hard kill | Same |
| Day 7 retention < 10% consistently | Hard kill | Core mechanic problem, features won't fix it |
| Share rate < 2% | Soft pivot | Stop investing in social features, focus on core puzzle quality |
| Web gets 10x more engagement than app | Soft pivot | Go PWA-first, deprioritize native Android |

---

## 5. The Roadmap

### Phase 0: Validate Before Building (Week 1-2)
**Goal**: Get the app in front of 100 real users and watch what they do.

| Validation | Method | Success Criteria | Status |
|------------|--------|-----------------|--------|
| Core mechanic appeal | Ship web version at sumgrid.dgeek.org (single-page, today's puzzle) | >30% completion rate, >10% return next day | Pending |
| Daily puzzle demand | Add Hard + Expert daily puzzles to home screen | DAU increase >20% within 2 weeks | Pending |
| Share card virality | Track share button taps + share-to-install funnel | >5% share rate per completion | Pending |
| Play Store listing | Publish to Play Store with optimized screenshots + description | >100 organic installs in first 30 days | Pending |
| Community seeding | Post to r/AndroidGaming, r/puzzles, Product Hunt | >500 installs from launch week | Pending |

### Phase 1: Retention & Polish (Month 1, ~20 days)
**Goal**: Make existing users stay. Fix the "nothing to do after 3 minutes" problem.

| Feature | Description | Complexity | Notes |
|---------|-------------|------------|-------|
| Daily puzzles at ALL difficulties | Add Hard (6x6) + Expert (7x7) daily puzzles to home screen | Simple | Session time: 3min -> 10-15min |
| Streak freeze | 1 free freeze per week (earned via 7-day streak) | Simple | Proven retention mechanic (Duolingo) |
| Daily reminder notifications | Push notification at user-chosen time | Simple | WorkManager, 1-2 days |
| Themes / customization | 4-5 unlockable color themes earned via streaks/achievements | Medium | Extend SumGridTheme, 4-6 days |
| Puzzle archive | Browse + solve past daily puzzles, calendar view | Medium | Calendar heatmap, medal system |
| Font scaling accessibility | Test + fix 200% font scale, Canvas text scaling | Medium | Reduces churn from a11y-conscious users |

**Success Criteria**: D7 retention >15%, D30 retention >8%, average session >5 minutes

### Phase 2: Monetization + Launch (Month 2-3, ~25 days)
**Goal**: Sustainable economics. Prove the app can generate any revenue.

| Feature | Description | Complexity | Notes |
|---------|-------------|------------|-------|
| Play Store launch | Full listing with optimized screenshots, ASO keywords | Simple | Keywords: "daily number puzzle", "math puzzle" |
| AdMob integration | Non-intrusive banner on home, optional rewarded video for hints | Medium | UMP consent for EU, 3-5 days |
| Premium tier | Remove ads + extra practice + themes ($2.99/yr or $0.99/mo) | Complex | RevenueCat recommended, 6-10 days |
| Landscape layout | Grid left, pad right; or portrait lock with explanation | Medium | WindowSizeClass, 3-5 days |
| Migrate DI to Koin | Replace manual AppContainer before feature count doubles | Medium | KMP-compatible, 2-3 days |

**Success Criteria**: >1,000 installs, >100 DAU, any non-zero revenue (even $1/month validates the model)

**DECISION POINT (Month 3)**: If DAU > 100, proceed to Phase 3. If DAU < 50, accept as portfolio piece and stop.

### Phase 3: Engagement & Social (Month 4-6, ~30 days)
**Goal**: Give users reasons to compete and share. Build the viral loop.

| Feature | Description | Complexity | Notes |
|---------|-------------|------------|-------|
| Achievement system | 15+ achievements: speed, streaks, difficulty milestones | Medium | Extends existing StreakBadge system |
| Google Play Games leaderboards | Daily leaderboard per difficulty, weekly/monthly rankings | Medium | Play Games SDK, 4-6 days |
| Home screen widget | Today's status, streak count, "Play Now" button | Complex | Glance, 5-8 days |
| TalkBack accessibility | Semantic overlay for Canvas grid, custom traversal | Complex | Prototype one difficulty first, 5-8 days |
| Hint system | Tiered: highlight row, reveal cell, show errors. Free hints earned via streaks | Medium | 3-4 days |

**Success Criteria**: >5,000 installs, >500 DAU, >5% share rate, any leaderboard engagement

### Phase 4: Expansion (Month 6-12)
**Goal**: Pick ONE stretch goal based on Phase 3 data.

| Feature | Description | Complexity | Notes |
|---------|-------------|------------|-------|
| Async challenge mode | Share a puzzle seed, compare times later (no real-time) | Complex | Firebase Firestore, avoids WebSocket complexity |
| Web version (PWA) | Single-page daily puzzle at sumgrid.dgeek.org | Complex | Kotlin/JS or standalone; SEO + share link target |
| Timed challenge mode | Solve max puzzles in 5 min, scaling difficulty | Medium | Shareable score card |
| Seasonal events | Monthly themed challenges, limited share cards | Medium | Content calendar, time-limited urgency |
| iOS port (if data supports) | Compose Multiplatform, shared engine | Very Complex | Only if >20% web traffic is iOS |

**Do NOT build both multiplayer AND iOS.** Pick based on data.

---

## 6. Go-To-Market Signals

### Distribution Channels
1. **Play Store ASO** -- Keywords: "daily number puzzle", "math puzzle game", "wordle for numbers", "grid puzzle"
2. **Reddit** -- r/AndroidGaming, r/puzzles, r/numbertheory, r/IndieGaming
3. **Product Hunt** -- Launch with compelling screenshots and story
4. **Puzzle aggregators** -- Listdle.com, LikeWordle, Crosswordle.com (free listings)
5. **Share cards** -- The viral loop: complete puzzle -> share -> recipient clicks link -> install
6. **Web version** -- SEO-indexed daily puzzle page drives organic traffic

### Pricing Strategy
- **Free tier**: All daily puzzles, basic practice mode, share cards, streaks
- **Premium ($2.99/yr or $0.99/mo)**: Remove ads, extra themes, unlimited hints, puzzle archive access
- **Benchmark**: Sudoku.com charges $3.99/mo. SumGrid undercuts dramatically.
- **Alternative**: Tip jar / "Buy me a coffee" for the no-ads philosophy crowd

### Launch Strategy (First 100 Users)
1. Week 1: Ship web version, share with puzzle communities
2. Week 2: Publish to Play Store, cross-promote from dgeek apps
3. Week 3: Product Hunt launch + Reddit posts (r/AndroidGaming, r/puzzles)
4. Week 4: Reach out to 5 puzzle/gaming YouTubers/bloggers for reviews
5. Ongoing: Daily share cards create slow organic growth loop

---

## 7. Decision Summary

### Should You Build This?
**Yes, with guardrails.** SumGrid has strong fundamentals: unique mechanic, clean architecture, pure-Kotlin engine (portable), beautiful Material 3 design, and a genuinely underserved market position (no competitor combines daily + practice + share cards for number puzzles).

**But** the biggest risk isn't technical -- it's discoverability and the assumption that "build it and they will come." The roadmap must be ruthlessly scoped to Phase 1-2 (~45 days), with a hard decision point at Month 3 before investing in Phase 3-4.

The Critical Analyst's pre-mortem is worth re-reading before every sprint. The most likely failure mode is an invisible launch followed by solo dev burnout on a zero-revenue project.

### Biggest Open Question
**Does the sum-puzzle mechanic create the "aha" moment that drives puzzle addiction, or is it experienced as "basic math homework"?** This can only be answered by putting the app in front of 100+ real users and measuring D7 retention and session frequency. No amount of feature work answers this question.

### Recommended Next Step
**Ship the web version at sumgrid.dgeek.org within 2 weeks.** This validates the mechanic with real users, gives the share card URL a landing page, enables SEO discovery, and costs ~5 days of effort. If the web version gets >30% completion rate and >10% next-day return, the mechanic works and the full roadmap is justified.

---

*Generated by Roadmap Planning Agent Team*
*Research: Idea Explorer, Market Researcher, Feasibility Analyst, Critical Analyst*
