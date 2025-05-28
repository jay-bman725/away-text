package app.awaytext.mobile

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

/**
 * Utility class for managing contacts
 */
object ContactUtils {
    private const val TAG = "ContactUtils"
    
    /**
     * Data class representing a contact
     */
    data class Contact(
        val id: String,
        val name: String,
        val phoneNumbers: List<String>
    ) {
        fun getDisplayText(): String {
            return if (phoneNumbers.size == 1) {
                "$name (${phoneNumbers.first()})"
            } else {
                "$name (${phoneNumbers.size} numbers)"
            }
        }
        
        fun getPrimaryPhoneNumber(): String? {
            return phoneNumbers.firstOrNull()
        }
    }
    
    /**
     * Get all contacts with phone numbers
     */
    fun getAllContactsWithPhoneNumbers(context: Context): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val contactMap = mutableMapOf<String, MutableContact>()
        
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use {
                val contactIdIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                while (it.moveToNext()) {
                    val contactId = it.getString(contactIdIndex)
                    val name = it.getString(nameIndex) ?: "Unknown"
                    val phoneNumber = it.getString(phoneIndex)?.replace("[^+\\d]".toRegex(), "") // Clean phone number
                    
                    if (!phoneNumber.isNullOrBlank()) {
                        val existingContact = contactMap[contactId]
                        if (existingContact != null) {
                            existingContact.phoneNumbers.add(phoneNumber)
                        } else {
                            contactMap[contactId] = MutableContact(
                                id = contactId,
                                name = name,
                                phoneNumbers = mutableListOf(phoneNumber)
                            )
                        }
                    }
                }
            }
            
            // Convert to immutable contacts
            contacts.addAll(contactMap.values.map { mutableContact ->
                Contact(
                    id = mutableContact.id,
                    name = mutableContact.name,
                    phoneNumbers = mutableContact.phoneNumbers.distinct() // Remove duplicates
                )
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting contacts: ${e.message}")
        }
        
        return contacts.sortedBy { it.name }
    }
    
    /**
     * Search contacts by name
     */
    fun searchContacts(context: Context, query: String): List<Contact> {
        if (query.isBlank()) {
            return getAllContactsWithPhoneNumbers(context)
        }
        
        return getAllContactsWithPhoneNumbers(context).filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
            contact.phoneNumbers.any { it.contains(query) }
        }
    }
    
    /**
     * Get contact by phone number
     */
    fun getContactByPhoneNumber(context: Context, phoneNumber: String): Contact? {
        val cleanedNumber = phoneNumber.replace("[^+\\d]".toRegex(), "")
        
        return getAllContactsWithPhoneNumbers(context).find { contact ->
            contact.phoneNumbers.any { contactNumber ->
                val cleanedContactNumber = contactNumber.replace("[^+\\d]".toRegex(), "")
                cleanedContactNumber == cleanedNumber ||
                cleanedContactNumber.endsWith(cleanedNumber.takeLast(10)) ||
                cleanedNumber.endsWith(cleanedContactNumber.takeLast(10))
            }
        }
    }
    
    /**
     * Helper class for building contacts during query
     */
    private data class MutableContact(
        val id: String,
        val name: String,
        val phoneNumbers: MutableList<String>
    )
}
