package io.novafoundation.nova.feature_xcm_api.multiLocation

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_xcm_api.versions.VersionedToDynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import java.math.BigInteger

abstract class MultiLocation(
    val interior: Interior
) {

    sealed class Interior {

        object Here : Interior()

        class Junctions(junctions: List<Junction>) : Interior() {
            val junctions = junctions.sorted()
        }
    }

    sealed class Junction {

        data class ParachainId(val id: ParaId) : Junction()

        data class GeneralKey(val key: String) : Junction()

        data class PalletInstance(val index: BigInteger) : Junction()

        data class GeneralIndex(val index: BigInteger) : Junction()

        data class AccountKey20(val accountId: AccountIdKey) : Junction()

        data class AccountId32(val accountId: AccountIdKey) : Junction()

        data class GlobalConsensus(val chainId: ChainId) : Junction()

        object Unsupported : Junction()
    }
}

val MultiLocation.Junction.order
    get() = when (this) {
        is MultiLocation.Junction.GlobalConsensus -> 0

        is MultiLocation.Junction.ParachainId -> 1

        // All of these are on the same "level" - mutually exhaustive
        is MultiLocation.Junction.PalletInstance,
        is MultiLocation.Junction.AccountKey20,
        is MultiLocation.Junction.AccountId32 -> 2

        is MultiLocation.Junction.GeneralKey,
        is MultiLocation.Junction.GeneralIndex -> 3

        MultiLocation.Junction.Unsupported -> Int.MAX_VALUE
    }

val MultiLocation.junctions: List<MultiLocation.Junction>
    get() = when (val interior = interior) {
        MultiLocation.Interior.Here -> emptyList()
        is MultiLocation.Interior.Junctions -> interior.junctions
    }

fun List<MultiLocation.Junction>.toInterior() = when (size) {
    0 -> MultiLocation.Interior.Here
    else -> MultiLocation.Interior.Junctions(this)
}

fun MultiLocation.Interior.isHere() = this is MultiLocation.Interior.Here

fun MultiLocation.accountId(): AccountIdKey? {
    return junctions.tryFindNonNull {
        when (it) {
            is MultiLocation.Junction.AccountId32 -> it.accountId
            is MultiLocation.Junction.AccountKey20 -> it.accountId
            else -> null
        }
    }
}

fun Junctions(vararg junctions: MultiLocation.Junction) = MultiLocation.Interior.Junctions(junctions.toList())

fun MultiLocation.paraIdOrNull(): ParaId? {
    return junctions.filterIsInstance<MultiLocation.Junction.ParachainId>()
        .firstOrNull()
        ?.id
}

private fun List<MultiLocation.Junction>.sorted(): List<MultiLocation.Junction> {
    return sortedBy(MultiLocation.Junction::order)
}
