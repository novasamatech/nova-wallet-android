package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy

import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyDepositWithQuantity
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class AddStakingProxyValidationPayload(
    val chain: Chain,
    val asset: Asset,
    val proxiedAccountId: AccountId,
    val proxyAddress: String,
    val fee: DecimalFee,
    val depositWithQuantity: ProxyDepositWithQuantity
)
