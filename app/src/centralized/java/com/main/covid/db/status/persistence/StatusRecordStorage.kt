package com.main.covid.db.status.persistence

import android.content.Context
import com.main.covid.db.AppDatabase

class StatusRecordStorage(val context: Context) {

    private val statusDao = AppDatabase.getDatabase(context).statusDao()

    suspend fun saveRecord(record: StatusRecord) {
        statusDao.insert(record)
    }

    fun nukeDb() {
        statusDao.nukeDb()
    }

    fun getAllRecords(): List<StatusRecord> {
        return statusDao.getCurrentRecords()
    }

    suspend fun purgeOldRecords(before: Long) {
        statusDao.purgeOldRecords(before)
    }
}
