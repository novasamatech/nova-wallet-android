@file:OptIn(ExperimentalTime::class)

package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.search

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.validators.current.search.SearchCustomValidatorsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.common.search.SearchStakeTargetViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapValidatorToValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.setCustomValidators
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

class SearchCustomValidatorsViewModel(
    private val router: StakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val interactor: SearchCustomValidatorsInteractor,
    resourceManager: ResourceManager,
    private val sharedStateSetup: SetupStakingSharedState,
    private val validatorRecommendatorFactory: ValidatorRecommendatorFactory,
    private val singleAssetSharedState: SingleAssetSharedState,
    tokenUseCase: TokenUseCase,
) : SearchStakeTargetViewModel<Validator>(resourceManager) {

    private val confirmSetupState = sharedStateSetup.setupStakingProcess
        .filterIsInstance<SetupStakingProcess.ReadyToSubmit>()
        .share()

    private val selectedValidators = confirmSetupState
        .map { it.payload.validators.toSet() }
        .inBackground()
        .share()

    private val currentTokenFlow = tokenUseCase.currentTokenFlow()
        .share()

    private val allElectedValidators by lazy {
        async { validatorRecommendatorFactory.create(router.currentStackEntryLifecycle).availableValidators.toSet() }
    }

    private val foundValidatorsState = enteredQuery
        .mapLatest {
            if (it.isNotEmpty()) {
                interactor.searchValidator(it, allElectedValidators() + selectedValidators.first())
            } else {
                null
            }
        }
        .inBackground()
        .share()

    override val dataFlow = combine(
        selectedValidators,
        foundValidatorsState,
        currentTokenFlow
    ) { selectedValidators, foundValidators, token ->
        val chain = singleAssetSharedState.chain()

        foundValidators?.map { validator ->
            mapValidatorToValidatorModel(
                chain = chain,
                validator = validator,
                iconGenerator = addressIconGenerator,
                token = token,
                isChecked = validator in selectedValidators
            )
        }
    }

    override fun itemClicked(item: ValidatorModel) {
        if (item.stakeTarget.prefs!!.blocked) {
            showError(resourceManager.getString(R.string.staking_custom_blocked_warning))
            return
        }

        launch {
            val newSelected = selectedValidators.first().toggle(item.stakeTarget)

            sharedStateSetup.setCustomValidators(newSelected.toList())
        }
    }

    override fun itemInfoClicked(item: ValidatorModel) {
        router.openValidatorDetails(mapValidatorToValidatorDetailsParcelModel(item.stakeTarget))
    }

    override fun backClicked() {
        router.back()
    }

    fun doneClicked() {
        router.back()
    }
}
