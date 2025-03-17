package io.novafoundation.nova.feature_xcm_api.multiLocation

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.isAscending
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import java.math.BigInteger

abstract class MultiLocation(
    open val interior: Interior
) {

    sealed class Interior {

        object Here : Interior()

        class Junctions(junctions: List<Junction>) : Interior() {
            val junctions = junctions.sorted()

            override fun equals(other: Any?): Boolean {
                if (other !is Junctions) return false
                return junctions == other.junctions
            }

            override fun hashCode(): Int {
                return junctions.hashCode()
            }

            override fun toString(): String {
                return junctions.toString()
            }
        }
    }

    sealed class Junction {

        data class ParachainId(val id: ParaId) : Junction() {

            constructor(id: Int) : this(id.toBigInteger())
        }

        data class GeneralKey(val key: String) : Junction()

        data class PalletInstance(val index: BigInteger) : Junction()

        data class GeneralIndex(val index: BigInteger) : Junction()

        data class AccountKey20(val accountId: AccountIdKey) : Junction()

        data class AccountId32(val accountId: AccountIdKey) : Junction()

        data class GlobalConsensus(val chainId: ChainId) : Junction()

        object Unsupported : Junction()
    }
}

val Junction.order
    get() = when (this) {
        is Junction.GlobalConsensus -> 0

        is Junction.ParachainId -> 1

        // All of these are on the same "level" - mutually exhaustive
        is Junction.PalletInstance,
        is Junction.AccountKey20,
        is Junction.AccountId32 -> 2

        is Junction.GeneralKey,
        is Junction.GeneralIndex -> 3

        Junction.Unsupported -> Int.MAX_VALUE
    }

val MultiLocation.junctions: List<Junction>
    get() = when (val interior = interior) {
        MultiLocation.Interior.Here -> emptyList()
        is MultiLocation.Interior.Junctions -> interior.junctions
    }

fun List<Junction>.toInterior() = when (size) {
    0 -> MultiLocation.Interior.Here
    else -> MultiLocation.Interior.Junctions(this)
}

fun Junction.toInterior() = MultiLocation.Interior.Junctions(listOf(this))

fun MultiLocation.Interior.isHere() = this is MultiLocation.Interior.Here

fun MultiLocation.accountId(): AccountIdKey? {
    return junctions.tryFindNonNull {
        when (it) {
            is Junction.AccountId32 -> it.accountId
            is Junction.AccountKey20 -> it.accountId
            else -> null
        }
    }
}

fun MultiLocation.Interior.asLocation(): AbsoluteMultiLocation {
    return AbsoluteMultiLocation(this)
}

fun MultiLocation.Interior.asRelativeLocation(): RelativeMultiLocation {
    return asLocation().toRelative()
}

fun List<Junction>.asLocation(): AbsoluteMultiLocation {
    return toInterior().asLocation()
}

fun Junction.asLocation(): AbsoluteMultiLocation {
    return toInterior().asLocation()
}

operator fun RelativeMultiLocation.plus(suffix: RelativeMultiLocation): RelativeMultiLocation {
    require(suffix.parents == 0) {
        "Appending multi location that has parents is not supported"
    }

    val newJunctions = junctions + suffix.junctions
    require(newJunctions.isAscending(compareBy { it.order })) {
        "Cannot append this multi location due to conflicting junctions"
    }

    return RelativeMultiLocation(
        parents = parents,
        interior = newJunctions.toInterior()
    )
}

fun AccountIdKey.toMultiLocation() = RelativeMultiLocation(
    parents = 0,
    interior = Junctions(
        when (value.size) {
            32 -> Junction.AccountId32(this)
            20 -> Junction.AccountKey20(this)
            else -> throw IllegalArgumentException("Unsupported account id length: ${value.size}")
        }
    )
)

fun Junctions(vararg junctions: Junction) = MultiLocation.Interior.Junctions(junctions.toList())

fun MultiLocation.paraIdOrNull(): ParaId? {
    return junctions.filterIsInstance<Junction.ParachainId>()
        .firstOrNull()
        ?.id
}

private fun List<Junction>.sorted(): List<Junction> {
    return sortedBy(Junction::order)
}
