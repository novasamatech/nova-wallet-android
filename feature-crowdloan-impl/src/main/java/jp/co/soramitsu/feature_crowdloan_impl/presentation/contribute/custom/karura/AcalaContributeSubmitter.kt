package jp.co.soramitsu.feature_crowdloan_impl.presentation.contribute.custom.karura

import jp.co.soramitsu.feature_crowdloan_impl.domain.contribute.custom.karura.AcalaContributeInteractor
import jp.co.soramitsu.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import jp.co.soramitsu.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter
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
