package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendationConfig
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator.Response
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenResponder
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.details.parachain
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorToCollatorParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenRequester
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.mapCollatorRecommendationConfigFromParcel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.mapCollatorRecommendationConfigToParcel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.CollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.mapCollatorToCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.mapCollatorToDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectCollatorViewModel(
    private val router: ParachainStakingRouter,
    private val selectCollatorInterScreenResponder: SelectCollatorInterScreenResponder,
    private val selectCollatorSettingsInterScreenRequester: SelectCollatorSettingsInterScreenRequester,
    private val collatorsUseCase: CollatorsUseCase,
    private val collatorRecommendatorFactory: CollatorRecommendatorFactory,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val tokenUseCase: TokenUseCase,
    private val selectedAssetState: StakingSharedState,
) : BaseViewModel() {

    private val collatorRecommendator by lazyAsync {
        collatorRecommendatorFactory.create(selectedAssetState.selectedOption(), computationalScope = this)
    }

    private val recommendationConfigFlow = MutableStateFlow(defaultConfig())

    val recommendationSettingsIcon = recommendationConfigFlow.map {
        val isChanged = it != CollatorRecommendationConfig.DEFAULT

        if (isChanged) R.drawable.ic_filter_indicator else R.drawable.ic_filter
    }
        .shareInBackground()

    val clearFiltersEnabled = recommendationConfigFlow.map {
        it != defaultConfig()
    }.shareInBackground()

    private val shownValidators = recommendationConfigFlow.map {
        collatorRecommendator().recommendations(it)
    }.shareInBackground()

    private val tokenFlow = tokenUseCase.currentTokenFlow()
        .inBackground()
        .share()

    val collatorModelsFlow = combine(shownValidators, tokenFlow, ::convertToModels)
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

    init {
        listenRecommendationConfigChanges()
    }

    fun clearFiltersClicked() {
        recommendationConfigFlow.value = defaultConfig()
    }

    fun backClicked() {
        router.back()
    }

    fun collatorInfoClicked(collatorModel: CollatorModel) {
        launch {
            val payload = withContext(Dispatchers.Default) {
                val parcel = mapCollatorToDetailsParcelModel(collatorModel.stakeTarget)

                StakeTargetDetailsPayload.parachain(parcel, collatorsUseCase)
            }

            router.openCollatorDetails(payload)
        }
    }

    fun collatorClicked(collatorModel: CollatorModel) = launch {
        val response = withContext(Dispatchers.Default) {
            Response(mapCollatorToCollatorParcelModel(collatorModel.stakeTarget))
        }

        selectCollatorInterScreenResponder.respond(response)
        router.returnToStartStaking()
    }

    fun settingsClicked() = launch {
        val currentConfig = mapCollatorRecommendationConfigToParcel(recommendationConfigFlow.first())

        selectCollatorSettingsInterScreenRequester.openRequest(Request((currentConfig)))
    }

    fun searchClicked() {
        router.openSearchCollator()
    }

    private fun listenRecommendationConfigChanges() {
        selectCollatorSettingsInterScreenRequester.responseFlow
            .map { mapCollatorRecommendationConfigFromParcel(it.newConfig) }
            .onEach { recommendationConfigFlow.value = it }
            .inBackground()
            .launchIn(this)
    }

    private fun defaultConfig() = CollatorRecommendationConfig(CollatorSorting.REWARDS)

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
