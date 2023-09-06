package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation.NominationPoolRecommenderFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting.SelectingNominationPoolInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.asPoolSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.SetupStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapNominationPoolToPoolRvItem
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolRvItem
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import java.math.BigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SelectCustomPoolViewModel(
    private val router: StakingRouter,
    private val nominationPoolRecommenderFactory: NominationPoolRecommenderFactory,
    private val setupStakingTypeSelectionMixinFactory: SetupStakingTypeSelectionMixinFactory,
    private val addressIconGenerator: AddressIconGenerator,
    private val payload: SelectCustomPoolPayload,
    private val resourceManager: ResourceManager,
    private val selectNominationPoolInteractor: SelectingNominationPoolInteractor,
    private val chainRegistry: ChainRegistry,
    private val externalActions: ExternalActions.Presentation,
) : BaseViewModel(), ExternalActions by externalActions {

    private val stakingOption = async(Dispatchers.Default) {
        val chainWithAsset = chainRegistry.chainWithAsset(payload.chainId, payload.assetId)
        createStakingOption(chainWithAsset, payload.stakingType)
    }

    private val setupStakingTypeSelectionMixin = setupStakingTypeSelectionMixinFactory.create(viewModelScope)

    private val poolsFlow = flowOf { selectNominationPoolInteractor.getSortedNominationPools(stakingOption(), viewModelScope) }

    private val nominationPoolRecommenderFlow = flowOf { nominationPoolRecommenderFactory.create(stakingOption(), viewModelScope) }

    private val selectedPoolFlow = MutableStateFlow<NominationPool?>(null)

    val poolModelsFlow = combine(poolsFlow, selectedPoolFlow) { allPools, selectedPool ->
        convertToModels(allPools, selectedPool)
    }
        .inBackground()
        .share()

    val selectedTitle = poolsFlow
        .map { resourceManager.getString(R.string.select_custom_pool_active_pools_count, it.size) }
        .shareInBackground()

    val fillWithRecommendedEnabled = combine(nominationPoolRecommenderFlow, selectedPoolFlow) { allPools, selectedPool ->
        allPools.recommendedPool().id != selectedPool?.id
    }
        .share()

    init {
        setupStakingTypeSelectionMixin.editableSelectionFlow
            .onEach {
                selectedPoolFlow.value = it.selection.asPoolSelection()?.pool
            }.launchIn(this)
    }

    fun backClicked() {
        router.back()
    }

    fun poolClicked(poolItem: PoolRvItem) {
        launch {
            val pool = getPoolById(poolItem.id) ?: return@launch
            setupStakingTypeSelectionMixin.selectNominationPoolAndApply(pool)
            router.finishSetupPoolFlow()
        }
    }

    fun searchClicked() {
    }

    fun selectRecommended() {
        launch {
            val recommendedPool = nominationPoolRecommenderFlow.first().recommendedPool()
            setupStakingTypeSelectionMixin.selectNominationPoolAndApply(recommendedPool)
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
                iconGenerator = addressIconGenerator,
                resourceManager = resourceManager,
                isChecked = pool.id == selectedPool?.id,
            )
        }
    }

    private suspend fun getPoolById(id: BigInteger): NominationPool? {
        return poolsFlow.first().firstOrNull { it.id.value == id }
    }
}
