package io.novafoundation.nova.feature_assets.presentation.common.analytics

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import java.io.IOException

fun Throwable.toSendFailureReason(): String {
    return when (this) {
        is SigningCancelledException -> "user_cancelled"
        is IOException -> "network_error"
        else -> "unknown"
    }
}
