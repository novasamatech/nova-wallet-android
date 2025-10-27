package io.novafoundation.nova.feature_gift_impl.domain.models

import io.novafoundation.nova.common.address.AccountIdKey

class GiftAccount(
    val seed: ByteArray,
    val account: AccountIdKey
)
