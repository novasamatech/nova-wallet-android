package io.novafoundation.nova.feature_staking_impl.presentation.pools.searchPool

import android.text.TextUtils
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.PlaceholderModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting.PoolAvailabilityPayload
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SearchPoolViewModel(
    private val router: StakingRouter,
    private val setupStakingTypeSelectionMixinFactory: SetupStakingTypeSelectionMixinFactory,
    private val payload: SelectingPoolPayload,
    private val resourceManager: ResourceManager,
    private val selectNominationPoolInteractor: SearchNominationPoolInteractor,
    private val chainRegistry: ChainRegistry,
    private val externalActions: ExternalActions.Presentation,
    private val poolDisplayFormatter: PoolDisplayFormatter,
    private val validationExecutor: ValidationExecutor,
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    val query = MutableStateFlow("")

    private val stakingOption = async(Dispatchers.Default) {
        val chainWithAsset = chainRegistry.chainWithAsset(payload.chainId, payload.assetId)
        createStakingOption(chainWithAsset, payload.stakingType)
    }

    private val setupStakingTypeSelectionMixin = setupStakingTypeSelectionMixinFactory.create(viewModelScope)

    private val editableSelection = setupStakingTypeSelectionMixin.editableSelectionFlow

    private val poolsFlow = flowOfAll { selectNominationPoolInteractor.searchNominationPools(query, stakingOption(), viewModelScope) }

    val poolModelsFlow = combine(poolsFlow, editableSelection) { pools, selection ->
        val selectedPool = selection.selection.asPoolSelection()?.pool
        convertToModels(pools, selectedPool)
    }
        .inBackground()
        .share()

    val selectedTitle = poolsFlow
        .map { resourceManager.getString(R.string.select_custom_pool_active_pools_count, it.size) }
        .shareInBackground()

    val placeholderFlow = combine(query, poolModelsFlow) { searchRaw, pools ->
        mapToPlaceholderModel(searchRaw, pools)
    }
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun poolInfoClicked(poolItem: PoolRvItem) {
        launch {
            externalActions.showAddressActions(
                poolItem.model.address,
                stakingOption().chain
            )
        }
    }

    fun poolClicked(poolItem: PoolRvItem) {
        launch {
            val pool = getPoolById(poolItem.id) ?: return@launch

            val validationSystem = selectNominationPoolInteractor.getValidationSystem()
            val payload = PoolAvailabilityPayload(pool, stakingOption().chain)

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { handleSelectPoolValidationFailure(it, resourceManager) },
            ) {
                finishSetupPoolFlow(pool)
            }
        }
    }

    private fun finishSetupPoolFlow(pool: NominationPool) {
        launch {
            setupStakingTypeSelectionMixin.selectNominationPoolAndApply(pool, stakingOption())
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

    private fun mapToPlaceholderModel(
        searchRaw: String,
        pools: List<PoolRvItem>
    ): PlaceholderModel? {
        return when {
            TextUtils.isEmpty(searchRaw) -> {
                PlaceholderModel(resourceManager.getString(R.string.common_search_placeholder_default), R.drawable.ic_placeholder)
            }

            pools.isEmpty() -> {
                PlaceholderModel(resourceManager.getString(R.string.search_pool_no_pools_found_placeholder), R.drawable.ic_planet_outline)
            }

            else -> null
        }
    }
}
