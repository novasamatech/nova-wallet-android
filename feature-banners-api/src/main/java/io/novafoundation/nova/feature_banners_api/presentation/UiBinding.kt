package io.novafoundation.nova.feature_banners_api.presentation

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.feature_banners_api.presentation.view.BannerPagerView

context(BaseFragment<T, *>)
fun <T : BaseViewModel> PromotionBannersMixin.bindWithAdapter(
    adapter: PromotionBannerAdapter,
    onSubmitList: () -> Unit = {}
) {
    adapter.setCallback(object : BannerPagerView.Callback {
        override fun onBannerClicked(page: BannerPageModel) {
            this@bindWithAdapter.startBannerAction(page)
        }

        override fun onBannerClosed(page: BannerPageModel) {
            this@bindWithAdapter.closeBanner(page)
        }
    })

    bannersFlow.observe {
        adapter.show(it is ExtendedLoadingState.Loaded && it.data.isNotEmpty())
        adapter.setBanners(it.dataOrNull.orEmpty())
        onSubmitList()
    }
}
