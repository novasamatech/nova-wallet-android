package io.novafoundation.nova.feature_deep_linking.presentation.deferred

sealed interface ReferralInstallResult {
    object AlreadyHandled : ReferralInstallResult
    data class DeeplinkExtracted(val deeplink: String) : ReferralInstallResult
    object NotSupported : ReferralInstallResult
    object ServiceUnavailable : ReferralInstallResult
    object ServiceDisconnected : ReferralInstallResult
}
