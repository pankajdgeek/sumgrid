package org.dgeek.sumgrid.streak

/**
 * Milestone badges awarded for maintaining a streak for a given number of consecutive days.
 * Once earned, a badge is never lost even if the streak resets.
 */
enum class StreakBadge(val requiredDays: Int, val displayName: String, val icon: String) {
    WEEKLY_WARRIOR(7, "Weekly Warrior", "\uD83D\uDD25"),    // 🔥
    MONTHLY_MASTER(30, "Monthly Master", "\u2B50"),          // ⭐
    CENTURY_SOLVER(100, "Century Solver", "\uD83D\uDC8E"),   // 💎
    YEAR_OF_LOGIC(365, "Year of Logic", "\uD83D\uDC51")      // 👑
}
