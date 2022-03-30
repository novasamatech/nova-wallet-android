package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.start

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.retractValidators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

class CustomValidatorsTexts(
    val title: String,
    val badge: String?
)

class StartChangeValidatorsViewModel(
    private val router: StakingRouter,
    private val validatorRecommendatorFactory: ValidatorRecommendatorFactory,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val appLinksProvider: AppLinksProvider,
    private val resourceManager: ResourceManager,
    private val interactor: StakingInteractor,
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val maxValidatorsPerNominator = flowOf {
        interactor.maxValidatorsPerNominator()
    }.share()

    val validatorsLoading = MutableStateFlow(true)

    val customValidatorsTexts = setupStakingSharedState.setupStakingProcess.transform {
        when {
            it is SetupStakingProcess.ReadyToSubmit && it.payload.validators.isNotEmpty() -> emit(
                CustomValidatorsTexts(
                    title = resourceManager.getString(R.string.staking_select_custom),
                    badge = resourceManager.getString(
                        R.string.staking_max_format,
                        it.payload.validators.size,
                        maxValidatorsPerNominator.first()
                    )
                )
            )
            it is SetupStakingProcess.Validators -> emit(
                CustomValidatorsTexts(
                    title = resourceManager.getString(R.string.staking_select_custom),
                    badge = null
                )
            )
        }
    }

    init {
        launch {
            validatorRecommendatorFactory.awaitValidatorLoading(router.currentStackEntryLifecycle)

            validatorsLoading.value = false
        }
    }

    fun goToCustomClicked() {
        router.openSelectCustomValidators()
    }

    fun goToRecommendedClicked() {
        router.openRecommendedValidators()
    }

    fun backClicked() {
        setupStakingSharedState.retractValidators()

        router.back()
    }

    fun recommendedLearnMoreClicked() {
        openBrowserEvent.value = appLinksProvider.recommendedValidatorsLearnMore.event()
    }
}
