package io.github.slopcommunistcooperative.meowmos.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object WidgetUpdateScheduler {
    private const val WIDGET_UPDATE_WORK = "meowmos_widget_update_work"

    fun scheduleWidgetUpdates(context: Context) {
        val updateRequest = PeriodicWorkRequestBuilder<MeowmosGlanceWidgetReceiver.WidgetUpdateWorker>(
            30, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WIDGET_UPDATE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )
    }

    fun cancelWidgetUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WIDGET_UPDATE_WORK)
    }

    fun cancelWidgetUpdatesIfNoWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val manager = GlanceAppWidgetManager(context)
            val hasMemosWidget = manager.getGlanceIds(MeowmosGlanceWidget::class.java).isNotEmpty()
            val hasMemoryWidget = manager.getGlanceIds(MemoryGlanceWidget::class.java).isNotEmpty()

            if (!hasMemosWidget && !hasMemoryWidget) {
                cancelWidgetUpdates(context)
            }
        }
    }

    suspend fun updateAllWidgets(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val memosWidgetIds = manager.getGlanceIds(MeowmosGlanceWidget::class.java)
        val memoryWidgetIds = manager.getGlanceIds(MemoryGlanceWidget::class.java)

        memosWidgetIds.forEach { glanceId ->
            // update() alone only redraws a widget whose session is still running; its memo list
            // reloads when refreshKey changes, so bump it like the widget's refresh button does
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[MeowmosWidgetKeys.refreshKey] = (prefs[MeowmosWidgetKeys.refreshKey] ?: 0L) + 1
            }
            MeowmosGlanceWidget().update(context, glanceId)
        }
        memoryWidgetIds.forEach { glanceId ->
            MemoryGlanceWidget().update(context, glanceId)
        }
    }
}
