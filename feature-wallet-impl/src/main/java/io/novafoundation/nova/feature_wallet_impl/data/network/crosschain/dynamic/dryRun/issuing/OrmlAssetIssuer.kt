package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.issuing

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.dryRun.AssetIssuer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.currencyId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.instances.AddressInstanceConstructor

class OrmlAssetIssuer(
    private val ormlType: Chain.Asset.Type.Orml,
    private val runtimeSnapshot: RuntimeSnapshot
) : AssetIssuer {

    override suspend fun composeIssueCall(amount: Balance, destination: AccountIdKey): GenericCall.Instance {
        return runtimeSnapshot.composeCall(
            moduleName = Modules.TOKENS,
            callName = "set_balance",
            args = mapOf(
                "who" to AddressInstanceConstructor.constructInstance(runtimeSnapshot.typeRegistry, destination.value),
                "currency_id" to ormlType.currencyId(runtimeSnapshot),
                "new_free" to amount,
                "new_reserved" to Balance.ZERO
            )
        )
    }
}
