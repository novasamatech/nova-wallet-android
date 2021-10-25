package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.karura

import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.acala.AcalaContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
import java.math.BigDecimal

class AcalaContributeSubmitter(
    private val interactor: AcalaContributeInteractor
) : CustomContributeSubmitter {

    override suspend fun submitOffChain(
        payload: BonusPayload,
        amount: BigDecimal
    ): Result<Unit> {
        require(payload is AcalaBonusPayload)

        return interactor.registerInBonusProgram(payload.referralCode, amount)
    }
}
