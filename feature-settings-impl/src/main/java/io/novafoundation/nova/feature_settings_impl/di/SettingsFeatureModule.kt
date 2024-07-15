package io.novafoundation.nova.feature_settings_impl.di

import dagger.Module
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_settings_impl.domain.CloudBackupSettingsInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealCloudBackupSettingsInteractor
import dagger.Provides
import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.common.data.repository.BannerVisibilityRepository
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.CoinGeckoLinkValidationFactory
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import io.novafoundation.nova.feature_settings_impl.domain.AddNetworkInteractor
import io.novafoundation.nova.feature_settings_impl.domain.CustomNodeInteractor
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementChainInteractor
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementInteractor
import io.novafoundation.nova.feature_settings_impl.domain.PreConfiguredNetworksInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealAddNetworkInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealCustomNodeInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealNetworkManagementChainInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealNetworkManagementInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealPreConfiguredNetworksInteractor
import io.novafoundation.nova.feature_settings_impl.domain.utils.CustomChainFactory
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.RealNetworkListAdapterItemFactory
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.explorer.BlockExplorerLinkFormatter
import io.novafoundation.nova.runtime.explorer.CommonBlockExplorerLinkFormatter
import io.novafoundation.nova.runtime.explorer.EtherscanBlockExplorerLinkFormatter
import io.novafoundation.nova.runtime.explorer.StatescanBlockExplorerLinkFormatter
import io.novafoundation.nova.runtime.explorer.SubscanBlockExplorerLinkFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory
import io.novafoundation.nova.runtime.multiNetwork.connection.node.healthState.NodeHealthStateTesterFactory
import io.novafoundation.nova.runtime.repository.ChainNodeRepository
import io.novafoundation.nova.runtime.repository.ChainRepository
import io.novafoundation.nova.runtime.repository.PreConfiguredChainsRepository

@Module
class SettingsFeatureModule {

    @Provides
    @FeatureScope
    fun provideCloudBackupSettingsInteractor(
        accountRepository: AccountRepository,
        cloudBackupService: CloudBackupService,
        cloudBackupFacade: LocalAccountsCloudBackupFacade
    ): CloudBackupSettingsInteractor {
        return RealCloudBackupSettingsInteractor(
            accountRepository,
            cloudBackupService,
            cloudBackupFacade
        )
    }

    @Provides
    @FeatureScope
    fun provideNetworkManagementInteractor(
        chainRegistry: ChainRegistry,
        bannerVisRepository: BannerVisibilityRepository
    ): NetworkManagementInteractor {
        return RealNetworkManagementInteractor(chainRegistry, bannerVisRepository)
    }

    @Provides
    @FeatureScope
    fun provideNetworkManagementChainInteractor(
        chainRegistry: ChainRegistry,
        nodeHealthStateTesterFactory: NodeHealthStateTesterFactory,
        chainRepository: ChainRepository
    ): NetworkManagementChainInteractor {
        return RealNetworkManagementChainInteractor(chainRegistry, nodeHealthStateTesterFactory, chainRepository)
    }

    @Provides
    @FeatureScope
    fun provideNetworkListAdapterItemFactory(
        resourceManager: ResourceManager
    ): NetworkListAdapterItemFactory {
        return RealNetworkListAdapterItemFactory(resourceManager)
    }

    @Provides
    @FeatureScope
    fun provideNodeChainIdRepositoryFactory(
        nodeConnectionFactory: NodeConnectionFactory,
        web3ApiFactory: Web3ApiFactory
    ): NodeChainIdRepositoryFactory {
        return NodeChainIdRepositoryFactory(nodeConnectionFactory, web3ApiFactory)
    }

    @Provides
    @FeatureScope
    fun provideCustomNodeInteractor(
        chainRegistry: ChainRegistry,
        chainNodeRepository: ChainNodeRepository,
        nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory,
        nodeConnectionFactory: NodeConnectionFactory
    ): CustomNodeInteractor {
        return RealCustomNodeInteractor(
            chainRegistry,
            chainNodeRepository,
            nodeChainIdRepositoryFactory,
            nodeConnectionFactory
        )
    }

    @Provides
    @FeatureScope
    fun providePreConfiguredNetworksInteractor(
        preConfiguredChainsRepository: PreConfiguredChainsRepository
    ): PreConfiguredNetworksInteractor {
        return RealPreConfiguredNetworksInteractor(
            preConfiguredChainsRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideBlockExplorerLinkFormatter(): BlockExplorerLinkFormatter {
        return CommonBlockExplorerLinkFormatter(
            listOf(
                SubscanBlockExplorerLinkFormatter(),
                StatescanBlockExplorerLinkFormatter(),
                EtherscanBlockExplorerLinkFormatter()
            )
        )
    }

    @Provides
    @FeatureScope
    fun provideCustomChainFactory(
        nodeConnectionFactory: NodeConnectionFactory,
        coinGeckoLinkParser: CoinGeckoLinkParser,
        blockExplorerLinkFormatter: BlockExplorerLinkFormatter,
        nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory
    ): CustomChainFactory {
        return CustomChainFactory(
            nodeConnectionFactory,
            nodeChainIdRepositoryFactory,
            coinGeckoLinkParser,
            blockExplorerLinkFormatter
        )
    }

    @Provides
    @FeatureScope
    fun provideAddNetworkInteractor(
        chainRepository: ChainRepository,
        chainRegistry: ChainRegistry,
        nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory,
        coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory,
        coinGeckoLinkParser: CoinGeckoLinkParser,
        nodeConnectionFactory: NodeConnectionFactory,
        customChainFactory: CustomChainFactory
    ): AddNetworkInteractor {
        return RealAddNetworkInteractor(
            chainRepository,
            chainRegistry,
            nodeChainIdRepositoryFactory,
            coinGeckoLinkValidationFactory,
            coinGeckoLinkParser,
            nodeConnectionFactory,
            customChainFactory
        )
    }
}
