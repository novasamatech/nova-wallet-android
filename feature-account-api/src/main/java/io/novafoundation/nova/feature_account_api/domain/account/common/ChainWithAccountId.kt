package io.novafoundation.nova.feature_account_api.domain.account.common

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ChainWithAccountId(
    val chain: Chain,
    val accountId: ByteArray
)

fun Chain.withAccountId(metaAccount: MetaAccount): ChainWithAccountId {
    return ChainWithAccountId(
        this,
        metaAccount.requireAccountIdIn(this)
    )
}
