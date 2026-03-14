# Feasibility Analysis: SumGrid Growth Features

## 1. Current Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 2.0.21 |
| UI Framework | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| Min SDK | 24 (Android 7.0) | ~97% device coverage |
| Target SDK | 35 | Android 15 |
| Build System | AGP 8.7.3, Gradle version catalog | |
| Navigation | Navigation Compose | 2.8.5 |
| Persistence | DataStore Preferences | 1.1.1 |
| Background Work | WorkManager | 2.10.0 |
| Analytics | Firebase Analytics + Crashlytics | BOM 33.7.0 |
| Review | Play In-App Review KTX | 2.0.2 |
| Async | Kotlin Coroutines | 1.9.0 |
| DI | Manual (AppContainer, lazy singletons) | No framework |
| Architecture | MVVM (ViewModel + StateFlow + Compose) | |
| Rendering | Custom Canvas (GridRenderer) | |
| Testing | JUnit 4 + Espresso + Compose UI Test | 53 test files |

**Key Observations:**
- Pure Kotlin, zero Java files. 48 source files, 53 test files -- well-tested.
- Puzzle engine is pure Kotlin with zero Android dependencies (portable).
- Manual DI via AppContainer -- simple but will strain at scale.
- No Room/SQLite database -- all persistence is DataStore (key-value only).
- No dependency injection framework (no Hilt/Koin/Dagger).
- Firebase plugins are declared but conditionally applied (google-services.json is gitignored).
- Grid rendered via Canvas draw calls, not composable cells -- high performance but harder to make accessible.
- Single Activity architecture with Compose Navigation.

---

## 2. Feature Complexity Matrix

| Feature | Complexity | Effort | Dependencies | Notes |
|---------|-----------|--------|-------------|-------|
| **Daily Reminder Notifications** | S (Simple) | 1-2 days | WorkManager (already in deps), NotificationChannel API | Schedule periodic worker, create notification channel. WorkManager already available. |
| **Landscape Layout** | M (Medium) | 3-5 days | WindowSizeClass, BoxWithConstraints | Grid is Canvas-drawn with `aspectRatio(1f)` -- needs side-by-side layout for numpad. Every screen needs landscape variant. |
| **Accessibility (Font Scaling)** | M (Medium) | 3-5 days | None new | Grid uses Canvas `drawText` with hardcoded `sp` sizes. Must respect `LocalDensity` font scale. NumberPad and all text already use `sp` (good). Canvas text is the main blocker. |
| **Accessibility (Screen Reader)** | C (Complex) | 5-8 days | TalkBack, Compose semantics API | Canvas-rendered grid is opaque to TalkBack. Need invisible semantic overlay with per-cell `contentDescription`, `Role`, `stateDescription`. Custom traversal order required. |
| **Leaderboards / Achievements (GPG)** | M (Medium) | 4-6 days | Play Games Services SDK (~2MB), Google Sign-In | New dependency. Requires Play Console setup, achievement/leaderboard definitions, sign-in flow. Well-documented API but auth adds complexity. |
| **Multiplayer / Challenge Mode** | VC (Very Complex) | 20-30 days | Firebase Realtime DB or Firestore, Cloud Functions, auth system | Requires backend infrastructure, real-time sync, matchmaking, anti-cheat. Massive scope increase for a solo dev. |
| **Themes / Customization** | M (Medium) | 4-6 days | CompositionLocal, DataStore | Theme system already exists (SumGridTheme with light/dark). Need to add color scheme persistence, theme picker UI, and extend GridColors. |
| **iOS Port (KMP)** | VC (Very Complex) | 40-60 days | Kotlin Multiplatform, Compose Multiplatform | Engine is portable (pure Kotlin). UI is 100% Jetpack Compose -- must be rewritten in Compose Multiplatform or SwiftUI. DataStore has KMP support. Navigation, lifecycle management diverge. |
| **Home Screen Widget** | C (Complex) | 5-8 days | Glance (Jetpack), WorkManager | Glance uses Compose-like API but is a separate composable tree. Need widget layout, data sync, click handling. Must work under RemoteViews constraints. |
| **Monetization - Ads** | M (Medium) | 3-5 days | AdMob SDK, GDPR consent (UMP SDK) | Banner/interstitial placement. Must handle EU consent. Need ad-free experience for premium. |
| **Monetization - Premium** | C (Complex) | 6-10 days | Google Play Billing Library v7 | Subscription/one-time purchase. Purchase verification, entitlement tracking, restore purchases. Non-trivial billing edge cases. |

