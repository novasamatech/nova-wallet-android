package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val ChangeDAppAuthorization_10_11 = object : Migration(10, 11) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE dapp_authorizations")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `dapp_authorizations` (
                `baseUrl` TEXT NOT NULL,
                `metaId` INTEGER NOT NULL,
                `dAppTitle` TEXT, `authorized` INTEGER,
                PRIMARY KEY(`baseUrl`, `metaId`)
            )
            """.trimIndent()
        )
    }
}
