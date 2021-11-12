package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.bifrost

import android.content.Context
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_crowdloan_impl.BuildConfig
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.ExtraBonusFlow
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.bifrost.BifrostContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.bifrost.BifrostContributeSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.bifrost.BifrostContributeViewState
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral.ReferralContributeView
import kotlinx.coroutines.CoroutineScope

private val BIFROST_BONUS_MULTIPLIER = 0.05.toBigDecimal() // 5%

class BifrostExtraBonusFlow(
    private val interactor: BifrostContributeInteractor,
    private val resourceManager: ResourceManager,
    private val termsLink: String = BuildConfig.BIFROST_TERMS_LINKS,
) : ExtraBonusFlow {

    override fun createViewState(scope: CoroutineScope, payload: CustomContributePayload): BifrostContributeViewState {
        return BifrostContributeViewState(
            interactor = interactor,
            customContributePayload = payload,
            resourceManager = resourceManager,
            termsLink = termsLink,
            bonusPercentage = BIFROST_BONUS_MULTIPLIER,
            bifrostInteractor = interactor
        )
    }

    override fun createView(context: Context) = ReferralContributeView(context)
}

class BifrostContributeFactory(
    override val submitter: BifrostContributeSubmitter,
    override val extraBonusFlow: BifrostExtraBonusFlow,
) : CustomContributeFactory {

    override val flowType = "Bifrost"
}
