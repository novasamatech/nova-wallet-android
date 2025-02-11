package io.novafoundation.nova.feature_banners_impl.presentation.banner

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import io.novafoundation.nova.common.domain.onError
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.asyncWithContext
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.toMap
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_banners_api.presentation.BannerPageModel
import io.novafoundation.nova.feature_banners_api.presentation.ClipableImage
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixin
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSource
import io.novafoundation.nova.feature_banners_impl.domain.PromotionBannersInteractor

class RealPromotionBannersMixinFactory(
    private val imageLoader: ImageLoader,
    private val context: Context,
    private val promotionBannersInteractor: PromotionBannersInteractor
) : PromotionBannersMixinFactory {

    override fun create(source: BannersSource): PromotionBannersMixin {
        return RealPromotionBannersMixin(
            promotionBannersInteractor,
            imageLoader,
            context,
            source
        )
    }
}

class RealPromotionBannersMixin(
    private val promotionBannersInteractor: PromotionBannersInteractor,
    private val imageLoader: ImageLoader,
    private val context: Context,
    private val bannersSource: BannersSource
) : PromotionBannersMixin {

    override val bannersFlow = flowOf {
        val banners = bannersSource.getBanners()
        val resources = loadResources(banners)

        banners.map { mapBanner(it, resources) }
    }.withSafeLoading()
        .onError {
            Log.e(LOG_TAG, "Error on banners loading", it)
        }

    private suspend fun loadResources(banners: List<PromotionBanner>): Map<String, Drawable> {
        val imagesSet = buildSet {
            addAll(banners.map { it.imageUrl })
            addAll(banners.map { it.backgroundUrl })
        }

        val loadingImagesResult = imagesSet.toMap {
            val imageRequest = ImageRequest.Builder(context)
                .data(it)
                .build()

            asyncWithContext { imageLoader.execute(imageRequest) }
        }

        return loadingImagesResult.mapValues { (_, value) -> value.await().drawable!! }
    }

    private fun mapBanner(
        banner: PromotionBanner,
        resources: Map<String, Drawable>
    ): BannerPageModel {
        return BannerPageModel(
            id = banner.id,
            title = banner.title,
            subtitle = banner.details,
            image = ClipableImage(
                resources.getValue(banner.imageUrl),
                banner.clipToBounds
            ),
            background = resources.getValue(banner.backgroundUrl),
        )
    }
}
