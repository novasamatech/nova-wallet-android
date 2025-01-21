package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorRecommendationConfig
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.chooseTarget.SearchAction
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.chooseTarget.SingleSelectChooseTargetViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.SelectMythosInterScreenResponder
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.MythosCollatorFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.details.mythos
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model.toParcelable
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.SelectMythCollatorSettingsInterScreenRequester
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.model.toDomain
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.model.toParcel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SelectMythosCollatorViewModel(
    private val router: MythosStakingRouter,
    private val recommendatorFactory: MythosCollatorRecommendatorFactory,
    private val resourceManager: ResourceManager,
    private val tokenUseCase: TokenUseCase,
    private val selectedAssetState: StakingSharedState,
    private val mythosCollatorFormatter: MythosCollatorFormatter,
    private val selectCollatorResponder: SelectMythosInterScreenResponder,
    private val settingsRequester: SelectMythCollatorSettingsInterScreenRequester
) : SingleSelectChooseTargetViewModel<MythosCollator, MythosCollatorRecommendationConfig>(
    router = router,
    recommendatorFactory = recommendatorFactory,
    resourceManager = resourceManager,
    tokenUseCase = tokenUseCase,
    selectedAssetState = selectedAssetState,
    state = MythosState(
        mythosCollatorFormatter = mythosCollatorFormatter,
        resourceManager = resourceManager,
        settingsRequester = settingsRequester
    )
) {

    override fun settingsClicked(currentConfig: MythosCollatorRecommendationConfig) {
        settingsRequester.openRequest(currentConfig.toParcel())
    }

    override suspend fun targetInfoClicked(target: MythosCollator) {
        val payload = StakeTargetDetailsPayload.mythos(target)
        router.openCollatorDetails(payload)
    }

    override suspend fun targetSelected(target: MythosCollator) {
        selectCollatorResponder.respond(target.toParcelable())
        router.returnToStartStaking()
    }

    class MythosState(
        private val mythosCollatorFormatter: MythosCollatorFormatter,
        private val resourceManager: ResourceManager,
        private val settingsRequester: SelectMythCollatorSettingsInterScreenRequester
    ) : SingleSelectChooseTargetState<MythosCollator, MythosCollatorRecommendationConfig> {

        override val defaultRecommendatorConfig: MythosCollatorRecommendationConfig = MythosCollatorRecommendationConfig.DEFAULT

        override val searchAction: SearchAction? = null

        override suspend fun convertTargetsToUi(
            targets: List<MythosCollator>,
            token: Token,
            config: MythosCollatorRecommendationConfig
        ): List<StakeTargetModel<MythosCollator>> {
            return targets.map {
                mythosCollatorFormatter.collatorToUi(it, token, config)
            }
        }

        override fun scoringHeaderFor(config: MythosCollatorRecommendationConfig): String {
            return when (config.sorting) {
                MythosCollatorSorting.REWARDS -> resourceManager.getString(R.string.staking_rewards)
                MythosCollatorSorting.TOTAL_STAKE -> resourceManager.getString(R.string.staking_validator_total_stake)
            }
        }

        override fun recommendationConfigChanges(): Flow<MythosCollatorRecommendationConfig> {
            return settingsRequester.responseFlow
                .map { it.toDomain() }
        }
    }
}
