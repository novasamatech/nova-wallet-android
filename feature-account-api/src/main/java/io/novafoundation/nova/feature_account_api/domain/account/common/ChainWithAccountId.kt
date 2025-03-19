package io.novafoundation.nova.feature_account_api.domain.account.common

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ChainWithAccountId(
    val chain: Chain,
    val accountId: ByteArray
)
