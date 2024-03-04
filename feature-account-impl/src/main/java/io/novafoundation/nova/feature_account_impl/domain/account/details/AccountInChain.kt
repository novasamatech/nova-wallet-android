package io.novafoundation.nova.feature_account_impl.domain.account.details

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

class AccountInChain(
    val chain: Chain,
    val projection: Projection?,
    val from: From
) {

    class Projection(val address: String, val accountId: AccountId)

    enum class From {
        META_ACCOUNT, CHAIN_ACCOUNT
    }
}
