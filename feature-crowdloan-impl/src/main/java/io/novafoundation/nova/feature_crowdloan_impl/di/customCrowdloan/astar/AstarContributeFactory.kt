package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.astar

import android.content.Context
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.ExtraBonusFlow
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.astar.AstarContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.astar.AstarContributeSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.astar.AstarContributeViewState
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral.ReferralContributeView
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal

private const val ASTAR_TERMS_LINK = "https://docs.google.com/document/d/1vKZrDqSdh706hg0cqJ_NnxfRSlXR2EThVHwoRl0nAkk"
private const val NOVA_REFERRAL_CODE = "1ChFWeNRLarAPRCTM3bfJmncJbSAbSS9yqjueWz7jX7iTVZ"
private val ASTAR_BONUS = 0.01.toBigDecimal() // 1%

class AstarExtraBonusFlow(
    private val interactor: AstarContributeInteractor,
    private val resourceManager: ResourceManager,
    private val termsLink: String = ASTAR_TERMS_LINK,
    private val novaReferralCode: String = NOVA_REFERRAL_CODE,
    private val bonusPercentage: BigDecimal = ASTAR_BONUS,
) : ExtraBonusFlow {

    override fun createViewState(scope: CoroutineScope, payload: CustomContributePayload): AstarContributeViewState {
        return AstarContributeViewState(
            interactor = interactor,
            customContributePayload = payload,
            resourceManager = resourceManager,
            defaultReferralCode = novaReferralCode,
            bonusPercentage = bonusPercentage,
            termsLink = termsLink
        )
    }

    override fun createView(context: Context) = ReferralContributeView(context)
}

class AstarContributeFactory(
    override val submitter: AstarContributeSubmitter,
    override val extraBonusFlow: AstarExtraBonusFlow,
) : CustomContributeFactory {

    override val flowType = "Astar"
}
