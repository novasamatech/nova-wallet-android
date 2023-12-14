package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.converters.MetaAccountTypeConverters
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal

val AddMetaAccountType_14_15 = object : Migration(14, 15) {

    override fun migrate(database: SupportSQLiteDatabase) {
        val converters = MetaAccountTypeConverters()

        // all accounts that exist till now are added via secrets
        val defaultType = MetaAccountLocal.Type.SECRETS
        val typeRepresentationInDb = converters.fromEnum(defaultType)

        database.execSQL("ALTER TABLE meta_accounts ADD COLUMN type TEXT NOT NULL DEFAULT '$typeRepresentationInDb'")
    }
}
