package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.issuing

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.StatemineAssetsRepository
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.calls.composeDispatchAs
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.OriginCaller
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.prepareIdForEncoding
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.instances.AddressInstanceConstructor

class StatemineAssetIssuer(
    private val chainId: ChainId,
    private val assetType: Chain.Asset.Type.Statemine,
    private val runtimeSnapshot: RuntimeSnapshot,
    private val statemineAssetsRepository: StatemineAssetsRepository,
) : AssetIssuer {

    override suspend fun composeIssueCall(amount: Balance, destination: AccountIdKey): GenericCall.Instance {
        val assetDetails = statemineAssetsRepository.getAssetDetails(chainId, assetType)
        val issuer = assetDetails.issuer

        // We're dispatching as issuer since only issuer is allowed to mint tokens
        return runtimeSnapshot.composeDispatchAs(
            call = composeMint(amount, destination),
            origin = OriginCaller.System.Signed(issuer)
        )
    }

    private fun composeMint(amount: Balance, destination: AccountIdKey): GenericCall.Instance {
        return runtimeSnapshot.composeCall(
            moduleName = assetType.palletNameOrDefault(),
            callName = "mint",
            args = mapOf(
                "id" to assetType.prepareIdForEncoding(runtimeSnapshot),
                "beneficiary" to AddressInstanceConstructor.constructInstance(runtimeSnapshot.typeRegistry, destination.value),
                "amount" to amount
            )
        )
    }
}
