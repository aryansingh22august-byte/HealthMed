package com.example.data.database

import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.sqlcipher.database.SupportFactory

/**
 * setup of SafeRoomOpenHelperFactory to bridge Room's OpenHelper
 * to the Zetetic SQLCipher DB decryption mechanism using a dynamically generated key.
 */
class SafeRoomOpenHelperFactory(private val passphraseBytes: ByteArray) : SupportSQLiteOpenHelper.Factory {
    
    private val delegateFactory = SupportFactory(passphraseBytes)

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return delegateFactory.create(configuration)
    }
}
