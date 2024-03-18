package io.novafoundation.nova.feature_deep_linking.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.AssetDetailsDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.BuyCallbackDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.DAppDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ImportMnemonicDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ReferendumDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.StakingDashboardDeepLinkHandler
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.RootDeepLinkHandler
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module()
class DeepLinkingFeatureModule {

    @Provides
    fun provideStakingDashboardDeepLinkHandler(
        deepLinkingRouter: DeepLinkingRouter,
        automaticInteractionGate: AutomaticInteractionGate
    ) = StakingDashboardDeepLinkHandler(deepLinkingRouter, automaticInteractionGate)

    @Provides
    fun provideImportMnemonicDeepLinkHandler(
        deepLinkingRouter: DeepLinkingRouter,
        encryptionDefaults: EncryptionDefaults,
        accountRepository: AccountRepository,
        automaticInteractionGate: AutomaticInteractionGate
    ) = ImportMnemonicDeepLinkHandler(
        deepLinkingRouter,
        encryptionDefaults,
        accountRepository,
        automaticInteractionGate
    )

    @Provides
    fun provideDappDeepLinkHandler(
        dAppMetadataRepository: DAppMetadataRepository,
        deepLinkingRouter: DeepLinkingRouter,
        automaticInteractionGate: AutomaticInteractionGate
    ) = DAppDeepLinkHandler(
        dAppMetadataRepository,
        deepLinkingRouter,
        automaticInteractionGate
    )

    @Provides
    fun provideReferendumDeepLinkHandler(
        deepLinkingRouter: DeepLinkingRouter,
        chainRegistry: ChainRegistry,
        mutableGovernanceState: MutableGovernanceState,
        automaticInteractionGate: AutomaticInteractionGate,
        resourceManager: ResourceManager
    ) = ReferendumDeepLinkHandler(
        deepLinkingRouter,
        chainRegistry,
        mutableGovernanceState,
        automaticInteractionGate,
        resourceManager
    )

    @Provides
    fun provideBuyCallbackDeepLinkHandler(
        resourceManager: ResourceManager
    ) = BuyCallbackDeepLinkHandler(resourceManager)

    @Provides
    fun provideAssetDetailsDeepLinkHandler(
        deepLinkingRouter: DeepLinkingRouter,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        automaticInteractionGate: AutomaticInteractionGate,
        resourceManager: ResourceManager
    ) = AssetDetailsDeepLinkHandler(
        router = deepLinkingRouter,
        accountRepository = accountRepository,
        chainRegistry = chainRegistry,
        automaticInteractionGate = automaticInteractionGate,
        resourceManager = resourceManager
    )

    @Provides
    fun provideRootDeepLinkHandler(
        stakingDashboardDeepLinkHandler: StakingDashboardDeepLinkHandler,
        importMnemonicDeepLinkHandler: ImportMnemonicDeepLinkHandler,
        dappDeepLinkHandler: DAppDeepLinkHandler,
        referendumDeepLinkHandler: ReferendumDeepLinkHandler,
        buyCallbackDeepLinkHandler: BuyCallbackDeepLinkHandler,
        assetDetailsDeepLinkHandler: AssetDetailsDeepLinkHandler
    ): RootDeepLinkHandler {
        val deepLinkHandlers = listOf(
            stakingDashboardDeepLinkHandler,
            importMnemonicDeepLinkHandler,
            dappDeepLinkHandler,
            referendumDeepLinkHandler,
            buyCallbackDeepLinkHandler,
            assetDetailsDeepLinkHandler
        )

        return RootDeepLinkHandler(deepLinkHandlers)
    }
}
