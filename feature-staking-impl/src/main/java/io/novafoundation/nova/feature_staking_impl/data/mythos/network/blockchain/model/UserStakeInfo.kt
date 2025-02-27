package io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.SessionValidators
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

class UserStakeInfo(
    val balance: Balance,
    val maybeLastUnstake: LastUnstake?,
    val candidates: List<AccountIdKey>,
    val maybeLastRewardSession: SessionIndex?
)

fun UserStakeInfo.hasActiveValidators(sessionValidators: SessionValidators): Boolean {
    val validatorSet = sessionValidators.toSet()

    return candidates.any { it in validatorSet }
}

class LastUnstake(
    val amount: Balance,
    val availableForRestakeAt: BlockNumber
)

typealias SessionIndex = BigInteger

fun bindUserStakeInfo(decoded: Any?): UserStakeInfo {
    val asStruct = decoded.castToStruct()

    return UserStakeInfo(
        balance = bindNumber(asStruct["stake"]),
        maybeLastUnstake = bindLastUnstake(asStruct["maybeLastUnstake"]),
        candidates = bindList(asStruct["candidates"], ::bindAccountIdKey),
        maybeLastRewardSession = asStruct.get<Any?>("maybeLastRewardSession")?.let(::bindNumber)
    )
}

private fun bindLastUnstake(decoded: Any?): LastUnstake? {
    if (decoded == null) return null

    // Tuple
    val (amountRaw, availableForRestakeAtRaw) = decoded.castToList()

    return LastUnstake(
        amount = bindNumber(amountRaw),
        availableForRestakeAt = bindBlockNumber(availableForRestakeAtRaw)
    )
}
