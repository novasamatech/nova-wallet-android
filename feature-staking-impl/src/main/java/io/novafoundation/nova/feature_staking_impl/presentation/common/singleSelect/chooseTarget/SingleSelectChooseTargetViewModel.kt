package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.chooseTarget

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.SingleSelectRecommendator
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.recommendations.SingleSelectRecommendatorConfig
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

abstract class SingleSelectChooseTargetViewModel<T, C : SingleSelectRecommendatorConfig<T>>(
    private val router: ReturnableRouter,
    private val recommendatorFactory: SingleSelectRecommendator.Factory<T>,
    private val resourceManager: ResourceManager,
    private val tokenUseCase: TokenUseCase,
    private val selectedAssetState: StakingSharedState,
    private val state: SingleSelectChooseTargetState<T, C>
) : BaseViewModel() {

    private val recommendator by lazyAsync {
        recommendatorFactory.create(selectedAssetState.selectedOption(), computationalScope = this)
    }

    private val recommendationConfigFlow = MutableStateFlow(state.defaultRecommendatorConfig)

    private val isChangedRecommendationConfigFlow = recommendationConfigFlow.map {
        it != state.defaultRecommendatorConfig
    }

    val recommendationSettingsIcon = isChangedRecommendationConfigFlow.map { isChanged ->
        if (isChanged) R.drawable.ic_filter_indicator else R.drawable.ic_filter
    }
        .shareInBackground()

    val clearFiltersEnabled = isChangedRecommendationConfigFlow

    private val shownTargets = recommendationConfigFlow.map { it ->
        ShownStakeTargets(recommendator().recommendations(it), it)
    }.shareInBackground()

    private val tokenFlow = tokenUseCase.currentTokenFlow()
        .inBackground()
        .share()

    val targetModelsFlow = combine(shownTargets, tokenFlow) { shownTargets, token ->
        state.convertTargetsToUi(shownTargets.targets, token, shownTargets.usedConfig)
    }
        .shareInBackground()

    val targetsCount = shownTargets.map {
        resourceManager.getString(R.string.staking_parachain_collators_number_format, it.targets.size)
    }.shareInBackground()

    val scoringHeader = recommendationConfigFlow.map(state::scoringHeaderFor)
        .shareInBackground()

    val searchVisible = state.searchAction != null

    init {
        listenRecommendationConfigChanges()
    }

    protected abstract fun settingsClicked(currentConfig: C)

    protected abstract suspend fun targetInfoClicked(target: T)

    protected abstract suspend fun targetSelected(target: T)

    fun clearFiltersClicked() {
        recommendationConfigFlow.value = state.defaultRecommendatorConfig
    }

    fun backClicked() {
        router.back()
    }

    fun targetInfoClicked(stakeTargetModel: StakeTargetModel<T>) = launchUnit {
        targetInfoClicked(stakeTargetModel.stakeTarget)
    }

    fun targetClicked(stakeTargetModel: StakeTargetModel<T>) = launchUnit {
        targetSelected(stakeTargetModel.stakeTarget)
    }

    fun settingsClicked() = launch {
        settingsClicked(recommendationConfigFlow.value)
    }

    fun searchClicked() {
        state.searchAction?.invoke()
    }

    private fun listenRecommendationConfigChanges() {
        state.recommendationConfigChanges()
            .onEach { recommendationConfigFlow.value = it }
            .inBackground()
            .launchIn(this)
    }

    interface SingleSelectChooseTargetState<T, C> {
        val defaultRecommendatorConfig: C

        val searchAction: SearchAction?

        suspend fun convertTargetsToUi(
            targets: List<T>,
            token: Token,
            config: C
        ): List<StakeTargetModel<T>>

        fun scoringHeaderFor(config: C): String

        fun recommendationConfigChanges(): Flow<C>

    }

    private inner class ShownStakeTargets(val targets: List<T>, val usedConfig: C)
}

typealias SearchAction = () -> Unit
