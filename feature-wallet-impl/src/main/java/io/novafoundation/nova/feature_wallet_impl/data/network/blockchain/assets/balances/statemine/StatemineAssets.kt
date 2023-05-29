package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine

import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindCollectionEnum
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine.AssetAccount.AccountStatus
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import java.math.BigInteger

class AssetDetails(
    val status: Status,
    val isSufficient: Boolean,
    val minimumBalance: BigInteger
) {

    enum class Status {
        Live, Frozen, Destroying
    }
}

val AssetDetails.Status.transfersFrozen: Boolean
    get() = this != AssetDetails.Status.Live

@UseCaseBinding
fun bindAssetDetails(decoded: Any?): AssetDetails {
    val dynamicInstance = decoded.cast<Struct.Instance>()

    return AssetDetails(
        status = bindAssetStatus(dynamicInstance),
        isSufficient = bindBoolean(dynamicInstance["isSufficient"]),
        minimumBalance = bindNumber(dynamicInstance["minBalance"])
    )
}

private fun bindAssetStatus(assetStruct: Struct.Instance): AssetDetails.Status {
    return when {
        assetStruct.get<Any>("isFrozen") != null -> bindIsFrozen(bindBoolean(assetStruct["isFrozen"]))
        assetStruct.get<Any>("status") != null -> bindCollectionEnum(assetStruct["status"])
        else -> incompatible()
    }
}

private fun bindIsFrozen(isFrozen: Boolean): AssetDetails.Status = if (isFrozen) AssetDetails.Status.Frozen else AssetDetails.Status.Live

class AssetAccount(
    val balance: BigInteger,
    val status: AccountStatus
) {

    enum class AccountStatus {
        Liquid, Frozen, Blocked
    }

    companion object {

        fun empty() = AssetAccount(
            balance = BigInteger.ZERO,
            status = AccountStatus.Liquid
        )
    }
}

val AssetAccount.isBalanceFrozen: Boolean
    get() = status == AccountStatus.Blocked || status == AccountStatus.Frozen

val AssetAccount.canAcceptFunds: Boolean
    get() = status != AccountStatus.Blocked

@UseCaseBinding
fun bindAssetAccount(decoded: Any): AssetAccount {
    val dynamicInstance = decoded.cast<Struct.Instance>()

    val status = when {
        // old version of assets pallet - isFrozen flag
        "isFrozen" in dynamicInstance.mapping -> {
            val isFrozen = bindBoolean(dynamicInstance["isFrozen"])
            if (isFrozen) AccountStatus.Frozen else AccountStatus.Liquid
        }

        // new version of assets pallet - status enum
        "status" in dynamicInstance.mapping -> {
            bindCollectionEnum(dynamicInstance["status"])
        }

        else -> incompatible()
    }

    return AssetAccount(
        balance = bindNumber(dynamicInstance["balance"]),
        status = status,
    )
}
