package org.dgeek.sumgrid.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM source-scan tests for BadgeDetailBottomSheet (S2C-F002).
 *
 * T024 — RED: these tests must fail before the composable is created.
 * T026 — REFACTOR: verify structural correctness after T025 implementation.
 */
class BadgeDetailBottomSheetTest {

    private val sheetSource: String by lazy {
        val file = java.io.File(
            "src/main/kotlin/org/dgeek/sumgrid/ui/components/BadgeDetailBottomSheet.kt"
        )
        if (file.exists()) file.readText() else ""
    }

    private val homeScreenSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt").readText()
    }

    @Test
    fun badgeDetailBottomSheet_fileExists() {
        assertTrue(
            "BadgeDetailBottomSheet.kt must exist at ui/components/",
            sheetSource.isNotEmpty()
        )
    }

    @Test
    fun badgeDetailBottomSheet_usesModalBottomSheet() {
        assertTrue(
            "BadgeDetailBottomSheet must use ModalBottomSheet",
            sheetSource.contains("ModalBottomSheet")
        )
    }

    @Test
    fun badgeDetailBottomSheet_showsBadgeName() {
        assertTrue(
            "BadgeDetailBottomSheet must display the badge displayName",
            sheetSource.contains("displayName")
        )
    }

    @Test
    fun badgeDetailBottomSheet_showsBadgeIcon() {
        assertTrue(
            "BadgeDetailBottomSheet must display the badge icon emoji",
            sheetSource.contains("badge.icon") || sheetSource.contains(".icon")
        )
    }

    @Test
    fun badgeDetailBottomSheet_hasEarnedParameter() {
        assertTrue(
            "BadgeDetailBottomSheet must accept an 'earned' boolean parameter",
            sheetSource.contains("earned")
        )
    }

    @Test
    fun badgeDetailBottomSheet_hasOnDismiss() {
        assertTrue(
            "BadgeDetailBottomSheet must have an onDismiss callback",
            sheetSource.contains("onDismiss")
        )
    }

    @Test
    fun homeScreen_wiresBadgeDetailSheet() {
        assertTrue(
            "HomeScreen must show BadgeDetailBottomSheet when selectedBadge is not null",
            homeScreenSource.contains("BadgeDetailBottomSheet")
        )
    }
}
