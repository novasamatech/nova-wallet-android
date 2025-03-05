package io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.validations

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class RedeemMythosStakingValidationPayload(
    val fee: Fee,
    val asset: Asset,
)

val RedeemMythosStakingValidationPayload.chainId: ChainId
    get() = asset.token.configuration.chainId
