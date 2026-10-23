package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_staking_impl.R

/** A read shown failed in place, as on iOS: what did not load, why, and Try again unless no retry can help. */
data class BittensorInlineFailure(val title: String, val message: String, val retryable: Boolean)

fun Throwable.bittensorSubnetsFailure(resources: ResourceManager) = bittensorInlineFailure(
    resources,
    title = R.string.bittensor_subnets_fetch_failed,
    message = R.string.bittensor_subnets_fetch_failed_message,
    deviceMessage = R.string.bittensor_subnets_fetch_device_message
)

fun Throwable.bittensorValidatorsFailure(resources: ResourceManager) = bittensorInlineFailure(
    resources,
    title = R.string.bittensor_validators_fetch_failed,
    message = R.string.bittensor_validators_fetch_failed_message,
    deviceMessage = R.string.bittensor_validators_fetch_device_message
)

fun AlertView.setBittensorFailure(failure: BittensorInlineFailure?, onRetry: () -> Unit) = letOrHide(failure) {
    setMessage(it.title)
    setSubMessage(it.message)
    setActionText(if (it.retryable) context.getString(R.string.common_try_again) else null)
    setOnLinkClickedListener(onRetry)
}

// As on iOS, a device that cannot use the backend is told so, and neither it nor a refused request offers Try again.
private fun Throwable.bittensorInlineFailure(
    resources: ResourceManager,
    @StringRes title: Int,
    @StringRes message: Int,
    @StringRes deviceMessage: Int
): BittensorInlineFailure {
    val kind = bittensorReadFailureKind()
    return BittensorInlineFailure(
        title = resources.getString(title),
        message = resources.getString(if (kind == BittensorReadFailureKind.DEVICE_UNSUPPORTED) deviceMessage else message),
        retryable = kind.retryable
    )
}
