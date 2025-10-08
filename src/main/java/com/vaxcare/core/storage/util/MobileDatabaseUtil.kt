/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.core.storage.util

import android.content.Context
import android.os.Build
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteStatement
import timber.log.Timber
import java.io.File

/**
 * Represent states of the database
 *
 */
sealed class MobileDatabaseState(val name: String) {
    object Encrypted : MobileDatabaseState("Encrypted")

    object OldEncrypted : MobileDatabaseState("OldEncrypted")

    object Unknown : MobileDatabaseState("Unknown")

    object UnEncrypted : MobileDatabaseState("UnEncrypted")

    object Corrupt : MobileDatabaseState("Corrupt")

    object NotExist : MobileDatabaseState("NotExist")
}

/**
 * Helper class to encrypt databases that are already available
 */
object MobileDatabaseUtil {
    private fun getDatabaseState(
        dbPath: File,
        newPass: String,
        oldPass: String
    ): MobileDatabaseState =
        if (dbPath.exists()) {
            when {
                openDatabase(dbPath, newPass) -> MobileDatabaseState.Encrypted
                openDatabase(dbPath, oldPass) -> MobileDatabaseState.OldEncrypted
                openDatabase(dbPath, "") -> MobileDatabaseState.UnEncrypted
                openDatabase(dbPath, Build.UNKNOWN) -> MobileDatabaseState.Unknown
                else -> MobileDatabaseState.Corrupt
            }
        } else {
            Timber.d("Database do not exist ${dbPath.absolutePath}")

            MobileDatabaseState.NotExist
        }

    private fun openDatabase(dbPath: File, oldPass: String): Boolean {
        var db: SQLiteDatabase? = null

        return try {
            db = SQLiteDatabase.openDatabase(
                dbPath.absolutePath,
                oldPass,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            true
        } catch (e: Exception) {
            Timber.d("Not able to open the database with pass")

            false
        } finally {
            db?.close()
        }
    }

    private fun encrypt(
        context: Context,
        originalFile: File,
        passphrase: ByteArray,
        oldPass: String
    ) {
        SQLiteDatabase.loadLibs(context)

        if (originalFile.exists()) {
            val newFile = File.createTempFile(
                "sqlcipherutils",
                "tmp",
                context.cacheDir
            )
            var db = SQLiteDatabase.openDatabase(
                originalFile.absolutePath,
                oldPass,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            val version = db.version
            db.close()
            db = SQLiteDatabase.openDatabase(
                newFile.absolutePath, passphrase,
                null, SQLiteDatabase.OPEN_READWRITE, null, null
            )
            val query = "ATTACH DATABASE ? AS plaintext KEY '$oldPass'"

            val st: SQLiteStatement =
                db.compileStatement(query)
            st.bindString(1, originalFile.absolutePath)
            st.execute()

            db.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
            db.rawExecSQL("DETACH DATABASE plaintext")
            db.version = version
            st.close()
            db.close()

            originalFile.delete()
            newFile.renameTo(originalFile)
            Timber.d("Database encrypted successfully")
        } else {
            Timber.d("Database: Error finding the database file")
        }
    }

    private fun removeFile(originalFile: File) =
        try {
            val result = originalFile.delete()
            Timber.d("Database file was corrupt and trying to delete was: $result")
        } catch (e: Exception) {
            Timber.e(e, "Database file was corrupt and trying to delete was false")
        }

    fun encryptDatabase(
        context: Context,
        passphrase: ByteArray,
        oldPass: String,
        databaseName: String
    ) {
        SQLiteDatabase.loadLibs(context)

        val originalFile = context.getDatabasePath(databaseName)

        val state = getDatabaseState(originalFile, String(passphrase), oldPass)
        Timber.d("Database: The state is: ${state.name}")

        when (state) {
            MobileDatabaseState.UnEncrypted -> encrypt(context, originalFile, passphrase, "")
            MobileDatabaseState.OldEncrypted -> encrypt(context, originalFile, passphrase, oldPass)
            MobileDatabaseState.Unknown -> encrypt(context, originalFile, passphrase, Build.UNKNOWN)
            MobileDatabaseState.Corrupt -> removeFile(originalFile)
            MobileDatabaseState.Encrypted -> Timber.d("Database encrypted opening")
            MobileDatabaseState.NotExist -> Timber.d("Database file was not found")
        }
    }
}
