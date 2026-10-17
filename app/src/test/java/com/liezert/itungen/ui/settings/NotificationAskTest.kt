package com.liezert.itungen.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Bug B1: "Don't allow" pertama kali dulu langsung melempar ke pengaturan notifikasi sistem. Pengaturan hanya dibuka
 * bila dialog izin memang tidak akan muncul lagi (sudah pernah ditanya dan Android tidak lagi menampilkan alasannya).
 */
class NotificationAskTest {
    @Test
    fun firstTime_andAfterOneDenial_showTheDialog() {
        assertEquals(NotificationAsk.REQUEST, notificationAsk(askedBefore = false, showRationale = false))
        assertEquals(NotificationAsk.REQUEST, notificationAsk(askedBefore = true, showRationale = true))
    }

    @Test
    fun permanentlyDenied_opensSettings() {
        assertEquals(NotificationAsk.OPEN_SETTINGS, notificationAsk(askedBefore = true, showRationale = false))
    }
}
