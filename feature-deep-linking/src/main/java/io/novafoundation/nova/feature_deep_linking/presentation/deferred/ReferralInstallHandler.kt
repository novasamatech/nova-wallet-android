package io.novafoundation.nova.feature_deep_linking.presentation.deferred

interface ReferralInstallHandler {

    suspend fun getResult(): ReferralInstallResult
}
