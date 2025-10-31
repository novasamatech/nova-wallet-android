package io.novafoundation.nova.feature_gift_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.GiftsDao
import io.novafoundation.nova.feature_gift_impl.data.GiftsRepository
import io.novafoundation.nova.feature_gift_impl.data.RealGiftsRepository
import io.novafoundation.nova.feature_gift_impl.domain.GiftsInteractor
import io.novafoundation.nova.feature_gift_impl.domain.RealGiftsInteractor
import io.novafoundation.nova.feature_gift_impl.domain.RealCreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.CreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.amount.GiftMinAmountProviderFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry

@Module()
class GiftFeatureModule {

    @Provides
    @FeatureScope
    fun providesGiftsRepository(
        giftsDao: GiftsDao
    ): GiftsRepository {
        return RealGiftsRepository(giftsDao)
    }

    @Provides
    @FeatureScope
    fun providesGiftsInteractor(repository: GiftsRepository): GiftsInteractor {
        return RealGiftsInteractor(repository)
    }

    @Provides
    @FeatureScope
    fun provideSelectGiftAmountInteractor(
        assetSourceRegistry: AssetSourceRegistry
    ): CreateGiftInteractor {
        return RealCreateGiftInteractor(assetSourceRegistry)
    }

    @Provides
    @FeatureScope
    fun provideGiftMinAmountProviderFactory(
        createGiftInteractor: CreateGiftInteractor
    ): GiftMinAmountProviderFactory {
        return GiftMinAmountProviderFactory(createGiftInteractor)
    }
}
