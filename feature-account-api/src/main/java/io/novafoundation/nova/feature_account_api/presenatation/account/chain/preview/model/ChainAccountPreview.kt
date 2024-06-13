package io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

class ChainAccountPreview(
    val chain: Chain,
    val accountId: AccountId,
)
