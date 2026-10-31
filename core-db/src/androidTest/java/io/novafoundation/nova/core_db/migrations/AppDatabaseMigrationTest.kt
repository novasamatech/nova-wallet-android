package io.novafoundation.nova.core_db.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import io.novafoundation.nova.core_db.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AppDatabaseMigrationTest(private val from: Int, private val to: Int) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "schema {0} → {1}")
        fun schemaVersions(): List<Array<Int>> {
            val assets = InstrumentationRegistry.getInstrumentation().context.assets
            val schemaDirectory = requireNotNull(AppDatabase::class.java.canonicalName)
            val versions = assets.list(schemaDirectory).orEmpty()
                .filter { it.endsWith(".json") }
                .map { it.removeSuffix(".json").toInt() }
                .sorted()
            check(versions.size >= 2) { "Migration tests require at least two exported Room schemas" }

            return versions.flatMapIndexed { index, from ->
                versions.drop(index + 1).map { to -> arrayOf(from, to) }
            }
        }
    }

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migratesBetweenExportedSchemas() {
        val databaseName = "migration-$from-$to"
        migrationHelper.createDatabase(databaseName, from).close()
        migrationHelper.runMigrationsAndValidate(databaseName, to, true, *AppDatabase.migrations)
    }
}
