package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.types

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.invokeOnCompletion
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct.DirectStakingSelection
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess.ReadyToSubmit
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.model.ConfirmMultiStakingTypeModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.model.ConfirmMultiStakingTypeModel.TypeDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class DirectConfirmMultiStakingType(
    private val selection: DirectStakingSelection,
    private val router: StartMultiStakingRouter,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val resourceManager: ResourceManager,
    private val parentContext: ConfirmMultiStakingTypeFactory.Context,
) : ConfirmMultiStakingType, CoroutineScope by parentContext.scope {

    init {
        clearStateOnCompletion()
    }

    override val stakingTypeModel: Flow<ConfirmMultiStakingTypeModel> = flowOf {
        constructUiModel()
    }

    override suspend fun onStakingTypeDetailsClicked() {
        // act as an adapter between new flow and legacy logic
        val reviewValidatorsState = ReadyToSubmit(
            newValidators = selection.validators,
            selectionMethod = ReadyToSubmit.SelectionMethod.RECOMMENDED,
            activeStake = selection.stake,
            currentlySelectedValidators = emptyList()
        )
        setupStakingSharedState.set(reviewValidatorsState)

        router.openSelectedValidators()
    }

    private fun clearStateOnCompletion() {
        invokeOnCompletion {
            setupStakingSharedState.set(SetupStakingProcess.Initial)
        }
    }

    private fun constructUiModel(): ConfirmMultiStakingTypeModel {
        return ConfirmMultiStakingTypeModel(
            stakingTypeValue = resourceManager.getString(R.string.setup_staking_type_direct_staking),
            stakingTypeDetails = TypeDetails(
                label = resourceManager.getString(R.string.staking_recommended_title),
                value = resourceManager.getString(R.string.staking_max_format, selection.validators.size, selection.validatorsLimit),
                icon = null
            )
        )
    }
}
