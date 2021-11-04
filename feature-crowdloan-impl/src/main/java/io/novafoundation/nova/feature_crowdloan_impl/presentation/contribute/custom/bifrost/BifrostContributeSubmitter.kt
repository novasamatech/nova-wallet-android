package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.bifrost

import android.os.Parcelable
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.bifrost.BifrostContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

class BifrostContributeSubmitter(
    private val interactor: BifrostContributeInteractor
) : CustomContributeSubmitter {

    override suspend fun injectOnChainSubmission(
        crowdloan: Crowdloan,
        customizationPayload: Parcelable?,
        bonusPayload: BonusPayload?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) {
        require(bonusPayload is BifrostBonusPayload?)

        bonusPayload?.let {
            interactor.submitOnChain(crowdloan.parachainId, bonusPayload.referralCode, extrinsicBuilder)
        }
    }

    override suspend fun submitOffChain(
        customizationPayload: Parcelable?,
        bonusPayload: BonusPayload?,
        amount: BigDecimal,
    ) {
        // Do nothing
    }
}
