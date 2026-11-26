package io.github.slopcommunistcooperative.meowmos.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import kotlinx.coroutines.launch
import io.github.slopcommunistcooperative.meowmos.R
import io.github.slopcommunistcooperative.meowmos.data.local.entity.ResourceEntity
import io.github.slopcommunistcooperative.meowmos.ext.string
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState
import io.github.slopcommunistcooperative.meowmos.viewmodel.MemoInputViewModel

@Composable
fun InputImage(
    resource: ResourceEntity,
    inputViewModel: MemoInputViewModel
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userStateViewModel = LocalUserState.current
    val imageLoader = remember(context, userStateViewModel.okHttpClient) {
        ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { userStateViewModel.okHttpClient }
                    )
                )
            }
            .build()
    }

    Box {
        AsyncImage(
            model = resource.localUri ?: resource.uri,
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .zIndex(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    menuExpanded = true
                },
            contentScale = ContentScale.Crop
        )
        DropdownMenu(expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            DropdownMenuItem(
                text = { Text(R.string.remove.string) },
                onClick = {
                    scope.launch {
                        inputViewModel.deleteResource(resource.identifier)
                        menuExpanded = false
                    }
                },
                leadingIcon = {
                    Icon(
                        ImageVector.vectorResource(R.drawable.symbol_delete),
                        contentDescription = null
                    )
                })
        }
    }
}
