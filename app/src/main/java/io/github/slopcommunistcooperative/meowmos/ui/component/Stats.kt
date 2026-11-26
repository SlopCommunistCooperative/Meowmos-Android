package io.github.slopcommunistcooperative.meowmos.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Composable
fun Stats() {
    val memosViewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val days = remember(userStateViewModel.currentUser, LocalDate.now()) {
        userStateViewModel.currentUser?.let { currentUser ->
            ChronoUnit.DAYS.between(
                currentUser.startDate.atZone(OffsetDateTime.now().offset).toLocalDate(),
                LocalDate.now(),
            ).coerceAtLeast(0)
        } ?: 0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 8.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatColumn(
            value = memosViewModel.memos.count().toString(),
            label = R.string.memo.string,
            modifier = Modifier.weight(1f),
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight().padding(vertical = 6.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        )
        StatColumn(
            value = memosViewModel.tags.count().toString(),
            label = R.string.tag.string,
            modifier = Modifier.weight(1f),
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight().padding(vertical = 6.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        )
        StatColumn(
            value = days.toString(),
            label = R.string.day.string,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
