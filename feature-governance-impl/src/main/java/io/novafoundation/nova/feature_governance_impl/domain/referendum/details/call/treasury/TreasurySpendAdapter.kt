package io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.treasury

import io.novafoundation.nova.common.data.network.runtime.binding.bindNonce
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallAdapter
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallParseContext
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.accountId
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.LocatableMultiAsset
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.bindVersionedLocatableMultiAsset
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.bindVersionedMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.chain.ChainMultiLocationConverterFactory
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class TreasurySpendAdapter(
    private val chainLocationConverterFactory: ChainMultiLocationConverterFactory,
    private val assetLocationConverterFactory: MultiLocationConverterFactory
) : ReferendumCallAdapter {

    override suspend fun fromCall(
        call: GenericCall.Instance,
        context: ReferendumCallParseContext
    ): ReferendumCall? {
        if (!call.instanceOf(Modules.TREASURY, "spend")) return null

        val amount = bindNonce(call.arguments["amount"])
        val beneficiaryLocation = bindVersionedMultiLocation(call.arguments["beneficiary"])
        val asset = bindVersionedLocatableMultiAsset(call.arguments["asset_kind"])

        return ReferendumCall.TreasuryRequest(
            amount = amount,
            beneficiary = beneficiaryLocation.accountId() ?: return null,
            chainAsset = resolveChainAsset(asset, context.chain) ?: return null
        )
    }

    private suspend fun resolveChainAsset(locatableMultiAsset: LocatableMultiAsset, chain: Chain): Chain.Asset? {
        val chainLocationConverter = chainLocationConverterFactory.resolveSelfAndChildrenParachains(chain)
        val resolvedChain = chainLocationConverter.toChain(locatableMultiAsset.location) ?: return null

        val assetLocationConverter = assetLocationConverterFactory.resolveLocalAssets(resolvedChain)
        return assetLocationConverter.toChainAsset(locatableMultiAsset.assetId.multiLocation)
    }
}
