package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom

import android.os.Parcelable
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

interface CustomContributeSubmitter {

    suspend fun injectOnChainSubmission(
        crowdloan: Crowdloan,
        customizationPayload: Parcelable?,
        bonusPayload: BonusPayload?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    )

    suspend fun injectFeeCalculation(
        crowdloan: Crowdloan,
        customizationPayload: Parcelable?,
        bonusPayload: BonusPayload?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) = injectOnChainSubmission(
        crowdloan,
        customizationPayload,
        bonusPayload,
        amount,
        extrinsicBuilder
    )

    suspend fun submitOffChain(
        customizationPayload: Parcelable?,
        bonusPayload: BonusPayload?,
        amount: BigDecimal,
    )
}
