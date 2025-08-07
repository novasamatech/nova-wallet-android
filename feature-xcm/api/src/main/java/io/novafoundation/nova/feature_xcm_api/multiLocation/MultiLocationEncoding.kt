package io.novafoundation.nova.feature_xcm_api.multiLocation

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.HexString
import io.novafoundation.nova.common.utils.padEnd
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.bindVersionedXcm
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.Ids
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.encrypt.json.copyBytes
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct

// ------ Decode ------

fun bindMultiLocation(instance: Any?): RelativeMultiLocation {
    val asStruct = instance.castToStruct()

    return RelativeMultiLocation(
        parents = bindInt(asStruct["parents"]),
        interior = bindInterior((asStruct["interior"]))
    )
}

fun bindVersionedMultiLocation(instance: Any?): VersionedXcm<RelativeMultiLocation> {
    return bindVersionedXcm(instance) { inner, _ -> bindMultiLocation(inner) }
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
    // Note that Interior.X1 is encoded differently in XCM v3 (a single junction) and V4 (single-element list)
    if (instance is List<*>) {
        return bindList(instance, ::bindJunction)
    } else {
        return listOf(bindJunction(instance))
    }
}

private fun bindJunction(instance: Any?): Junction {
    val asDictEnum = instance.castToDictEnum()

    return when (asDictEnum.name) {
        "GeneralKey" -> Junction.GeneralKey(bindGeneralKey(asDictEnum.value))
        "PalletInstance" -> Junction.PalletInstance(bindNumber(asDictEnum.value))
        "Parachain" -> Junction.ParachainId(bindNumber(asDictEnum.value))
        "GeneralIndex" -> Junction.GeneralIndex(bindNumber(asDictEnum.value))
        "GlobalConsensus" -> bindGlobalConsensusJunction(asDictEnum.value)
        "AccountKey20" -> Junction.AccountKey20(bindAccountIdJunction(asDictEnum.value, accountIdKey = "key"))
        "AccountId32" -> Junction.AccountId32(bindAccountIdJunction(asDictEnum.value, accountIdKey = "id"))

        else -> Junction.Unsupported
    }
}

private fun bindGeneralKey(instance: Any?): HexString {
    val keyBytes = if (instance is Struct.Instance) {
        // v3+ structure
        val keyLength = bindInt(instance["length"])
        val keyPadded = bindByteArray(instance["data"])

        keyPadded.copyBytes(0, keyLength)
    } else {
        bindByteArray(instance)
    }

    return keyBytes.toHexString(withPrefix = true)
}

private fun bindAccountIdJunction(instance: Any?, accountIdKey: String): AccountIdKey {
    val asStruct = instance.castToStruct()

    return bindAccountId(asStruct[accountIdKey]).intoKey()
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

internal fun RelativeMultiLocation.toEncodableInstanceExt(xcmVersion: XcmVersion) = structOf(
    "parents" to parents.toBigInteger(),
    "interior" to interior.toEncodableInstance(xcmVersion)
)

private fun MultiLocation.Interior.toEncodableInstance(xcmVersion: XcmVersion) = when (this) {
    MultiLocation.Interior.Here -> DictEnum.Entry("Here", null)

    is MultiLocation.Interior.Junctions -> if (junctions.size == 1 && xcmVersion <= XcmVersion.V3) {
        // X1 is encoded as a single junction in V3 and prior
        DictEnum.Entry(
            name = "X1",
            value = junctions.single().toEncodableInstance(xcmVersion)
        )
    } else {
        DictEnum.Entry(
            name = "X${junctions.size}",
            value = junctions.map { it.toEncodableInstance(xcmVersion) }
        )
    }
}

private fun Junction.toEncodableInstance(xcmVersion: XcmVersion) = when (this) {
    is Junction.GeneralKey -> DictEnum.Entry("GeneralKey", encodableGeneralKey(xcmVersion, key))
    is Junction.PalletInstance -> DictEnum.Entry("PalletInstance", index)
    is Junction.ParachainId -> DictEnum.Entry("Parachain", id)
    is Junction.AccountKey20 -> DictEnum.Entry("AccountKey20", accountId.toJunctionAccountIdInstance(accountIdKey = "key", xcmVersion))
    is Junction.AccountId32 -> DictEnum.Entry("AccountId32", accountId.toJunctionAccountIdInstance(accountIdKey = "id", xcmVersion))
    is Junction.GeneralIndex -> DictEnum.Entry("GeneralIndex", index)
    is Junction.GlobalConsensus -> toEncodableInstance()
    Junction.Unsupported -> error("Unsupported junction")
}

private fun encodableGeneralKey(xcmVersion: XcmVersion, generalKey: HexString): Any {
    val bytes = generalKey.fromHex()

    return if (xcmVersion >= XcmVersion.V3) {
        structOf(
            "length" to bytes.size.toBigInteger(),
            "data" to bytes.padEnd(expectedSize = 32, padding = 0)
        )
    } else {
        bytes
    }
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

private fun AccountIdKey.toJunctionAccountIdInstance(accountIdKey: String, xcmVersion: XcmVersion) = structOf(
    "network" to emptyNetworkField(xcmVersion),
    accountIdKey to value
)

private fun emptyNetworkField(xcmVersion: XcmVersion): Any? {
    return if (xcmVersion >= XcmVersion.V3) {
        // Network in V3+ is encoded as Option<NetworkId>
        null
    } else {
        // Network in V2- is encoded as Enum with Any variant
        DictEnum.Entry("Any", null)
    }
}
