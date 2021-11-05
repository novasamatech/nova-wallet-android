package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations

import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class ContributeValidationPayload(
    val crowdloan: Crowdloan,
    val asset: Asset,
    val fee: BigDecimal,
    val contributionAmount: BigDecimal,
)
