package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.BiometricTelemetryDao
import com.example.data.database.dao.UserLifestyleLogDao
import com.example.data.database.entity.BiometricTelemetryEntity
import com.example.data.database.entity.UserLifestyleLogEntity
import com.example.data.database.dao.EnvironmentalLogDao
import com.example.data.database.entity.EnvironmentalLogEntity
import com.example.data.database.dao.BloodReportDao
import com.example.data.database.entity.BloodReportEntity
import com.example.data.database.dao.MedicationDao
import com.example.data.database.entity.MedicationEntity
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase

@Database(
    entities = [BiometricTelemetryEntity::class, UserLifestyleLogEntity::class, EnvironmentalLogEntity::class, BloodReportEntity::class, MedicationEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AegisHealthDatabase : RoomDatabase() {

    abstract fun biometricTelemetryDao(): BiometricTelemetryDao
    abstract fun userLifestyleLogDao(): UserLifestyleLogDao
    abstract fun environmentalLogDao(): EnvironmentalLogDao
    abstract fun bloodReportDao(): BloodReportDao
    abstract fun medicationDao(): MedicationDao

    companion object {
        private const val DB_NAME = "aegis_health_secure_db.sqlite"

        @Volatile
        private var INSTANCE: AegisHealthDatabase? = null

        /**
         * Correctly configures and retrieves the database using SupportFactory(passphrase)
         * via SQLCipher to ensure absolute cryptographic privacy.
         */
        fun getDatabase(context: Context, passphrase: ByteArray): AegisHealthDatabase {
            return INSTANCE ?: synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    try {
                        instance = buildSecureDatabase(context, passphrase)
                        // Trigger a simple database open to verify key decryption is correct.
                        instance.openHelper.writableDatabase
                    } catch (e: Throwable) {
                        android.util.Log.e("AegisHealthDB", "Decryption/migration failure, resetting encrypted DB files", e)
                        try {
                            val dbFile = context.getDatabasePath(DB_NAME)
                            if (dbFile.exists()) {
                                dbFile.delete()
                            }
                            context.getDatabasePath("$DB_NAME-journal").delete()
                            context.getDatabasePath("$DB_NAME-shm").delete()
                            context.getDatabasePath("$DB_NAME-wal").delete()
                        } catch (delEx: Throwable) {
                            android.util.Log.e("AegisHealthDB", "Failed to delete old DB files", delEx)
                        }
                        instance = buildSecureDatabase(context, passphrase)
                    }
                    INSTANCE = instance
                }
                instance!!
            }
        }

        private fun buildSecureDatabase(context: Context, passphrase: ByteArray): AegisHealthDatabase {
            val appCtx = context.applicationContext
            
            // Ensure SQLCipher native binaries are loaded up front.
            SQLiteDatabase.loadLibs(appCtx)

            // Create SupportFactory using the high-entropy passphrase key.
            val supportFactory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                appCtx,
                AegisHealthDatabase::class.java,
                DB_NAME
            )
            .openHelperFactory(supportFactory)
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
