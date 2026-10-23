package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import kotlinx.coroutines.flow.Flow

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

internal fun BittensorStakeResultBottomSheet?.dismissWithoutResultCallback(): BittensorStakeResultBottomSheet? {
    // The dismiss listener runs later; a model-driven reset or a button must not let it forget a sheet shown after this one.
    this?.setOnDismissListener(null)
    this?.dismiss()
    return null
}
