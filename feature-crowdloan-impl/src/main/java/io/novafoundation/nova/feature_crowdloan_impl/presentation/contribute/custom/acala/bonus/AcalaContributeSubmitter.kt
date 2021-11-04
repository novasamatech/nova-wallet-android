package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.bonus

import android.os.Parcelable
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.acala.AcalaContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.AcalaCustomizationPayload
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

class AcalaContributeSubmitter(
    private val interactor: AcalaContributeInteractor
) : CustomContributeSubmitter {

    override suspend fun injectOnChainSubmission(
        crowdloan: Crowdloan,
        customizationPayload: Parcelable?,
        bonusPayload: BonusPayload?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) {
        require(customizationPayload is AcalaCustomizationPayload)
        require(bonusPayload is AcalaBonusPayload?)

        interactor.injectOnChainSubmission(
            contributionType = customizationPayload.contributionType,
            referralCode = bonusPayload?.referralCode,
            amount = amount,
            extrinsicBuilder = extrinsicBuilder
        )
    }

    override suspend fun injectFeeCalculation(
        crowdloan: Crowdloan,
        customizationPayload: Parcelable?,
        bonusPayload: BonusPayload?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) {
        require(customizationPayload is AcalaCustomizationPayload)
        require(bonusPayload is AcalaBonusPayload?)

        interactor.injectFeeCalculation(
            contributionType = customizationPayload.contributionType,
            referralCode = bonusPayload?.referralCode,
            amount = amount,
            extrinsicBuilder = extrinsicBuilder
        )
    }

    override suspend fun submitOffChain(customizationPayload: Parcelable?, bonusPayload: BonusPayload?, amount: BigDecimal) {
        require(bonusPayload is AcalaBonusPayload?)
        require(customizationPayload is AcalaCustomizationPayload)

        interactor.registerContributionOffChain(
            amount = amount,
            contributionType = customizationPayload.contributionType,
            referralCode = bonusPayload?.referralCode
        )
    }
}
