package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddTypeExtrasToMetaAccount_67_68 = object : Migration(67, 68) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meta_accounts ADD COLUMN typeExtras TEXT")
    }
}
