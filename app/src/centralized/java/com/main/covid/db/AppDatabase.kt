package com.main.covid.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.main.covid.db.status.persistence.StatusRecord
import com.main.covid.db.status.persistence.StatusRecordDao
import com.main.covid.db.streetpass.StreetPassRecord
import com.main.covid.db.streetpass.StreetPassRecordDao


@Database(
    entities = [StreetPassRecord::class, StatusRecord::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordDao(): StreetPassRecordDao
    abstract fun statusDao(): StatusRecordDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance =
                INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "record_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
