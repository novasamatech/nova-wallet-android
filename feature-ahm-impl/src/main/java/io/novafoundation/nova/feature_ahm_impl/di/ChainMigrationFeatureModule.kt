package io.novafoundation.nova.feature_ahm_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.repository.ToggleFeatureRepository
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.feature_ahm_api.data.repository.ChainMigrationRepository
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_impl.data.config.ChainMigrationConfigApi
import io.novafoundation.nova.feature_ahm_impl.data.repository.RealChainMigrationRepository
import io.novafoundation.nova.feature_ahm_impl.data.repository.RealMigrationInfoRepository
import io.novafoundation.nova.feature_ahm_impl.di.modules.DeepLinkModule
import io.novafoundation.nova.feature_ahm_api.domain.AssetMigrationUseCase
import io.novafoundation.nova.feature_ahm_impl.domain.ChainMigrationDetailsInteractor
import io.novafoundation.nova.feature_ahm_impl.domain.RealAssetMigrationUseCase
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module(
    includes = [DeepLinkModule::class]
)
class ChainMigrationFeatureModule {

    @Provides
    @FeatureScope
    fun provideChainMigrationConfigApi(
        apiCreator: NetworkApiCreator
    ): ChainMigrationConfigApi {
        return apiCreator.create(ChainMigrationConfigApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideChainMigrationRepository(
        assetDao: AssetDao,
        preferences: Preferences,
        chainRegistry: ChainRegistry,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource
    ): ChainMigrationRepository {
        return RealChainMigrationRepository(
            assetDao,
            preferences,
            chainRegistry,
            remoteStorageSource
        )
    }

    @Provides
    @FeatureScope
    fun provideMigrationInfoRepository(
        api: ChainMigrationConfigApi
    ): MigrationInfoRepository {
        return RealMigrationInfoRepository(api)
    }

    @Provides
    @FeatureScope
    fun provideChainMigrationDetailsInteractor(
        chainRegistry: ChainRegistry,
        chainMigrationRepository: ChainMigrationRepository,
        migrationInfoRepository: MigrationInfoRepository
    ): ChainMigrationDetailsInteractor {
        return ChainMigrationDetailsInteractor(
            chainRegistry,
            chainMigrationRepository,
            migrationInfoRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideAssetMigrationUseCase(
        migrationInfoRepository: MigrationInfoRepository,
        toggleFeatureRepository: ToggleFeatureRepository,
        chainRegistry: ChainRegistry
    ): AssetMigrationUseCase {
        return RealAssetMigrationUseCase(
            migrationInfoRepository,
            toggleFeatureRepository,
            chainRegistry
        )
    }
}
