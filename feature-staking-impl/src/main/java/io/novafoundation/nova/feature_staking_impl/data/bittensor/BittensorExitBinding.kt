package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct

internal fun bindBittensorExitAvailability(decoded: Any?, coldkey: AccountId, netuid: Int): BittensorExitAvailability {
    val entries = decoded as? List<*> ?: error("Invalid stake availability response")
    val wallet = entries.map { it as? List<*> ?: error("Invalid coldkey availability tuple") }.singleOrNull {
        it.size == 2 && bindAccountIdKey(it[0]).value.contentEquals(coldkey)
    } ?: error("Missing coldkey availability")
    val subnets = wallet[1] as? List<*> ?: error("Invalid subnet availability list")
    val row = subnets.map { it as? List<*> ?: error("Invalid subnet availability tuple") }.singleOrNull {
        it.size == 2 && bindBittensorU16(it[0]) == netuid
    } ?: error("Missing subnet availability")
    val value = row[1].castToStruct()
    val total = bindBittensorU64(value["total"])
    val locked = bindBittensorU64(value["locked"])
    val available = bindBittensorU64(value["available"])
    require(locked <= total && available <= total && locked + available == total) { "Inconsistent stake availability" }
    return BittensorExitAvailability(total, locked, available)
}
