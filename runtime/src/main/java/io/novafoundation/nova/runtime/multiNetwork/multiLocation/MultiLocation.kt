package io.novafoundation.nova.runtime.multiNetwork.multiLocation

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

fun Junctions(vararg junctions: MultiLocation.Junction) = MultiLocation.Interior.Junctions(junctions.toList())

class MultiLocation(
    val parents: BigInteger,
    val interior: Interior
) {

    sealed class Interior {

        object Here : Interior()

        class Junctions(junctions: List<Junction>) : Interior() {
            val junctions = junctions.sorted()
        }
    }

    sealed class Junction {

        class ParachainId(val id: ParaId) : Junction()

        class GeneralKey(val key: String) : Junction()

        class PalletInstance(val index: BigInteger) : Junction()

        class GeneralIndex(val index: BigInteger) : Junction()

        class AccountKey20(val accountId: AccountId) : Junction()

        class AccountId32(val accountId: AccountId) : Junction()

        class GlobalConsensus(chainId: ChainId) : Junction() {

            val chainId = chainId.removeHexPrefix()
        }

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

val MultiLocation.Interior.junctionList: List<MultiLocation.Junction>
    get() = when (this) {
        MultiLocation.Interior.Here -> emptyList()
        is MultiLocation.Interior.Junctions -> junctions
    }

fun List<MultiLocation.Junction>.toInterior() = when (size) {
    0 -> MultiLocation.Interior.Here
    else -> MultiLocation.Interior.Junctions(this)
}

fun MultiLocation.Interior.isHere() = this is MultiLocation.Interior.Here

fun MultiLocation.Interior.paraIdOrNull(): ParaId? {
    if (this !is MultiLocation.Interior.Junctions) return null

    return junctions.filterIsInstance<MultiLocation.Junction.ParachainId>()
        .firstOrNull()
        ?.id
}

private fun List<MultiLocation.Junction>.sorted(): List<MultiLocation.Junction> {
    return sortedBy(MultiLocation.Junction::order)
}
