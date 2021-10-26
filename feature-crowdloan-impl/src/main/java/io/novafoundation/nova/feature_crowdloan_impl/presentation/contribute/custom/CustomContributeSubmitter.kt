package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom

import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

interface CustomContributeSubmitter {

    suspend fun submitOnChain(
        crowdloan: Crowdloan,
        payload: BonusPayload?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) {
        // do nothing by default
    }

    suspend fun submitOffChain(
        payload: BonusPayload?,
        amount: BigDecimal,
    ): Result<Unit> {
        // do nothing by default

        return Result.success(Unit)
    }
}
