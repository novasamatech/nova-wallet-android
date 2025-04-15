package io.novafoundation.nova.feature_account_api.data.fee.types

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder

class NativeFeePayment : FeePayment {

    override suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder) {
        // no modifications needed
    }

    override suspend fun convertNativeFee(nativeFee: Fee): Fee {
        return nativeFee
    }

    override suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean {
        return false
    }
}
