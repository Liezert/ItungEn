package com.liezert.itungen.ui.settings

enum class NotificationAsk { REQUEST, OPEN_SETTINGS }

/**
 * Langkah saat pengingat dinyalakan tanpa izin notifikasi. Setelah ditolak permanen, Android tidak menampilkan dialog
 * izin lagi; tandanya: sudah pernah ditanya dan Android tidak lagi meminta penjelasan ([showRationale] false).
 * Hanya saat itu pengaturan notifikasi dibuka; penolakan biasa cukup dihormati.
 */
fun notificationAsk(askedBefore: Boolean, showRationale: Boolean): NotificationAsk =
    if (askedBefore && !showRationale) NotificationAsk.OPEN_SETTINGS else NotificationAsk.REQUEST
