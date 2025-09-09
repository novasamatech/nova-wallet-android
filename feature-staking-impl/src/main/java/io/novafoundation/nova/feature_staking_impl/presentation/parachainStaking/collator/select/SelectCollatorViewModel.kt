package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendationConfig
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.chooseTarget.SearchAction
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.chooseTarget.SingleSelectChooseTargetViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator.Response
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenResponder
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.details.parachain
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorToCollatorParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenRequester
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.mapCollatorRecommendationConfigFromParcel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.mapCollatorRecommendationConfigToParcel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.mapCollatorToCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.mapCollatorToDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SelectCollatorViewModel(
    private val router: ParachainStakingRouter,
    private val selectCollatorInterScreenResponder: SelectCollatorInterScreenResponder,
    private val selectCollatorSettingsInterScreenRequester: SelectCollatorSettingsInterScreenRequester,
    private val collatorsUseCase: CollatorsUseCase,
    private val amountFormatter: AmountFormatter,
    collatorRecommendatorFactory: CollatorRecommendatorFactory,
    addressIconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
    tokenUseCase: TokenUseCase,
    selectedAssetState: StakingSharedState,
) : SingleSelectChooseTargetViewModel<Collator, CollatorRecommendationConfig>(
    router = router,
    recommendatorFactory = collatorRecommendatorFactory,
    resourceManager = resourceManager,
    tokenUseCase = tokenUseCase,
    selectedAssetState = selectedAssetState,
    state = CollatorState(
        router = router,
        selectCollatorSettingsInterScreenRequester = selectCollatorSettingsInterScreenRequester,
        addressIconGenerator = addressIconGenerator,
        resourceManager = resourceManager,
        selectedAssetState = selectedAssetState,
        amountFormatter = amountFormatter
    )
) {

    class CollatorState(
        private val router: ParachainStakingRouter,
        private val selectCollatorSettingsInterScreenRequester: SelectCollatorSettingsInterScreenRequester,
        private val addressIconGenerator: AddressIconGenerator,
        private val resourceManager: ResourceManager,
        private val selectedAssetState: StakingSharedState,
        private val amountFormatter: AmountFormatter,
    ) : SingleSelectChooseTargetState<Collator, CollatorRecommendationConfig> {

        override val defaultRecommendatorConfig: CollatorRecommendationConfig = CollatorRecommendationConfig.DEFAULT

        override val searchAction: SearchAction = {
            router.openSearchCollator()
        }

        override fun recommendationConfigChanges(): Flow<CollatorRecommendationConfig> {
            return selectCollatorSettingsInterScreenRequester.responseFlow
                .map { mapCollatorRecommendationConfigFromParcel(it.newConfig) }
        }

        override fun scoringHeaderFor(config: CollatorRecommendationConfig): String {
            return when (config.sorting) {
                CollatorSorting.REWARDS -> resourceManager.getString(R.string.staking_rewards)
                CollatorSorting.MIN_STAKE -> resourceManager.getString(R.string.staking_main_minimum_stake_title)
                CollatorSorting.TOTAL_STAKE -> resourceManager.getString(R.string.staking_validator_total_stake)
                CollatorSorting.OWN_STAKE -> resourceManager.getString(R.string.staking_parachain_collator_own_stake)
            }
        }

        override suspend fun convertTargetsToUi(
            targets: List<Collator>,
            token: Token,
            config: CollatorRecommendationConfig,
        ): List<StakeTargetModel<Collator>> {
            return targets.map { collator ->
                mapCollatorToCollatorModel(
                    chain = selectedAssetState.chain(),
                    collator = collator,
                    addressIconGenerator = addressIconGenerator,
                    sorting = config.sorting,
                    resourceManager = resourceManager,
                    token = token,
                    amountFormatter = amountFormatter
                )
            }
        }
    }

    override fun settingsClicked(currentConfig: CollatorRecommendationConfig) {
        val configPayload = mapCollatorRecommendationConfigToParcel(currentConfig)
        selectCollatorSettingsInterScreenRequester.openRequest(Request((configPayload)))
    }

    override suspend fun targetSelected(target: Collator) {
        val response = Response(mapCollatorToCollatorParcelModel(target))

        selectCollatorInterScreenResponder.respond(response)
        router.returnToStartStaking()
    }

    override suspend fun targetInfoClicked(target: Collator) {
        val payload = withContext(Dispatchers.Default) {
            val parcel = mapCollatorToDetailsParcelModel(target)

            StakeTargetDetailsPayload.parachain(parcel, collatorsUseCase)
        }

        router.openCollatorDetails(payload)
    }
}
