package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.astar

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_impl.data.network.blockhain.extrinsic.addMemo
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.isValidAddress
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder

class AstarContributeInteractor(
    private val selectedAssetSharedState: SingleAssetSharedState,
) {

    suspend fun isReferralCodeValid(code: String): Boolean {
        val currentChain = selectedAssetSharedState.chain()

        return currentChain.isValidAddress(code)
    }

    suspend fun submitOnChain(
        paraId: ParaId,
        referralCode: String,
        extrinsicBuilder: ExtrinsicBuilder,
    ) {
        val currentChain = selectedAssetSharedState.chain()
        val referralAccountId = currentChain.accountIdOf(referralCode)

        extrinsicBuilder.addMemo(paraId, referralAccountId)
    }
}