---

## 3. Build vs Buy Decisions

| Feature | Recommendation | Rationale |
|---------|---------------|-----------|
| **Daily Notifications** | Platform API | WorkManager + NotificationManager. No library needed, already have WorkManager. |
| **Landscape Layout** | Build custom | Compose adaptive layouts with `WindowSizeClass`. No library needed. |
| **Font Scaling** | Build custom | Compose handles most of this. Canvas drawing needs manual scaling. |
| **Screen Reader** | Build custom | Compose semantics API. No shortcut -- must annotate Canvas grid manually. |
| **Leaderboards** | Platform API (Google Play Games) | Use official Play Games SDK. Do not build custom leaderboard backend. |
| **Multiplayer** | Firebase + custom | Firebase Realtime DB for sync. Build matchmaking/challenge logic. Consider deferring -- highest effort, lowest solo-dev ROI. |
| **Themes** | Build custom | Extend existing SumGridTheme. Material 3 dynamic color is already in the code (disabled). |
| **iOS Port** | Compose Multiplatform | Engine ports directly. UI requires Compose Multiplatform (CMP). Alternative: native SwiftUI for iOS -- higher quality, higher effort. |
| **Widget** | Glance (Jetpack) | Official Compose-based widget framework. Much better than raw RemoteViews. |
| **Ads** | AdMob SDK | Industry standard. Use Google UMP SDK for consent. |
| **Premium** | Play Billing Library | Official SDK. No alternative for Play Store purchases. Consider RevenueCat wrapper to simplify (adds dependency but saves weeks). |

---

## 4. Solo Dev Feasibility: 6-Month Plan

### Can one person ship all 9 features in 6 months?

**No. Not all 9.** Multiplayer alone is 20-30 days and requires ongoing backend maintenance. iOS port is 40-60 days. Together they consume 60-90 days of a ~130 working-day window, leaving almost nothing for the other 7 features.

### Recommended Prioritization (6-month budget: ~130 working days)

**Phase 1: Retention & Polish (Weeks 1-4, ~20 days)**
- Daily Reminder Notifications: 2 days
- Themes / Customization: 5 days
- Font Scaling Accessibility: 4 days
- Landscape Layout: 5 days
- Buffer: 4 days

**Phase 2: Monetization (Weeks 5-8, ~20 days)**
- Ads (AdMob + consent): 5 days
- Premium / In-App Purchase: 8 days
- Buffer: 7 days

**Phase 3: Engagement & Social (Weeks 9-14, ~30 days)**
- Leaderboards & Achievements (GPG): 6 days
- Home Screen Widget: 7 days
- Screen Reader Accessibility (TalkBack): 7 days
- Buffer: 10 days

**Phase 4: Stretch Goals (Weeks 15-26, ~60 days)**
- Multiplayer Challenge Mode (simplified async version): 25 days
- OR iOS Port (engine only + minimal UI): 35 days
- NOT both in the same 6 months

**Total estimated effort for Phase 1-3: ~70 days.** Achievable with buffer.
**Phase 4 is pick-one.** Multiplayer or iOS, not both.

### DI Framework Decision Point

At 7+ features with cross-cutting concerns (auth, billing, analytics, notifications), the manual `AppContainer` will become unwieldy. Recommend migrating to **Koin** (lightweight, no code gen, KMP-compatible) before Phase 2. Estimated migration: 2-3 days. This also positions the codebase for a future KMP port.

---

## 5. Technical Risks

