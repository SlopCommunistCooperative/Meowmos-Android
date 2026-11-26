package io.github.slopcommunistcooperative.meowmos.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.slopcommunistcooperative.meowmos.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SyncStatusBadge(
    syncing: Boolean,
    unsyncedCount: Int,
    onSync: () -> Unit,
    enabled: Boolean = !syncing,
) {
    val status = when {
        syncing -> stringResource(R.string.loading)
        unsyncedCount > 0 -> "${stringResource(R.string.sync_status_unsynced)}: $unsyncedCount"
        else -> null
    }
    ActionIconButton(
        label = stringResource(R.string.sync_status_sync_now),
        enabled = enabled && !syncing,
        onClick = onSync,
        modifier = if (status == null) Modifier else Modifier.semantics { stateDescription = status },
    ) {
        if (syncing) {
            LoadingIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            BadgedBox(
                badge = {
                    if (unsyncedCount > 0) {
                        Badge { Text(if (unsyncedCount > 99) "99+" else unsyncedCount.toString()) }
                    }
                },
            ) {
                Icon(ImageVector.vectorResource(R.drawable.symbol_sync), contentDescription = null)
            }
        }
    }
}
