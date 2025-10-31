package io.novafoundation.nova.feature_gift_impl.domain.models

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

data class CreateGiftModel(
    val metaAccount: MetaAccount,
    val chain: Chain,
    val chainAsset: Chain.Asset,
    val giftAccount: AccountIdKey,
    val amount: BigDecimal
)
