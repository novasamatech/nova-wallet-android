package io.novafoundation.nova.core_db.migrations

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.novafoundation.nova.core_db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith


private const val DB_TEST_NAME = "test-db"

@RunWith(AndroidJUnit4::class)
abstract class BaseMigrationTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,

        FrameworkSQLiteOpenHelperFactory()
    )

    protected fun runMigrationTest(
        from: Int,
        to: Int,
        vararg migrations: Migration,
        preMigrateBlock: (SupportSQLiteDatabase) -> Unit = {},
        postMigrateBlock: suspend (AppDatabase) -> Unit = {}
    ) {
        runBlocking {
            val db = migrationHelper.createDatabase(DB_TEST_NAME, from)
            preMigrateBlock(db)

            val validateDroppedTables = true
            migrationHelper.runMigrationsAndValidate(DB_TEST_NAME, to, validateDroppedTables, *migrations)

            postMigrateBlock(getMigratedRoomDatabase(*migrations))
        }
    }

    protected fun validateSchema(
        from: Int,
        to: Int,
        vararg migrations: Migration,
    ) = runMigrationTest(from, to, *migrations)

    private fun getMigratedRoomDatabase(vararg migrations: Migration): AppDatabase {
        val database: AppDatabase = Room.databaseBuilder(ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java, DB_TEST_NAME)
            .addMigrations(*migrations)
            .build()

        migrationHelper.closeWhenFinished(database)

        return database
    }
}
