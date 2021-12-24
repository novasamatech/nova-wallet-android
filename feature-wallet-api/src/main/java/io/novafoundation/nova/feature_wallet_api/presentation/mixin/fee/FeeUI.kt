package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import android.widget.ProgressBar
import android.widget.TextView
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView

class FeeViews(
    val progress: ProgressBar,
    val fiat: TextView,
    val token: TextView,
)

interface WithFeeLoaderMixin {

    val feeLoaderMixin: FeeLoaderMixin
}

fun <V> BaseFragmentMixin<V>.setupFeeLoading(viewModel: V, feeView: FeeView) where V : BaseViewModel, V : FeeLoaderMixin {
    observeRetries(viewModel)

    viewModel.feeLiveData.observe(feeView::setFeeStatus)
}

fun BaseFragmentMixin<*>.setupFeeLoading(withFeeLoaderMixin: WithFeeLoaderMixin, feeView: FeeView) {
    observeRetries(withFeeLoaderMixin.feeLoaderMixin)

    withFeeLoaderMixin.feeLoaderMixin.feeLiveData.observe(feeView::setFeeStatus)
}

fun displayFeeStatus(
    feeStatus: FeeStatus,
    feeViews: FeeViews,
) = with(feeViews) {
    val context = progress.context

    when (feeStatus) {
        is FeeStatus.Loading -> feeProgressShown(true, feeViews)
        is FeeStatus.Error -> {
            feeProgressShown(false, feeViews)

            token.text = context.getString(R.string.common_error_general_title)
            fiat.text = ""
        }
        is FeeStatus.Loaded -> {
            feeProgressShown(false, feeViews)

            fiat.text = feeStatus.feeModel.displayFiat
            token.text = feeStatus.feeModel.displayToken
        }
    }
}

private fun feeProgressShown(
    shown: Boolean,
    feeViews: FeeViews,
) = with(feeViews) {
    fiat.setVisible(!shown)
    token.setVisible(!shown)

    progress.setVisible(shown)
}
