package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam

import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamCrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

class MoonbeamCrowdloanSubmitter(
    private val interactor: MoonbeamCrowdloanInteractor,
) : CustomContributeSubmitter {

    override suspend fun submitOnChain(
        crowdloan: Crowdloan,
        payload: BonusPayload?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) {
        interactor.additionalSubmission(crowdloan, extrinsicBuilder)
    }
}
