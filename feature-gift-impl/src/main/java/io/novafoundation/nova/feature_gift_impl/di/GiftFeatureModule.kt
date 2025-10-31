package io.novafoundation.nova.feature_gift_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.GiftsDao
import io.novafoundation.nova.feature_gift_impl.data.GiftsRepository
import io.novafoundation.nova.feature_gift_impl.data.RealGiftsRepository
import io.novafoundation.nova.feature_gift_impl.domain.GiftsInteractor
import io.novafoundation.nova.feature_gift_impl.domain.RealGiftsInteractor

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
}
