package org.dgeek.sumgrid.ui

import androidx.compose.ui.graphics.Color
import org.dgeek.sumgrid.ui.components.GridColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * JVM unit tests for [GridColors] data class.
 *
 * Tests are pure JVM — they do not require an Android device or emulator.
 * GridColors is a plain data holder; no Compose runtime is needed.
 */
class GridRendererTest {

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun Color.toArgb(): Int {
        val a = (alpha * 255f + 0.5f).toInt()
        val r = (red   * 255f + 0.5f).toInt()
        val g = (green * 255f + 0.5f).toInt()
        val b = (blue  * 255f + 0.5f).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun Int.toHexString(): String = "0x%08X".format(this)

    private fun assertColorEquals(expectedArgb: Long, actual: Color, name: String) {
        val expected = expectedArgb.toInt()
        val actualArgb = actual.toArgb()
        assertEquals(
            "$name: expected ${expected.toHexString()} but was ${actualArgb.toHexString()}",
            expected,
            actualArgb
        )
    }

    // ── Construction ─────────────────────────────────────────────────────────

    @Test
    fun `GridColors can be constructed with all 9 named color fields`() {
        val colors = GridColors(
            givenCellBg    = Color(0xFFDDE1FF),
            userCellBg     = Color(0xFFFFFBFF),
            gridLine       = Color(0xFF1B1B1F),
            selectedBorder = Color(0xFFEFC400),
            cellText       = Color(0xFF1B1B1F),
            givenText      = Color(0xFF0001AC),
            sumGreen       = Color(0xFF1B6C2E),
            sumRed         = Color(0xFFBA1A1A),
            sumGray        = Color(0xFF49454F)
        )
        assertNotNull(colors)
    }

    @Test
    fun `GridColors fields are all non-null`() {
        val colors = GridColors.defaults()
        assertNotNull(colors.givenCellBg)
        assertNotNull(colors.userCellBg)
        assertNotNull(colors.gridLine)
        assertNotNull(colors.selectedBorder)
        assertNotNull(colors.cellText)
        assertNotNull(colors.givenText)
        assertNotNull(colors.sumGreen)
        assertNotNull(colors.sumRed)
        assertNotNull(colors.sumGray)
    }

    // ── Default factory values ────────────────────────────────────────────────

    @Test
    fun `defaults givenCellBg is IndigoContainer90`() {
        assertColorEquals(0xFFDDE1FFL, GridColors.defaults().givenCellBg, "givenCellBg")
    }

    @Test
    fun `defaults userCellBg is Neutral99`() {
        assertColorEquals(0xFFFFFBFFL, GridColors.defaults().userCellBg, "userCellBg")
    }

    @Test
    fun `defaults gridLine is Neutral10`() {
        assertColorEquals(0xFF1B1B1FL, GridColors.defaults().gridLine, "gridLine")
    }

    @Test
    fun `defaults selectedBorder is Amber80`() {
        assertColorEquals(0xFFEFC400L, GridColors.defaults().selectedBorder, "selectedBorder")
    }

    @Test
    fun `defaults cellText is Neutral10`() {
        assertColorEquals(0xFF1B1B1FL, GridColors.defaults().cellText, "cellText")
    }

    @Test
    fun `defaults givenText is Indigo20`() {
        assertColorEquals(0xFF0001ACL, GridColors.defaults().givenText, "givenText")
    }

    @Test
    fun `defaults sumGreen is semantic success green`() {
        assertColorEquals(0xFF1B6C2EL, GridColors.defaults().sumGreen, "sumGreen")
    }

    @Test
    fun `defaults sumRed is ErrorRed40`() {
        assertColorEquals(0xFFBA1A1AL, GridColors.defaults().sumRed, "sumRed")
    }

    @Test
    fun `defaults sumGray is muted gray`() {
        assertColorEquals(0xFF49454FL, GridColors.defaults().sumGray, "sumGray")
    }

    // ── Data class equality ───────────────────────────────────────────────────

    @Test
    fun `two GridColors with same values are equal`() {
        val a = GridColors.defaults()
        val b = GridColors.defaults()
        assertEquals(a, b)
    }

    @Test
    fun `GridColors copy produces independent instance with modified field`() {
        val original = GridColors.defaults()
        val modified = original.copy(sumRed = Color.Blue)
        assertColorEquals(0xFF0000FFL, modified.sumRed, "modified sumRed")
        // original is unchanged
        assertColorEquals(0xFFBA1A1AL, original.sumRed, "original sumRed unchanged")
    }
}
