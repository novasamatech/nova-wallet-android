package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation.Junction
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum

// ------ Decode ------

fun bindMultiLocation(instance: Any?): MultiLocation {
    val asStruct = instance.castToStruct()

    return MultiLocation(
        parents = bindNumber(asStruct["parents"]),
        interior = bindInterior((asStruct["interior"]))
    )
}

private fun bindInterior(instance: Any?): MultiLocation.Interior {
    val asDictEnum = instance.castToDictEnum()

    return when (asDictEnum.name) {
        "Here" -> MultiLocation.Interior.Here
        "X1" -> {
            val junction = bindJunction(asDictEnum.value)
            MultiLocation.Interior.Junctions(listOf(junction))
        }
        else -> {
            val junctions = bindList(asDictEnum.value, ::bindJunction)
            MultiLocation.Interior.Junctions(junctions)
        }
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
        "AccountKey20" -> Junction.AccountKey20(bindAccountIdJunction(asDictEnum.value, accountIdKey = "key"))
        "AccountId32" -> Junction.AccountId32(bindAccountIdJunction(asDictEnum.value, accountIdKey = "id"))

        else -> Junction.Unsupported
    }
}

private fun bindAccountIdJunction(instance: Any?, accountIdKey: String): AccountId {
    val asStruct = instance.castToStruct()

    return bindAccountId(asStruct[accountIdKey])
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
    Junction.Unsupported -> error("Unsupported junction")
}

private fun AccountId.toJunctionAccountIdInstance(accountIdKey: String) = structOf(
    "network" to DictEnum.Entry("Any", null),
    accountIdKey to this
)
