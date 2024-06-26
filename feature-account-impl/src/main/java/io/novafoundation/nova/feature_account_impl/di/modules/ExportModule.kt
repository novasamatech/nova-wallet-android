package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.ExportJsonInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.mnemonic.ExportMnemonicInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.seed.ExportPrivateKeyInteractor
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

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
    ) = ExportPrivateKeyInteractor(
        accountRepository,
        secretStoreV2,
        chainRegistry
    )
}
