package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

class RemoveStakingProxyValidationPayload(
    val chain: Chain,
    val asset: Asset,
    val proxiedAccountId: AccountId,
    val proxyAddress: String,
    val fee: DecimalFee
)
