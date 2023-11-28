package io.novafoundation.nova.app.root.presentation.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.BuyCallbackDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.DAppDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.DeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ImportMnemonicDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ReferendumDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.RootDeepLinkHandler
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class DeepLinkModule {

    @Provides
    @IntoSet
    fun provideImportMnemonicDeepLinkHandler(
        accountRouter: AccountRouter,
        encryptionDefaults: EncryptionDefaults,
        accountRepository: AccountRepository,
        automaticInteractionGate: AutomaticInteractionGate
    ): DeepLinkHandler {
        return ImportMnemonicDeepLinkHandler(
            accountRouter,
            encryptionDefaults,
            accountRepository,
            automaticInteractionGate
        )
    }

    @Provides
    @IntoSet
    fun provideDappDeepLinkHandler(
        accountRepository: AccountRepository,
        dAppMetadataRepository: DAppMetadataRepository,
        dAppRouter: DAppRouter,
        automaticInteractionGate: AutomaticInteractionGate
    ): DeepLinkHandler {
        return DAppDeepLinkHandler(
            accountRepository,
            dAppMetadataRepository,
            dAppRouter,
            automaticInteractionGate
        )
    }

    @Provides
    @IntoSet
    fun provideReferendumDeepLinkHandler(
        governanceRouter: GovernanceRouter,
        chainRegistry: ChainRegistry,
        mutableGovernanceState: MutableGovernanceState,
        accountRepository: AccountRepository,
        automaticInteractionGate: AutomaticInteractionGate
    ): DeepLinkHandler {
        return ReferendumDeepLinkHandler(
            governanceRouter,
            chainRegistry,
            mutableGovernanceState,
            accountRepository,
            automaticInteractionGate
        )
    }

    @Provides
    @IntoSet
    fun provideBuyCallbackDeepLinkHandler(
        interactor: RootInteractor,
        resourceManager: ResourceManager
    ): DeepLinkHandler {
        return BuyCallbackDeepLinkHandler(interactor, resourceManager)
    }

    @Provides
    fun provideRootDeepLinkHandler(
        deepLinkHandlers: Set<@JvmSuppressWildcards DeepLinkHandler>
    ): RootDeepLinkHandler {
        return RootDeepLinkHandler(deepLinkHandlers.toList())
    }
}