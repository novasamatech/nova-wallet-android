package io.novafoundation.nova.feature_account_migration.di

import io.novafoundation.nova.feature_account_migration.di.deeplinks.AccountMigrationDeepLinks
import io.novafoundation.nova.feature_account_migration.utils.AccountMigrationMixinProvider

interface AccountMigrationFeatureApi {

    val accountMigrationMixinProvider: AccountMigrationMixinProvider

    val accountMigrationDeepLinks: AccountMigrationDeepLinks
}
