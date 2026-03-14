package org.dgeek.sumgrid.engine.models

data class Puzzle(
    val size: Int,
    val cells: Array<Array<Cell>>,
    val rowTargets: IntArray,
    val colTargets: IntArray,
    val difficulty: Difficulty,
    val seed: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Puzzle) return false
        if (size != other.size) return false
        if (difficulty != other.difficulty) return false
        if (seed != other.seed) return false
        if (!rowTargets.contentEquals(other.rowTargets)) return false
        if (!colTargets.contentEquals(other.colTargets)) return false
        if (cells.size != other.cells.size) return false
        for (i in cells.indices) {
            if (!cells[i].contentEquals(other.cells[i])) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + seed.hashCode()
        result = 31 * result + rowTargets.contentHashCode()
        result = 31 * result + colTargets.contentHashCode()
        result = 31 * result + cells.fold(1) { acc, row -> 31 * acc + row.contentHashCode() }
        return result
    }
}
