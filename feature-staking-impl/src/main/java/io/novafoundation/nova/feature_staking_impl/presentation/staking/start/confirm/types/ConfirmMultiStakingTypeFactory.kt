package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.types

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct.DirectStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.NominationPoolSelection
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import kotlinx.coroutines.CoroutineScope

interface ConfirmMultiStakingTypeFactory {

    class Context(
        val externalActions: ExternalActions.Presentation,
        val scope: CoroutineScope,
    )

    suspend fun constructConfirmMultiStakingType(
        selection: StartMultiStakingSelection,
        parentContext: Context
    ): ConfirmMultiStakingType
}

class RealConfirmMultiStakingTypeFactory(
    private val router: StartMultiStakingRouter,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val resourceManager: ResourceManager,
    private val poolDisplayFormatter: PoolDisplayFormatter,
): ConfirmMultiStakingTypeFactory {

    override suspend fun constructConfirmMultiStakingType(
        selection: StartMultiStakingSelection,
        parentContext: ConfirmMultiStakingTypeFactory.Context
    ): ConfirmMultiStakingType {
        return when(selection) {
            is DirectStakingSelection -> createDirect(selection, parentContext)
            is NominationPoolSelection -> createPool(selection, parentContext)
            else -> error("Unknown staking type")
        }
    }

    private fun createDirect(selection: DirectStakingSelection, parentContext: ConfirmMultiStakingTypeFactory.Context): ConfirmMultiStakingType {
        return DirectConfirmMultiStakingType(
            selection = selection,
            router = router,
            setupStakingSharedState = setupStakingSharedState,
            resourceManager = resourceManager,
            parentContext = parentContext
        )
    }

    private fun createPool(selection: NominationPoolSelection, parentContext: ConfirmMultiStakingTypeFactory.Context): ConfirmMultiStakingType {
        return PoolsConfirmMultiStakingType(
            selection = selection,
            resourceManager = resourceManager,
            poolDisplayFormatter = poolDisplayFormatter,
            parentContext = parentContext
        )
    }
}
