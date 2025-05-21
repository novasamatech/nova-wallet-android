package io.novafoundation.nova.feature_account_migration.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.usecase.GetSelectedAccountMnemonicUseCase
import io.novafoundation.nova.feature_account_api.domain.usecase.GetSelectedMetaAccountUseCase
import io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.AccountMigrationDeepLinkHandler
import io.novafoundation.nova.feature_account_migration.utils.AccountMigrationMixinProvider
import io.novafoundation.nova.feature_account_migration.utils.common.KeyExchangeUtils

@Module
class AccountMigrationFeatureModule {

    @Provides
    @FeatureScope
    fun provideKeyExchangeUtils(): KeyExchangeUtils {
        return KeyExchangeUtils()
    }

    @Provides
    @FeatureScope
    fun provideExchangeSecretsMixinProvider(
        keyExchangeUtils: KeyExchangeUtils,
        accountUseCase: GetSelectedMetaAccountUseCase,
        mnemonicUseCase: GetSelectedAccountMnemonicUseCase
    ): AccountMigrationMixinProvider {
        return AccountMigrationMixinProvider(accountUseCase, mnemonicUseCase, keyExchangeUtils)
    }

    @Provides
    @FeatureScope
    fun provideSettingsInteractor(
        exchangeSeSecretsMixinProvider: AccountMigrationMixinProvider
    ) = io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.AccountMigrationDeepLinkHandler(exchangeSeSecretsMixinProvider)
}
