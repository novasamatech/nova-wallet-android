@file:Suppress("RedundantUnitExpression")

package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry0
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry3
import io.novafoundation.nova.runtime.storage.source.query.api.storage0
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novafoundation.nova.runtime.storage.source.query.api.storage3
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import java.math.BigInteger

/**
 * One leg of the route registered on chain. [pool] is the pool type name as the runtime spells it:
 * `Omnipool`, `XYK`, `Stableswap`, `Aave`. [poolId] is only carried by `Stableswap`, where it is the
 * pool's share token - the asset its oracle entries are stored against
 */
class HydrationRouteHop(
    val pool: String,
    val poolId: HydraDxAssetId?,
    val assetIn: HydraDxAssetId,
    val assetOut: HydraDxAssetId
)

/**
 * A stored `EmaOracle.Oracles` entry: EMA price for an ordered asset pair - planks of the lower asset id
 * per plank of the higher one - and the block the entry was last updated at
 */
class HydrationOracleEntry(
    val numerator: BigInteger,
    val denominator: BigInteger,
    val updatedAt: BigInteger
)

@JvmInline
value class HydrationRouterApi(override val module: Module) : QueryableModule

@JvmInline
value class HydrationEmaOracleApi(override val module: Module) : QueryableModule

@JvmInline
value class HydrationSystemApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.hydrationRouter: HydrationRouterApi
    get() = HydrationRouterApi(module("Router"))

context(StorageQueryContext)
val RuntimeMetadata.hydrationEmaOracle: HydrationEmaOracleApi
    get() = HydrationEmaOracleApi(module("EmaOracle"))

context(StorageQueryContext)
val RuntimeMetadata.hydrationSystem: HydrationSystemApi
    get() = HydrationSystemApi(system())

context(StorageQueryContext)
val HydrationSystemApi.number: QueryableStorageEntry0<BigInteger>
    get() = storage0(name = "Number", binding = { decoded -> bindNumber(decoded) })

context(StorageQueryContext)
val HydrationRouterApi.routes: QueryableStorageEntry1<Struct.Instance, List<HydrationRouteHop>>
    get() = storage1(
        name = "Routes",
        binding = { decoded, _ -> bindRoute(decoded) },
    )

context(StorageQueryContext)
val HydrationEmaOracleApi.oracles: QueryableStorageEntry3<ByteArray, Pair<HydraDxAssetId, HydraDxAssetId>, String, HydrationOracleEntry>
    get() = storage3(
        name = "Oracles",
        binding = { decoded, _, _, _ -> bindOracleEntry(decoded) },
        key2ToInternalConverter = { (first, second) -> listOf(first, second) },
        key3ToInternalConverter = { period -> DictEnum.Entry(period, null) },
        key2FromInternalConverter = { instance ->
            val (first, second) = instance.castToList()
            bindNumber(first) to bindNumber(second)
        },
        key3FromInternalConverter = { instance -> instance.castToDictEnum().name }
    )

fun hydrationAssetPairKey(first: HydraDxAssetId, second: HydraDxAssetId): Struct.Instance {
    return structOf("assetIn" to first, "assetOut" to second)
}

private fun bindRoute(decoded: Any?): List<HydrationRouteHop> {
    return decoded.castToList().map { hop ->
        val asStruct = hop.castToStruct()
        val pool = asStruct.get<Any?>("pool").castToDictEnum()

        HydrationRouteHop(
            pool = pool.name,
            poolId = pool.value?.let { runCatching { bindNumber(it) }.getOrNull() },
            assetIn = bindNumber(asStruct["assetIn"]),
            assetOut = bindNumber(asStruct["assetOut"])
        )
    }
}

private fun bindOracleEntry(decoded: Any?): HydrationOracleEntry {
    val entry = decoded.castToList().first().castToStruct()
    val price = entry.get<Any?>("price").castToStruct()

    return HydrationOracleEntry(
        numerator = bindNumber(price["n"]),
        denominator = bindNumber(price["d"]),
        updatedAt = bindNumber(entry["updatedAt"])
    )
}
