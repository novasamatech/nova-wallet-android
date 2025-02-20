package io.novafoundation.nova.feature_banners_api.presentation

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_banners_api.presentation.view.BannerPagerView

context(BaseFragment<T>)
fun <T : BaseViewModel> PromotionBannersMixin.bindWithAdapter(adapter: PromotionBannerAdapter) {
    adapter.setCallback(object : BannerPagerView.Callback {
        override fun onBannerClicked(page: BannerPageModel) {
            this@bindWithAdapter.startBannerAction(page)
        }

        override fun onBannerClosed(page: BannerPageModel) {
            this@bindWithAdapter.closeBanner(page)
        }
    })

    bannersFlow.observe {
        when (it) {
            is ExtendedLoadingState.Loaded -> {
                adapter.show(it.data.isNotEmpty())
                adapter.showShimmering(false)
                adapter.setBanners(it.data)
            }

            is ExtendedLoadingState.Loading -> {
                adapter.showShimmering(true)
            }

            else -> {}
        }
    }
}
