package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import android.view.View
import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.feature_account_api.presenatation.fee.select.FeeAssetSelectorBottomSheet
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView
import kotlinx.coroutines.flow.Flow

interface WithFeeLoaderMixin {

    val originFeeMixin: GenericFeeLoaderMixin<*>?
}

fun <V> BaseFragmentMixin<V>.setupFeeLoading(viewModel: V, feeView: FeeView) where V : BaseViewModel, V : GenericFeeLoaderMixin<*> {
    observeRetries(viewModel)

    viewModel.feeLiveData.observe(feeView::setFeeStatus)
    viewModel.changeFeeTokenState.observe { editableStatus ->
        feeView.setFeeEditable(editableStatus.isEditable()) {
            openEditFeeBottomSheet(viewModel, editableStatus)
        }
    }
}

fun BaseFragmentMixin<*>.setupFeeLoading(withFeeLoaderMixin: WithFeeLoaderMixin, feeView: FeeView) {
    val mixin = withFeeLoaderMixin.originFeeMixin

    if (mixin != null) {
        setupFeeLoading(mixin, feeView)
    } else {
        feeView.makeGone()
    }
}

fun BaseFragmentMixin<*>.setupFeeLoading(mixin: GenericFeeLoaderMixin<*>, feeView: FeeView) {
    observeRetries(mixin)

    mixin.feeLiveData.observe(feeView::setFeeStatus)
    mixin.changeFeeTokenState.observe { editableStatus ->
        feeView.setFeeEditable(editableStatus.isEditable()) {
            openEditFeeBottomSheet(mixin, editableStatus)
        }
    }
}

fun BaseFragmentMixin<*>.setupSelectableFeeToken(
    tokenSelectableFlow: Flow<Boolean>,
    feeView: FeeView,
    onEditTokenClick: View.OnClickListener
) {
    tokenSelectableFlow.observe { canChangeFeeToken -> feeView.setFeeEditable(canChangeFeeToken, onEditTokenClick) }
}

private fun BaseFragmentMixin<*>.openEditFeeBottomSheet(mixin: GenericFeeLoaderMixin<*>, editableStatus: ChangeFeeTokenState) {
    if (editableStatus !is ChangeFeeTokenState.Editable) return

    val payload = FeeAssetSelectorBottomSheet.Payload(
        options = editableStatus.availableAssets,
        selectedOption = editableStatus.selectedCommissionAsset
    )

    FeeAssetSelectorBottomSheet(
        context = providedContext,
        payload = payload,
        onOptionClicked = { mixin.setCommissionAsset(it) },
        onCancel = { }
    ).show()
}
