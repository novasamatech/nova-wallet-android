package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.*
import io.novafoundation.nova.common.utils.assets
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import java.math.BigInteger

class AssetDetails(
    val isFrozen: Boolean,
    val minimumBalance: BigInteger
)

@UseCaseBinding
fun bindAssetDetails(scale: String, runtime: RuntimeSnapshot): AssetDetails {
    val type = runtime.metadata.assets().storage("Asset").returnType()

    val dynamicInstance = type.fromHexOrNull(runtime, scale).cast<Struct.Instance>()

    return AssetDetails(
        isFrozen = bindBoolean(dynamicInstance["isFrozen"]),
        minimumBalance = bindNumber(dynamicInstance["minBalance"])
    )
}

class AssetAccount(
    val balance: BigInteger,
    val isFrozen: Boolean
) {

    companion object {

        fun empty() = AssetAccount(
            balance = BigInteger.ZERO,
            isFrozen = false
        )
    }
}

@UseCaseBinding
fun bindAssetAccount(scale: String, runtime: RuntimeSnapshot): AssetAccount {
    val type = runtime.metadata.assets().storage("Account").returnType()

    val dynamicInstance = type.fromHexOrNull(runtime, scale).cast<Struct.Instance>()

    return AssetAccount(
        balance = bindNumber(dynamicInstance["balance"]),
        isFrozen = bindBoolean(dynamicInstance["isFrozen"]),
    )
}
