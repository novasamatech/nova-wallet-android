package io.novafoundation.nova.feature_account_impl.di.modules.deeplinks

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_account_api.di.deeplinks.AccountDeepLinks
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.importing.deeplink.ImportMnemonicDeepLinkHandler

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideImportMnemonicDeepLinkHandler(
        router: AccountRouter,
        encryptionDefaults: EncryptionDefaults,
        accountRepository: AccountRepository,
        automaticInteractionGate: AutomaticInteractionGate
    ) = ImportMnemonicDeepLinkHandler(
        router,
        encryptionDefaults,
        accountRepository,
        automaticInteractionGate
    )

    @Provides
    @FeatureScope
    fun provideDeepLinks(importMnemonic: ImportMnemonicDeepLinkHandler): AccountDeepLinks {
        return AccountDeepLinks(listOf(importMnemonic))
    }
}
