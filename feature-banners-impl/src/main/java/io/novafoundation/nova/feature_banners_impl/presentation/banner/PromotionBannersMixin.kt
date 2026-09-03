package io.novafoundation.nova.feature_banners_impl.presentation.banner

import android.content.Context
import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.request.ImageRequest
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.common.utils.launchDeepLink
import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import io.novafoundation.nova.common.utils.scopeAsync
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_banners_api.presentation.BannerPageModel
import io.novafoundation.nova.feature_banners_api.presentation.ClipableImage
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixin
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSource
import io.novafoundation.nova.feature_banners_impl.domain.PromotionBannersInteractor
import io.novafoundation.nova.feature_banners_impl.presentation.banner.source.RealBannersSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class RealPromotionBannersMixinFactory(
    private val imageLoader: ImageLoader,
    private val context: Context,
    private val promotionBannersInteractor: PromotionBannersInteractor,
    private val analyticsService: AnalyticsService
) : PromotionBannersMixinFactory {

    override fun create(source: BannersSource, coroutineScope: CoroutineScope): PromotionBannersMixin {
        return RealPromotionBannersMixin(
            promotionBannersInteractor,
            imageLoader,
            context,
            source,
            analyticsService,
            coroutineScope
        )
    }
}

class RealPromotionBannersMixin(
    private val promotionBannersInteractor: PromotionBannersInteractor,
    private val imageLoader: ImageLoader,
    private val context: Context,
    private val bannersSource: BannersSource,
    private val analyticsService: AnalyticsService,
    coroutineScope: CoroutineScope
) : PromotionBannersMixin, CoroutineScope by coroutineScope {

    override val bannersFlow = bannersSource.observeBanners()
        .map { banners ->
            val resources = loadResources(banners)
            banners.map { mapBanner(it, resources) }
        }.withSafeLoading()
        .shareInBackground()

    override fun closeBanner(banner: BannerPageModel) {
        promotionBannersInteractor.closeBanner(banner.id)
    }

    override fun startBannerAction(page: BannerPageModel) {
        val url = page.actionUrl ?: return

        analyticsService.track(AnalyticsEvent.BannerClicked(bannerId = page.id, bannerTitle = page.title, screen = sourceScreen()))

        context.launchDeepLink(url)
    }

    private fun sourceScreen(): String {
        val bannersUrl = (bannersSource as? RealBannersSource)?.bannersUrl ?: return "unknown"

        return bannersUrl.substringAfter("/content/", missingDelimiterValue = "")
            .substringBefore("/")
            .ifEmpty { "unknown" }
    }

    private suspend fun loadResources(banners: List<PromotionBanner>): Map<String, Drawable> {
        val imagesSet = buildSet {
            addAll(banners.map { it.imageUrl })
            addAll(banners.map { it.backgroundUrl })
        }

        val loadingImagesResult = imagesSet.associateWith {
            val imageRequest = ImageRequest.Builder(context)
                .data(it)
                .build()

            scopeAsync { imageLoader.execute(imageRequest) }
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
            actionUrl = banner.actionLink
        )
    }
}
