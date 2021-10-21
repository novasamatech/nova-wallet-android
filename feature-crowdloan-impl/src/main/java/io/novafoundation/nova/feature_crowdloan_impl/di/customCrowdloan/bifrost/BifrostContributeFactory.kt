package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.bifrost

import android.content.Context
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.bifrost.BifrostContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.bifrost.BifrostContributeSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.bifrost.BifrostContributeViewState
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral.ReferralContributeView
import kotlinx.coroutines.CoroutineScope

class BifrostContributeFactory(
    override val submitter: BifrostContributeSubmitter,
    private val interactor: BifrostContributeInteractor,
    private val resourceManager: ResourceManager,
    private val termsLink: String
) : CustomContributeFactory {

    override val flowType = "Bifrost"

    override fun createViewState(scope: CoroutineScope, payload: CustomContributePayload): BifrostContributeViewState {
        return BifrostContributeViewState(interactor, payload, resourceManager, termsLink, interactor)
    }

    override fun createView(context: Context) = ReferralContributeView(context)
}
