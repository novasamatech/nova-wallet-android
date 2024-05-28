package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.common.utils.collectAll
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal

val AddGloballyUniqueIdToMetaAccounts_57_58 = object : Migration(57, 58) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE meta_accounts ADD COLUMN globallyUniqueId TEXT NOT NULL DEFAULT ''")

        val ids = database.getAllMetaAccountIds()

        ids.forEach { id ->
            val uuid = MetaAccountLocal.generateGloballyUniqueId()
            database.execSQL("UPDATE meta_accounts as m SET globallyUniqueId = '$uuid' WHERE m.id = $id")
        }
    }

    private fun SupportSQLiteDatabase.getAllMetaAccountIds(): List<Long> {
        val cursor = query("SELECT id FROM meta_accounts")
        val column = cursor.getColumnIndex("id")

        val ids = cursor.collectAll { cursor.getLong(column) }

        cursor.close()

        return ids
    }
}
