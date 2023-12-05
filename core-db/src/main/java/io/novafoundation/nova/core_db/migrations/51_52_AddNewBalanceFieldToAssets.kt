package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.converters.AssetConverters
import io.novafoundation.nova.core_db.model.AssetLocal

val AddBalanceModesToAssets_51_52 = object : Migration(51, 52) {

    override fun migrate(database: SupportSQLiteDatabase) {
        val assetsConverters = AssetConverters()
        val defaultTransferableMode = assetsConverters.fromTransferableMode(AssetLocal.defaultTransferableMode())
        val defaultEdCountingMode = assetsConverters.fromEdCountingMode(AssetLocal.defaultEdCountingMode())

        database.execSQL("ALTER TABLE assets ADD COLUMN transferableMode INTEGER NOT NULL DEFAULT $defaultTransferableMode")
        database.execSQL("ALTER TABLE assets ADD COLUMN edCountingMode INTEGER NOT NULL DEFAULT $defaultEdCountingMode")
    }
}
