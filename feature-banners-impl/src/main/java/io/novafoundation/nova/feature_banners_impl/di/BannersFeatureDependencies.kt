package io.novafoundation.nova.feature_banners_impl.di

import android.content.Context
import coil.ImageLoader
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.LanguagesHolder
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory


interface BannersFeatureDependencies {

    val imageLoader: ImageLoader

    val context: Context

    val preferences: Preferences

    val languagesHolder: LanguagesHolder

    val networkApiCreator: NetworkApiCreator
}
