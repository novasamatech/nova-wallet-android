package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import kotlinx.coroutines.flow.Flow

fun BaseFragmentMixin<*>.bindBittensorStakeResults(
    results: Flow<BittensorStakeResultUi?>,
    onDismiss: () -> Unit,
    onTryAgain: () -> Unit,
    onViewPosition: () -> Unit,
    onDiscoverSubnets: () -> Unit = {}
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
            val sheet = resultSheet ?: BittensorStakeResultBottomSheet(
                context = fragment.requireContext(),
                onClose = { resultSheet?.dismiss() },
                onTryAgain = {
                    resultSheet?.dismiss()
                    onTryAgain()
                },
                onDiscoverSubnets = {
                    resultSheet?.dismiss()
                    onDiscoverSubnets()
                },
                onViewPosition = {
                    resultSheet?.dismiss()
                    onViewPosition()
                }
            ).also { sheet ->
                resultSheet = sheet
                sheet.setOnDismissListener {
                    resultSheet = null
                    onDismiss()
                }
            }
            sheet.setState(state)
            if (!sheet.isShowing) sheet.show()
        }
    }
}

internal fun BittensorStakeResultBottomSheet?.dismissWithoutResultCallback(): BittensorStakeResultBottomSheet? {
    // A model-driven reset (including signing cancellation) must not run user-dismiss navigation.
    this?.setOnDismissListener(null)
    this?.dismiss()
    return null
}
