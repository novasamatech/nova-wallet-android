package io.novafoundation.nova.feature_banners_impl.presentation.banner

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState

context(BaseFragment<T>)
fun <T : BaseViewModel> PromotionBannersMixin.bindWithAdapter(adapter: PromotionBannerAdapter) {
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
