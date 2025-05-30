package io.novafoundation.nova.feature_account_migration.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter

interface AccountMigrationRouter : ReturnableRouter {

    fun openAccountMigrationPairing(scheme: String)

    fun finishMigrationFlow()

    fun openPinCodeSet()
}
