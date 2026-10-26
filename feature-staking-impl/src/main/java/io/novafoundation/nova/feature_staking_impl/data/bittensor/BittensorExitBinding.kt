package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

internal fun bindBittensorExitAvailability(decoded: Any?, coldkey: AccountId, netuid: Int): BittensorExitAvailability {
    val entries = decoded as? List<*> ?: error("Invalid stake availability response")
    val wallet = entries.map { it as? List<*> ?: error("Invalid coldkey availability tuple") }
        .singleOrNull {
            it.size == 2 && bindAccountIdKey(it[0]).value.contentEquals(coldkey)
        } ?: error("Missing coldkey availability")

    val subnets = wallet[1] as? List<*> ?: error("Invalid subnet availability list")
    // The runtime omits a subnet on which the coldkey has neither stake nor a lock: nothing is available there.
    val row = subnets.map { it as? List<*> ?: error("Invalid subnet availability tuple") }
        .singleOrNull {
            it.size == 2 && bindBittensorU16(it[0]) == netuid
        } ?: return BittensorExitAvailability(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO)

    // Available also excludes miner collateral and saturates at zero, so it need not equal total minus locked.
    val value = row[1].castToStruct()
    val total = bindBittensorU64(value["total"])

    val locked = bindBittensorU64(value["locked"])
    val available = bindBittensorU64(value["available"])
    return BittensorExitAvailability(total, locked, available)
}
