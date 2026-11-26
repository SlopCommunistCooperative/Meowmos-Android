package io.github.slopcommunistcooperative.meowmos.ui.component

import android.text.TextUtils
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.model.Memo
import io.github.slopcommunistcooperative.meowmos.ui.page.memos.MemoComments

@Composable
fun ExploreMemoCard(
    memo: Memo
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = meowmosCardColors(),
        border = meowmosCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (memo.creator != null && !TextUtils.isEmpty(memo.creator.name)) {
                    WayfindingBadge(
                        text = "@${memo.creator.name}",
                        tone = WayfindingTone.Beacon,
                    )
                }
                Text(
                    DateUtils.getRelativeTimeSpanString(memo.date.toEpochMilli(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MemoContent(memo, previewMode = false)
            memo.accountKey?.let { accountKey ->
                var expanded by rememberSaveable(accountKey, memo.remoteId) { mutableStateOf(false) }
                MemoComments(
                    memo, accountKey, memo.space,
                    onViewAll = if (expanded) null else ({ expanded = true }),
                )
                if (expanded) {
                    TextButton(onClick = { expanded = false }, modifier = Modifier.padding(horizontal = 15.dp)) {
                        Text(stringResource(R.string.memo_comment_hide))
                    }
                }
            }
        }
    }
}
