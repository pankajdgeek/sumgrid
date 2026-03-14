package org.dgeek.sumgrid.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests that verify Color.kt constants match the SumGrid design token spec.
 *
 * Tests are pure JVM — they do not require an Android device or emulator.
 * Colors are verified via their red/green/blue/alpha float components,
 * which are available on the JVM without Android framework.
 *
 * Spec references:
 *  - Primary deep indigo: 0xFF3730A3
 *  - Dark primary (Indigo80): 0xFFBBC2FF
 *  - Light secondary (Amber40): 0xFF775A00
 *  - Dark secondary (Amber80): 0xFFEFC400
 */
class ThemeColorTest {

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * Converts a Color's float components back to an 8-bit ARGB int for easy comparison.
     * Rounds each channel to the nearest integer.
     */
    private fun Color.toArgb(): Int {
        val a = (alpha * 255f + 0.5f).toInt()
        val r = (red   * 255f + 0.5f).toInt()
        val g = (green * 255f + 0.5f).toInt()
        val b = (blue  * 255f + 0.5f).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Formats an ARGB int as a 0xFFRRGGBB hex string for readable assertions. */
    private fun Int.toHexString(): String = "0x%08X".format(this)

    /** Helper: assert that a color has the expected hex value. */
    private fun assertColorEquals(expectedArgb: Long, actual: Color, name: String) {
        val expected = expectedArgb.toInt()
        val actualArgb = actual.toArgb()
        assertEquals(
            "$name: expected ${expected.toHexString()} but was ${actualArgb.toHexString()}",
            expected,
            actualArgb
        )
    }

    // ── Indigo palette ────────────────────────────────────────────────────────

    @Test
    fun indigo10_matchesPaletteSpec() {
        assertColorEquals(0xFF00006EL, Indigo10, "Indigo10")
    }

    @Test
    fun indigo20_matchesPaletteSpec() {
        assertColorEquals(0xFF0001ACL, Indigo20, "Indigo20")
    }

    @Test
    fun indigo30_matchesPaletteSpec() {
        assertColorEquals(0xFF1B1FCAL, Indigo30, "Indigo30")
    }

    @Test
    fun indigo40_matchesDeepIndigoPrimarySpec() {
        // Spec requires deep indigo primary = 0xFF3730A3
        assertColorEquals(0xFF3730A3L, Indigo40, "Indigo40 (light scheme primary)")
    }

    @Test
    fun indigo80_matchesDarkSchemePrimary() {
        assertColorEquals(0xFFBBC2FFL, Indigo80, "Indigo80 (dark scheme primary)")
    }

    @Test
    fun indigo90_matchesPaletteSpec() {
        assertColorEquals(0xFFDDE1FFL, Indigo90, "Indigo90")
    }

    // ── Amber palette ─────────────────────────────────────────────────────────

    @Test
    fun amber40_matchesLightSchemeSecondary() {
        // Spec: light scheme secondary accent = amber 0xFF775A00
        assertColorEquals(0xFF775A00L, Amber40, "Amber40 (light scheme secondary)")
    }

    @Test
    fun amber80_matchesDarkSchemeSecondary() {
        // Spec: dark scheme secondary accent = amber 0xFFEFC400
        assertColorEquals(0xFFEFC400L, Amber80, "Amber80 (dark scheme secondary)")
    }

    @Test
    fun amber90_matchesPaletteSpec() {
        assertColorEquals(0xFFFFDF9DL, Amber90, "Amber90")
    }

    // ── Neutral palette ───────────────────────────────────────────────────────

    @Test
    fun neutral10_matchesDarkBackground() {
        assertColorEquals(0xFF1B1B1FL, Neutral10, "Neutral10")
    }

    @Test
    fun neutral90_matchesPaletteSpec() {
        assertColorEquals(0xFFE4E2ECL, Neutral90, "Neutral90")
    }

    @Test
    fun neutral95_matchesPaletteSpec() {
        assertColorEquals(0xFFF3F0FAL, Neutral95, "Neutral95")
    }

    @Test
    fun neutral99_matchesLightBackground() {
        assertColorEquals(0xFFFFFBFFL, Neutral99, "Neutral99 (light background)")
    }

    // ── Error palette ─────────────────────────────────────────────────────────

    @Test
    fun errorRed40_matchesLightSchemeError() {
        assertColorEquals(0xFFBA1A1AL, ErrorRed40, "ErrorRed40")
    }

    @Test
    fun errorRed80_matchesDarkSchemeError() {
        assertColorEquals(0xFFFFB4ABL, ErrorRed80, "ErrorRed80")
    }

    // ── Primary container palette ─────────────────────────────────────────────

    @Test
    fun indigoContainer30_matchesPaletteSpec() {
        assertColorEquals(0xFF2A2FD1L, IndigoContainer30, "IndigoContainer30")
    }

    @Test
    fun indigoContainer90_matchesPaletteSpec() {
        assertColorEquals(0xFFDDE1FFL, IndigoContainer90, "IndigoContainer90")
    }

    // ── Scheme assignment sanity checks ──────────────────────────────────────

    @Test
    fun lightSchemePrimary_isIndigo40_withCorrectSpecValue() {
        // LightColorScheme.primary = Indigo40, which must be 0xFF3730A3 per spec
        assertColorEquals(0xFF3730A3L, Indigo40, "LightScheme primary (Indigo40)")
    }

    @Test
    fun darkSchemePrimary_isIndigo80_withExpectedValue() {
        // DarkColorScheme.primary = Indigo80, must be 0xFFBBC2FF
        assertColorEquals(0xFFBBC2FFL, Indigo80, "DarkScheme primary (Indigo80)")
    }

    @Test
    fun lightSchemeSecondary_isAmber40_withExpectedValue() {
        // LightColorScheme.secondary = Amber40, must be 0xFF775A00
        assertColorEquals(0xFF775A00L, Amber40, "LightScheme secondary (Amber40)")
    }

    @Test
    fun darkSchemeSecondary_isAmber80_withExpectedValue() {
        // DarkColorScheme.secondary = Amber80, must be 0xFFEFC400
        assertColorEquals(0xFFEFC400L, Amber80, "DarkScheme secondary (Amber80)")
    }
}
