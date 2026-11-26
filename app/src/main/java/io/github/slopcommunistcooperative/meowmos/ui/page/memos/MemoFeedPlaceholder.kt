package io.github.slopcommunistcooperative.meowmos.ui.page.memos

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.ui.component.meowmosCardBorder
import io.github.slopcommunistcooperative.meowmos.ui.component.meowmosCardColors

@Composable
internal fun MemoFeedPlaceholder() {
    val loading = stringResource(R.string.loading)
    val pulse by rememberInfiniteTransition(label = "Memo placeholders").animateFloat(
        initialValue = 1f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Placeholder contrast",
    )
    Column(Modifier.semantics {
        contentDescription = loading
        liveRegion = LiveRegionMode.Polite
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    }) {
        repeat(3) { index ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.medium,
                colors = meowmosCardColors(),
                border = meowmosCardBorder(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).graphicsLayer { alpha = pulse },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PlaceholderLine(0.24f, 12)
                    PlaceholderLine(if (index == 1) 0.76f else 0.94f, 16)
                    PlaceholderLine(if (index == 1) 0.48f else 0.65f, 16)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderLine(width: Float, height: Int) {
    Box(
        Modifier.fillMaxWidth(width).height(height.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall),
    )
}
