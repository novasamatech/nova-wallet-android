package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.flowAction

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.SetupStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.getSelectedValidators
import kotlinx.coroutines.CoroutineScope

interface ReviewValidatorsFlowActionFactory {

    suspend fun create(coroutineScope: CoroutineScope): ReviewValidatorsFlowAction
}

interface ReviewValidatorsFlowAction {

    suspend fun execute(coroutineScope: CoroutineScope)
}

