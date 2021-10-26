package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute

import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.OnChainSubmission
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import java.math.BigDecimal

fun additionalOnChainSubmission(
    bonusPayload: BonusPayload?,
    crowdloan: Crowdloan,
    amount: BigDecimal,
    factory: CustomContributeFactory,
): OnChainSubmission {
    val submitter = factory.submitter

    return {
        submitter.submitOnChain(crowdloan, bonusPayload, amount, this)
    }
}
