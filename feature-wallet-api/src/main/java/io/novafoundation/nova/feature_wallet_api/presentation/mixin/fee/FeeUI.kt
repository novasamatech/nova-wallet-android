package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView

interface WithFeeLoaderMixin {

    val originFeeMixin: GenericFeeLoaderMixin<*>?
}

fun <V> BaseFragmentMixin<V>.setupFeeLoading(viewModel: V, feeView: FeeView) where V : BaseViewModel, V : GenericFeeLoaderMixin<*> {
    observeRetries(viewModel)

    viewModel.feeLiveData.observe(feeView::setFeeStatus)
}

fun BaseFragmentMixin<*>.setupFeeLoading(mixin: GenericFeeLoaderMixin<*>, feeView: FeeView) {
    observeRetries(mixin)

    mixin.feeLiveData.observe(feeView::setFeeStatus)
}

fun BaseFragmentMixin<*>.setupFeeLoading(withFeeLoaderMixin: WithFeeLoaderMixin, feeView: FeeView) {
    val mixin = withFeeLoaderMixin.originFeeMixin

    if (mixin != null) {
        setupFeeLoading(mixin, feeView)
    } else {
        feeView.makeGone()
    }
}
