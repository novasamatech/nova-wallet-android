package io.novafoundation.nova.feature_banners_impl.presentation.banner

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.asError
import io.novafoundation.nova.common.domain.asLoaded
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_banners_api.domain.PromotionBanner
import io.novafoundation.nova.common.utils.asyncWithContext
import io.novafoundation.nova.common.utils.toMap
import io.novafoundation.nova.feature_banners_api.presentation.BannerPageModel
import io.novafoundation.nova.feature_banners_api.presentation.ClipableImage
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixin
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSource
import io.novafoundation.nova.feature_banners_impl.domain.PromotionBannersInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class RealPromotionBannersMixinFactory(
    private val imageLoader: ImageLoader,
    private val context: Context,
    private val promotionBannersInteractor: PromotionBannersInteractor
) : PromotionBannersMixinFactory {

    override fun create(source: BannersSource, coroutineScope: CoroutineScope): PromotionBannersMixin {
        return RealPromotionBannersMixin(
            promotionBannersInteractor,
            imageLoader,
            context,
            source,
            coroutineScope
        )
    }
}

class RealPromotionBannersMixin(
    private val promotionBannersInteractor: PromotionBannersInteractor,
    private val imageLoader: ImageLoader,
    private val context: Context,
    private val bannersSource: BannersSource,
    private val coroutineScope: CoroutineScope
) : PromotionBannersMixin {

    override val bannersFlow: MutableStateFlow<ExtendedLoadingState<List<BannerPageModel>>> = MutableStateFlow(ExtendedLoadingState.Loading)

    init {
        coroutineScope.launch {
            runCatching {
                val banners = bannersSource.getBanners()
                val resources = loadResources(banners)

                banners.map { mapBanner(it, resources) }
            }.onSuccess { bannersFlow.value = it.asLoaded() }
                .onFailure { bannersFlow.value = it.asError() }
        }
    }

    override fun closeBanner(banner: BannerPageModel) {
        promotionBannersInteractor.closeBanner(banner.id)
    }

    override fun closeAllBanners() {
        bannersFlow.value = emptyList<BannerPageModel>().asLoaded()
    }

    override fun handleBannerAction(page: BannerPageModel) {
        val url = page.actionUrl ?: return

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error while running an activity", e)
        }
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
            actionUrl = banner.actionLink
        )
    }
}
