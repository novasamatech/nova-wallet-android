package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.flowAction

import kotlinx.coroutines.CoroutineScope

class EmptyReviewValidatorsFlowAction : ReviewValidatorsFlowAction {

    override suspend fun execute(coroutineScope: CoroutineScope) {
        // Do nothing
    }
}
