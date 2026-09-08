package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddAssetEnabledOverride_74_75 = object : Migration(74, 75) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chain_assets ADD COLUMN `enabledOverriddenByUser` INTEGER NOT NULL DEFAULT 0")

        // Assets have always arrived enabled, so a disabled one can only be a deliberate choice.
        // Marking those keeps automatic enabling by balance from undoing what the user hid.
        db.execSQL("UPDATE chain_assets SET `enabledOverriddenByUser` = 1 WHERE `enabled` = 0")
    }
}
