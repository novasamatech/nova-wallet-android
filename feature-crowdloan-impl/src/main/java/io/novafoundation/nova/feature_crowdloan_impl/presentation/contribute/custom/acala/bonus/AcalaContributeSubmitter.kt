package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.bonus

import android.os.Parcelable
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.acala.AcalaContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

class AcalaContributeSubmitter(
    private val interactor: AcalaContributeInteractor
) : CustomContributeSubmitter {

    override suspend fun submitOnChain(
        crowdloan: Crowdloan,
        customizationPayload: Parcelable?,
        bonusPayload: BonusPayload?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) {
        // Do nothing
    }

    override suspend fun submitOffChain(customizationPayload: Parcelable?, bonusPayload: BonusPayload?, amount: BigDecimal) {
        require(bonusPayload is AcalaBonusPayload?)

        bonusPayload?.let {
            interactor.registerInBonusProgram(bonusPayload.referralCode, amount)
        }
    }
}
