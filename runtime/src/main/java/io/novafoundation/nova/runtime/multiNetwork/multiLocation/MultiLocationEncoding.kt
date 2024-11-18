package io.novafoundation.nova.runtime.multiNetwork.multiLocation

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.Ids
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation.Junction
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

// ------ Decode ------

fun bindMultiLocation(instance: Any?): MultiLocation {
    val asStruct = instance.castToStruct()

    return MultiLocation(
        parents = bindNumber(asStruct["parents"]),
        interior = bindInterior((asStruct["interior"]))
    )
}

fun bindVersionedMultiLocation(instance: Any?): MultiLocation {
    val asDictEnum = instance.castToDictEnum()
    return bindMultiLocation(asDictEnum.value)
}

private fun bindInterior(instance: Any?): MultiLocation.Interior {
    val asDictEnum = instance.castToDictEnum()

    return when (asDictEnum.name) {
        "Here" -> MultiLocation.Interior.Here

        else -> {
            val junctions = bindJunctions(asDictEnum.value)
            MultiLocation.Interior.Junctions(junctions)
        }
    }
}

private fun bindJunctions(instance: Any?): List<Junction> {
    if (instance is List<*>) {
        // For XCM V4 or Interior.X2 and higher
        return bindList(instance, ::bindJunction)
    } else {
        // For XCM V3 and Interior.X1
        return listOf(bindJunction(instance))
    }
}

private fun bindJunction(instance: Any?): Junction {
    val asDictEnum = instance.castToDictEnum()

    return when (asDictEnum.name) {
        "GeneralKey" -> {
            val keyBytes = bindByteArray(asDictEnum.value)
            Junction.GeneralKey(keyBytes.toHexString(withPrefix = true))
        }

        "PalletInstance" -> Junction.PalletInstance(bindNumber(asDictEnum.value))
        "Parachain" -> Junction.ParachainId(bindNumber(asDictEnum.value))
        "GeneralIndex" -> Junction.GeneralIndex(bindNumber(asDictEnum.value))
        "GlobalConsensus" -> bindGlobalConsensusJunction(asDictEnum.value)
        "AccountKey20" -> Junction.AccountKey20(bindAccountIdJunction(asDictEnum.value, accountIdKey = "key"))
        "AccountId32" -> Junction.AccountId32(bindAccountIdJunction(asDictEnum.value, accountIdKey = "id"))

        else -> Junction.Unsupported
    }
}

private fun bindAccountIdJunction(instance: Any?, accountIdKey: String): AccountId {
    val asStruct = instance.castToStruct()

    return bindAccountId(asStruct[accountIdKey])
}

private fun bindGlobalConsensusJunction(instance: Any?): Junction {
    val asDictEnum = instance.castToDictEnum()

    return when (asDictEnum.name) {
        "ByGenesis" -> {
            val genesis = bindByteArray(asDictEnum.value).toHexString(withPrefix = false)
            Junction.GlobalConsensus(chainId = genesis)
        }

        "Polkadot" -> Junction.GlobalConsensus(chainId = Chain.Geneses.POLKADOT)
        "Kusama" -> Junction.GlobalConsensus(chainId = Chain.Geneses.KUSAMA)
        "Westend" -> Junction.GlobalConsensus(chainId = Chain.Geneses.WESTEND)
        "Ethereum" -> Junction.GlobalConsensus(chainId = Chain.Ids.ETHEREUM)
        else -> Junction.Unsupported
    }
}

// ------ Encode ------

fun MultiLocation.toEncodableInstance() = structOf(
    "parents" to parents,
    "interior" to interior.toEncodableInstance()
)

private fun MultiLocation.Interior.toEncodableInstance() = when (this) {
    MultiLocation.Interior.Here -> DictEnum.Entry("Here", null)

    is MultiLocation.Interior.Junctions -> if (junctions.size == 1) {
        DictEnum.Entry(
            name = "X1",
            value = junctions.first().toEncodableInstance()
        )
    } else {
        DictEnum.Entry(
            name = "X${junctions.size}",
            value = junctions.map(Junction::toEncodableInstance)
        )
    }
}

private fun Junction.toEncodableInstance() = when (this) {
    is Junction.GeneralKey -> DictEnum.Entry("GeneralKey", key.fromHex())
    is Junction.PalletInstance -> DictEnum.Entry("PalletInstance", index)
    is Junction.ParachainId -> DictEnum.Entry("Parachain", id)
    is Junction.AccountKey20 -> DictEnum.Entry("AccountKey20", accountId.toJunctionAccountIdInstance(accountIdKey = "key"))
    is Junction.AccountId32 -> DictEnum.Entry("AccountId32", accountId.toJunctionAccountIdInstance(accountIdKey = "id"))
    is Junction.GeneralIndex -> DictEnum.Entry("GeneralIndex", index)
    is Junction.GlobalConsensus -> toEncodableInstance()
    Junction.Unsupported -> error("Unsupported junction")
}

private fun Junction.GlobalConsensus.toEncodableInstance(): Any {
    val innerValue = when (chainId) {
        Chain.Geneses.POLKADOT -> DictEnum.Entry("Polkadot", null)
        Chain.Geneses.KUSAMA -> DictEnum.Entry("Kusama", null)
        Chain.Geneses.WESTEND -> DictEnum.Entry("Westend", null)
        Chain.Ids.ETHEREUM -> DictEnum.Entry("Ethereum", null)
        else -> DictEnum.Entry("ByGenesis", chainId.fromHex())
    }

    return DictEnum.Entry("GlobalConsensus", innerValue)
}

private fun AccountId.toJunctionAccountIdInstance(accountIdKey: String) = structOf(
    "network" to DictEnum.Entry("Any", null),
    accountIdKey to this
)
