package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.cycle
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendationConfig
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.CollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.mapCollatorToCollatorModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SelectCollatorViewModel(
    private val router: StakingRouter,
    private val collatorRecommendatorFactory: CollatorRecommendatorFactory,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val tokenUseCase: TokenUseCase,
    private val selectedAssetState: SingleAssetSharedState,
) : BaseViewModel() {

    private val collatorRecommendator by lazyAsync {
        collatorRecommendatorFactory.create(router.currentStackEntryLifecycle, selectedAssetState.chainId())
    }

    // TODO sorting screen
    private val sortingIterator = CollatorSorting.values().toList().cycle().iterator()

    // TODO sorting screen
    private val recommendationConfigFlow = MutableStateFlow(CollatorRecommendationConfig(CollatorSorting.REWARDS))

    val recommendationSettingsIcon = recommendationConfigFlow.map {
        val isChanged = it != CollatorRecommendationConfig.DEFAULT

        if (isChanged) R.drawable.ic_filter_indicator else R.drawable.ic_filter
    }
        .shareInBackground()

    private val shownValidators = recommendationConfigFlow.map {
        collatorRecommendator().recommendations(it)
    }.shareInBackground()

    private val tokenFlow = tokenUseCase.currentTokenFlow()
        .inBackground()
        .share()

    val validatorModelsFlow = combine(shownValidators, tokenFlow, ::convertToModels)
        .shareInBackground()

    val collatorsTitle = shownValidators.map {
        resourceManager.getString(R.string.staking_parachain_collators_number_format, it.size)
    }.shareInBackground()

    val scoringHeader = recommendationConfigFlow.map {
        when (it.sorting) {
            CollatorSorting.REWARDS -> resourceManager.getString(R.string.staking_rewards)
            CollatorSorting.MIN_STAKE -> resourceManager.getString(R.string.staking_main_minimum_stake_title)
            CollatorSorting.TOTAL_STAKE -> resourceManager.getString(R.string.staking_validator_total_stake)
            CollatorSorting.OWN_STAKE -> resourceManager.getString(R.string.staking_parachain_collator_own_stake)
        }
    }.shareInBackground()


    fun clearFiltersClicked() = launch {
        recommendationConfigFlow.value  = CollatorRecommendationConfig(sortingIterator.next())
    }

    fun backClicked() {
        router.back()
    }

    fun collatorInfoClicked(collatorModel: CollatorModel) {
        showMessage("TODO show collator info")
    }

    fun collatorClicked(collatorModel: CollatorModel) {
        showMessage("TODO collator clicked")
    }

    fun settingsClicked() {
        showMessage("TODO show settings")
    }

    fun searchClicked() {
        showMessage("TODO show search")
    }

    private suspend fun convertToModels(
        collators: List<Collator>,
        token: Token,
    ): List<CollatorModel> {
        return collators.map { collator ->
            mapCollatorToCollatorModel(
                chain = selectedAssetState.chain(),
                collator = collator,
                addressIconGenerator = addressIconGenerator,
                sorting = recommendationConfigFlow.first().sorting,
                resourceManager = resourceManager,
                token = token
            )
        }
    }
}
