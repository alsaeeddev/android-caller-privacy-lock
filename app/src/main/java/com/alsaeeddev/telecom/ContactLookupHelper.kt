package com.alsaeeddev.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.alsaeeddev.security.PrivacyLogger

object ContactLookupHelper {

    data class ContactDetails(
        val name: String?,
        val formattedNumber: String?,
        val photoUri: String? = null
    )

    /**
     * Resolves the contact's name and details from Android's Contacts Provider.
     * Prioritizes PhoneLookup URI filter with normalization.
     */
    fun resolveContact(context: Context, rawPhoneNumber: String?): ContactDetails {
        if (rawPhoneNumber.isNullOrBlank()) {
            return ContactDetails(name = null, formattedNumber = null)
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            PrivacyLogger.w("READ_CONTACTS permission not granted. Cannot query contact name for incoming call.")
            return ContactDetails(name = null, formattedNumber = rawPhoneNumber)
        }

        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(rawPhoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.NUMBER,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor: Cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER)
                    val photoIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)

                    val resolvedName = if (nameIndex != -1) cursor.getString(nameIndex) else null
                    val resolvedNumber = if (numberIndex != -1) cursor.getString(numberIndex) else null
                    val resolvedPhoto = if (photoIndex != -1) cursor.getString(photoIndex) else null

                    if (!resolvedName.isNullOrBlank()) {
                        PrivacyLogger.i("Contact successfully resolved from ContactsContract: '$resolvedName' for $rawPhoneNumber")
                        return ContactDetails(
                            name = resolvedName.trim(),
                            formattedNumber = resolvedNumber?.ifBlank { rawPhoneNumber } ?: rawPhoneNumber,
                            photoUri = resolvedPhoto
                        )
                    }
                }
            }
        } catch (e: Exception) {
            PrivacyLogger.e("Exception querying ContactsContract: ${e.message}", e)
        }

        return ContactDetails(name = null, formattedNumber = rawPhoneNumber)
    }
}
