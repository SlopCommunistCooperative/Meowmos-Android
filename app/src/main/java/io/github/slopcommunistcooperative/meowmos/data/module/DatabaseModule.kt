package io.github.slopcommunistcooperative.meowmos.data.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.slopcommunistcooperative.meowmos.data.local.FileStorage
import io.github.slopcommunistcooperative.meowmos.data.local.MeowmosDatabase
import io.github.slopcommunistcooperative.meowmos.data.local.dao.MemoDao
import io.github.slopcommunistcooperative.meowmos.data.repository.LocalDatabaseRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): MeowmosDatabase {
        return MeowmosDatabase.getDatabase(context)
    }
    
    @Singleton
    @Provides
    fun provideMemoDao(database: MeowmosDatabase) = database.memoDao()

    @Singleton
    @Provides
    fun provideLocalDatabaseRepository(
        memoDao: MemoDao,
        fileStorage: FileStorage
    ) = LocalDatabaseRepository(memoDao, fileStorage)
} 
