package io.novafoundation.nova.feature_deep_linking.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.AssetDetailsDeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.DAppDeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.ImportMnemonicDeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.ReferendumDeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.StakingDashboardDeepLinkHandler
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_deep_link_building.presentation.AssetDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_deep_link_building.presentation.ReferendumDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.RootDeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.BuyCallbackDeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers.WalletConnectPairDeeplinkHandler
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
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
        referendumDetailsDeepLinkConfigurator: ReferendumDetailsDeepLinkConfigurator
    ) = ReferendumDeepLinkHandler(
        deepLinkingRouter,
        chainRegistry,
        mutableGovernanceState,
        automaticInteractionGate,
        referendumDetailsDeepLinkConfigurator
    )

    @Provides
    fun provideBuyCallbackDeepLinkHandler(
        resourceManager: ResourceManager
    ) = BuyCallbackDeepLinkHandler(resourceManager)

    @Provides
    fun provideWalletConnectDeepLinkHandler(
        walletConnectService: WalletConnectService,
        automaticInteractionGate: AutomaticInteractionGate
    ) = WalletConnectPairDeeplinkHandler(walletConnectService, automaticInteractionGate)

    @Provides
    fun provideAssetDetailsDeepLinkHandler(
        deepLinkingRouter: DeepLinkingRouter,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        automaticInteractionGate: AutomaticInteractionGate,
        assetDetailsDeepLinkConfigurator: AssetDetailsDeepLinkConfigurator
    ) = AssetDetailsDeepLinkHandler(
        router = deepLinkingRouter,
        accountRepository = accountRepository,
        chainRegistry = chainRegistry,
        automaticInteractionGate = automaticInteractionGate,
        assetDetailsDeepLinkConfigurator = assetDetailsDeepLinkConfigurator
    )

    @Provides
    fun provideRootDeepLinkHandler(
        stakingDashboardDeepLinkHandler: StakingDashboardDeepLinkHandler,
        importMnemonicDeepLinkHandler: ImportMnemonicDeepLinkHandler,
        dappDeepLinkHandler: DAppDeepLinkHandler,
        referendumDeepLinkHandler: ReferendumDeepLinkHandler,
        buyCallbackDeepLinkHandler: BuyCallbackDeepLinkHandler,
        assetDetailsDeepLinkHandler: AssetDetailsDeepLinkHandler,
        walletConnectPairDeeplinkHandler: WalletConnectPairDeeplinkHandler
    ): RootDeepLinkHandler {
        val deepLinkHandlers = listOf(
            stakingDashboardDeepLinkHandler,
            importMnemonicDeepLinkHandler,
            dappDeepLinkHandler,
            referendumDeepLinkHandler,
            buyCallbackDeepLinkHandler,
            assetDetailsDeepLinkHandler,
            walletConnectPairDeeplinkHandler
        )

        return RootDeepLinkHandler(deepLinkHandlers)
    }
}
