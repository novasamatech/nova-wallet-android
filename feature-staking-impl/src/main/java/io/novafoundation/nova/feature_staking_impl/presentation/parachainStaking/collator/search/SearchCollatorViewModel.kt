@file:OptIn(ExperimentalTime::class)

package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.search

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.search.SearchCollatorsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendationConfig
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.search.SearchStakeTargetViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenResponder
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorToCollatorParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.mapCollatorToCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

class SearchCollatorViewModel(
    private val router: ParachainStakingRouter,
    private val interactor: SearchCollatorsInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectCollatorInterScreenResponder: SelectCollatorInterScreenResponder,
    private val collatorRecommendatorFactory: CollatorRecommendatorFactory,
    private val singleAssetSharedState: SingleAssetSharedState,
    resourceManager: ResourceManager,
    tokenUseCase: TokenUseCase,
) : SearchStakeTargetViewModel<Collator>(resourceManager) {

    private val currentTokenFlow = tokenUseCase.currentTokenFlow()
        .share()

    private val electedCollators by lazyAsync {
        val chainId = singleAssetSharedState.chainId()

        collatorRecommendatorFactory.create(router.currentStackEntryLifecycle, chainId)
            .recommendations(CollatorRecommendationConfig.DEFAULT)
    }

    private val foundCollatorsFlow = enteredQuery
        .mapLatest {
            if (it.isNotEmpty()) {
                interactor.searchValidator(it, electedCollators())
            } else {
                null
            }
        }
        .shareInBackground()

    override val dataFlow = combine(
        foundCollatorsFlow,
        currentTokenFlow
    ) { foundCollators, token ->
        val chain = singleAssetSharedState.chain()

        foundCollators?.map { collator ->
            mapCollatorToCollatorModel(
                chain = chain,
                collator = collator,
                token = token,
                addressIconGenerator = addressIconGenerator,
                resourceManager = resourceManager,
                sorting = CollatorRecommendationConfig.DEFAULT.sorting
            )
        }
    }

    override fun itemClicked(item: StakeTargetModel<Collator>) {
        launch {
            val response = withContext(Dispatchers.Default) {
                val parcel = mapCollatorToCollatorParcelModel(item.stakeTarget)

                SelectCollatorInterScreenCommunicator.Response(parcel)
            }

            selectCollatorInterScreenResponder.respond(response)
            router.returnToStartStaking()
        }
    }

    override fun itemInfoClicked(item: StakeTargetModel<Collator>) {
        showMessage("TODO show collator info")
    }

    override fun backClicked() {
        router.back()
    }
}
