package com.liezert.itungen.data.images

import org.junit.Assert.assertEquals
import org.junit.Test

/** Fase 2: ukuran gambar produk dan pembersihan file yatim di goal_images. */
class GoalImagesTest {
    @Test
    fun scaledSize_capsLongestSide_keepsAspect() {
        assertEquals(1080 to 720, scaledSize(3000, 2000))
        assertEquals(720 to 1080, scaledSize(2000, 3000))
        assertEquals(800 to 600, scaledSize(800, 600)) // tidak diperbesar
        assertEquals(1080 to 1, scaledSize(5000, 1)) // sisi pendek minimal 1 px
    }

    @Test
    fun orphans_areFilesNoGoalUses() {
        val files = listOf("goal_a.webp", "goal_b.webp", "goal_c.webp.tmp")
        assertEquals(listOf("goal_b.webp", "goal_c.webp.tmp"), orphanImages(files, referenced = setOf("goal_a.webp", null)))
        assertEquals(emptyList<String>(), orphanImages(emptyList(), setOf("goal_a.webp")))
    }
}
