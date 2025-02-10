package io.novafoundation.nova.common.di.modules

import android.content.Context
import coil.ImageLoader
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.feature_banners_impl.data.BannersApi
import io.novafoundation.nova.feature_banners_impl.data.BannersRepository
import io.novafoundation.nova.feature_banners_impl.data.RealBannersRepository
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_banners_impl.domain.PromotionBannersInteractor
import io.novafoundation.nova.feature_banners_impl.presentation.banner.PromotionBannersMixinFactory
import io.novafoundation.nova.common.resources.LanguagesHolder

@Module
class BannersModule {

}
