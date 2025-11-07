package io.novafoundation.nova.feature_gift_impl.domain.models

import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.scale.EncodableStruct

class ClaimableGift(
    val accountId: AccountId,
    val chain: Chain,
    val chainAsset: Chain.Asset,
    val secrets: EncodableStruct<ChainAccountSecrets>
)
