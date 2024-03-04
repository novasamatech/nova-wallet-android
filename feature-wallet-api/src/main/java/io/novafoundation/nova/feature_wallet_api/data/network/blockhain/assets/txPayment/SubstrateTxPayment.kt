package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.txPayment

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

interface SubstrateTxPayment {

    suspend fun ExtrinsicBuilder.setFeeAsset(asset: Chain.Asset)
}
