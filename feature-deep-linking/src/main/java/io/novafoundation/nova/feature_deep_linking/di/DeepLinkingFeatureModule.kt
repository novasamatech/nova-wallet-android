package io.novafoundation.nova.feature_deep_linking.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.AssetDetailsDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.BuyCallbackDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.DAppDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ImportMnemonicDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ReferendumDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.StakingDashboardDeepLinkHandler
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_api.presentation.DAppRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.RootDeepLinkHandler
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Qualifier

@Module()
class DeepLinkingFeatureModule {

    @Provides
    fun provideStakingDashboardDeepLinkHandler(
        assetsRouter: AssetsRouter,
        automaticInteractionGate: AutomaticInteractionGate
    ) = StakingDashboardDeepLinkHandler(assetsRouter, automaticInteractionGate)

    @Provides
    fun provideImportMnemonicDeepLinkHandler(
        accountRouter: AccountRouter,
        encryptionDefaults: EncryptionDefaults,
        accountRepository: AccountRepository,
        automaticInteractionGate: AutomaticInteractionGate
    ) = ImportMnemonicDeepLinkHandler(
        accountRouter,
        encryptionDefaults,
        accountRepository,
        automaticInteractionGate
    )

    @Provides
    fun provideDappDeepLinkHandler(
        dAppMetadataRepository: DAppMetadataRepository,
        dAppRouter: DAppRouter,
        automaticInteractionGate: AutomaticInteractionGate
    ) = DAppDeepLinkHandler(
        dAppMetadataRepository,
        dAppRouter,
        automaticInteractionGate
    )

    @Provides
    fun provideReferendumDeepLinkHandler(
        governanceRouter: GovernanceRouter,
        chainRegistry: ChainRegistry,
        mutableGovernanceState: MutableGovernanceState,
        accountRepository: AccountRepository,
        automaticInteractionGate: AutomaticInteractionGate,
        resourceManager: ResourceManager
    ) = ReferendumDeepLinkHandler(
        governanceRouter,
        chainRegistry,
        mutableGovernanceState,
        accountRepository,
        automaticInteractionGate,
        resourceManager
    )

    @Provides
    fun provideBuyCallbackDeepLinkHandler(
        resourceManager: ResourceManager
    ) = BuyCallbackDeepLinkHandler(resourceManager)

    @Provides
    fun provideAssetDetailsDeepLinkHandler(
        assetsRouter: AssetsRouter,
        automaticInteractionGate: AutomaticInteractionGate,
        resourceManager: ResourceManager
    ) = AssetDetailsDeepLinkHandler(
        assetsRouter = assetsRouter,
        automaticInteractionGate = automaticInteractionGate,
        resourceManager = resourceManager
    )

    @Provides
    fun provideRootDeepLinkHandler(
        stakingDashboardDeepLinkHandler: StakingDashboardDeepLinkHandler,
        importMnemonicDeepLinkHandler: ImportMnemonicDeepLinkHandler,
        dappDeepLinkHandler: DAppDeepLinkHandler,
        referendumDeepLinkHandler: ReferendumDeepLinkHandler,
        buyCallbackDeepLinkHandler: BuyCallbackDeepLinkHandler
    ): RootDeepLinkHandler {
        val deepLinkHandlers = listOf(
            stakingDashboardDeepLinkHandler,
            importMnemonicDeepLinkHandler,
            dappDeepLinkHandler,
            referendumDeepLinkHandler,
            buyCallbackDeepLinkHandler
        )

        return RootDeepLinkHandler(deepLinkHandlers)
    }
}