### Risk 1: Canvas Grid is Not Accessible (Severity: HIGH)
The entire puzzle grid is rendered via `Canvas` draw calls. TalkBack cannot traverse it. Adding semantic overlays for a variable-size grid (3x3 to 7x7) with per-cell state is non-trivial. Risk: accessibility work takes 2-3x longer than estimated because Canvas semantics require invisible overlay composables positioned precisely over each cell.

**Mitigation**: Prototype a single-difficulty semantic overlay first. Use `clearAndSetSemantics` with a grid-like traversal. Test with TalkBack on a real device early.

### Risk 2: DataStore Limitations at Scale (Severity: MEDIUM)
DataStore Preferences is key-value only. Features like leaderboard history, multiplayer state, achievement progress, and theme preferences will need structured storage. DataStore is not designed for relational queries or large datasets.

**Mitigation**: Introduce Room when the first feature needs it (likely leaderboards or multiplayer). Migrate completion/streak data incrementally. Room + DataStore can coexist.

### Risk 3: Google Play Billing Edge Cases (Severity: MEDIUM)
Billing Library v7 has known complexity: pending purchases, subscription grace periods, account holds, purchase token verification, proration modes. Solo devs frequently underestimate this.

**Mitigation**: Consider RevenueCat SDK as a wrapper -- handles server-side verification, webhook management, and cross-platform entitlements. Cost: $0 up to $2.5K MRR, then 1%. Saves 3-5 days of edge-case handling.

### Risk 4: Multiplayer Requires Backend Infrastructure (Severity: HIGH)
No existing backend. Multiplayer needs: user auth, matchmaking, real-time puzzle sync, anti-cheat validation, and ongoing server costs. Firebase can handle the tech, but operational overhead is significant for a solo dev.

**Mitigation**: Start with async challenge mode (share a puzzle seed, compare times later) instead of real-time multiplayer. This eliminates real-time sync, matchmaking, and most backend complexity. Can be built with Firebase Firestore alone.

### Risk 5: iOS Port Scope Creep (Severity: HIGH)
The puzzle engine is pure Kotlin and ports cleanly to KMP. However, the UI layer (48 files of Jetpack Compose) is entirely Android-specific. Compose Multiplatform for iOS is stable but has gaps (no Glance, different lifecycle, platform-specific APIs for notifications/billing/widgets). The "port" is really "rebuild the UI layer."

**Mitigation**: If pursuing iOS, use Compose Multiplatform for shared UI where possible, but budget for iOS-specific implementations of: notifications, widgets, in-app purchase (StoreKit), and analytics. Consider SwiftUI for iOS-only features.

### Risk 6: Manual DI Won't Scale (Severity: LOW-MEDIUM)
AppContainer currently has ~10 lazy singletons. Adding auth, billing, ad manager, notification scheduler, theme repository, and multiplayer services could double this. Constructor chains get deep. Testing becomes harder without proper scoping.

**Mitigation**: Migrate to Koin before feature count doubles. Koin is zero-codegen, KMP-compatible, and can be adopted incrementally (one module at a time). Estimated: 2-3 days.

### Risk 7: ProGuard/R8 Breakage with New SDKs (Severity: LOW)
Adding Play Games, AdMob, and Billing SDKs introduces new ProGuard rules. R8 is already enabled (`isMinifyEnabled = true`). Each new SDK may require specific keep rules.

**Mitigation**: Test release builds after each SDK integration. Keep ProGuard rules in separate files per SDK for clarity.

---

## Summary

The SumGrid codebase is clean, well-tested, and well-architected for its current scope. The pure-Kotlin engine is a major asset for portability. The main technical debts that need addressing before growth features are: (1) Canvas accessibility, (2) DataStore-to-Room migration path, and (3) DI framework adoption.

A solo dev can realistically ship 7 of the 9 features in 6 months (everything except multiplayer AND iOS -- pick at most one). The recommended order prioritizes retention (notifications, themes) before monetization (ads, premium) before engagement (leaderboards, widgets).
