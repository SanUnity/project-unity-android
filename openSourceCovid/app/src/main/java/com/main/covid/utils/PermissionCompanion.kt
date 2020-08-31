package com.main.covid.utils

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.main.covid.AppConfig
import com.main.covid.db.SharedPrefsRepository
import com.main.covid.network.DataUploader
import org.koin.java.KoinJavaComponent
import java.util.ArrayList
import java.util.HashMap


/**
 * Created by Rubén Izquierdo, Global Incubator
 */
object PermissionCompanion {

    private val sharedPrefsRepository by KoinJavaComponent.inject(SharedPrefsRepository::class.java)

    fun isBTServiceActivated(): Boolean = sharedPrefsRepository.getBTOn()

    fun isTimeToCreateNotification(): Boolean {
        val isTime = System.currentTimeMillis() - sharedPrefsRepository.getLastUploadTimestamp()
        return isTime  > AppConfig.periodUploadGPS
    }

    fun initializeServices() {
        sharedPrefsRepository.putBTOn(true)
        sharedPrefsRepository.putGPSOn(true)
    }

    fun logout() {
        sharedPrefsRepository.clear()
    }

    fun restartNotificationTime() {
        sharedPrefsRepository.putLastUploadTimestamp(0)
    }

    fun getApiToken() = sharedPrefsRepository.getApiToken()

    fun getContacts(context: Context): HashMap<String, String> {
        val resolver: ContentResolver = context.contentResolver
        val phones: MutableMap<Long, MutableList<String>> =
            HashMap()
        val contactsMap =
            HashMap<String, String>()
        var getContactsCursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )
        if (getContactsCursor != null) {
            while (getContactsCursor.moveToNext()) {
                val contactId = getContactsCursor.getLong(0)
                val phone = getContactsCursor.getString(1)
                var list: MutableList<String>
                if (phones.containsKey(contactId)) {
                    list = phones[contactId]!!
                } else {
                    list = ArrayList()
                    phones[contactId] = list
                }
                list.add(phone)
            }
            getContactsCursor.close()
        }
        getContactsCursor = resolver.query(
            ContactsContract.Contacts.CONTENT_URI, arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null, null, null
        )
        while (getContactsCursor != null &&
            getContactsCursor.moveToNext()
        ) {
            val contactId = getContactsCursor.getLong(0)
            val name = getContactsCursor.getString(1)
            val contactPhones: List<String>? = phones[contactId]
            if (contactPhones != null) {
                for (phone in contactPhones) {
                    val trimmedPhone = phone.replace(" ", "")
                    contactsMap[trimmedPhone] = name
                }
            }
        }
        return contactsMap
    }

    fun resetShareSuccess() {
        sharedPrefsRepository.putSharedResult(null)
    }

    fun isShareSuccessful(): Boolean{
        return sharedPrefsRepository.getSharedResult() != null
    }
}