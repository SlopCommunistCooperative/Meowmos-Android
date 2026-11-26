package io.github.slopcommunistcooperative.meowmos.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.slopcommunistcooperative.meowmos.ui.theme.MeowmosTheme

enum class WayfindingTone {
    /** High-visibility signal yellow/ink pill reserved for pinned status & posting destination. */
    Signal,
    /** Inverted architectural charcoal/ivory pill for authors & key status markers. */
    Beacon,
    /** Quiet tonal chip for secondary metadata. */
    Muted,
    /** Error/offline alert badge. */
    Alert,
}

/**
 * Compact architectural badge borrowed from airport terminal wayfinding signage.
 * Gives immediate scannability for destination space, pinned notes, author handles, and sync state.
 */
@Composable
fun WayfindingBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: WayfindingTone = WayfindingTone.Signal,
) {
    val (containerColor, contentColor) = when (tone) {
        WayfindingTone.Signal -> MeowmosTheme.colors.wayfindingContainer to MeowmosTheme.colors.onWayfindingContainer
        WayfindingTone.Beacon -> MeowmosTheme.colors.beaconContainer to MeowmosTheme.colors.onBeaconContainer
        WayfindingTone.Muted -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
        WayfindingTone.Alert -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun WayfindingSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        trailing?.invoke()
    }
}

@Composable
fun MeowmosEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
                    .size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun meowmosCardColors(
    containerColor: Color = MeowmosTheme.colors.cardSurface,
): CardColors = CardDefaults.cardColors(
    containerColor = containerColor,
    contentColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
fun meowmosCardBorder(pinned: Boolean = false): BorderStroke = BorderStroke(
    width = if (pinned) 1.5.dp else 1.dp,
    color = if (pinned) {
        MeowmosTheme.colors.pinnedBorder
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    },
)
