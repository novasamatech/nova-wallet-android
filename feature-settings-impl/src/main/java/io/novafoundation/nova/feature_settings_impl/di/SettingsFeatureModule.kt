package io.novafoundation.nova.feature_settings_impl.di

import dagger.Module
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_settings_impl.domain.CloudBackupSettingsInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealCloudBackupSettingsInteractor
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealNetworkManagementInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListAdapterItemFactory
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.RealNetworkListAdapterItemFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

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
        chainRegistry: ChainRegistry
    ): NetworkManagementInteractor {
        return RealNetworkManagementInteractor(chainRegistry)
    }

    @Provides
    @FeatureScope
    fun provideNetworkListAdapterItemFactory(
        resourceManager: ResourceManager
    ): NetworkListAdapterItemFactory {
        return RealNetworkListAdapterItemFactory(resourceManager)
    }
}
