package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations

import android.os.Parcelable
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class ContributeValidationPayload(
    val crowdloan: Crowdloan,
    val customizationPayload: Parcelable?,
    val asset: Asset,
    val fee: BigDecimal,
    val contributionAmount: BigDecimal,
)
