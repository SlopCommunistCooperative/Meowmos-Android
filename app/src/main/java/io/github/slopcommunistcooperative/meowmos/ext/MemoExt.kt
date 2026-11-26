package io.github.slopcommunistcooperative.meowmos.ext

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility

val MemoVisibility.icon: ImageVector
    @Composable
    get() = ImageVector.vectorResource(
        when (this) {
            MemoVisibility.PRIVATE -> R.drawable.symbol_lock
            MemoVisibility.PROTECTED -> R.drawable.symbol_house
            MemoVisibility.PUBLIC -> R.drawable.symbol_public
            MemoVisibility.SPACE -> R.drawable.symbol_group
        },
    )

val MemoVisibility.titleResource: Int get() = when (this) {
    MemoVisibility.PRIVATE -> R.string.memo_visibility_private
    MemoVisibility.PROTECTED -> R.string.memo_visibility_protected
    MemoVisibility.PUBLIC -> R.string.memo_visibility_public
    MemoVisibility.SPACE -> R.string.memo_visibility_space
}
