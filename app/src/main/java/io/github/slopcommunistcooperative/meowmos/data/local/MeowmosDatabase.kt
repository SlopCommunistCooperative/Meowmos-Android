package io.github.slopcommunistcooperative.meowmos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.slopcommunistcooperative.meowmos.data.local.dao.MemoDao
import io.github.slopcommunistcooperative.meowmos.data.local.entity.MemoEntity
import io.github.slopcommunistcooperative.meowmos.data.local.entity.ResourceEntity

@Database(
    entities = [MemoEntity::class, ResourceEntity::class],
    version = 3
)
@TypeConverters(Converters::class)
abstract class MeowmosDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memos ADD COLUMN location_placeholder TEXT")
                db.execSQL("ALTER TABLE memos ADD COLUMN location_latitude REAL")
                db.execSQL("ALTER TABLE memos ADD COLUMN location_longitude REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memos ADD COLUMN space TEXT")
            }
        }

        @Volatile
        private var INSTANCE: MeowmosDatabase? = null

        fun getDatabase(context: Context): MeowmosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeowmosDatabase::class.java,
                    "meowmos_database_localfirst"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
