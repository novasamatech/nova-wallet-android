package io.novafoundation.nova.feature_ahm_impl.di

import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.repository.ToggleFeatureRepository
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

interface ChainMigrationFeatureDependencies {

    val resourceManager: ResourceManager

    val promotionBannersMixinFactory: PromotionBannersMixinFactory

    val bannersSourceFactory: BannersSourceFactory

    val assetDao: AssetDao

    val preferences: Preferences

    val chainRegistry: ChainRegistry

    val apiCreator: NetworkApiCreator

    val automaticInteractionGate: AutomaticInteractionGate

    val toggleFeatureRepository: ToggleFeatureRepository

    val chainStateRepository: ChainStateRepository

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageSource(): StorageDataSource
}
