package io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.pool

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display.PoolDisplayUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PoolRewardDetailViewModel(
    val operation: OperationParcelizeModel.PoolReward,
    private val poolDisplayUseCase: PoolDisplayUseCase,
    private val router: AssetsRouter,
    private val chainRegistry: ChainRegistry,
    private val externalActions: ExternalActions.Presentation
) : BaseViewModel(),
    ExternalActions by externalActions {

    val chain by lazyAsync {
        chainRegistry.getChain(operation.chainId)
    }

    val poolDisplayFlow = flowOf {
        poolDisplayUseCase.getPoolDisplay(operation.poolId, chain())
    }
        .shareInBackground()

    val chainUi = flowOf {
        mapChainToUi(chain())
    }
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun transactionIdClicked() {
        operation.extrinsicHash?.let { hash ->
            shoExternalActions(ExternalActions.Type.Extrinsic(hash))
        }

    }

    fun poolClicked() = launch {
        val poolStash = poolDisplayFlow.first().poolAccountId

        externalActions.showAddressActions(poolStash, chain())
    }

    private fun shoExternalActions(type: ExternalActions.Type) = launch {
        externalActions.showExternalActions(type, chain())
    }
}
