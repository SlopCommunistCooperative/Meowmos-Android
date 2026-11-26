package io.github.slopcommunistcooperative.meowmos.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.slopcommunistcooperative.meowmos.data.model.DailyUsageStat
import io.github.slopcommunistcooperative.meowmos.ui.theme.MeowmosTheme
import java.time.LocalDate

@Composable
fun HeatmapStat(day: DailyUsageStat) {
    val extended = MeowmosTheme.colors
    val color = when (day.count) {
        0 -> extended.heatmapEmpty
        1 -> extended.heatmapLevel1
        2 -> extended.heatmapLevel2
        in 3..4 -> extended.heatmapLevel3
        else -> extended.heatmapLevel4
    }
    val cellShape = MaterialTheme.shapes.extraSmall
    var modifier = Modifier
        .fillMaxSize()
        .aspectRatio(1F, true)
        .clip(cellShape)
        .background(color = color)
    if (day.date == LocalDate.now()) {
        modifier = modifier.border(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.onSurface,
            shape = cellShape,
        )
    }

    Box(modifier = modifier)
}
