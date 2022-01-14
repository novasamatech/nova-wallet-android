package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddDAppAuthorizations_1_2 = object : Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `dapp_authorizations` (
                `baseUrl` TEXT NOT NULL,
                `authorized` INTEGER,
                PRIMARY KEY(`baseUrl`)
            )
            """.trimIndent()
        )
    }
}
