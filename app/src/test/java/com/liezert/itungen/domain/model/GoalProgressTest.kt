package com.liezert.itungen.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Progres goal: bar dibatasi 0..1, persen sebenarnya (dibulatkan ke bawah), tercapai saat terkumpul ≥ target. */
class GoalProgressTest {
    private fun goal(saved: Long, target: Long) = Goal("Sepeda", saved = saved, target = target)

    @Test
    fun zero() = with(goal(0, 800_000)) {
        assertEquals(Triple(0f, 0, false), Triple(progress, percent, achieved))
    }

    @Test
    fun belowTarget_floorsPercent() {
        assertEquals(Triple(0.5f, 50, false), with(goal(400_000, 800_000)) { Triple(progress, percent, achieved) })
        // 99,9%: belum tercapai, jadi jangan tampil "100%".
        assertEquals(99 to false, with(goal(999, 1_000)) { percent to achieved })
    }

    @Test
    fun exactlyTarget_isAchieved() = with(goal(800_000, 800_000)) {
        assertEquals(Triple(1f, 100, true), Triple(progress, percent, achieved))
    }

    @Test
    fun aboveTarget_barStaysFull_percentKeepsGoing() = with(goal(1_200_000, 800_000)) {
        assertEquals(Triple(1f, 150, true), Triple(progress, percent, achieved))
        assertEquals(0L, remaining)
    }

    @Test
    fun zeroTarget_isNeverAchieved() = with(goal(5_000, 0)) {
        assertEquals(Triple(0f, 0, false), Triple(progress, percent, achieved))
    }
}
