package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine

import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import java.math.BigInteger

class AssetDetails(
    val isFrozen: Boolean,
    val isSufficient: Boolean,
    val minimumBalance: BigInteger
)

@UseCaseBinding
fun bindAssetDetails(decoded: Any?): AssetDetails {
    val dynamicInstance = decoded.cast<Struct.Instance>()

    return AssetDetails(
        isFrozen = bindBoolean(dynamicInstance["isFrozen"]),
        isSufficient = bindBoolean(dynamicInstance["isSufficient"]),
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
fun bindAssetAccount(decoded: Any): AssetAccount {
    val dynamicInstance = decoded.cast<Struct.Instance>()

    return AssetAccount(
        balance = bindNumber(dynamicInstance["balance"]),
        isFrozen = bindBoolean(dynamicInstance["isFrozen"]),
    )
}
