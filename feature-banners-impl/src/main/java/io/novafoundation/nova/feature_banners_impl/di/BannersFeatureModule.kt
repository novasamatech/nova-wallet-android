package io.novafoundation.nova.feature_banners_impl.di

import android.content.Context
import coil.ImageLoader
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.LanguagesHolder
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory
import io.novafoundation.nova.feature_banners_impl.data.BannersApi
import io.novafoundation.nova.feature_banners_impl.data.BannersRepository
import io.novafoundation.nova.feature_banners_impl.data.RealBannersRepository
import io.novafoundation.nova.feature_banners_impl.domain.PromotionBannersInteractor
import io.novafoundation.nova.feature_banners_impl.presentation.banner.RealPromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_impl.presentation.banner.source.RealBannersSourceFactory

@Module()
class BannersFeatureModule {

    @Provides
    @FeatureScope
    fun provideBannersApi(networkApiCreator: NetworkApiCreator): BannersApi {
        return networkApiCreator.create(BannersApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideBannersRepository(
        bannersApi: BannersApi,
        languagesHolder: LanguagesHolder
    ): BannersRepository {
        return RealBannersRepository(bannersApi, languagesHolder)
    }

    @Provides
    @FeatureScope
    fun provideBannersInteractor(
        preferences: Preferences,
        repository: BannersRepository
    ): PromotionBannersInteractor {
        return PromotionBannersInteractor(preferences, repository)
    }

    @Provides
    @FeatureScope
    fun sourceFactory(promotionBannersInteractor: PromotionBannersInteractor): BannersSourceFactory {
        return RealBannersSourceFactory(promotionBannersInteractor)
    }

    @Provides
    @FeatureScope
    fun providePromotionBannersMixinFactory(
        promotionBannersInteractor: PromotionBannersInteractor,
        imageLoader: ImageLoader,
        context: Context
    ): PromotionBannersMixinFactory {
        return RealPromotionBannersMixinFactory(imageLoader, context, promotionBannersInteractor)
    }
}
