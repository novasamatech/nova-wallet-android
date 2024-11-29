package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.custom.moonbeam

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_account_api.data.model.Fee

class MoonbeamTermsPayload(
    val fee: Fee,
    val asset: Asset
)
