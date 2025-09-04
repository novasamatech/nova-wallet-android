package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam

import android.os.Parcelable
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamCrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

class MoonbeamCrowdloanSubmitter(
    private val interactor: MoonbeamCrowdloanInteractor,
) : CustomContributeSubmitter {

    override suspend fun injectOnChainSubmission(
        crowdloan: Crowdloan,
        customizationPayload: Parcelable?,
        bonusPayload: BonusPayload?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) {
        interactor.additionalSubmission(crowdloan, extrinsicBuilder)
    }

    override suspend fun submitOffChain(
        customizationPayload: Parcelable?,
        bonusPayload: BonusPayload?,
        amount: BigDecimal,
    ) {
        // Do nothing
    }
}
