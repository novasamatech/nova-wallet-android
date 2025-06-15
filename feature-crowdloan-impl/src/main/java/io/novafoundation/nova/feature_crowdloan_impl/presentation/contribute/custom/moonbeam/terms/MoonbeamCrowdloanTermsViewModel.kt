package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.terms

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamCrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.custom.moonbeam.MoonbeamTermsPayload
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.custom.moonbeam.MoonbeamTermsValidationSystem
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.mapParachainMetadataFromParcel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.getCurrentAsset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class SubmitActionState {
    object Loading : SubmitActionState()

    object Available : SubmitActionState()

    class Unavailable(val reason: String) : SubmitActionState()
}

class TermsLinkContent(
    val title: String,
    val iconUrl: String,
)

class MoonbeamCrowdloanTermsViewModel(
    private val interactor: MoonbeamCrowdloanInteractor,
    val payload: ContributePayload,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val resourceManager: ResourceManager,
    private val router: CrowdloanRouter,
    private val assetUseCase: AssetUseCase,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: MoonbeamTermsValidationSystem,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
) : BaseViewModel(),
    FeeLoaderMixin by feeLoaderMixin,
    Validatable by validationExecutor,
    Browserable,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    init {
        loadFee()
    }

    val termsLinkContent = TermsLinkContent(
        title = resourceManager.getString(R.string.crowdloan_terms_conditions_named, payload.parachainMetadata!!.name),
        iconUrl = payload.parachainMetadata.iconLink
    )

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val termsCheckedFlow = MutableStateFlow(false)

    private val submittingInProgressFlow = MutableStateFlow(false)

    private val parachainMetadata = flowOf { mapParachainMetadataFromParcel(payload.parachainMetadata!!) }
        .inBackground()
        .share()

    val submitButtonState = combine(
        termsCheckedFlow,
        submittingInProgressFlow
    ) { termsChecked, submitInProgress ->
        when {
            submitInProgress -> SubmitActionState.Loading
            termsChecked -> SubmitActionState.Available
            else -> SubmitActionState.Unavailable(
                reason = resourceManager.getString(R.string.crowdloan_agree_with_policy)
            )
        }
    }

    fun backClicked() {
        router.back()
    }

    fun termsLinkClicked() {
        openBrowserEvent.value = Event(interactor.getTermsLink())
    }

    fun submitClicked() = launch {
        submittingInProgressFlow.value = true

        val fee = feeLoaderMixin.awaitFee()
        submitAfterValidation(fee)
    }

    private fun submitAfterValidation(fee: Fee) = launch {
        val validationPayload = MoonbeamTermsPayload(
            fee = fee,
            asset = assetUseCase.getCurrentAsset()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = validationPayload,
            validationFailureTransformer = { moonbeamTermsValidationFailure(it, resourceManager) },
            progressConsumer = submittingInProgressFlow.progressConsumer()
        ) {
            submit()
        }
    }

    private fun submit() = launch {
        interactor.submitAgreement(parachainMetadata.first())
            .onFailure(::showError)
            .onSuccess {
                startNavigation(it.submissionHierarchy) { router.openContribute(payload) }
            }

        submittingInProgressFlow.value = false
    }

    private fun loadFee() = launch {
        feeLoaderMixin.loadFee(
            coroutineScope = this,
            feeConstructor = { interactor.calculateTermsFee() },
            onRetryCancelled = ::backClicked
        )
    }
}
