package io.novafoundation.nova.feature_settings_impl.di

import dagger.Module
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_settings_impl.domain.CloudBackupSettingsInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealCloudBackupSettingsInteractor
import dagger.Provides
import io.novafoundation.nova.common.data.repository.BannerVisibilityRepository
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import io.novafoundation.nova.feature_settings_impl.domain.CustomNodeInteractor
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementChainInteractor
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealCustomNodeInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealNetworkManagementChainInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealNetworkManagementInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.RealNetworkListAdapterItemFactory
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory
import io.novafoundation.nova.runtime.multiNetwork.connection.node.healthState.NodeHealthStateTesterFactory
import io.novafoundation.nova.runtime.repository.ChainNodeRepository

@Module
class SettingsFeatureModule {

    @Provides
    @FeatureScope
    fun provideCloudBackupSettingsInteractor(
        cloudBackupService: CloudBackupService,
        cloudBackupFacade: LocalAccountsCloudBackupFacade
    ): CloudBackupSettingsInteractor {
        return RealCloudBackupSettingsInteractor(
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
        nodeHealthStateTesterFactory: NodeHealthStateTesterFactory
    ): NetworkManagementChainInteractor {
        return RealNetworkManagementChainInteractor(chainRegistry, nodeHealthStateTesterFactory)
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
        nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory
    ): CustomNodeInteractor {
        return RealCustomNodeInteractor(
            chainRegistry,
            chainNodeRepository,
            nodeChainIdRepositoryFactory
        )
    }
}
