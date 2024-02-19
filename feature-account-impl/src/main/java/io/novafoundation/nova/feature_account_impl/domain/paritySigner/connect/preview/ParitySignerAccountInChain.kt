package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.preview

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

class ParitySignerAccountInChain(
    val chain: Chain,
    val accountId: AccountId,
)
