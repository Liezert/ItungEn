package com.liezert.itungen.ui.goals

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.ui.components.ProgressBar
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText

/** Bar progres goal: aksen biru, hijau setelah tercapai. Setoran di atas target tetap bar penuh (tidak meluber). */
@Composable
fun GoalProgressBar(goal: Goal, height: Dp, modifier: Modifier = Modifier) = ProgressBar(
    goal.progress,
    if (goal.achieved) SolidColor(Ink.Green) else Brush.horizontalGradient(listOf(Ink.Accent, Ink.AccentLight)),
    height,
    Ink.Track,
    modifier,
)

/**
 * Status goal: "Tercapai" dengan ikon centang (bukan hanya warna; TalkBack membaca "Target tercapai"), atau
 * "65% tercapai". Warna chip tercapai = token hijau tema + teks di atasnya ([Ink.OnGreen]), kontras diuji per tema.
 */
@Composable
fun GoalStatusChip(goal: Goal, modifier: Modifier = Modifier) {
    if (goal.achieved) {
        val reached = stringResource(R.string.goal_reached_a11y)
        Row(
            modifier
                .clip(CircleShape)
                .background(Ink.Green)
                .padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp)
                .clearAndSetSemantics { contentDescription = reached },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(Ink.OnGreen))
            Text(stringResource(R.string.goal_reached), style = appText(12f, 16f, Bold, Ink.OnGreen))
        }
    } else {
        Box(modifier.clip(CircleShape).background(Ink.Accent.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(stringResource(R.string.goal_percent, goal.percent), style = appText(12f, 16f, SemiBold, Ink.AccentLight))
        }
    }
}

/** Aksen halus kartu goal yang sudah tercapai (hijau tipis dari atas); null = kartu biasa. */
fun goalTint(goal: Goal): Brush? =
    if (goal.achieved) Brush.verticalGradient(listOf(Ink.Green.copy(alpha = 0.16f), Color.Transparent)) else null
