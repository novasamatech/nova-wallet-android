package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.types

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.NominationPoolSelection
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.model.ConfirmMultiStakingTypeModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.model.ConfirmMultiStakingTypeModel.TypeDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class PoolsConfirmMultiStakingType(
    private val selection: NominationPoolSelection,
    private val resourceManager: ResourceManager,
    private val poolDisplayFormatter: PoolDisplayFormatter,
    private val parentContext: ConfirmMultiStakingTypeFactory.Context,
) : ConfirmMultiStakingType, CoroutineScope by parentContext.scope {

    override val stakingTypeModel: Flow<ConfirmMultiStakingTypeModel> = flowOf {
        constructUiModel()
    }

    override suspend fun onStakingTypeDetailsClicked() {
        parentContext.externalActions.showAddressActions(
            accountId = selection.pool.stashAccountId,
            chain = selection.stakingOption.chain
        )
    }

    private suspend fun constructUiModel(): ConfirmMultiStakingTypeModel {
        val poolDisplay = poolDisplayFormatter.format(selection.pool, selection.stakingOption.chain)

        return ConfirmMultiStakingTypeModel(
            stakingTypeValue = resourceManager.getString(R.string.setup_staking_type_pool_staking),
            stakingTypeDetails = TypeDetails(
                label = resourceManager.getString(R.string.nomination_pools_pool),
                value = poolDisplay.title,
                icon = poolDisplay.icon
            )
        )
    }
}
