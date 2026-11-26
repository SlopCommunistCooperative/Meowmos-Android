package io.github.slopcommunistcooperative.meowmos.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import io.github.slopcommunistcooperative.meowmos.R

internal class MemoMenuAction(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val onClick: () -> Unit,
)

@Composable
internal fun MemoOverflowMenu(
    onDelete: () -> Unit,
    actions: () -> List<MemoMenuAction>,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ActionIconButton(label = stringResource(R.string.more_options), onClick = { expanded = true }) {
            Icon(ImageVector.vectorResource(R.drawable.symbol_more_vert), contentDescription = null)
        }
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            val entries = actions()
            val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MenuDefaults.GroupSpacing),
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShape(0, 2),
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = containerColor,
                ) {
                    entries.forEachIndexed { index, action ->
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                action.onClick()
                            },
                            text = { Text(stringResource(action.label)) },
                            shape = MenuDefaults.itemShape(index, entries.size).shape,
                            leadingIcon = {
                                Icon(ImageVector.vectorResource(action.icon), contentDescription = null)
                            },
                        )
                    }
                }
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShape(1, 2),
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = containerColor,
                ) {
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onDelete()
                        },
                        text = { Text(stringResource(R.string.delete)) },
                        shape = MenuDefaults.standaloneItemShape,
                        leadingIcon = {
                            Icon(ImageVector.vectorResource(R.drawable.symbol_delete), contentDescription = null)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                        ),
                    )
                }
            }
        }
    }
}
