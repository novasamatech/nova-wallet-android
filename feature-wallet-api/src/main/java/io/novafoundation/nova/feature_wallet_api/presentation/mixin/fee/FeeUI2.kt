package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView

interface WithFeeLoaderMixinV2 {

    val originFeeMixin: FeeLoaderMixinV2.Presentation<*, FeeDisplay>
}

fun BaseFragment<*>.setupFeeLoading(withFeeLoaderMixin: WithFeeLoaderMixinV2, feeView: FeeView) {
    val mixin = withFeeLoaderMixin.originFeeMixin

    setupFeeLoading(mixin, feeView)
}

fun BaseFragment<*>.setupFeeLoading(mixin: FeeLoaderMixinV2.Presentation<*, FeeDisplay>, feeView: FeeView) {
    observeRetries(mixin)

    mixin.setupFeeLoading(feeView)
}
