package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.custom.moonbeam

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee

class MoonbeamTermsPayload(
    val fee: DecimalFee,
    val asset: Asset
)
