package io.novafoundation.nova.app.root.presentation.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.app.root.presentation.deepLinks.BuyCallbackDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.DeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.ReferendumDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.RootDeepLinkHandler
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.GovernanceStateUpdater
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class DeepLinkModule {

    @Provides
    @IntoSet
    fun provideReferendumDeepLinkHandler(
        governanceRouter: GovernanceRouter,
        chainRegistry: ChainRegistry,
        governanceStateUpdater: GovernanceStateUpdater,
        accountRepository: AccountRepository
    ): DeepLinkHandler {
        return ReferendumDeepLinkHandler(
            governanceRouter,
            chainRegistry,
            governanceStateUpdater,
            accountRepository
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
