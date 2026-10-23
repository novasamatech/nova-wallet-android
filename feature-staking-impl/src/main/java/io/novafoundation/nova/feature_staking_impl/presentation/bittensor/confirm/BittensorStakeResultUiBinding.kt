package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun BaseFragmentMixin<*>.bindBittensorStakeResults(
    results: Flow<BittensorStakeResultUi?>,
    onAction: (BittensorResultAction) -> Unit
) {
    var resultSheet: BittensorStakeResultBottomSheet? = null
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            resultSheet = resultSheet.dismissWithoutResultCallback()
        }
    })
    results.observeWhenVisible { state ->
        if (state == null) {
            resultSheet = resultSheet.dismissWithoutResultCallback()
        } else {
            val sheet = resultSheet ?: BittensorStakeResultBottomSheet(fragment.requireContext()) { action ->
                resultSheet = resultSheet.dismissWithoutResultCallback()
                onAction(action)
            }.also { sheet ->
                resultSheet = sheet
                // Only the app closes it without a button (before the PIN check); the screen shows it again when it is back.
                sheet.setOnDismissListener { if (resultSheet === sheet) resultSheet = null }
            }
            sheet.setState(state)
            if (!sheet.isShowing) sheet.show()
        }
    }
}

/** For a view model with one callback per button: the sheet offers the buttons those callbacks cover. */
fun BaseFragmentMixin<*>.bindBittensorStakeResults(
    results: Flow<BittensorStakeResultUi?>,
    onDismiss: () -> Unit,
    onTryAgain: () -> Unit,
    onViewPosition: () -> Unit,
    onDiscoverSubnets: () -> Unit = {},
    onUnconfirmedDone: () -> Unit = {}
) = bindBittensorStakeResults(results.map { it?.withCallbackActions() }) { action ->
    when (action) {
        BittensorResultAction.TRY_AGAIN -> {
            onDismiss()
            onTryAgain()
        }
        BittensorResultAction.VIEW_POSITION, BittensorResultAction.YOUR_BITTENSOR -> onViewPosition()
        BittensorResultAction.DISCOVER_SUBNETS -> onDiscoverSubnets()
        BittensorResultAction.DONE -> onUnconfirmedDone()
        BittensorResultAction.CLOSE, BittensorResultAction.BACK_TO_POSITION -> onDismiss()
    }
}

private fun BittensorStakeResultUi.withCallbackActions(): BittensorStakeResultUi = when {
    actions.isNotEmpty() -> this
    this is BittensorStakeResultUi.Success -> copy(
        actions = listOfNotNull(BittensorResultAction.DISCOVER_SUBNETS.takeIf { canDiscoverSubnets }, BittensorResultAction.VIEW_POSITION)
    )
    this is BittensorStakeResultUi.Unconfirmed -> copy(actions = listOf(BittensorResultAction.DONE))
    else -> this
}

internal fun BittensorStakeResultBottomSheet?.dismissWithoutResultCallback(): BittensorStakeResultBottomSheet? {
    // The dismiss listener runs later; a model-driven reset or a button must not let it forget a sheet shown after this one.
    this?.setOnDismissListener(null)
    this?.dismiss()
    return null
}
