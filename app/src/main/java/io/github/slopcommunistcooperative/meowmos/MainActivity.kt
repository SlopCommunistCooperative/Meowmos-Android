package io.github.slopcommunistcooperative.meowmos

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.slopcommunistcooperative.meowmos.ui.page.common.Navigation
import io.github.slopcommunistcooperative.meowmos.ui.security.AppLockGate
import io.github.slopcommunistcooperative.meowmos.util.AppLanguage
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalMemos
import io.github.slopcommunistcooperative.meowmos.viewmodel.LocalUserState
import io.github.slopcommunistcooperative.meowmos.viewmodel.MemosViewModel
import io.github.slopcommunistcooperative.meowmos.viewmodel.UserStateViewModel

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val userStateViewModel: UserStateViewModel by viewModels()
    private val memosViewModel: MemosViewModel by viewModels()

    var pendingIntent: Intent? by mutableStateOf(null)

    companion object {
        const val ACTION_NEW_MEMO = "io.github.slopcommunistcooperative.meowmos.action.NEW_MEMO"
        const val ACTION_EDIT_MEMO = "io.github.slopcommunistcooperative.meowmos.action.EDIT_MEMO"
        const val ACTION_VIEW_MEMO = "io.github.slopcommunistcooperative.meowmos.action.VIEW_MEMO"
        const val EXTRA_MEMO_ID = "memoId"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (savedInstanceState == null) {
            pendingIntent = intent
        }
        setContent {
            CompositionLocalProvider(
                LocalUserState provides userStateViewModel,
                LocalMemos provides memosViewModel
            ) {
                AppLockGate {
                    Navigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingIntent = intent
    }
}
