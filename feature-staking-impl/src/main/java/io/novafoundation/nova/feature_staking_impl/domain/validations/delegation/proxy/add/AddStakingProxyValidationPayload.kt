package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

class AddStakingProxyValidationPayload(
    val chain: Chain,
    val asset: Asset,
    val proxiedAccountId: AccountId,
    val proxyAddress: String,
    val fee: Fee,
    val deltaDeposit: Balance,
    val currentQuantity: Int
)
