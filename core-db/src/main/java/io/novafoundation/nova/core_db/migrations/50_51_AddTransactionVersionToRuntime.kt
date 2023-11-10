package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddTransactionVersionToRuntime_50_51 = object : Migration(50, 51) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chain_runtimes ADD COLUMN transactionVersion INTEGER")
    }
}
