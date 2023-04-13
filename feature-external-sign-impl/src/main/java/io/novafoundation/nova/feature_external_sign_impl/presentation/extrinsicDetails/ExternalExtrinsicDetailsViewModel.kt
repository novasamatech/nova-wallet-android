package io.novafoundation.nova.feature_external_sign_impl.presentation.extrinsicDetails

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_external_sign_impl.ExternalSignRouter

class ExternalExtrinsicDetailsViewModel(
    private val router: ExternalSignRouter,
    val extrinsicContent: String
) : BaseViewModel() {

    fun closeClicked() {
        router.back()
    }
}
