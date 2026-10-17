package com.liezert.itungen.ui.goals

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.liezert.itungen.ItungEnApp

/**
 * Gambar produk goal dari penyimpanan app (tanpa jaringan), memenuhi kotaknya. Digambar di atas [placeholder]:
 * tanpa gambar, atau bila file-nya hilang (mis. setelah memulihkan cadangan di HP lain), placeholder yang terlihat.
 */
@Composable
fun GoalImage(
    imagePath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: @Composable BoxScope.() -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        placeholder()
        if (imagePath != null) {
            val images = (LocalContext.current.applicationContext as ItungEnApp).container.goalImages
            AsyncImage(
                model = images.file(imagePath),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
