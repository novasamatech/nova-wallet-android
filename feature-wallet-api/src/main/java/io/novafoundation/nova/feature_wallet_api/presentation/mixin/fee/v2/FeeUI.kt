package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin.Action
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.feature_account_api.presenatation.fee.select.FeeAssetSelectorBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.ChooseFeeCurrencyPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

// TODO We use <V> here since star projections cause 1.7.21 compiler to fail
// https://youtrack.jetbrains.com/issue/KT-51277/NoSuchElementException-Collection-contains-no-element-matching-the-predicate-with-context-receivers-and-star-projection
// We can return to star projections after upgrading Kotlin to at least 1.8.20
context(BaseFragment<V, *>)
fun <F, D, V : BaseViewModel> FeeLoaderMixinV2<F, D>.setupFeeLoading(
    setFeeStatus: (FeeStatus<F, D>) -> Unit,
    setUserCanChangeFeeAsset: (Boolean) -> Unit
) {
    observeRetries(this)

    fee.observe(setFeeStatus)

    userCanChangeFeeAsset.observe(setUserCanChangeFeeAsset)

    chooseFeeAsset.awaitableActionLiveData.observeEvent {
        openEditFeeBottomSheet(it)
    }
}

context(BaseFragment<V, *>)
fun <F, V : BaseViewModel> FeeLoaderMixinV2<F, FeeDisplay>.setupFeeLoading(feeView: FeeView) {
    setupFeeLoading(
        setFeeStatus = { feeView.setFeeStatus(it) },
        setUserCanChangeFeeAsset = {
            feeView.setFeeEditable(it) {
                changePaymentCurrencyClicked()
            }
        }
    )
}

context(BaseFragment<V, *>)
private fun <V : BaseViewModel> openEditFeeBottomSheet(action: Action<ChooseFeeCurrencyPayload, Chain.Asset>) {
    val payload = FeeAssetSelectorBottomSheet.Payload(
        options = action.payload.availableAssets,
        selectedOption = action.payload.selectedCommissionAsset
    )

    FeeAssetSelectorBottomSheet(
        context = providedContext,
        payload = payload,
        onOptionClicked = action.onSuccess,
        onCancel = action.onCancel
    ).show()
}
