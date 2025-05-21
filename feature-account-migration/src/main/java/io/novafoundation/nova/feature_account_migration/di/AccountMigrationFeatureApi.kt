package io.novafoundation.nova.feature_account_migration.di

import io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.AccountMigrationDeepLinkHandler
import io.novafoundation.nova.feature_account_migration.utils.AccountMigrationMixinProvider

interface AccountMigrationFeatureApi {

    val accountMigrationDeepLinks: io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.AccountMigrationDeepLinkHandler

    val accountMigrationMixinProvider: AccountMigrationMixinProvider
}
