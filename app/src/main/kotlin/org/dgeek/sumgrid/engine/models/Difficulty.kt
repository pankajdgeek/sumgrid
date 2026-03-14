package org.dgeek.sumgrid.engine.models

enum class Difficulty(val size: Int, val maxVal: Int, val emptyCells: Int, val seedOffset: Int) {
    BEGINNER(size = 3, maxVal = 5, emptyCells = 4, seedOffset = 0),
    EASY(size = 4, maxVal = 7, emptyCells = 8, seedOffset = 1),
    MEDIUM(size = 5, maxVal = 9, emptyCells = 15, seedOffset = 2),
    HARD(size = 6, maxVal = 9, emptyCells = 18, seedOffset = 3),
    EXPERT(size = 7, maxVal = 9, emptyCells = 24, seedOffset = 4)
}
