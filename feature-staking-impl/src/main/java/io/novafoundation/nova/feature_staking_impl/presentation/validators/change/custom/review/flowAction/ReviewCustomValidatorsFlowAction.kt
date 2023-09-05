package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.flowAction

import kotlinx.coroutines.CoroutineScope

interface ReviewValidatorsFlowActionFactory {

    suspend fun create(coroutineScope: CoroutineScope): ReviewValidatorsFlowAction
}

interface ReviewValidatorsFlowAction {

    suspend fun execute(coroutineScope: CoroutineScope)
}
