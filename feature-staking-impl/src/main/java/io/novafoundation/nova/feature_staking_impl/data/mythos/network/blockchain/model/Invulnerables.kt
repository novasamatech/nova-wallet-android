package io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindSet

typealias Invulnerables = Set<AccountIdKey>

fun bindInvulnerables(decoded: Any?): Invulnerables {
    return bindSet(decoded, ::bindAccountIdKey)
}
