package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.KnownNovaPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.novaPoolIdForChain
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.getValidatorsOrEmpty
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.SelectingPoolPayload
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.FORCED_NOVA_POOL_CHAIN_IDS
import io.novafoundation.nova.runtime.ext.isDirectStaking
import io.novafoundation.nova.runtime.ext.isPoolStaking
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope

class SetupStakingTypeFlowExecutorFactory(
    private val router: StakingRouter,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val knownNovaPools: KnownNovaPools,
) {

    fun create(chainId: ChainId, assetId: Int, stakingType: Chain.Asset.StakingType): SetupStakingTypeFlowExecutor {
        return when {
            stakingType.isDirectStaking() -> SetupDirectStakingFlowExecutor(
                router,
                setupStakingSharedState,
                editableSelectionStoreProvider
            )

            stakingType.isPoolStaking() -> SetupPoolStakingFlowExecutor(
                router,
                chainId,
                assetId,
                stakingType,
                knownNovaPools
            )

            else -> throw IllegalArgumentException("Unsupported staking type: $stakingType")
        }
    }
}

interface SetupStakingTypeFlowExecutor {

    suspend fun execute(coroutineScope: CoroutineScope)
}

class SetupDirectStakingFlowExecutor(
    private val router: StakingRouter,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
) : SetupStakingTypeFlowExecutor {

    override suspend fun execute(coroutineScope: CoroutineScope) {
        val selectionStore = editableSelectionStoreProvider.getSelectionStore(coroutineScope)
        setupStakingSharedState.set(
            SetupStakingProcess.ReadyToSubmit(
                activeStake = selectionStore.getCurrentSelection()?.selection?.stake.orZero(),
                newValidators = selectionStore.getValidatorsOrEmpty(),
                selectionMethod = SetupStakingProcess.ReadyToSubmit.SelectionMethod.CUSTOM,
                currentlySelectedValidators = emptyList()
            )
        )
        router.openSelectCustomValidators()
    }
}

class SetupPoolStakingFlowExecutor(
    private val router: StakingRouter,
    private val chainId: ChainId,
    private val assetId: Int,
    private val stakingType: Chain.Asset.StakingType,
    private val knownNovaPools: KnownNovaPools,
) : SetupStakingTypeFlowExecutor {

    override suspend fun execute(coroutineScope: CoroutineScope) {
        val novaPoolId = knownNovaPools.novaPoolIdForChain(chainId)

        // For forced Nova pool chains, the automatic recommendation already selects the Nova pool.
        // Do nothing here to preserve the Automatic selection source, which keeps
        // the "Recommended" label on the amount screen instead of switching to a Manual selection
        // that would display the specific pool name.
        if (chainId in FORCED_NOVA_POOL_CHAIN_IDS && novaPoolId != null) {
            return
        }

        val selectingPoolPayload = SelectingPoolPayload(
            chainId,
            assetId,
            stakingType
        )
        router.openSelectPool(selectingPoolPayload)
    }
}
