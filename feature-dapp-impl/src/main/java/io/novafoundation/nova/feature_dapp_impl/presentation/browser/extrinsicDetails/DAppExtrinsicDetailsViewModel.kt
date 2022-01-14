package io.novafoundation.nova.feature_dapp_impl.presentation.browser.extrinsicDetails

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_dapp_impl.DAppRouter

class DAppExtrinsicDetailsViewModel(
    private val router: DAppRouter,
    val extrinsicContent: String
) : BaseViewModel() {

    fun closeClicked() {
        router.back()
    }
}
