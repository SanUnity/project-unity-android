package com.main.covid.db.streetpass

import android.content.Context
import com.main.covid.db.AppDatabase

class StreetPassRecordStorage(val context: Context) {

    private val recordDao = AppDatabase.getDatabase(context).recordDao()

    suspend fun saveRecord(record: StreetPassRecord) {
        recordDao.insert(record)
    }

    fun nukeDb() {
        recordDao.nukeDb()
    }

    fun getAllRecords(): List<StreetPassRecord> {
        return recordDao.getCurrentRecords()
    }

    suspend fun purgeOldRecords(before: Long) {
        recordDao.purgeOldRecords(before)
    }
}
