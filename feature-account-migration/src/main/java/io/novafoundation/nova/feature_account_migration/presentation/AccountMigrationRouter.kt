package io.novafoundation.nova.feature_account_migration.presentation

interface AccountMigrationRouter {

    fun openAccountMigrationPairing(scheme: String)

    fun finishMigrationFlow()
}
