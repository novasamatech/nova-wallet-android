package jp.co.soramitsu.feature_account_impl.di

import dagger.Module
import dagger.Provides
import jp.co.soramitsu.common.data.secrets.v2.SecretStoreV2
import jp.co.soramitsu.common.di.scope.FeatureScope
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountRepository
import jp.co.soramitsu.feature_account_impl.domain.account.export.json.ExportJsonInteractor
import jp.co.soramitsu.feature_account_impl.domain.account.export.mnemonic.ExportMnemonicInteractor
import jp.co.soramitsu.feature_account_impl.domain.account.export.seed.ExportSeedInteractor
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry

@Module
class ExportModule {

    @Provides
    @FeatureScope
    fun provideExportJsonInteractor(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
    ) = ExportJsonInteractor(
        accountRepository,
        chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideExportMnemonicInteractor(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        secretStoreV2: SecretStoreV2,
    ) = ExportMnemonicInteractor(
        accountRepository,
        secretStoreV2,
        chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideExportSeedInteractor(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        secretStoreV2: SecretStoreV2,
    ) = ExportSeedInteractor(
        accountRepository,
        secretStoreV2,
        chainRegistry
    )
}
