package io.novafoundation.nova.feature_account_api.presenatation.navigation

import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy

interface ExtrinsicNavigationWrapper {

    suspend fun startNavigation(
        submissionHierarchy: SubmissionHierarchy,
        fallback: suspend () -> Unit
    )
}
