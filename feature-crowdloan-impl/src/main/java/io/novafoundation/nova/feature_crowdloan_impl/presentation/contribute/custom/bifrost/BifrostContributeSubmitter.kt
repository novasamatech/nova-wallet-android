package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.bifrost

import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.bifrost.BifrostContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

class BifrostContributeSubmitter(
    private val interactor: BifrostContributeInteractor
) : CustomContributeSubmitter {

    override suspend fun submitOnChain(
        crowdloan: Crowdloan,
        payload: BonusPayload?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) {
        require(payload is BifrostBonusPayload?)

        payload?.let {
            interactor.submitOnChain(payload.parachainId, payload.referralCode, extrinsicBuilder)
        }
    }
}
