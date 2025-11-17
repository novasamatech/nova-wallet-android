package io.novafoundation.nova.feature_crowdloan_impl.domain.claimContributions.validation

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class ClaimContributionValidationPayload(
    val fee: Fee,
    val asset: Asset,
)

val ClaimContributionValidationPayload.chainId: ChainId
    get() = asset.token.configuration.chainId
