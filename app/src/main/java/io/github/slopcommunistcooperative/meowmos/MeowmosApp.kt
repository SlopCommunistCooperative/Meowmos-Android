package io.github.slopcommunistcooperative.meowmos

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import io.github.slopcommunistcooperative.meowmos.ui.security.AppLockSession
import io.github.slopcommunistcooperative.meowmos.util.AppLanguage
import timber.log.Timber

@HiltAndroidApp
class MeowmosApp: Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var CONTEXT: Context
    }

    override fun attachBaseContext(base: Context?) {
        CONTEXT = this
        if (base != null) {
            AppLanguage.initialize(base)
            super.attachBaseContext(AppLanguage.wrap(base))
        } else {
            super.attachBaseContext(base)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Timber.plant(Timber.DebugTree())
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                AppLockSession.markAppForegrounded()
            }

            override fun onStop(owner: LifecycleOwner) {
                AppLockSession.markAppBackgrounded()
            }
        })
    }
}
