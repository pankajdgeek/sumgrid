package org.dgeek.sumgrid.engine.models

data class Cell(val value: Int, val isGiven: Boolean) {
    val isEmpty: Boolean get() = value == 0
}
