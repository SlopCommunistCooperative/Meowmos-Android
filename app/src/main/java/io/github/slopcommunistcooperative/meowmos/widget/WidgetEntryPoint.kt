package io.github.slopcommunistcooperative.meowmos.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.slopcommunistcooperative.meowmos.data.service.MemoService

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun memoService(): MemoService
}
