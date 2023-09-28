package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation.NominationPoolRecommenderFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting.SearchNominationPoolInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.asPoolSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.SetupStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapNominationPoolToPoolRvItem
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolRvItem
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.SelectingPoolPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import java.math.BigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SelectPoolViewModel(
    private val router: StakingRouter,
    private val nominationPoolRecommenderFactory: NominationPoolRecommenderFactory,
    private val setupStakingTypeSelectionMixinFactory: SetupStakingTypeSelectionMixinFactory,
    private val payload: SelectingPoolPayload,
    private val resourceManager: ResourceManager,
    private val selectNominationPoolInteractor: SearchNominationPoolInteractor,
    private val chainRegistry: ChainRegistry,
    private val externalActions: ExternalActions.Presentation,
    private val poolDisplayFormatter: PoolDisplayFormatter,
) : BaseViewModel(), ExternalActions by externalActions {

    private val stakingOption = async(Dispatchers.Default) {
        val chainWithAsset = chainRegistry.chainWithAsset(payload.chainId, payload.assetId)
        createStakingOption(chainWithAsset, payload.stakingType)
    }

    private val setupStakingTypeSelectionMixin = setupStakingTypeSelectionMixinFactory.create(viewModelScope)

    private val editableSelection = setupStakingTypeSelectionMixin.editableSelectionFlow

    private val poolsFlow = flowOf { selectNominationPoolInteractor.getSortedNominationPools(stakingOption(), viewModelScope) }

    private val nominationPoolRecommenderFlow = flowOf { nominationPoolRecommenderFactory.create(stakingOption(), viewModelScope) }

    val poolModelsFlow = combine(poolsFlow, editableSelection) { allPools, selection ->
        val selectedPool = selection.selection.asPoolSelection()?.pool
        convertToModels(allPools, selectedPool)
    }
        .withSafeLoading()
        .shareInBackground()

    val selectedTitle = poolsFlow
        .map { resourceManager.getString(R.string.select_custom_pool_active_pools_count, it.size) }
        .shareInBackground()

    val fillWithRecommendedEnabled = combine(nominationPoolRecommenderFlow, editableSelection) { recommender, selection ->
        val selectedPool = selection.selection.asPoolSelection()?.pool
        recommender.recommendedPool().id != selectedPool?.id
    }
        .share()

    fun backClicked() {
        router.back()
    }

    fun poolInfoClicked(poolItem: PoolRvItem) {
        launch {
            externalActions.showExternalActions(
                ExternalActions.Type.Address(poolItem.model.address),
                stakingOption().chain
            )
        }
    }

    fun poolClicked(poolItem: PoolRvItem) {
        launch {
            val pool = getPoolById(poolItem.id) ?: return@launch
            setupStakingTypeSelectionMixin.selectNominationPoolAndApply(pool, stakingOption())
            router.finishSetupPoolFlow()
        }
    }

    fun searchClicked() {
        router.openSearchPool(payload)
    }

    fun selectRecommended() {
        launch {
            val recommendedPool = nominationPoolRecommenderFlow.first().recommendedPool()
            setupStakingTypeSelectionMixin.selectNominationPoolAndApply(recommendedPool, stakingOption())
            router.finishSetupPoolFlow()
        }
    }

    private suspend fun convertToModels(
        pools: List<NominationPool>,
        selectedPool: NominationPool?
    ): List<PoolRvItem> {
        return pools.map { pool ->
            mapNominationPoolToPoolRvItem(
                chain = stakingOption().chain,
                pool = pool,
                resourceManager = resourceManager,
                poolDisplayFormatter = poolDisplayFormatter,
                isChecked = pool.id == selectedPool?.id,
            )
        }
    }

    private suspend fun getPoolById(id: BigInteger): NominationPool? {
        return poolsFlow.first().firstOrNull { it.id.value == id }
    }
}
