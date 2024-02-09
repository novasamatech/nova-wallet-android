package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
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
    val deltaDeposit: Balance,
    val currentQuantity: Int
)
