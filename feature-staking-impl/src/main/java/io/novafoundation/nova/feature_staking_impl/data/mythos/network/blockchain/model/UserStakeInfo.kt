package io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

class UserStakeInfo(
    val balance: Balance,
    val maybeLastUnstake: LastUnstake?,
    val candidates: List<AccountId>,
    val maybeLastRewardSession: SessionIndex?
)

class LastUnstake(
    val amount: Balance,
    val availableForRestakeAt: BlockNumber
)

typealias SessionIndex = BigInteger

fun bindUserStakeInfo(decoded: Any?): UserStakeInfo {
    val asStruct = decoded.castToStruct()

    return UserStakeInfo(
        balance = bindNumber(asStruct["balance"]),
        maybeLastUnstake = bindLastUnstake(asStruct["maybeLastUnstake"]),
        candidates = bindList(asStruct["candidates"], ::bindAccountId),
        maybeLastRewardSession = asStruct.get<Any?>("maybeLastRewardSession")?.let(::bindNumber)
    )
}

private fun bindLastUnstake(decoded: Any?): LastUnstake {
    // Tuple
    val (amountRaw, availableForRestakeAtRaw) = decoded.castToList()

    return LastUnstake(
        amount = bindNumber(amountRaw),
        availableForRestakeAt = bindBlockNumber(availableForRestakeAtRaw)
    )
}
