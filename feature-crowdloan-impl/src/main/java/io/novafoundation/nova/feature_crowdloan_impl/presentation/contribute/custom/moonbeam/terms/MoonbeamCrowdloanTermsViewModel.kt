package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.terms

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withFlagSet
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamCrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.mapParachainMetadataFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
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
) : BaseViewModel(), FeeLoaderMixin by feeLoaderMixin, Browserable {

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

    fun submitClicked() = launch {
        submittingInProgressFlow.withFlagSet {
            interactor.submitAgreement(parachainMetadata.first())
                .onFailure(::showError)
                .onSuccess {
                    router.openContribute(payload)
                }
        }
    }

    private fun loadFee() = launch {
        feeLoaderMixin.loadFee(
            coroutineScope = this,
            feeConstructor = {
                interactor.calculateTermsFee()
            },
            onRetryCancelled = ::backClicked
        )
    }

    fun termsLinkClicked() {
        openBrowserEvent.value = Event(interactor.getTermsLink())
    }
}
